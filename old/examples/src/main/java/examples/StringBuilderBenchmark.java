/*
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

package examples;

import static java.lang.Character.MIN_SURROGATE;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;

/**
 * Tests the performance of various StringBuilder methods.
 */
public class StringBuilderBenchmark {

  @Param({"1", "10", "100"}) private int length;

  @Benchmark void appendBoolean(int reps) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < reps; ++i) {
      sb.setLength(0);
      for (int j = 0; j < length; ++j) {
        sb.append(true);
        sb.append(false);
      }
    }
  }

  @Benchmark void appendChar(int reps) {
    for (int i = 0; i < reps; ++i) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < length; ++j) {
        sb.append('c');
      }
    }
  }

  @Benchmark void appendCharArray(int reps) {
    char[] chars = "chars".toCharArray();
    for (int i = 0; i < reps; ++i) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < length; ++j) {
        sb.append(chars);
      }
    }
  }

  @Benchmark void appendCharSequence(int reps) {
    CharSequence cs = "chars";
    for (int i = 0; i < reps; ++i) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < length; ++j) {
        sb.append(cs);
      }
    }
  }

  @Benchmark void appendDouble(int reps) {
    double d = 1.2;
    for (int i = 0; i < reps; ++i) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < length; ++j) {
        sb.append(d);
      }
    }
  }

  @Benchmark void appendFloat(int reps) {
    float f = 1.2f;
    for (int i = 0; i < reps; ++i) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < length; ++j) {
        sb.append(f);
      }
    }
  }

  @Benchmark void appendInt(int reps) {
    int n = 123;
    for (int i = 0; i < reps; ++i) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < length; ++j) {
        sb.append(n);
      }
    }
  }

  @Benchmark void appendLong(int reps) {
    long l = 123;
    for (int i = 0; i < reps; ++i) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < length; ++j) {
        sb.append(l);
      }
    }
  }

  @Benchmark void appendObject(int reps) {
    Object o = new Object();
    for (int i = 0; i < reps; ++i) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < length; ++j) {
        sb.append(o);
      }
    }
  }

  @Benchmark void appendString(int reps) {
    String s = "chars";
    for (int i = 0; i < reps; ++i) {
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < length; ++j) {
        sb.append(s);
      }
    }
  }

  @Benchmark void appendNull(int reps) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < reps; ++i) {
      sb.setLength(0);
      for (int j = 0; j < length; ++j) {
        sb.append((String)null);
        sb.append((StringBuilder)null);
      }
    }
  }

  /** Times .reverse() when no surrogates are present. */
  @Benchmark void reverseNoSurrogates(int reps) {
    final int length = Math.min(this.length, MIN_SURROGATE);
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < length; j++) {
      sb.appendCodePoint(j);
    }
    for (int i = 0; i < reps; i++) {
      for (int j = 0; j < 4; j++) {
        sb.reverse();
      }
      if (sb.codePointAt(0) > MIN_SURROGATE)
        throw new Error();
    }
  }

  /** Times .codePointAt(int) when no surrogates are present. */
  @Benchmark void codePointAtNoSurrogates(int reps) {
    final int length = Math.min(this.length, MIN_SURROGATE);
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < length; j++) {
      sb.appendCodePoint(j);
    }
    for (int i = 0; i < reps; i++) {
      for (int j = 0; j < 4; j++) {
        for (int k = 0; k < length - 1; k++) {
          if (sb.codePointAt(k) > MIN_SURROGATE)
            throw new Error();
        }
      }
    }
  }

  /** Times .codePointBefore(int) when no surrogates are present. */
  @Benchmark void codePointBeforeNoSurrogates(int reps) {
    final int length = Math.min(this.length, MIN_SURROGATE);
    StringBuilder sb = new StringBuilder();
    for (int j = 0; j < length; j++) {
      sb.appendCodePoint(j);
    }
    for (int i = 0; i < reps; i++) {
      for (int j = 0; j < 4; j++) {
        for (int k = 1; k < length; k++) {
          if (sb.codePointBefore(k) > MIN_SURROGATE)
            throw new Error();
        }
      }
    }
  }
}
