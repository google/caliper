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

    Run toEncode = new Run(ImmutableMap.of(a15dalvik, new MeasurementSetMeta(new MeasurementSet(1200.1, 1198.8), ""),
        b15dalvik, new MeasurementSetMeta(new MeasurementSet(1100.2, 1110.0), "")), "examples.FooBenchmark", new Date());
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
