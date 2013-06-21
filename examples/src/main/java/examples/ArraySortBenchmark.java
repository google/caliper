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

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;

import java.util.Arrays;
import java.util.Random;

/**
 * Measures sorting on different distributions of integers.
 */
public class ArraySortBenchmark {

  @Param({"10", "100", "1000", "10000"}) private int length;

  @Param private Distribution distribution;

  private int[] values;
  private int[] copy;

  @BeforeExperiment void setUp() throws Exception {
    values = distribution.create(length);
    copy = new int[length];
  }

  @Benchmark void sort(int reps) {
    for (int i = 0; i < reps; i++) {
      System.arraycopy(values, 0, copy, 0, values.length);
      Arrays.sort(copy);
    }
  }

  public enum Distribution {
    SAWTOOTH {
      @Override
      int[] create(int length) {
        int[] result = new int[length];
        for (int i = 0; i < length; i += 5) {
          result[i] = 0;
          result[i + 1] = 1;
          result[i + 2] = 2;
          result[i + 3] = 3;
          result[i + 4] = 4;
        }
        return result;
      }
    },
    INCREASING {
      @Override
      int[] create(int length) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
          result[i] = i;
        }
        return result;
      }
    },
    DECREASING {
      @Override
      int[] create(int length) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
          result[i] = length - i;
        }
        return result;
      }
    },
    RANDOM {
      @Override
      int[] create(int length) {
        Random random = new Random();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
          result[i] = random.nextInt();
        }
        return result;
      }
    };

    abstract int[] create(int length);
  }
}
