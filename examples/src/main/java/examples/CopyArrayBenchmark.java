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

package examples;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;

import java.util.Arrays;
import java.util.Random;

/**
 * Tests each of four ways to copy an array, for all nine array types.
 *
 * <p>Once upon a time, {@code clone} was much slower than the other array copy techniques, but
 * that was fixed in Sun bug:
 *
 * <a href="http://bugs.sun.com/view_bug.do?bug_id=6428387">
 * array clone() much slower than Arrays.copyOf</a>
 *
 * at which time all copy methods were equally efficient.
 *
 * <p>Recent (2011) measurements with OpenJDK 7 on Linux are less clear.  Results suggests that:
 *
 * <ul>
 * <li>The different methods of copying have indistinguishable performance with hotspot server for
 *     all nine types, except that the naive LOOP is slower.
 *     With the "client" compiler, LOOP beats CLONE, which is the slowest.
 * <li>As array sizes get large, the runtime is indeed proportional to the size of the array in
 *     memory (boolean arrays count as byte arrays!).
 * </ul>
 */
public class CopyArrayBenchmark {
  public enum Strategy {
    CLONE {
      @Override Object[] copy(Object[] array) {
        return array.clone();
      }
      @Override boolean[] copy(boolean[] array) {
        return array.clone();
      }
      @Override byte[] copy(byte[] array) {
        return array.clone();
      }
      @Override char[] copy(char[] array) {
        return array.clone();
      }
      @Override double[] copy(double[] array) {
        return array.clone();
      }
      @Override float[] copy(float[] array) {
        return array.clone();
      }
      @Override int[] copy(int[] array) {
        return array.clone();
      }
      @Override long[] copy(long[] array) {
        return array.clone();
      }
      @Override short[] copy(short[] array) {
        return array.clone();
      }
    },
    ARRAYS_COPYOF {
      @Override Object[] copy(Object[] array) {
        return Arrays.copyOf(array, array.length);
      }
      @Override boolean[] copy(boolean[] array) {
        return Arrays.copyOf(array, array.length);
      }
      @Override byte[] copy(byte[] array) {
        return Arrays.copyOf(array, array.length);
      }
      @Override char[] copy(char[] array) {
        return Arrays.copyOf(array, array.length);
      }
      @Override double[] copy(double[] array) {
        return Arrays.copyOf(array, array.length);
      }
      @Override float[] copy(float[] array) {
        return Arrays.copyOf(array, array.length);
      }
      @Override int[] copy(int[] array) {
        return Arrays.copyOf(array, array.length);
      }
      @Override long[] copy(long[] array) {
        return Arrays.copyOf(array, array.length);
      }
      @Override short[] copy(short[] array) {
        return Arrays.copyOf(array, array.length);
      }
    },
    SYSTEM_ARRAYCOPY {
      @Override Object[] copy(Object[] array) {
        Object[] copy = new Object[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
      }
      @Override boolean[] copy(boolean[] array) {
        boolean[] copy = new boolean[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
      }
      @Override byte[] copy(byte[] array) {
        byte[] copy = new byte[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
      }
      @Override char[] copy(char[] array) {
        char[] copy = new char[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
      }
      @Override double[] copy(double[] array) {
        double[] copy = new double[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
      }
      @Override float[] copy(float[] array) {
        float[] copy = new float[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
      }
      @Override int[] copy(int[] array) {
        int[] copy = new int[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
      }
      @Override long[] copy(long[] array) {
        long[] copy = new long[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
      }
      @Override short[] copy(short[] array) {
        short[] copy = new short[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
      }
    },
    LOOP {
      @Override Object[] copy(Object[] array) {
        int len = array.length;
        Object[] copy = new Object[len];
        for (int i = 0; i < len; i++) {
          copy[i] = array[i];
        }
        return copy;
      }
      @Override boolean[] copy(boolean[] array) {
        int len = array.length;
        boolean[] copy = new boolean[array.length];
        for (int i = 0; i < len; i++) {
          copy[i] = array[i];
        }
        return copy;
      }
      @Override byte[] copy(byte[] array) {
        int len = array.length;
        byte[] copy = new byte[array.length];
        for (int i = 0; i < len; i++) {
          copy[i] = array[i];
        }
        return copy;
      }
      @Override char[] copy(char[] array) {
        int len = array.length;
        char[] copy = new char[array.length];
        for (int i = 0; i < len; i++) {
          copy[i] = array[i];
        }
        return copy;
      }
      @Override double[] copy(double[] array) {
        int len = array.length;
        double[] copy = new double[array.length];
        for (int i = 0; i < len; i++) {
          copy[i] = array[i];
        }
        return copy;
      }
      @Override float[] copy(float[] array) {
        int len = array.length;
        float[] copy = new float[array.length];
        for (int i = 0; i < len; i++) {
          copy[i] = array[i];
        }
        return copy;
      }
      @Override int[] copy(int[] array) {
        int len = array.length;
        int[] copy = new int[array.length];
        for (int i = 0; i < len; i++) {
          copy[i] = array[i];
        }
        return copy;
      }
      @Override long[] copy(long[] array) {
        int len = array.length;
        long[] copy = new long[array.length];
        for (int i = 0; i < len; i++) {
          copy[i] = array[i];
        }
        return copy;
      }
      @Override short[] copy(short[] array) {
        int len = array.length;
        short[] copy = new short[array.length];
        for (int i = 0; i < len; i++) {
          copy[i] = array[i];
        }
        return copy;
      }
    },
    ;

    abstract Object[] copy(Object[] array);
    abstract boolean[] copy(boolean[] array);
    abstract byte[] copy(byte[] array);
    abstract char[] copy(char[] array);
    abstract double[] copy(double[] array);
    abstract float[] copy(float[] array);
    abstract int[] copy(int[] array);
    abstract long[] copy(long[] array);
    abstract short[] copy(short[] array);
  }

  @Param Strategy strategy;

  @Param({"5", "500", "50000"}) int size;

  Object[] objectArray;
  boolean[] booleanArray;
  byte[] byteArray;
  char[] charArray;
  double[] doubleArray;
  float[] floatArray;
  int[] intArray;
  long[] longArray;
  short[] shortArray;

  @BeforeExperiment void setUp() {
    objectArray = new Object[size];
    booleanArray = new boolean[size];
    byteArray = new byte[size];
    charArray = new char[size];
    doubleArray = new double[size];
    floatArray = new float[size];
    intArray = new int[size];
    longArray = new long[size];
    shortArray = new short[size];

    Random random = new Random();
    for (int i = 0; i < size; i++) {
      int num = random.nextInt();
      objectArray[i] = new Object();
      booleanArray[i] = num % 2 == 0;
      byteArray[i] = (byte) num;
      charArray[i] = (char) num;
      doubleArray[i] = num;
      floatArray[i] = num;
      intArray[i] = num;
      longArray[i] = num;
      shortArray[i] = (short) num;
    }
  }

  @Benchmark int objects(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += System.identityHashCode(strategy.copy(objectArray));
    }
    return dummy;
  }

  @Benchmark int booleans(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += System.identityHashCode(strategy.copy(booleanArray));
    }
    return dummy;
  }

  @Benchmark int bytes(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += System.identityHashCode(strategy.copy(byteArray));
    }
    return dummy;
  }

  @Benchmark int chars(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += System.identityHashCode(strategy.copy(charArray));
    }
    return dummy;
  }

  @Benchmark int doubles(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += System.identityHashCode(strategy.copy(doubleArray));
    }
    return dummy;
  }

  @Benchmark int floats(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += System.identityHashCode(strategy.copy(floatArray));
    }
    return dummy;
  }

  @Benchmark int ints(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += System.identityHashCode(strategy.copy(intArray));
    }
    return dummy;
  }

  @Benchmark int longs(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += System.identityHashCode(strategy.copy(longArray));
    }
    return dummy;
  }

  @Benchmark int shorts(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += System.identityHashCode(strategy.copy(shortArray));
    }
    return dummy;
  }
}
