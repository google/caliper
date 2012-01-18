// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.memory;

import com.google.caliper.memory.ObjectGraphMeasurer.Footprint;
import com.google.common.collect.ImmutableMultiset;

import junit.framework.TestCase;

/**
 * Tests for ObjectGraphMeasurer.
 */
public class ObjectGraphMeasurerTest extends TestCase {
  enum DummyEnum {
    VALUE;
  }
  static final Object oneEnumField = new Object() {
    @SuppressWarnings("unused") DummyEnum enumField = DummyEnum.VALUE;
  };
  
  // enums are treated as statics (and ignored)
  public void testEnum() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(oneEnumField);
    assertEquals(new Footprint(1, 1, NO_PRIMITIVES), footprint);
  }
  
  static final Object oneClassField = new Object() {
    @SuppressWarnings("unused") Class<?> clazz = Object.class;
  };
  
  // Class instances are treated as statics (and ignored)
  public void testClass() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(oneClassField);
    assertEquals(new ObjectGraphMeasurer.Footprint(1, 1, NO_PRIMITIVES), footprint);
  }
  
  static final Object oneObjectField = new Object() {
    @SuppressWarnings("unused") Object objectField = new Object();
  };
  
  public void testObject() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(oneObjectField);
    assertEquals(new ObjectGraphMeasurer.Footprint(2, 1, NO_PRIMITIVES), footprint);
  }
  
  static final Object oneStringField = new Object() {
    @SuppressWarnings("unused") String stringField = "test";
  };
  
  public void testString() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(oneStringField);
    // three objects: ours, the String, and the char[]
    assertEquals(new ObjectGraphMeasurer.Footprint(3, 2, 
        ImmutableMultiset.<Class<?>>builder()
        .addCopies(char.class, 4)
        .addCopies(int.class, 3)
        .build()), footprint);
  }
  
  static final Object withCycle = new Object() {
    Object[] array = new Object[1];
    {
      array[0] = this;
    }
  };
  
  public void testCycle() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(withCycle);
    assertEquals(new ObjectGraphMeasurer.Footprint(2, 2, NO_PRIMITIVES), footprint);
  }
  
  static final Object multiplePathsToObject = new Object() {
    Object object = new Object();
    @SuppressWarnings("unused") Object ref1 = object;
    @SuppressWarnings("unused") Object ref2 = object;
  };
  
  public void testMultiplePathsToObject() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(multiplePathsToObject);
    assertEquals(new ObjectGraphMeasurer.Footprint(2, 3, NO_PRIMITIVES), footprint);
  }
  
  static final Object multiplePathsToClass = new Object() {
    Object object = Object.class;
    @SuppressWarnings("unused") Object ref1 = object;
    @SuppressWarnings("unused") Object ref2 = object;
  };
  
  public void testMultiplePathsToClass() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(multiplePathsToClass);
    assertEquals(new ObjectGraphMeasurer.Footprint(1, 3, NO_PRIMITIVES), footprint);
  }
  
  static class WithStaticField {
    static WithStaticField INSTANCE = new WithStaticField();
  }
  
  public void testStaticFields() {
    ObjectGraphMeasurer.Footprint footprint = ObjectGraphMeasurer.measure(new WithStaticField());
    assertEquals(new ObjectGraphMeasurer.Footprint(1, 0, NO_PRIMITIVES), footprint);
  }
  
  private static final ImmutableMultiset<Class<?>> NO_PRIMITIVES = ImmutableMultiset.of();
}
