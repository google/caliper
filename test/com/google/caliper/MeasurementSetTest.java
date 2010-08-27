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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;

public class MeasurementSetTest extends TestCase {

  Ordering<Measurement> MEASUREMENT_BY_NANOS = new Ordering<Measurement>() {
    @Override public int compare(Measurement a, Measurement b) {
      return Double.compare(a.getRaw(), b.getRaw());
    }
  };
  
  public void testIncompatibleMeasurements() {
    Measurement[] measurements = new Measurement[2];
    measurements[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements[1] = new Measurement(ImmutableMap.of("triplens", 1), 3.8, 7.6);
    try {
      new MeasurementSet(measurements);
      fail("illegal argument exception not thrown");
    } catch (IllegalArgumentException e) {
      // success
    }
  }

  public void testIncompatibleAddedMeasurements() {
    Measurement[] measurements = new Measurement[1];
    measurements[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    MeasurementSet measurementSet = new MeasurementSet(measurements);
    try {
      measurementSet.plusMeasurement(new Measurement(ImmutableMap.of("triplens", 1), 3.8, 7.6));
      fail("illegal argument exception not thrown");
    } catch (IllegalArgumentException e) {
      // success
    }
  }

  public void testSize() {
    Measurement[] measurements = new Measurement[3];
    measurements[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    MeasurementSet measurementSet = new MeasurementSet(measurements);
    assertEquals(3, measurementSet.size());

    Measurement[] measurements2 = new Measurement[4];
    measurements2[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements2[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements2[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    measurements2[3] = new Measurement(ImmutableMap.of("doublens", 1), 7.2, 14.4);
    MeasurementSet measurementSet2 =
        new MeasurementSet(measurements2);
    assertEquals(4, measurementSet2.size());
  }

  public void testPlusMeasurement() {
    Measurement[] measurements = new Measurement[3];
    measurements[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    MeasurementSet measurementSet = new MeasurementSet(measurements);

    Measurement[] measurements2 = new Measurement[4];
    measurements2[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements2[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements2[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    measurements2[3] = new Measurement(ImmutableMap.of("doublens", 1), 7.2, 14.4);
    MeasurementSet measurementSet2 =
        new MeasurementSet(measurements2);

    MeasurementSet measurementSet3 = measurementSet.plusMeasurement(measurements2[3]);

    assertDoubleListsEquals(measurementSet2.getMeasurementsRaw(),
        measurementSet3.getMeasurementsRaw(), 0.0000001);
    assertDoubleListsEquals(measurementSet2.getMeasurementUnits(),
        measurementSet3.getMeasurementUnits(), 0.0000001);
    assertEquals(measurementSet2.getUnitNames(), measurementSet3.getUnitNames());

    List<Measurement> measurementList1 =
        MEASUREMENT_BY_NANOS.sortedCopy(measurementSet2.getMeasurements());
    List<Measurement> measurementList2 =
        MEASUREMENT_BY_NANOS.sortedCopy(measurementSet3.getMeasurements());
    assertEquals(measurementList1.size(), measurementList2.size());
    for (int i = 0; i < measurementList1.size(); i++) {
      assertEquals(measurementList1.get(i).getRaw(),
          measurementList2.get(i).getRaw());
      assertEquals(measurementList1.get(i).getProcessed(),
          measurementList2.get(i).getProcessed());
    }
  }

  public void testMedian() {
    Measurement[] measurements = new Measurement[3];
    measurements[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    MeasurementSet measurementSet = new MeasurementSet(measurements);
    assertEquals(2.3, measurementSet.medianRaw(), 0.00000001);
    assertEquals(4.6, measurementSet.medianUnits(), 0.00000001);

    Measurement[] measurements2 = new Measurement[4];
    measurements2[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements2[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements2[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    measurements2[3] = new Measurement(ImmutableMap.of("doublens", 1), 7.2, 14.4);
    MeasurementSet measurementSet2 =
        new MeasurementSet(measurements2);
    assertEquals((2.3 + 3.8) / 2, measurementSet2.medianRaw(), 0.00000001);
    assertEquals((4.6 + 7.6) / 2, measurementSet2.medianUnits(), 0.00000001);
  }

  public void testMean() {
    Measurement[] measurements = new Measurement[3];
    measurements[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    MeasurementSet measurementSet = new MeasurementSet(measurements);
    assertEquals((1.1 + 3.8 + 2.3) / 3, measurementSet.meanRaw(), 0.00000001);
    assertEquals((2.2 + 7.6 + 4.6) / 3, measurementSet.meanUnits(), 0.00000001);

    Measurement[] measurements2 = new Measurement[4];
    measurements2[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements2[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements2[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    measurements2[3] = new Measurement(ImmutableMap.of("doublens", 1), 7.2, 14.4);
    MeasurementSet measurementSet2 =
        new MeasurementSet(measurements2);
    assertEquals((1.1 + 2.3 + 3.8 + 7.2) / 4, measurementSet2.meanRaw(), 0.00000001);
    assertEquals((2.2 + 4.6 + 7.6 + 14.4) / 4, measurementSet2.meanUnits(), 0.00000001);
  }

  public void testStandardDeviation() {
    Measurement[] measurements = new Measurement[3];
    measurements[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    MeasurementSet measurementSet = new MeasurementSet(measurements);
    assertEquals(1.35277, measurementSet.standardDeviationRaw(), 0.00001);
    assertEquals(2.70555, measurementSet.standardDeviationUnits(), 0.00001);
  }

  public void testMax() {
    Measurement[] measurements = new Measurement[3];
    measurements[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    MeasurementSet measurementSet = new MeasurementSet(measurements);
    assertEquals(3.8, measurementSet.maxRaw(), 0.00000001);
    assertEquals(7.6, measurementSet.maxUnits(), 0.00000001);
  }

  public void testMin() {
    Measurement[] measurements = new Measurement[3];
    measurements[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    MeasurementSet measurementSet = new MeasurementSet(measurements);
    assertEquals(1.1, measurementSet.minRaw(), 0.00000001);
    assertEquals(2.2, measurementSet.minUnits(), 0.00000001);
  }

  public void testJsonRoundtrip() {
    Measurement[] measurements = new Measurement[3];
    measurements[0] = new Measurement(ImmutableMap.of("doublens", 1), 1.1, 2.2);
    measurements[1] = new Measurement(ImmutableMap.of("doublens", 1), 3.8, 7.6);
    measurements[2] = new Measurement(ImmutableMap.of("doublens", 1), 2.3, 4.6);
    MeasurementSet measurementSet = new MeasurementSet(measurements);
    MeasurementSet roundTripped =
        Json.measurementSetFromJson(Json.measurementSetToJson(measurementSet));
    assertDoubleListsEquals(measurementSet.getMeasurementsRaw(),
        roundTripped.getMeasurementsRaw(), 0.00000001);
    assertDoubleListsEquals(measurementSet.getMeasurementUnits(),
        roundTripped.getMeasurementUnits(), 0.00000001);
    assertEquals(measurementSet.getUnitNames(), roundTripped.getUnitNames());
  }

  @SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
  public void testFromLegacyString() {
    MeasurementSet measurementSet = Json.measurementSetFromJson("122.0 133.0 144.0");
    assertDoubleListsEquals(Arrays.asList(122.0, 133.0, 144.0),
        measurementSet.getMeasurementsRaw(), 0.00000001);
    assertDoubleListsEquals(Arrays.asList(122.0, 133.0, 144.0),
        measurementSet.getMeasurementUnits(), 0.00000001);
    assertEquals(ImmutableMap.of("ns", 1, "us", 1000, "ms", 1000000, "s", 1000000000),
        measurementSet.getUnitNames());
  }

  private void assertDoubleListsEquals(List<Double> expected, List<Double> actual, double epsilon) {
    assertEquals(expected.size(), actual.size());
    Collections.sort(expected);
    Collections.sort(actual);
    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i), actual.get(i), epsilon);
    }
  }
}
