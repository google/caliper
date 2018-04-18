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

package com.google.caliper.worker;

import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
import static android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * {@link Activity} that acts as a proxy the Caliper runner can use to manage worker processes.
 *
 * @author Colin Decker
 */
public final class CaliperProxyActivity extends Activity {

  private static final String TAG = "CaliperProxy";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setTitle("Caliper Proxy");

    // Prevent the app's orientation from changing, which would destroy this Activity and create a
    // new one.
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
    setPerformanceOptions();

    UUID id = getProxyId();
    try {
      startProxy(id);
    } catch (Throwable e) {
      logErrorAndExit(id, "failed to start proxy service", e);
    }
  }

  private void logErrorAndExit(UUID id, String message, Throwable e) {
    Log.e(TAG, "<" + id + "> " + message, e);
    System.exit(1);
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
   * Sets up and starts the Caliper proxy service, adding a listener to it that will exit this
   * process when it exits.
   */
  private void startProxy(final UUID id) throws IOException {
    // TODO(dpb): Maybe some of this could go in a Dagger module (but we probably don't want
    // to be creating files as a side effect in @Provides methods)
    InetSocketAddress runnerAddress =
        new InetSocketAddress(InetAddress.getLocalHost(), getRunnerPort());
    String classpath = AndroidClasspath.getClasspath(getApplicationInfo());

    String androidDataDir = System.getProperty("java.io.tmpdir") + "/data";
    createWritableDalvikCache(androidDataDir);
    ImmutableMap<String, String> processEnv = ImmutableMap.of("ANDROID_DATA", androidDataDir);

    CaliperProxy proxy =
        DaggerCaliperProxyComponent.builder()
            .id(id)
            .clientAddress(runnerAddress)
            .classpath(classpath)
            .processEnv(processEnv)
            .build()
            .caliperProxy();

    proxy.addListener(
        new Listener() {
          @Override
          public void failed(State from, Throwable e) {
            logErrorAndExit(id, "proxy service failed", e);
          }

          @Override
          public void terminated(State from) {
            System.exit(0);
          }
        },
        MoreExecutors.directExecutor());

    proxy.startAsync();
  }

  private UUID getProxyId() {
    String id = getIntent().getExtras().getString("com.google.caliper.proxy_id");
    return UUID.fromString(id);
  }

  private int getRunnerPort() {
    String port = getIntent().getExtras().getString("com.google.caliper.runner_port");
    return Integer.parseInt(port);
  }

  private void createWritableDalvikCache(String androidDataDir) {
    // The worker processes won't be able to write to the default location DexOpt wants to write
    // optimized dexes to (/data/dalvik-cache), which will cause DexOpt (and the workers) to fail.
    // To fix this, change the ANDROID_DATA env variable for the workers from /data to a location
    // that's writable by the process.
    // Note: the tmpdir for an app is specific to that app and not shared.
    // Also create the dalvik-cache directory, since DexOpt will expect it to already exist.
    File dalvikCache = new File(androidDataDir + "/dalvik-cache");
    dalvikCache.mkdirs();
  }
}
