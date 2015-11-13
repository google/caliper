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

package com.google.caliper.platform.jvm;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Thread.currentThread;

import com.google.caliper.platform.Platform;
import com.google.caliper.platform.VirtualMachineException;
import com.google.caliper.util.Util;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * An abstraction of a standard Java Virtual Machine platform.
 */
public final class JvmPlatform extends Platform {

  /**
   * Some default JVM args to keep worker VMs somewhat predictable.
   */
  @VisibleForTesting
  public static final ImmutableSet<String> INSTRUMENT_JVM_ARGS = ImmutableSet.of(
      // do compilation serially
      "-Xbatch",
      // make sure compilation doesn't run in parallel with itself
      "-XX:CICompilerCount=1",
      // ensure the parallel garbage collector
      "-XX:+UseParallelGC",
      // generate classes or don't, but do it immediately
      "-Dsun.reflect.inflationThreshold=0");

  private static final ImmutableSet<String> WORKER_PROCESS_ARGS = ImmutableSet.of(
      "-XX:+PrintFlagsFinal",
      "-XX:+PrintCompilation",
      "-XX:+PrintGC");


  private static final Predicate<String> PROPERTIES_TO_RETAIN = new Predicate<String>() {
    @Override public boolean apply(String input) {
      return input.startsWith("java.vm")
          || input.startsWith("java.runtime")
          || input.equals("java.version")
          || input.equals("java.vendor")
          || input.equals("sun.reflect.noInflation")
          || input.equals("sun.reflect.inflationThreshold");
    }
  };

  public JvmPlatform() {
    super(Type.JVM);
  }

  @Override
  public File vmExecutable(File javaHome) {
    // TODO(gak): support other platforms. This currently supports finding the java executable on
    // standard configurations of unix systems and windows.
    File bin = new File(javaHome, "bin");
    Preconditions.checkState(bin.exists() && bin.isDirectory(),
        "Could not find %s under java home %s", bin, javaHome);
    File jvm = new File(bin, "java");
    if (!jvm.exists() || jvm.isDirectory()) {
      jvm = new File(bin, "java.exe");
      if (!jvm.exists() || jvm.isDirectory()) {
        throw new IllegalStateException(
            String.format("Cannot find java binary in %s, looked for java and java.exe", bin));
      }
    }

    return jvm;
  }

  @Override
  public ImmutableSet<String> commonInstrumentVmArgs() {
    return INSTRUMENT_JVM_ARGS;
  }

  @Override
  public ImmutableSet<String> workerProcessArgs() {
    return WORKER_PROCESS_ARGS;
  }

  @Override
  public String workerClassPath() {
    return getClassPath();
  }

  private static String getClassPath() {
    // Use the effective class path in case this is being invoked in an isolated class loader
    String classpath =
        EffectiveClassPath.getClassPathForClassLoader(currentThread().getContextClassLoader());
    return classpath;
  }

  @Override
  public Collection<String> inputArguments() {
    return Collections2.filter(ManagementFactory.getRuntimeMXBean().getInputArguments(),
        new Predicate<String>() {
          @Override
          public boolean apply(String input) {
            // Exclude the -agentlib:jdwp param which configures the socket debugging protocol.
            // If this is set in the parent VM we do not want it to be inherited by the child
            // VM.  If it is, the child will die immediately on startup because it will fail to
            // bind to the debug port (because the parent VM is already bound to it).
            return !input.startsWith("-agentlib:jdwp");
          }
        });
  }

  @Override
  public Predicate<String> vmPropertiesToRetain() {
    return PROPERTIES_TO_RETAIN;
  }

  @Override
  public void checkVmProperties(Map<String, String> options) {
    checkState(!options.isEmpty());
  }

  @Override
  public File customVmHomeDir(Map<String, String> vmGroupMap, String vmConfigName)
          throws VirtualMachineException {
    // Configuration can either be:
    //   vm.<vmConfigName>.home = <homeDir>
    // or
    //   vm.baseDirectory = <baseDir>
    //   homeDir = <baseDir>/<vmConfigName>
    ImmutableMap<String, String> vmMap = Util.subgroupMap(vmGroupMap, vmConfigName);
    return getJdkHomeDir(vmGroupMap.get("baseDirectory"), vmMap.get("home"), vmConfigName);
  }

  // TODO(gak): check that the directory seems to be a jdk home (with a java binary and all of that)
  // TODO(gak): make this work with different directory layouts.  I'm looking at you OS X...
  public static File getJdkHomeDir(@Nullable String baseDirectoryPath,
          @Nullable String homeDirPath, String vmConfigName)
          throws VirtualMachineException {
    if (homeDirPath == null) {
      File baseDirectory = getBaseDirectory(baseDirectoryPath);
      File homeDir = new File(baseDirectory, vmConfigName);
      checkConfiguration(homeDir.isDirectory(), "%s is not a directory", homeDir);
      return homeDir;
    } else {
      File potentialHomeDir = new File(homeDirPath);
      if (potentialHomeDir.isAbsolute()) {
        checkConfiguration(potentialHomeDir.isDirectory(), "%s is not a directory",
                potentialHomeDir);
        return potentialHomeDir;
      } else {
        File baseDirectory = getBaseDirectory(baseDirectoryPath);
        File homeDir = new File(baseDirectory, homeDirPath);
        checkConfiguration(homeDir.isDirectory(), "%s is not a directory", potentialHomeDir);
        return homeDir;
      }
    }
  }

  private static File getBaseDirectory(@Nullable String baseDirectoryPath)
          throws VirtualMachineException {
    if (baseDirectoryPath == null) {
      throw new VirtualMachineException(
              "must set either a home directory or a base directory");
    } else {
      File baseDirectory = new File(baseDirectoryPath);
      checkConfiguration(baseDirectory.isAbsolute(), "base directory cannot be a relative path");
      checkConfiguration(baseDirectory.isDirectory(), "base directory must be a directory");
      return baseDirectory;
    }
  }

  private static void checkConfiguration(boolean check, String message)
          throws VirtualMachineException {
    if (!check) {
      throw new VirtualMachineException(message);
    }
  }

  private static void checkConfiguration(boolean check, String messageFormat, Object... args)
          throws VirtualMachineException {
    if (!check) {
      throw new VirtualMachineException(String.format(messageFormat, args));
    }
  }
}
