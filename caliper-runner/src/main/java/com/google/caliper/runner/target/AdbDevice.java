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

import static com.google.caliper.runner.config.VmType.ANDROID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.StandardSystemProperty.PATH_SEPARATOR;
import static com.google.common.base.Strings.nullToEmpty;

import com.google.caliper.bridge.StartVmRequest;
import com.google.caliper.runner.config.DeviceConfig;
import com.google.caliper.runner.config.InvalidConfigurationException;
import com.google.caliper.runner.config.VmConfig;
import com.google.caliper.runner.config.VmType;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.runner.server.ServerSocketService;
import com.google.caliper.runner.target.Shell.Result;
import com.google.caliper.runner.target.VmProcess.Logger;
import com.google.caliper.util.InvalidCommandException;
import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * An ADB-connected Android device or emulator.
 *
 * @author Colin Decker
 */
@Singleton
final class AdbDevice extends Device {

  /** L; required for adb reverse. */
  private static final int MIN_API_LEVEL = 21;

  private static final CharMatcher DIGITS = CharMatcher.inRange('0', '9');
  private static final Splitter CLASSPATH_SPLITTER = Splitter.on(PATH_SEPARATOR.value());

  private static final String CALIPER_PACKAGE_NAME = "com.google.caliper";

  private final CaliperOptions caliperOptions;

  private Shell shell;
  private final String adb = "adb"; // TODO(cgdecker): Make this configurable?

  private final ServerSocketService server;
  private final ProxyConnectionService proxyConnection;
  private String remoteClasspath;

  private int port;

  @Inject
  AdbDevice(
      DeviceConfig config,
      ShutdownHookRegistrar shutdownHookRegistrar,
      CaliperOptions caliperOptions,
      Shell shell,
      ServerSocketService server,
      ProxyConnectionService proxyConnection) {
    super(config, shutdownHookRegistrar);
    this.caliperOptions = caliperOptions;
    this.shell = shell;
    this.server = server;
    this.proxyConnection = proxyConnection;
  }

  @Override
  protected void startUp() throws Exception {
    shell.execute(adb, "start-server").orThrow();

    String selector = nullToEmpty(config().options().get("selector"));

    // selector string may consist of multiple arguments or be an empty string, so write this
    // command as a single string to let shell handle splitting it
    String deviceSerialNumber = shell.execute(adb + " " + selector + " get-serialno")
        .orThrow()
        .stdout();

    selectDevice(deviceSerialNumber);
    install(getWorkerApk());

    // This method waits for the server to be running. We need to get it here rather than injecting
    // the port since both the AdbDevice and the ServerSocketService need to be started up by the
    // same ServiceManager and we'd deadlock trying to get the port if we tried to inject it.
    this.port = server.getPort();
    setReversePortForwarding();

    startActivity(
        "com.google.caliper/.worker.CaliperProxyActivity",
        ImmutableMap.of(
            "com.google.caliper.runner_port", "" + port,
            "com.google.caliper.proxy_id", proxyConnection.proxyId().toString()));

    proxyConnection.startAsync().awaitRunning();

    this.remoteClasspath = proxyConnection.getRemoteClasspath();
  }

  private void selectDevice(String serialNumber) {
    shell = shell.withEnv(ImmutableMap.of("ANDROID_SERIAL", serialNumber));
    checkDeviceApiLevel();
  }

  private void checkDeviceApiLevel() {
    String out = shell.execute(adb + " shell getprop ro.build.version.sdk").orThrow().stdout();
    if (!DIGITS.matchesAllOf(out)) {
      throw new ShellException(
          "unexpected output from command 'adb shell getprop ro.build.version.sdk': " + out);
    }
    int apiLevel = Integer.parseInt(out);

    if (apiLevel < MIN_API_LEVEL) {
      throw new InvalidConfigurationException(
          String.format(
              "device API level %s is not supported; Caliper only supports API level %s+",
              apiLevel, MIN_API_LEVEL));
    }
  }

  /**
   * Sets up reverse port forwarding to allow the device to connect to the given port on this
   * computer.
   */
  private void setReversePortForwarding() {
    String tcpPort = "tcp:" + port;
    shell.execute(adb, "reverse", tcpPort, tcpPort).orThrow();
  }

