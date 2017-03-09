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
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.io.BaseEncoding;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link Activity} that runs Caliper on Android.
 *
 * @author Colin Decker
 */
public final class CaliperActivity extends Activity {

  private static final String TAG = "Caliper";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setTitle("Caliper");
    setPerformanceOptions();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.submit(
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
   * <p>Set up mostly involves pulling the args for running Caliper from various places: the name
   * of the benchmark class from the app's manifest, the command line flags from the intent's extras
   * and the classpath to use for workers from the application info.
   */
  private void runAndExit() {
    /*
     * Caliper prints messages intended for the user to stdout and stderr by default, but it allows
     * replacing them with other streams. Here, we replace them with streams that use Android's
     * logging facilities, logging the lines to the Caliper-stdout and Caliper-stderr tags
     * respectively. The wrapper script running on the user's computer watches the device's log for
     * messages with these tags so it can output the same things to the user that would be output
     * if the benchmarks were running locally on their machine.
     */
    PrintWriter stdout = new PrintWriter(new LoggingWriter(Log.INFO, TAG));
    PrintWriter stderr = new PrintWriter(new LoggingWriter(Log.ERROR, TAG));

    Bundle extras = getIntent().getExtras();
    // Identifier provided by the script to help it find this run in the log.
    String runId = extras.getString("com.google.caliper.run_id");

    // Print the message the script uses to find the start of this run in the log.
    stdout.println("[START]: " + runId);

    int code = 1;
    try {
      File filesDir = getFilesDir();
      File resultsDir = new File(filesDir, "results");
      resultsDir.mkdirs();
      File outputFile = new File(resultsDir, runId + ".json");

      String benchmarkClass = getBenchmarkClass();

      String[] args = getArgs(extras, benchmarkClass, filesDir, outputFile);
      String classpath = getClasspath();

      // Print the message the script will use to find the results file after the run completes.
      stdout.println("[RESULTS FILE]: " + outputFile);

      // exitlessMain catches and handles all exceptions, so this will not throw
      CaliperMain main = new CaliperMain(classpath);
      code = main.exitlessMain(args, stdout, stderr);
    } catch (Throwable e) {
      Log.e(TAG, "An error occurred getting the arguments for starting Caliper", e);
      code = 1;
    } finally {
      // Print the message the script will use to find the end of the run in the log.
      stdout.println("[FINISH]: " + runId);
    }
    System.exit(code);
  }

  /**
   * Gets the fully qualified name of the benchmark class, which is specified in the manifest.
   */
  private String getBenchmarkClass() {
    try {
      Bundle metadata = getPackageManager()
          .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA)
          .metaData;
      if (metadata == null || !metadata.containsKey("benchmark_class")) {
        throw new RuntimeException("Unable to get benchmark class");
      }

      return metadata.getString("benchmark_class");
    } catch (NameNotFoundException e) {
      throw new RuntimeException("Unable to get benchmark class", e);
    }
  }

  /**
   * Gets the args array to pass to the {@code CaliperMain} class. Most of the args come from the
   * user running the script; in the script, those args are joined into a string separated with a
   * separator that's unlikely to appear in the args themselves ("|^|") and then base64 encoded so
   * they can be passed as a single extra argument (com.google.caliper.benchmark_args) in the adb
   * command that starts this activity.
   *
   * <p>The other args are:
   *
   * <ul>
   *   <li>The benchmark class name, which we get from the manifest.
   *   <li>The directory that Caliper should save files to.
   *   <li>The path at which Caliper should save the results file.
   * </ul>
   */
  private static String[] getArgs(
      Bundle extras, String benchmarkClass, File filesDir, File outputFile) {
    List<String> args = new ArrayList<>();

    // explicitly set the directory to save files to since otherwise it will end up being
    // /.caliper, which isn't even accessible
    // TODO(cgdecker): Can this be set through the default config for the platform?
    args.add("--directory");
    args.add(filesDir.toString());

    // Set the file to output to
    args.add("-Cresults.file.options.file=" + outputFile);

    String base64Args = Strings.nullToEmpty(extras.getString("com.google.caliper.benchmark_args"));
    if (!"".equals(base64Args)) {
      String argsString = new String(BaseEncoding.base64().decode(base64Args), Charsets.UTF_8);
      Iterables.addAll(args, Splitter.on("|^|").split(argsString));
    }

    args.add(benchmarkClass);

    return args.toArray(new String[0]);
  }

  /**
   * Gets the classpath that Caliper should use when launching worker processes. In this case, the
   * classpath should just be the location of the apk containing this app, since it should contain
   * all the classes the worker needs.
   */
  private String getClasspath() throws IOException {
    return getApplicationInfo().sourceDir;
  }
}
