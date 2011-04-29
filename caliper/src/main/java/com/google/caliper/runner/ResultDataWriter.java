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
import com.google.caliper.model.VM;

import java.util.Map;
import java.util.TreeMap;

// This is just a placeholder

public class ResultDataWriter {
  CaliperData data = new CaliperData();

  public void writeEnvironment(Map<String, String> properties) {
    Environment env = new Environment();
    env.localName = "A";
    env.properties = new TreeMap<String, String>(properties);

    data.environments.add(env);
  }

  public void writeVM(VirtualMachine virtualMachine) {
    VM vm = new VM();
    vm.localName = virtualMachine.name;
    vm.detectedProperties = virtualMachine.detectProperties();
    data.vms.add(vm);
  }
}
