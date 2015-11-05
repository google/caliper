/**
 * Copyright (C) 2009 Google Inc.
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


package com.google.caliper.cloud.client;

import java.util.Arrays;

public final class Key {

  private Value[] elements;

  public Key(int length) {
    elements = new Value[length];
  }

  public void set(Variable variable, Value value) {
    elements[variable.keyIndex()] = value;
  }

  @Override public boolean equals(Object o) {
    return o instanceof Key
        && Arrays.equals(((Key) o).elements, elements);
  }

  @Override public int hashCode() {
    return Arrays.hashCode(elements);
  }

  @Override public String toString() {
    return Arrays.toString(elements);
  }
}
