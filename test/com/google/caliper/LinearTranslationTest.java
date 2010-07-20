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

import junit.framework.TestCase;

public class LinearTranslationTest extends TestCase {
  public void test() {
    LinearTranslation ctof = new LinearTranslation(0, 32, 100, 212);
    assertEquals(-40.0, ctof.translate(-40.0));

    LinearTranslation reversed = new LinearTranslation(5, 42, 69, 0);
    assertEquals(-21.0, reversed.translate(101.0));
  }
}
