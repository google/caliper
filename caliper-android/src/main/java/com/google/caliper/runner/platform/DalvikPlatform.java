/*
 * Copyright (C) 2015 Google Inc.
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

package com.google.caliper.runner.platform;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * An abstraction of the Dalvik (aka Android) platform.
 *
 * <p>Although this talks about dalvik it actually works with ART too.
 */
public final class DalvikPlatform extends Platform {

  /**
   * Set of VM executables that the user may specify with --vm=<vm>.
   *
   * <p>Unlike the JVM notion of the vm flag, where it's the name of a directory under some root
   * that is expected to contain {@code bin/java}, the vm flag for Android is expected to be one of
   * these executable names, which is then expected to be found under {@code $vmHome/bin/$vm}.
   */
  /*
   * This is likely a temporary solution to how we handle VMs for Android. At the point when we're
   * running the main Caliper process on the host machine and potentially running workers on both
   * JVMs and Android devices, we'll need to be able to distinguish between both the executable to
   * use and the device/target for the worker. In that case, we'll likely need a different set of
   * flags.
   */
  private static final ImmutableSet<String> VM_EXECUTABLES = ImmutableSet.of(
      "dalvikvm",
      "dalvikvm32",
      "dalvikvm64",
      "art",
      "app_process");

  private String vmExecutable = "dalvikvm";

  private final String classpath;

  public DalvikPlatform(String classpath) {
    super(Type.DALVIK);
    this.classpath = classpath;
  }

  @Override
  public File vmExecutable(File vmHome) {
    File bin = new File(vmHome, "bin");
    checkState(
        bin.exists() && bin.isDirectory(), "Could not find %s under android root %s", bin, vmHome);
    File executable = new File(bin, vmExecutable);
    if (!executable.exists() || executable.isDirectory()) {
      throw new IllegalStateException(
          String.format("Cannot find %s binary in %s", vmExecutable, bin));
    }

    return executable;
  }

  @Override
  public ImmutableSet<String> commonInstrumentVmArgs() {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<String> workerProcessArgs() {
    if (vmExecutable.equals("app_process")) {
      // app_process expects a command directory argument; use the bin directory where the binary
      // is
      return ImmutableSet.of(new File(defaultVmHomeDir(), "bin").toString());
    }
    return ImmutableSet.of();
  }

  @Override
  protected String workerClassPath() {
    return classpath;
  }

  @Override
  public ImmutableList<String> workerClassPathArgs() {
    // Unlike -cp <classpath>, this works for both dalvikvm and app_process executables
    return ImmutableList.of("-Djava.class.path=" + workerClassPath());
  }

  @Override
  public Collection<String> inputArguments() {
    return Collections.emptyList();
  }

  @Override
  public Predicate<String> vmPropertiesToRetain() {
    return Predicates.alwaysFalse();
  }

  @Override
  public void checkVmProperties(Map<String, String> options) {
    checkState(options.isEmpty());
  }

  @Override
  public void setWorkerEnvironment(Map<String, String> env) {
    // The worker processes won't be able to write to the default location DexOpt wants to write
    // optimized dexes to (/data/dalvik-cache), which will cause DexOpt (and the workers) to fail.
    // To fix this, change the ANDROID_DATA env variable for the workers from /data to a location
    // that's writeable by the process.
    // Note: the tmpdir for an app is specific to that app and not shared.
    String dataDir = System.getProperty("java.io.tmpdir") + "/data";
    env.put("ANDROID_DATA", dataDir);

    // Also create the dalvik-cache directory, since DexOpt will expect it to already exist.
    File dalvikCache = new File(dataDir + "/dalvik-cache");
    dalvikCache.mkdirs();
  }

  @Override
  public File defaultVmHomeDir() {
    String androidRoot = System.getenv("ANDROID_ROOT");
    return androidRoot != null ? new File(androidRoot) : super.defaultVmHomeDir();
  }

  @Override
  public File customVmHomeDir(Map<String, String> vmGroupMap, String vmConfigName)
      throws VirtualMachineException {
    // This is kind of a hack; the main thing we're doing here is not actually changing the home
    // dir under which we want to look for the VM binary, but setting the name of the VM binary
    // that we want to use (under the default home dir).
    if (!VM_EXECUTABLES.contains(vmConfigName)) {
      throw new VirtualMachineException(vmConfigName
          + " is not a supported VM for Android; supported values are: "
          + Joiner.on(", ").join(VM_EXECUTABLES));
    }

    this.vmExecutable = vmConfigName;
    return defaultVmHomeDir();
  }
}
