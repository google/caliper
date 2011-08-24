/*
 * Copyright (C) 2011 Google Inc.
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

import com.google.caliper.model.CaliperData;
import com.google.caliper.model.Environment;
import com.google.caliper.model.Result;
import com.google.caliper.model.VM;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.TreeMap;

/**
 * Utility class to create and populate a {@link CaliperData}. Instances of this class are not
 * threadsafe.
 */
public final class ResultDataWriter {
  // TODO(schmoe): currently, ResultDataWriter can only write data with a single instrument and a
  // single environment. We'll need to adapt that to make this work for more-general cases
  private static final String ENVIRONMENT_LOCAL_NAME = "A";
  private static final String INSTRUMENT_LOCAL_NAME = "A";

  private CaliperData data = new CaliperData();
  private final BiMap<VirtualMachine, String> virtualMachineLocalNames = HashBiMap.create();
  private final BiMap<Scenario, String> scenarioLocalNames = HashBiMap.create();

  /**
   * Return the {@code CaliperData} that was populated by the write* methods.
   */
  public CaliperData getData() {
    CaliperData oldData = data;
    // I don't want accidental future calls to affect the already-returned CaliperData, but this
    // doesn't really need to be usable after this call either. This is just lazy - for now, this
    // is easier.
    // TODO(schmoe): Determine whether a ResultDataWriter should be usable after calling getData(),
    // and document it or make all of the write* methods throw if getData() has already been called.
    data = new CaliperData();
    virtualMachineLocalNames.clear();
    scenarioLocalNames.clear();
    return oldData;
  }

  /**
   * Writes a copy of the given {@code env} to the {@code CaliperData}. This method uses the given
   * environment's localName if it's set; otherwise, it assigns a meaningless localName to the copy.
   */
  public String writeEnvironment(Environment env) {
    Preconditions.checkState(data.environments.isEmpty());
    Environment copy = new Environment();
    copy.properties = Maps.newTreeMap(env.properties);
    copy.localName = Objects.firstNonNull(env.localName, "A");
    data.environments.add(copy);
    return copy.localName;
  }

  public String writeVM(VirtualMachine virtualMachine) {
    String localName = virtualMachineLocalNames.get(virtualMachine);
    if (localName != null) {
      return localName;
    }

    VM vm = new VM();
    vm.localName = virtualMachine.name;
    vm.vmName = virtualMachine.name;
    vm.detectedProperties = virtualMachine.detectProperties();
    data.vms.add(vm);
    virtualMachineLocalNames.put(virtualMachine, vm.localName);
    return vm.localName;
  }

  public String writeInstrument(Instrument instrument) {
    Preconditions.checkState(data.instruments.isEmpty());
    com.google.caliper.model.Instrument modelInstrument = new com.google.caliper.model.Instrument();
    modelInstrument.localName = INSTRUMENT_LOCAL_NAME;
    modelInstrument.className = instrument.getClass().getName();
    modelInstrument.properties = new TreeMap<String, String>(instrument.options);
    data.instruments.add(modelInstrument);
    return INSTRUMENT_LOCAL_NAME;
  }

  public void writeTrialResult(TrialResult trialResult) {
    Scenario scenario = trialResult.getScenario();
    String vmLocalName = writeVM(scenario.vm());
    String scenarioLocalName = writeScenario(scenario, vmLocalName);
    writeResult(scenarioLocalName, trialResult);
  }

  private String writeScenario(Scenario scenario, String vmLocalName) {
    String localName = scenarioLocalNames.get(scenario);
    if (localName != null) {
      return localName;
    }

    localName = generateUniqueName(data.scenarios.size());
    com.google.caliper.model.Scenario modelScenario = new com.google.caliper.model.Scenario();
    modelScenario.localName = localName;
    modelScenario.benchmarkMethodName = scenario.benchmarkMethod().name();
    modelScenario.benchmarkClassName = scenario.benchmarkMethod().className();
    modelScenario.vmLocalName = vmLocalName;
    modelScenario.vmArguments = new TreeMap<String, String>(scenario.vmArguments());
    modelScenario.environmentLocalName = ENVIRONMENT_LOCAL_NAME;
    modelScenario.userParameters = new TreeMap<String, String>(scenario.userParameters());
    data.scenarios.add(modelScenario);
    scenarioLocalNames.put(scenario, localName);
    return localName;
  }

  private void writeResult(String scenarioLocalName, TrialResult trialResult) {
    Result result = new Result();
    result.localName = generateUniqueName(data.results.size());
    result.scenarioLocalName = scenarioLocalName;
    result.instrumentLocalName = INSTRUMENT_LOCAL_NAME;
    result.measurements = ImmutableList.copyOf(trialResult.getMeasurements());
    result.messages = ImmutableList.copyOf(trialResult.getMessages());
    result.vmCommandLine = trialResult.getVmCommandLine();
//    result.reportInfo = ...
    data.results.add(result);
  }

  private String generateUniqueName(int index) {
    if (index < 26) {
      return String.valueOf((char) ('A' + index));
    } else {
      return generateUniqueName(index / 26 - 1) + generateUniqueName(index % 26);
    }
  }
}