  /** Removes the reverse port forwarding for the given port. */
  private void removeReversePortForwarding() {
    String tcpPort = "tcp:" + port;
    shell.execute(adb, "reverse", "--remove", tcpPort).orThrow();
  }

  /** Installs the given apk file to the device. */
  private void install(File apk) {
    shell.execute(adb, "install", "-r", apk.getAbsolutePath()).orThrow();
  }

  /** Uninstalls the package with the given name from the device. */
  private void uninstall(String packageName) {
    shell.execute(adb, "uninstall", packageName);
  }

  /**
   * Starts the activity with the given intent, adding the given extras to the parameters for the
   * activity.
   */
  private void startActivity(String intent, Map<String, String> extras) {
    ImmutableList.Builder<String> builder =
        ImmutableList.<String>builder()
            .add(adb)
            .add("shell")
            .add("am", "start")
            .add("-n", intent)
            .add("-a", "android.intent.action.MAIN")
            .add("-c", "android.intent.category.LAUNCHER");
    for (Map.Entry<String, String> entry : extras.entrySet()) {
      builder.add("-e", entry.getKey(), entry.getValue());
    }
    shell.execute(builder.build()).orThrow("failed to start activity");
  }

  private File getWorkerApk() {
    Optional<String> optionalClasspath = caliperOptions.workerClasspath(ANDROID.toString());
    if (!optionalClasspath.isPresent()) {
      throw new InvalidCommandException("No worker classpath for VM type %s provided", ANDROID);
    }
    List<String> classpath = CLASSPATH_SPLITTER.splitToList(optionalClasspath.get());

    if (classpath.size() != 1 || !classpath.get(0).endsWith(".apk")) {
      throw new InvalidCommandException(
          "Android worker classpath must consist of a single file with extension .apk");
    }

    File apk = new File(classpath.get(0));
    if (!apk.isFile()) {
      throw new InvalidCommandException(
          "Android worker apk '" + apk + "' does not exist or isn't a regular file");
    }

    return apk;
  }

  @Override
  protected void shutDown() throws Exception {
    try {
      proxyConnection.stopAsync();
      uninstall(CALIPER_PACKAGE_NAME);
    } finally {
      removeReversePortForwarding();
      proxyConnection.awaitTerminated();
    }
  }

  @Override
  public String vmExecutablePath(Vm vm) {
    checkState(isRunning(), "AdbDevice service must be running to get VM executable path");
    String executable = "/system/bin/" + vm.executable();
    if (!fileExists(executable)) {
      throw new VirtualMachineException(
          "VM executable " + executable + " doesn't exist on device " + this);
    }
    return executable;
  }

  @Override
  public String workerClasspath(VmType type) {
    checkArgument(type.equals(ANDROID), "type must be ANDROID, not %s", type);
    checkState(isRunning(), "AdbDevice service must be running to get worker classpath");
    return remoteClasspath;
  }

  @Override
  public VmType defaultVmType() {
    return ANDROID;
  }

  @Override
  public VmConfig defaultVmConfig() {
    return VmConfig.builder().name("default").type(ANDROID).executable("dalvikvm").build();
  }

  @Override
  protected VmProcess doStartVm(VmProcess.Spec spec, Logger logger) throws Exception {
    StartVmRequest request = StartVmRequest.create(spec.id(), createCommand(spec));
    return proxyConnection.startVm(request);
  }

  private boolean fileExists(String path) {
    Result result = shell.execute(adb, "shell", "[", "-f", path, "]");
    if (!result.isSuccessful() && !result.stderr().isEmpty()) {
      // only throw if there was output to stderr in addition to not being successful; a non-zero
      // exit code is expected if the command "succeeds" but the file didn't exist
      result.orThrow();
      throw new AssertionError(); // unreachable
    }
    return result.isSuccessful();
  }

  private static ImmutableList<String> createCommand(VmProcess.Spec spec) {
    return new ImmutableList.Builder<String>()
        .add(spec.target().vmExecutablePath())
        .addAll(spec.vmOptions())
        .add(spec.mainClass())
        .addAll(spec.mainArgs())
        .build();
  }
}
