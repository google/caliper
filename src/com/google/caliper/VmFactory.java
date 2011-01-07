/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.caliper;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;

public final class VmFactory {
  public static ImmutableSet<String> defaultVms() {
    String vmName = DalvikVm.isDalvikVm()
        ? DalvikVm.vmName()
        : StandardVm.defaultVmName();
    return ImmutableSet.of(vmName);
  }

  public Vm createVm(Scenario scenario) {
    List<String> vmList = Arrays.asList(scenario.getVariables().get(Scenario.VM_KEY).split("\\s+"));
    Vm vm = null;
    if (!vmList.isEmpty()) {
      if (vmList.get(0).endsWith("app_process")) {
        vm = new DalvikVm();
      } else if (vmList.get(0).endsWith("java")) {
        vm = new StandardVm();
      }
    }
    if (vm == null) {
      vm = new Vm();
    }
    return vm;
  }
}
