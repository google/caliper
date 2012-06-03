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

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ParameterTest extends TestCase {

  public static class A extends SimpleBenchmark {
    @Param({"value1", "value2"}) String param;
  }

  public void testFromAnnotation() throws Exception {
    Map<String,Parameter<?>> map = Parameter.forClass(A.class);
    Parameter<?> p = map.get("param");
    assertEquals("param", p.getName());
    assertEquals(String.class, p.getType());

    checkParameterValues(A.class, "value1", "value2");
  }

  public enum Foo { VALUE1, VALUE2 }

  public static class H extends SimpleBenchmark {
    @Param Foo param;
  }

  public void testAllEnums() throws Exception {
    checkParameterValues(H.class, Foo.VALUE1, Foo.VALUE2);
  }

  public static class I extends SimpleBenchmark {
    @Param boolean param;
  }

  public void testBoolean() throws Exception {
    checkParameterValues(I.class, true, false);
  }

  private static void checkParameterValues(Class<? extends SimpleBenchmark> bClass,
      Object... expected) throws Exception {
    Map<String,Parameter<?>> map = Parameter.forClass(bClass);
    assertEquals(1, map.size());
    Parameter<?> p = map.get("param");
    List<Object> values = ImmutableList.copyOf(p.values());
    assertEquals(Arrays.asList(expected), values);
  }
}
