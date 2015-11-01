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

package com.google.caliper.memory;

import com.google.caliper.memory.ObjectGraphMeasurer.Footprint;
import com.google.common.collect.ImmutableMultiset;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for ObjectGraphMeasurer.
 */
@RunWith(JUnit4.class)
public class ObjectGraphMeasurerTest extends TestCase {
  enum DummyEnum {
    VALUE;
  }
  static final Object oneEnumField = new Object() {
    @SuppressWarnings("unused") DummyEnum enumField = DummyEnum.VALUE;
  };

  // enums are treated as statics (and ignored)
  @Test public void testEnum() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(oneEnumField);
    assertEquals(new Footprint(1, 1, 0, NO_PRIMITIVES), footprint);
  }

  static final Object oneClassField = new Object() {
    @SuppressWarnings("unused") Class<?> clazz = Object.class;
  };

  // Class instances are treated as statics (and ignored)
  @Test public void testClass() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(oneClassField);
    assertEquals(new ObjectGraphMeasurer.Footprint(1, 1, 0, NO_PRIMITIVES), footprint);
  }

  static final Object oneObjectField = new Object() {
    @SuppressWarnings("unused") Object objectField = new Object();
  };

  @Test public void testObject() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(oneObjectField);
    assertEquals(new ObjectGraphMeasurer.Footprint(2, 1, 0, NO_PRIMITIVES), footprint);
  }

  static final Object withCycle = new Object() {
    Object[] array = new Object[1];
    {
      array[0] = this;
    }
  };

  @Test public void testCycle() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(withCycle);
    assertEquals(new ObjectGraphMeasurer.Footprint(2, 2, 0, NO_PRIMITIVES), footprint);
  }

  static final Object multiplePathsToObject = new Object() {
    Object object = new Object();
    @SuppressWarnings("unused") Object ref1 = object;
    @SuppressWarnings("unused") Object ref2 = object;
  };

  @Test public void testMultiplePathsToObject() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(multiplePathsToObject);
    assertEquals(new ObjectGraphMeasurer.Footprint(2, 3, 0, NO_PRIMITIVES), footprint);
  }

  static final Object multiplePathsToClass = new Object() {
    Object object = Object.class;
    @SuppressWarnings("unused") Object ref1 = object;
    @SuppressWarnings("unused") Object ref2 = object;
  };

  @Test public void testMultiplePathsToClass() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(multiplePathsToClass);
    assertEquals(new ObjectGraphMeasurer.Footprint(1, 3, 0, NO_PRIMITIVES), footprint);
  }

  static class WithStaticField {
    static WithStaticField INSTANCE = new WithStaticField();
  }

  @Test public void testStaticFields() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(new WithStaticField());
    assertEquals(new ObjectGraphMeasurer.Footprint(1, 0, 0, NO_PRIMITIVES), footprint);
  }

  @SuppressWarnings("unused") // unused test fields
  static final Object oneNullOneNonNull = new Object() {
    Object nonNull1 = new Object();
    Object nonNull2 = nonNull1;
    Object null1 = null;
    Object null2 = null;
    Object null3 = null;
  };

  @Test public void testNullField() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(oneNullOneNonNull);
    assertEquals(new ObjectGraphMeasurer.Footprint(2, 2, 3, NO_PRIMITIVES), footprint);
  }

  private static final ImmutableMultiset<Class<?>> NO_PRIMITIVES = ImmutableMultiset.of();
}
