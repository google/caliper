// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.caliper.config;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.util.Util;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Represents caliper Config.  By default, {@code ~/.caliper/config} and
 * {@code global.caliperrc}.
 *
 * @author gak@google.com (Gregory Kick)
 */
public final class CaliperConfig {
  private final ImmutableMap<String, String> properties;

  CaliperConfig(ImmutableMap<String, String> properties) {
    this.properties = checkNotNull(properties);
  }

  public NewVmConfig getVmConfig(String name) throws InvalidConfigurationException {
    checkNotNull(name);
    ImmutableMap<String, String> vmGroupMap = subgroupMap(properties, "vm");
    ImmutableMap<String, String> vmMap = subgroupMap(vmGroupMap, name);
    File homeDir = getJdkHomeDir(vmGroupMap.get("baseDirectory"), vmMap.get("home"), name);
    return new NewVmConfig.Builder(homeDir)
        .addAllOptions(getArgs(vmGroupMap))
        .addAllOptions(getArgs(vmMap))
        .build();
  }

  public NewInstrumentConfig getInstrumentConfig(String name) {
    checkNotNull(name);
    ImmutableMap<String, String> instrumentGroupMap = subgroupMap(properties, "instrument");
    ImmutableMap<String, String> insrumentMap = subgroupMap(instrumentGroupMap, name);
    return new NewInstrumentConfig.Builder()
        .className(insrumentMap.get("class"))
        .addAllOptions(subgroupMap(insrumentMap, "options"))
        .build();
  }

  public NewResultProcessorConfig getResultProcessorConfig(String name) {
    checkNotNull(name);
    ImmutableMap<String, String> resultsGroupMap = subgroupMap(properties, "results");
    ImmutableMap<String, String> resultsMap = subgroupMap(resultsGroupMap, name);
    return new NewResultProcessorConfig.Builder()
        .className(resultsMap.get("class"))
        .addAllOptions(subgroupMap(resultsMap, "options"))
        .build();
  }

  private static final ImmutableMap<String, String> subgroupMap(ImmutableMap<String, String> map,
      String groupName) {
    return Util.prefixedSubmap(map, groupName + ".");
  }

  private static final Splitter ARGS_SPLITTER = Splitter.on(' ').omitEmptyStrings();

  private static List<String> getArgs(Map<String, String> properties) {
    return ImmutableList.copyOf(ARGS_SPLITTER.split(Strings.nullToEmpty(properties.get("args"))));
  }

  // TODO(gak): check that the directory seems to be a jdk home (with a java binary and all of that)
  // TODO(gak): make this work with different directory layouts.  I'm looking at you OS X...
  private static File getJdkHomeDir(@Nullable String baseDirectoryPath,
      @Nullable String homeDirPath, String vmConfigName)
          throws InvalidConfigurationException {
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
      throws InvalidConfigurationException {
    if (baseDirectoryPath == null) {
      throw new InvalidConfigurationException(
          "must set either a home directory or a base directory");
    } else {
      File baseDirectory = new File(baseDirectoryPath);
      checkConfiguration(baseDirectory.isAbsolute(), "base directory cannot be a relative path");
      checkConfiguration(baseDirectory.isDirectory(), "base directory must be a directory");
      return baseDirectory;
    }
  }

  private static void checkConfiguration(boolean check, String message)
      throws InvalidConfigurationException {
    if (!check) {
      throw new InvalidConfigurationException(message);
    }
  }

  private static void checkConfiguration(boolean check, String messageFormat, Object... args)
      throws InvalidConfigurationException {
    if (!check) {
      throw new InvalidConfigurationException(String.format(messageFormat, args));
    }
  }

  public CaliperRc asCaliperRc() {
    return new CaliperRc(properties);
  }
}
