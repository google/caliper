/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.caliper.runner;

import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
import static android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import com.google.caliper.runner.options.OptionsModule;
import com.google.caliper.runner.platform.DalvikPlatform;
import com.google.caliper.runner.platform.PlatformModule;
import com.google.caliper.util.OutputModule;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * {@link Activity} that runs Caliper on Android.
 *
 * @author Colin Decker
 */
public final class CaliperActivity extends Activity {

  private static final String TAG = "Caliper";

  private PrintWriter stdout;
  private PrintWriter stderr;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setTitle("Caliper");
    setPerformanceOptions();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(
        new Runnable() {
          @Override
          public void run() {
            runAndExit();
          }
        });
  }

  /**
   * Sets some options that attempt to make the performance consistent for the whole run.
   *
   * <p>As a baseline, turn the screen on and keep it on for the duration of the run in hopes that
   * will keep the device from going into some low power mode.
   *
   * <p>On Android N and above, also call {@code setSustainedPerformanceMode}, which is intended to
   * keep things like CPU clock consistent for the run. It only works for specific devices that
   * implement it, though.
   */
  private void setPerformanceOptions() {
    // This is just some basic stuff and I'm under no illusion that it is all that's needed to
    // control for consistent performance.
    Window window = getWindow();
    window.addFlags(FLAG_SHOW_WHEN_LOCKED | FLAG_TURN_SCREEN_ON | FLAG_KEEP_SCREEN_ON);
    if (VERSION.SDK_INT >= VERSION_CODES.N) {
      window.setSustainedPerformanceMode(true);
    }
  }

  /**
   * Sets up and runs Caliper, then exits the process.
   *
   * <p>Set up mostly involves pulling the args for running Caliper from various places: the name of
   * the benchmark class from the app's manifest, the command line flags from the intent's extras
   * and the classpath to use for workers from the application info.
   */
  private void runAndExit() {
    Bundle extras = getIntent().getExtras();
    // Identifier provided by the script to help it find this run in the log.
    String runId = extras.getString("com.google.caliper.run_id");

    /*
     * Caliper prints messages intended for the user to stdout and stderr by default, but it allows
     * replacing them with other streams. Here, we replace them with streams that use Android's
     * logging facilities, logging the lines to the Caliper-stdout and Caliper-stderr tags
     * respectively. The wrapper script running on the user's computer watches the device's log for
     * messages with these tags so it can output the same things to the user that would be output
     * if the benchmarks were running locally on their machine.
     */
    this.stdout = new PrintWriter(new LoggingWriter(Log.INFO, TAG, runId));
    this.stderr = new PrintWriter(new LoggingWriter(Log.ERROR, TAG, runId));

    int code = 1;
    try {
      File filesDir = getFilesDir();
      File outputFile = new File(filesDir, runId + ".json");
      Files.touch(outputFile);

      CaliperRunner runner = createCaliperRunner(getArgs(extras, filesDir, outputFile));

      // run() catches and handles all exceptions, so this will not throw
      code = runner.run();

      if (code == 0) {
        stdout.println("[RESULTS FILE] " + outputFile);
      }
    } catch (Throwable e) {
      stderr.println("An error occurred getting the arguments for starting Caliper");
      e.printStackTrace(stderr);
      code = 1;
    } finally {
      // Print the message the script will use to find the end of the run in the log.
      stdout.println("[FINISH]");
    }
    System.exit(code);
  }

  private CaliperRunner createCaliperRunner(String[] args) throws IOException {
    return DaggerAndroidCaliperRunnerComponent.builder()
        .optionsModule(OptionsModule.withBenchmarkClass(args))
        .platformModule(new PlatformModule(new DalvikPlatform(getClasspath())))
        .outputModule(new OutputModule(stdout, stderr))
        .build()
        .getRunner();
  }

  /**
   * Gets the args array to pass to the {@code CaliperMain} class. Most of the args come from the
   * user running the script; in the script, those args are joined into a string separated with a
   * separator that's unlikely to appear in the args themselves ("|^|") and then base64 encoded so
   * they can be passed as a single extra argument (com.google.caliper.benchmark_args) in the adb
   * command that starts this activity. The benchmark class is included in these args.
   *
   * <p>The other args are:
   *
   * <ul>
   *   <li>The directory that Caliper should save files to.
   *   <li>The path at which Caliper should save the results file.
   * </ul>
   */
  private static String[] getArgs(Bundle extras, File filesDir, File outputFile) {
    List<String> args = new ArrayList<>();

    // explicitly set the directory to save files to since otherwise it will end up being
    // /.caliper, which isn't even accessible
    // TODO(cgdecker): Can this be set through the default config for the platform?
    args.add("--directory");
    args.add(filesDir.toString());

    // Set the file to output to
    args.add("-Cresults.file.options.file=" + outputFile);

    File logsDir = new File(filesDir, "logs");
    logsDir.mkdirs();

    // Also set the worker log output directory, since the default (tmpdir) may not be writable
    args.add("-Cworker.output=" + logsDir);

    String base64Args = extras.getString("com.google.caliper.benchmark_args");
    String argsString = new String(BaseEncoding.base64().decode(base64Args), Charsets.UTF_8);
    Iterables.addAll(args, Splitter.on("|^|").split(argsString));

    return args.toArray(new String[0]);
  }

  /**
   * Gets the classpath that Caliper should use when launching worker processes.
   *
   * <p>By default (apk with a single dex or when multidex is natively supported), the classpath is
   * just the location of the apk containing this app. If the apk has multiple dexes and multidex
   * isn't natively supported, all dexes are extracted from the apk and the classpath contains the
   * path to each.
   */
  private String getClasspath() throws IOException {
    String apk = getApplicationInfo().sourceDir;

    File codeCache = new File(getApplicationInfo().dataDir, "code_cache");

    // If the secondary-dexes dir exists, that means that
    // A) the apk is multidex, and
    // B) there's no native multidex support on this Android version
    File secondaryDexDir = new File(codeCache, "secondary-dexes");
    if (!secondaryDexDir.exists()) {
      return apk;
    }

    // Note: we don't use any dexes extracted to the secondary-dexes dir because they may have been
    // modified (e.g. dexopt'ed) in place, which will cause DexOpt to fail when trying to start a
    // worker process.
    List<File> dexFiles = extractDexes(apk, new File(codeCache, "worker-dexes"));

    return Joiner.on(System.getProperty("path.separator")).join(dexFiles);
  }

  /**
   * Extracts {@code classes.dex} and all secondary {@code classes<N>.dex} files from the given apk
   * file to the given target directory.
   */
  private static List<File> extractDexes(String apk, File targetDir) throws IOException {
    targetDir.mkdirs();
    List<File> results = new ArrayList<>();
    Closer closer = Closer.create();
    try {
      ZipFile zip = closer.register(new CloseableZipFile(apk)).zipFile;

      ZipEntry classesEntry = zip.getEntry("classes.dex");
      if (classesEntry == null) {
        throw new AssertionError("classes.dex not found in apk");
      }

      int i = 1;
      do {
        InputStream in = closer.register(zip.getInputStream(classesEntry));

        File outFile = new File(targetDir, classesEntry.getName());
        OutputStream out = closer.register(new FileOutputStream(outFile));

        ByteStreams.copy(in, out);

        results.add(outFile);

        classesEntry = zip.getEntry("classes" + (++i) + ".dex");
      } while (classesEntry != null);
      return results;
    } catch (Throwable t) {
      throw closer.rethrow(t);
    } finally {
      closer.close();
    }
  }

  /**
   * Workaround for the fact that {@code ZipFile} wasn't {@code Closeable} in earlier Java and
   * Android versions, so that we can use it with {@link Closer}.
   */
  private static final class CloseableZipFile implements Closeable {
    final ZipFile zipFile;

    CloseableZipFile(String file) throws IOException {
      this.zipFile = new ZipFile(file);
    }

    @Override
    public void close() throws IOException {
      zipFile.close();
    }
  }
}
