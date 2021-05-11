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

package com.google.caliper.runner.target;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.runner.config.CaliperConfig;
import com.google.caliper.runner.config.DeviceConfig;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.config.VmType;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.runner.options.ParsedOptions;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/** {@link Device} for the local machine. */
@Singleton
public final class LocalDevice extends Device {

  /**
   * Returns whether or not we're currently running on an Android device.
   *
   * <p>This is temporary until we stop supporting running Caliper directly on Android devices.
   */
  private static boolean isAndroidDevice() {
    return System.getProperty("java.vendor").toLowerCase().contains("android");
  }

  private static final Joiner ARG_JOINER = Joiner.on(' ');

  private final CaliperConfig caliperConfig; // for legacy options
  private final boolean redirectErrorStream;
  private final Helper helper;

  @Inject
  LocalDevice(
      DeviceConfig config,
      CaliperConfig caliperConfig,
      CaliperOptions caliperOptions,
      ShutdownHookRegistrar shutdownHookRegistrar) {
    this(config, caliperConfig, caliperOptions, shutdownHookRegistrar, false);
  }

  private LocalDevice(
      DeviceConfig config,
      CaliperConfig caliperConfig,
      CaliperOptions caliperOptions,
      ShutdownHookRegistrar shutdownHookRegistrar,
      boolean redirectErrorStream) {
    super(config, shutdownHookRegistrar);
    this.caliperConfig = caliperConfig;
    this.redirectErrorStream = redirectErrorStream;
    this.helper =
        isAndroidDevice()
            ? new AndroidDeviceHelper(caliperOptions)
            : new NonAndroidDeviceHelper(caliperOptions);
  }

  @Override
  protected void startUp() {
    helper.setUp();
  }

  @Override
  protected void shutDown() {}

  @Override
  public VmType defaultVmType() {
    Optional<String> type = config().option("defaultVmType");
    return type.isPresent() ? VmType.of(type.get()) : helper.defaultVmType();
  }

  @Override
  public VmConfig defaultVmConfig() {
    VmConfig.Builder builder = VmConfig.builder().name("default").type(helper.defaultVmType());
    helper.configureDefaultVm(builder);
    return builder.addAllArgs(caliperConfig.getVmArgs()).build();
  }

  private static final ImmutableList<String> EXECUTABLE_DIRS = ImmutableList.of("bin/", "");
  private static final ImmutableList<String> EXECUTABLE_EXTENSIONS = ImmutableList.of("", ".exe");

  @Override
  public String vmExecutablePath(Vm vm) {
    File homeDir = vmHomeDir(vm);
    for (String extension : EXECUTABLE_EXTENSIONS) {
      for (String dir : EXECUTABLE_DIRS) {
        File file = new File(homeDir, dir + vm.executable() + extension);
        if (file.isFile()) {
          return file.getAbsolutePath();
        }
      }
    }
    throw new VirtualMachineException(
        String.format(
            "VM executable %s for VM %s not found under home dir %s",
            vm.executable(), vm, homeDir));
  }

  @Override
  public String workerClasspath(VmType type) {
    return helper.getWorkerClasspath(type);
  }

  @Override
  public Optional<String> workerNativeLibraryDir(VmType type) {
    return Optional.absent();
  }

  private File vmHomeDir(Vm vm) {
    if (!vm.home().isPresent()) {
      File homeDir = helper.getHomeDir(vm, vmBaseDirectory(vm));
      checkConfiguration(homeDir.isDirectory(), "%s is not a directory", homeDir);
      return homeDir;
    }

    String homeDirPath = vm.home().get();
    File potentialHomeDir = new File(homeDirPath);
    if (potentialHomeDir.isAbsolute()) {
      checkConfiguration(potentialHomeDir.isDirectory(), "%s is not a directory", potentialHomeDir);
      return potentialHomeDir;
    }

    File homeDir = new File(vmBaseDirectory(vm), homeDirPath);
    checkConfiguration(homeDir.isDirectory(), "%s is not a directory", potentialHomeDir);
    return homeDir;
  }

  private volatile Optional<File> vmBaseDirectory = null;

  private File vmBaseDirectory(Vm vm) {
    if (vmBaseDirectory == null) {
      vmBaseDirectory = getVmBaseDirectory();
    }
    if (!vmBaseDirectory.isPresent()) {
      throw new VirtualMachineException(
          "must set either a home directory or a base directory: config = " + vm.config());
    }
    return vmBaseDirectory.get();
  }

  private Optional<File> getVmBaseDirectory() {
    Optional<String> baseDirectoryPath =
        config()
            .option("vmBaseDirectory")
            .or(Optional.fromNullable(caliperConfig.properties().get("vm.baseDirectory")));
    if (!baseDirectoryPath.isPresent()) {
      return Optional.absent();
    }
    File result = new File(baseDirectoryPath.get());
    checkConfiguration(result.isAbsolute(), "VM base directory cannot be a relative path");
    checkConfiguration(result.isDirectory(), "VM base directory must be a directory");
    return Optional.of(result);
  }

