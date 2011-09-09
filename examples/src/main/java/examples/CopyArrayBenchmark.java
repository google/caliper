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

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

import java.util.Arrays;
import java.util.Random;

/**
 * Tests each of the three (non-manual) ways to copy an array, for all nine array types.
 *
 * <p>For a recent (2010) build of OpenJDK on linux, this benchmark confirms these facts:
 *
 * <ul>
 * <li>The three methods of copying have indistinguishable performance for all nine types.
 * <li>As array sizes get large, the runtime is indeed proportional to the size of the array in
 *     memory (boolean arrays count as byte arrays!).
 * </ul>
 *
 */
public class CopyArrayBenchmark extends SimpleBenchmark {
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

  @Override protected void setUp() {
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
      floatArray[i] = (float) num;
      intArray[i] = num;
      longArray[i] = num;
      shortArray[i] = (short) num;
    }
  }

  public int timeObjects(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.copy(objectArray).hashCode();
    }
    return dummy;
  }

  public int timeBooleans(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.copy(booleanArray).hashCode();
    }
    return dummy;
  }

  public int timeBytes(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.copy(byteArray).hashCode();
    }
    return dummy;
  }

  public int timeChars(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.copy(charArray).hashCode();
    }
    return dummy;
  }

  public int timeDoubles(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.copy(doubleArray).hashCode();
    }
    return dummy;
  }

  public int timeFloats(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.copy(floatArray).hashCode();
    }
    return dummy;
  }

  public int timeInts(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.copy(intArray).hashCode();
    }
    return dummy;
  }

  public int timeLongs(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.copy(longArray).hashCode();
    }
    return dummy;
  }

  public int timeShorts(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      dummy += strategy.copy(shortArray).hashCode();
    }
    return dummy;
  }

  public static void main(String[] args) {
    Runner.main(CopyArrayBenchmark.class, args);
  }
}
