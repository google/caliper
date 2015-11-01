/*
 * Copyright (C) 2012 Google Inc.
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
 *
 * Original author gak@google.com (Gregory Kick)
 */

package dk.ilios.spanner.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.ilios.spanner.output.ResultProcessor;
import dk.ilios.spanner.util.Util;

/**
 * Represents the global Gauge configuration, which is shared across all tests.
 */
public final class SpannerConfiguration {
    @VisibleForTesting
    final ImmutableMap<String, String> properties;
    private final ImmutableMap<Class<? extends ResultProcessor>, ResultProcessorConfig>
            resultProcessorConfigs;

    @VisibleForTesting
    public SpannerConfiguration(ImmutableMap<String, String> properties)
            throws InvalidConfigurationException {
        this.properties = checkNotNull(properties);
        this.resultProcessorConfigs = findResultProcessorConfigs(subgroupMap(properties, "results"));
    }

    private static final Pattern CLASS_PROPERTY_PATTERN = Pattern.compile("(\\w+)\\.class");

    private static <T> ImmutableBiMap<String, Class<? extends T>> mapGroupNamesToClasses(
            ImmutableMap<String, String> groupProperties, Class<T> type)
            throws InvalidConfigurationException {
        BiMap<String, Class<? extends T>> namesToClasses = HashBiMap.create();
        for (Entry<String, String> entry : groupProperties.entrySet()) {
            Matcher matcher = CLASS_PROPERTY_PATTERN.matcher(entry.getKey());
            if (matcher.matches() && !entry.getValue().isEmpty()) {
                try {
                    Class<?> someClass = Class.forName(entry.getValue());
                    checkState(type.isAssignableFrom(someClass));
                    @SuppressWarnings("unchecked")
                    Class<? extends T> verifiedClass = (Class<? extends T>) someClass;
                    namesToClasses.put(matcher.group(1), verifiedClass);
                } catch (ClassNotFoundException e) {
                    throw new InvalidConfigurationException("Cannot find result processor class: "
                            + entry.getValue());
                }
            }
        }
        return ImmutableBiMap.copyOf(namesToClasses);
    }

    private static ImmutableMap<Class<? extends ResultProcessor>, ResultProcessorConfig>
    findResultProcessorConfigs(ImmutableMap<String, String> resultsProperties) throws InvalidConfigurationException {
        ImmutableBiMap<String, Class<? extends ResultProcessor>> processorToClass =
                mapGroupNamesToClasses(resultsProperties, ResultProcessor.class);
        ImmutableMap.Builder<Class<? extends ResultProcessor>, ResultProcessorConfig> builder = ImmutableMap.builder();
        for (Entry<String, Class<? extends ResultProcessor>> entry : processorToClass.entrySet()) {
            builder.put(entry.getValue(), getResultProcessorConfig(resultsProperties, entry.getKey()));
        }
        return builder.build();
    }

    public ImmutableMap<String, String> properties() {
        return properties;
    }

//    /**
//     * Returns the configuration of the current host JVM (including the flags used to create it). Any
//     * args specified using {@code vm.args} will also be applied
//     */
//    public VmConfig getDefaultVmConfig() {
//        return new Builder(new File(System.getProperty("java.home")))
//                .addAllOptions(Collections2.filter(ManagementFactory.getRuntimeMXBean().getInputArguments(),
//                        new Predicate<String>() {
//                            @Override
//                            public boolean apply(@Nullable String input) {
//                                // Exclude the -agentlib:jdwp param which configures the socket debugging protocol.
//                                // If this is set in the parent VM we do not want it to be inherited by the child
//                                // VM.  If it is, the child will die immediately on startup because it will fail to
//                                // bind to the debug port (because the parent VM is already bound to it).
//                                return !input.startsWith("-agentlib:jdwp");
//                            }
//                        }))
//                        // still incorporate vm.args
//                .addAllOptions(getArgs(subgroupMap(properties, "vm")))
//                .build();
//    }
//
//    public VmConfig getVmConfig(String name) throws InvalidConfigurationException {
//        checkNotNull(name);
//        ImmutableMap<String, String> vmGroupMap = subgroupMap(properties, "vm");
//        ImmutableMap<String, String> vmMap = subgroupMap(vmGroupMap, name);
//        File homeDir = getJdkHomeDir(vmGroupMap.get("baseDirectory"), vmMap.get("home"), name);
//        return new VmConfig.Builder(homeDir)
//                .addAllOptions(getArgs(vmGroupMap))
//                .addAllOptions(getArgs(vmMap))
//                .build();
//    }

