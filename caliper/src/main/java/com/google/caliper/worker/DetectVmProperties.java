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

package com.google.caliper.worker;

import java.util.Arrays;
import java.util.List;

/**
 * @author Kevin Bourrillion
 */
public class DetectVmProperties {
  private static final List<String> PROPERTY_NAMES = Arrays.asList(
      "java.runtime.version",
      "java.version",
      "java.vm.name",
      "java.vm.version");

  public static void main(String[] args) {
    for (String propName : PROPERTY_NAMES) {
      System.out.format("%s=%s%n", propName, System.getProperty(propName));
    }
  }
}
