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

import com.google.caliper.runner.ParsedOptionsTest;
import com.google.caliper.util.SimpleDurationTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(CaliperTest.class);
    suite.addTestSuite(InterleavedReaderTest.class);
    suite.addTestSuite(JsonTest.class);
    suite.addTestSuite(LinearTranslationTest.class);
    suite.addTestSuite(MeasurementSetTest.class);
    suite.addTestSuite(ParameterTest.class);

    // too slow...
    // suite.addTestSuite(WarmupOverflowTest.class);

    suite.addTestSuite(ParsedOptionsTest.class);
    suite.addTestSuite(SimpleDurationTest.class);
    return suite;
  }
}
