/*
 * Copyright (C) 2018 Google Inc.
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

package com.google.caliper.runner.target;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.runner.config.InvalidConfigurationException;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.config.VmType;
import com.google.caliper.runner.options.CaliperOptions;
import java.io.File;
import java.util.Map;
import javax.annotation.Nullable;

/** Helper for when the local device is an Android device. */
final class AndroidDeviceHelper implements LocalDevice.Helper {

  private final CaliperOptions options;

  @Nullable private volatile String androidDataDir = null;

  AndroidDeviceHelper(CaliperOptions options) {
    this.options = checkNotNull(options);
  }

  @Override
  public void setUp() {
    // The worker processes won't be able to write to the default location DexOpt wants to write
    // optimized dexes to (/data/dalvik-cache), which will cause DexOpt (and the workers) to fail.
    // To fix this, change the ANDROID_DATA env variable for the workers from /data to a location
    // that's writable by the process.
    // Note: the tmpdir for an app is specific to that app and not shared.
    // Also create the dalvik-cache directory, since DexOpt will expect it to already exist.
    androidDataDir = System.getProperty("java.io.tmpdir") + "/data";
    File dalvikCache = new File(androidDataDir + "/dalvik-cache");
    dalvikCache.mkdirs();
  }

  @Override
  public VmType defaultVmType() {
    return VmType.ANDROID;
  }

  @Override
  public void configureDefaultVm(VmConfig.Builder builder) {
    String home = System.getenv("ANDROID_ROOT");
    if (home == null) {
      home = System.getProperty("java.home");
    }
    builder.home(home).executable("dalvikvm");
  }

  @Override
  public File getHomeDir(Vm vm, File baseDirectory) {
    return baseDirectory;
  }

  @Override
  public String getWorkerClasspath(VmType type) {
    if (type.equals(VmType.JVM)) {
      throw new InvalidConfigurationException("can't run a JVM on Android");
    }
    // Guaranteed to be present since we explicitly set this in CaliperActivity
    return options.workerClasspath(type.toString()).get();
  }

  @Override
  public void addToWorkerProcessEnvironment(Map<String, String> env) {
    env.put("ANDROID_DATA", androidDataDir);
  }
}
