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

package test;

import com.google.caliper.Measurement;
import com.google.caliper.MeasurementSet;
import com.google.caliper.MeasurementSetMeta;
import com.google.caliper.Run;
import com.google.caliper.Scenario;
import com.google.caliper.Xml;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import junit.framework.TestCase;

public class RunXmlTest extends TestCase {

  public void testXmlRoundtrip() {
    Scenario a15dalvik = new Scenario(ImmutableMap.of(
        "foo", "A", "bar", "15", "vm", "dalvikvm"));
    Scenario b15dalvik = new Scenario(ImmutableMap.of(
        "foo", "B", "bar", "15", "vm", "dalvikvm"));

    Measurement[] measurements = new Measurement[3];
    measurements[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    MeasurementSet measurementSet = new MeasurementSet(measurements);

    Measurement[] measurements2 = new Measurement[3];
    measurements2[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.0, 6.3);
    measurements2[1] = new Measurement(ImmutableMap.of("doublens", 1), 1.3, 0.7);
    measurements2[2] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 9.8);
    MeasurementSet measurementSet2 = new MeasurementSet(measurements);

    Run toEncode = new Run(ImmutableMap.of(a15dalvik,
        new MeasurementSetMeta(measurementSet, a15dalvik, ""),
        b15dalvik,
        new MeasurementSetMeta(measurementSet2, b15dalvik, "")),
        "examples.FooBenchmark", new Date());
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    Xml.runToXml(toEncode, bytesOut);

    // we don't validate the XML directly because it's a hassle to cope with arbitrary orderings of
    // an element's attributes

    ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
    Run decoded = Xml.runFromXml(bytesIn);

    assertEquals(toEncode.getBenchmarkName(), decoded.getBenchmarkName());
    assertEquals(toEncode.getMeasurements().keySet(), decoded.getMeasurements().keySet());
  }
}