    private static final Pattern INSTRUMENT_CLASS_PATTERN = Pattern.compile("([^\\.]+)\\.class");

    public ImmutableSet<String> getConfiguredInstruments() {
        ImmutableSet.Builder<String> resultBuilder = ImmutableSet.builder();
        for (String key : subgroupMap(properties, "instrument").keySet()) {
            Matcher matcher = INSTRUMENT_CLASS_PATTERN.matcher(key);
            if (matcher.matches()) {
                resultBuilder.add(matcher.group(1));
            }
        }
        return resultBuilder.build();
    }

    public InstrumentConfig getInstrumentConfig(String name) {
        checkNotNull(name);
        ImmutableMap<String, String> instrumentGroupMap = subgroupMap(properties, "instrument");
        ImmutableMap<String, String> insrumentMap = subgroupMap(instrumentGroupMap, name);
        String className = insrumentMap.get("class");
        checkArgument(className != null, "no instrument configured named %s", name);
        return new InstrumentConfig.Builder()
                .className(className)
                .addAllOptions(subgroupMap(insrumentMap, "options"))
                .build();
    }

    public ImmutableSet<Class<? extends ResultProcessor>> getConfiguredResultProcessors() {
        return resultProcessorConfigs.keySet();
    }

    public ResultProcessorConfig getResultProcessorConfig(Class<? extends ResultProcessor> resultProcessorClass) {
        return resultProcessorConfigs.get(resultProcessorClass);
    }

    private static ResultProcessorConfig getResultProcessorConfig(
            ImmutableMap<String, String> resultsProperties, String name) {
        ImmutableMap<String, String> resultsMap = subgroupMap(resultsProperties, name);
        return new ResultProcessorConfig.Builder()
                .className(resultsMap.get("class"))
                .addAllOptions(subgroupMap(resultsMap, "options"))
                .build();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("properties", properties)
                .toString();
    }

    private static final ImmutableMap<String, String> subgroupMap(ImmutableMap<String, String> map,
                                                                  String groupName) {
        return Util.prefixedSubmap(map, groupName + ".");
    }

    private static List<String> getArgs(Map<String, String> properties) {
        String argsString = Strings.nullToEmpty(properties.get("args"));
        ImmutableList.Builder<String> args = ImmutableList.builder();
        StringBuilder arg = new StringBuilder();
        for (int i = 0; i < argsString.length(); i++) {
            char c = argsString.charAt(i);
            switch (c) {
                case '\\':
                    arg.append(argsString.charAt(++i));
                    break;
                case ' ':
                    if (arg.length() > 0) {
                        args.add(arg.toString());
                    }
                    arg = new StringBuilder();
                    break;
                default:
                    arg.append(c);
                    break;
            }
        }
        if (arg.length() > 0) {
            args.add(arg.toString());
        }
        return args.build();
    }

    // TODO(gak): check that the directory seems to be a jdk home (with a java binary and all of that)
    // TODO(gak): make this work with different directory layouts.  I'm looking at you OS X...
    private static File getJdkHomeDir(String baseDirectoryPath, String homeDirPath, String vmConfigName)
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

    private static File getBaseDirectory(String baseDirectoryPath)
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

    /**
     * Helper method that searches a directory for json files. The newest json file will be returned.
     * Using this methods assume that all json files in the folder are valid trial results.
     *
     * @param dir Directory to search.
     * @return Reference to the latest JSON file or null if no json files where found.
     */
    public static File getLatestJsonFile(File dir) {
        File fl = dir;
        File[] files = fl.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        if (files == null) {
            return null;
        }
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (!file.getName().toLowerCase().endsWith(".json")) continue;
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        return choice;
    }

}
