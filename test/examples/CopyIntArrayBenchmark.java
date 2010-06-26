// Copyright 2010 Google Inc. All Rights Reserved.

package examples;

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

import java.util.Arrays;
import java.util.Random;

/**
 * Times copying an int[] three ways.
 *
 * @author Kevin Bourrillion
 */
public class CopyIntArrayBenchmark extends SimpleBenchmark {
  @Param({"1", "10", "10000"}) int size;

  int[] array;

  public enum Method {
    CLONE {
      @Override int[] copy(int[] array) {
        return array.clone();
      }
    },
    ARRAYS_COPYOF {
      @Override int[] copy(int[] array) {
        return Arrays.copyOf(array, array.length);
      }
    },
    SYSTEM_ARRAYCOPY {
      @Override int[] copy(int[] array) {
        int[] copy = new int[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return array;
      }
    },
    ;

    abstract int[] copy(int[] array);
  }

  @Param Method method;

  @Override protected void setUp() {
    array = new int[size];

    Random random = new Random();
    for (int i = 0; i < size; i++) {
      array[i] = random.nextInt();
    }
  }

  public int time(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      int[] copy = method.copy(array);
      dummy += copy.hashCode();
    }
    return dummy;
  }

  public static void main(String[] args) {
    Runner.main(CopyIntArrayBenchmark.class, args);
  }
}
