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

package com.google.caliper.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LinearTranslationTest {
  private static final double CLOSE_ENOUGH = 1.0E-13;

  @Test public void linearTranslation() {
    LinearTranslation ctof = new LinearTranslation(0, 32, 100, 212);
    assertEquals(32, ctof.translate(0), CLOSE_ENOUGH);
    assertEquals(212, ctof.translate(100), CLOSE_ENOUGH);
    assertEquals(98.6, ctof.translate(37), CLOSE_ENOUGH);
    assertEquals(-40, ctof.translate(-40), CLOSE_ENOUGH);

    LinearTranslation reversed = new LinearTranslation(5, 42, 69, 0);
    assertEquals(-21, reversed.translate(101), CLOSE_ENOUGH);
  }
}
