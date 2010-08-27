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

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.util.List;

public final class StandardVm implements Vm {

  @Override public List<String> getVmSpecificOptions(MeasurementType type) {
    if (type == MeasurementType.TIME) {
      return Lists.newArrayList("-Xbatch", "-XX:+UseSerialGC", "-XX:+PrintCompilation");
    } else {
      // don't bother printing compilation if we're not measuring time. It spews a bunch of
      // useless information when measuring, for example, instances allocated.
      return Lists.newArrayList("-Xbatch", "-XX:+UseSerialGC");
    }
  }

  @Override public LogParser getLogParser(BufferedReader logReader) {
    return new StdOutLogParser(logReader);
  }

  @Override public void init() {
    // nothing to do
  }

  @Override public void cleanup() {
    // nothing to do
  }
}