  private static void checkConfiguration(boolean check, String message) {
    if (!check) {
      throw new VirtualMachineException(message);
    }
  }

  private static void checkConfiguration(boolean check, String messageFormat, Object... args) {
    if (!check) {
      throw new VirtualMachineException(String.format(messageFormat, args));
    }
  }

  @Override
  public VmProcess doStartVm(VmProcess.Spec spec, VmProcess.Logger logger) throws Exception {
    ProcessBuilder builder = new ProcessBuilder().redirectErrorStream(redirectErrorStream);
    helper.addToWorkerProcessEnvironment(builder.environment());

    ImmutableList<String> command = createCommand(spec);
    logger.log("Command: " + ARG_JOINER.join(command) + "\n");
    builder.command(command);

    return new LocalProcess(builder.start());
  }

  @VisibleForTesting
  ImmutableList<String> createCommand(VmProcess.Spec spec) {
    return new ImmutableList.Builder<String>()
        .add(spec.target().vmExecutablePath())
        .addAll(spec.vmOptions())
        .add(spec.mainClass())
        .addAll(spec.mainArgs())
        .build();
  }

  /** Helper to be implemented for each type of device the Caliper runner itself may be run on. */
  interface Helper {
    /** Do anything that may be needed when starting up the device service. */
    void setUp();

    /** Returns the default VM type for this device. */
    VmType defaultVmType();

    /**
     * Sets some configuration options for the default VM config for the device on the given
     * builder.
     */
    void configureDefaultVm(VmConfig.Builder builder);

    /** Gets the home directory to use for the given VM. */
    File getHomeDir(Vm vm, File baseDirectory);

    /** Gets the classpath to use for a worker of the given type. */
    String getWorkerClasspath(VmType type);

    /** May add mappings to the environment for worker processes. */
    void addToWorkerProcessEnvironment(Map<String, String> env);
  }

  /** A worker process running on the local machine. */
  private static final class LocalProcess extends VmProcess {

    private final Process process;

    LocalProcess(Process process) {
      this.process = process;
    }

    @Override
    public InputStream stdout() {
      return process.getInputStream();
    }

    @Override
    public InputStream stderr() {
      return process.getErrorStream();
    }

    @Override
    public int doAwaitExit() throws InterruptedException {
      return process.waitFor();
    }

    @Override
    public void doKill() {
      process.destroy();
    }
  }

  /** Creates a new {@link LocalDevice} builder. */
  @VisibleForTesting
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for {@link LocalDevice} instances. */
  @VisibleForTesting
  public static final class Builder {
    private DeviceConfig deviceConfig;
    private CaliperConfig caliperConfig;
    private CaliperOptions caliperOptions;
    private ShutdownHookRegistrar shutdownHookRegistrar;
    private boolean redirectErrorStream = false;

    /** Sets the {@link DeviceConfig} to use. */
    public Builder deviceConfig(DeviceConfig deviceConfig) {
      this.deviceConfig = checkNotNull(deviceConfig);
      return this;
    }

    /** Sets the {@link CaliperConfig} to use. */
    public Builder caliperConfig(CaliperConfig caliperConfig) {
      this.caliperConfig = checkNotNull(caliperConfig);
      return this;
    }

    /** Sets the {@link CaliperOptions} to use. */
    public Builder caliperOptions(CaliperOptions caliperOptions) {
      this.caliperOptions = checkNotNull(caliperOptions);
      return this;
    }

    /** Sets the {@link ShutdownHookRegistrar} to use. */
    public Builder shutdownHookRegistrar(ShutdownHookRegistrar shutdownHookRegistrar) {
      this.shutdownHookRegistrar = checkNotNull(shutdownHookRegistrar);
      return this;
    }

    /** Sets the config for the device. */
    public Builder redirectErrorStream(boolean redirectErrorStream) {
      this.redirectErrorStream = redirectErrorStream;
      return this;
    }

    /** Creates a new {@link LocalDevice}. */
    public LocalDevice build() {
      if (caliperConfig == null) {
        caliperConfig = new CaliperConfig(ImmutableMap.of("device.local.type", "local"));
      }
      if (deviceConfig == null) {
        deviceConfig = caliperConfig.getDeviceConfig("local");
      }
      if (caliperOptions == null) {
        caliperOptions = ParsedOptions.from(new String[] {}, false);
      }
      if (shutdownHookRegistrar == null) {
        shutdownHookRegistrar = new RuntimeShutdownHookRegistrar();
      }
      return new LocalDevice(
          deviceConfig, caliperConfig, caliperOptions, shutdownHookRegistrar, redirectErrorStream);
    }
  }
}
