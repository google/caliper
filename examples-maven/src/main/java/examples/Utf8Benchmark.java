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

package examples;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;

import java.nio.charset.Charset;
import java.util.Random;

/**
 * Benchmark for operations with the UTF-8 charset.
 */
public class Utf8Benchmark {

  static final Charset UTF_8 = Charset.forName("UTF-8");

  /**
   * The maximum code point used in generated text.  Different values
   * provide reasonable models of different real-world human text.
   */
  static class MaxCodePoint {
    final int value;

    /**
     * Convert the input string to a code point.  Accepts regular
     * decimal numerals, hex strings, and some symbolic names
     * meaningful to humans.
     */
    private static int decode(String userFriendly) {
      try {
        return Integer.decode(userFriendly);
      } catch (NumberFormatException ignored) {
        if (userFriendly.matches("(?i)(?:American|English|ASCII)")) {
          // 1-byte UTF-8 sequences - "American" ASCII text
          return 0x80;
        } else if (userFriendly.matches("(?i)(?:French|Latin|Western.*European)")) {
          // Mostly 1-byte UTF-8 sequences, mixed with occasional 2-byte
          // sequences - "Western European" text
          return 0x90;
        } else if (userFriendly.matches("(?i)(?:Branch.*Prediction.*Hostile)")) {
          // Defeat branch predictor for: c < 0x80 ; branch taken 50% of the time.
          return 0x100;
        } else if (userFriendly.matches("(?i)(?:Greek|Cyrillic|European|ISO.?8859)")) {
          // Mostly 2-byte UTF-8 sequences - "European" text
          return 0x800;
        } else if (userFriendly.matches("(?i)(?:Chinese|Han|Asian|BMP)")) {
          // Mostly 3-byte UTF-8 sequences - "Asian" text
          return Character.MIN_SUPPLEMENTARY_CODE_POINT;
        } else if (userFriendly.matches("(?i)(?:Cuneiform|rare|exotic|supplementary.*)")) {
          // Mostly 4-byte UTF-8 sequences - "rare exotic" text
          return Character.MAX_CODE_POINT;
        } else {
          throw new IllegalArgumentException("Can't decode codepoint " + userFriendly);
        }
      }
    }

    public static MaxCodePoint valueOf(String userFriendly) {
      return new MaxCodePoint(userFriendly);
    }

    private MaxCodePoint(String userFriendly) {
      value = decode(userFriendly);
    }
  }

  /**
   * The default values of maxCodePoint below provide pretty good
   * performance models of different kinds of common human text.
   * @see MaxCodePoint#decode
   */
  @Param({"0x80", "0x100", "0x800", "0x10000", "0x10ffff"}) MaxCodePoint maxCodePoint;

  static final int STRING_COUNT = 1 << 7;

  @Param({"65536"}) int charCount;
  private String[] strings;

  /**
   * Computes arrays of valid unicode Strings.
   */
  @BeforeExperiment void setUp() {
    final long seed = 99;
    final Random rnd = new Random(seed);
    strings = new String[STRING_COUNT];
    for (int i = 0; i < STRING_COUNT; i++) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < charCount; j++) {
        int codePoint;
        // discard illegal surrogate "codepoints"
        do {
          codePoint = rnd.nextInt(maxCodePoint.value);
        } while (isSurrogate(codePoint));
        sb.appendCodePoint(codePoint);
      }
      strings[i] = sb.toString();
    }
    // The reps will continue until the non-determinism detector is pacified!
    getBytes(100);
  }

  /**
   * Benchmarks {@link String#getBytes} on valid strings containing
   * pseudo-randomly-generated codePoints less than {@code
   * maxCodePoint}.  A constant seed is used, so separate runs perform
   * identical computations.
   */
  @Benchmark void getBytes(int reps) {
    final String[] strings = this.strings;
    final int mask = STRING_COUNT - 1;
    for (int i = 0; i < reps; i++) {
      String string = strings[i & mask];
      byte[] bytes = string.getBytes(UTF_8);
      if (bytes[0] == 86 && bytes[bytes.length - 1] == 99) {
        throw new Error("Unlikely! We're just defeating the optimizer!");
      }
    }
  }

  /** Character.isSurrogate was added in Java SE 7. */
  private boolean isSurrogate(int c) {
    return (Character.MIN_HIGH_SURROGATE <= c &&
            c <= Character.MAX_LOW_SURROGATE);
  }
}
