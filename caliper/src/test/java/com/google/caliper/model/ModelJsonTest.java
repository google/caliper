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

package com.google.caliper.model;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.TreeMap;

public class ModelJsonTest extends TestCase {

  public void test() {

    Scenario scenario = new Scenario();
    scenario.localName = "A";
    scenario.environmentLocalName = "B";
    scenario.vmLocalName = "C";
    scenario.benchmarkClassName = "examples.Foo";
    scenario.benchmarkMethodName = "bar";
    scenario.userParameters = new TreeMap<String, String>();
    scenario.vmArguments = new TreeMap<String, String>();
    
    CaliperData result = new CaliperData();
    result.scenarios = Arrays.asList(scenario);

    String json = ModelJson.toJson(result);
    // System.out.println(json);

    CaliperData result1 = ModelJson.fromJson(json, CaliperData.class);
    // System.out.println(result1.scenarios);

    // WTF does this test do?

  }

}
