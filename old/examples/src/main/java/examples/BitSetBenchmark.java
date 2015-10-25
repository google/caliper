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

import java.util.BitSet;
import java.util.Random;

/**
 * A simple example of a benchmark for BitSet showing some of the issues with
 * micro-benchmarking.
 *
 * <p>The following is a discussion of how the benchmarks evolved and what they
 * may (or may not) tell us. This discussion is based on the following set of
 * results:
 *
 * <p><pre>
 *  0% Scenario{vm=java, benchmark=SetBitSetX64} 233.45ns; σ=0.31ns @ 3 trials
 * 20% Scenario{vm=java, benchmark=SetMaskX64} 116.62ns; σ=0.09ns @ 3 trials
 * 40% Scenario{vm=java, benchmark=CharsToBitSet} 748.40ns; σ=23.52ns @ 10 trials
 * 60% Scenario{vm=java, benchmark=CharsToMask} 198.55ns; σ=9.46ns @ 10 trials
 * 80% Scenario{vm=java, benchmark=BaselineIteration} 67.85ns; σ=0.44ns @ 3 trials
 *
 *         benchmark   ns logarithmic runtime
 *      SetBitSetX64  233 XXXXXXXXX|||||||||||||||
 *        SetMaskX64  117 XXXX|||||||||||||||||
 *     CharsToBitSet  748 XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
 *       CharsToMask  199 XXXXXXX||||||||||||||||
 * BaselineIteration   68 XX|||||||||||||||||
 * </pre>
 *
 * <p>Initially things look simple. The {@link #setBitSetX64(int)} benchmark
 * takes approximately twice as long as {@link #setMaskX64(int)}. However
 * the inner loops in these benchmarks have almost no content, so a more
 * 'real world' benchmark was devised in an attempt to back up these results.
 *
 * <p>The {@link #charsToMask(int)} and {@link #charsToBitSet(int)}
 * benchmarks convert a simple char[] of '1's and '0's to a corresponding BitSet
 * or bit mask. These also processes 64 bits per iteration and so appears to be
 * doing the same amount of work as the first benchmarks.
 *
 * <p>Additionally the {@link BitSetBenchmark#baselineIteration(int)}
 * benchmark attempts to measure the raw cost of looping through and reading the
 * source data.
 *
 * <p>When comparing the benchmarks that use bit masking, we see that the
 * measured time of the SetMaskX64 benchmark (117ns) is roughly the same
 * as the CharsToMask benchmark (199ns) with the BaselineIteration time (68ms)
 * subtracted from it. This gives us some confidence that both benchmarks are
 * resulting in the same underlying work on the CPU.
 *
 * <p>However the CharsToBitSet and the SetBitSetX64 benchmarks differ very
 * significantly (approximately 3x) even when accounting for the
 * BaselineIteration result. This suggests that the performance of
 * {@link BitSet#set} is quite dependent on the surrounding code and how
 * it is optimized by the JVM.
 *
 * <p>The conclusions we can draw from this are:
 *
 * <p><b>1:</b> Using BitSet is slower than using bit masks directly. At best it
 * seems about 2x slower than a bit mask, but could easily be 5x slower in real
 * applications.
 *
 * <p>While these are only estimates, we can conclude that when performance is
 * important and where bit set operations occur in tight loops, bit masks
 * should be used in favor of BitSets.
 *
 * <p><b>2:</b>Overly simplistic benchmarks can give a very false impression of
 * performance.
 */
public class BitSetBenchmark {
  private BitSet bitSet;
  private char[] bitString;

  @BeforeExperiment void setUp() throws Exception {
    bitSet = new BitSet(64);
    bitString = new char[64];
    Random r = new Random();
    for (int n = 0; n < 64; n++) {
      bitString[n] = r.nextBoolean() ? '1' : '0';
    }
  }

  /**
   * This benchmark attempts to measure performance of {@link BitSet#set}.
   */
  @Benchmark int setBitSetX64(int reps) {
    long count = 64L * reps;
    for (int i = 0; i < count; i++) {
      bitSet.set(i & 0x3F, true);
    }
    return bitSet.hashCode();
  }

  /**
   * This benchmark attempts to measure performance of direct bit-manipulation.
   */
  @Benchmark long setMaskX64(int reps) {
    long count = 64L * reps;
    long bitMask = 0L;
    for (int i = 0; i < count; i++) {
      bitMask |= 1 << (i & 0x3F);
    }
    return bitMask;
  }

  /**
   * This benchmark parses a char[] of 1's and 0's into a BitSet. Results from
   * this benchmark should be comparable with those from
   * {@link #charsToMask(int)}.
   */
  @Benchmark String charsToBitSet(int reps) {
    /*
     * This benchmark now measures the complete parsing of a char[] rather than
     * a single invocation of {@link BitSet#set}. However this fine because
     * it is intended to be a comparative benchmark.
     */
    for (int i = 0; i < reps; i++) {
      for (int n = 0; n < bitString.length; n++) {
        bitSet.set(n, bitString[n] == '1');
      }
    }
    return bitSet.toString();
  }

  /**
   * This benchmark parses a char[] of 1's and 0's into a bit mask. Results from
   * this benchmark should be comparable with those from
   * {@link #charsToBitSet(int)}.
   */
  @Benchmark long charsToMask(int reps) {
    /*
     * Comparing results we see a far more realistic sounding result whereby
     * using a bit mask is a little over 4x faster than using BitSet.
     */
    long bitMask = 0;
    for (int i = 0; i < reps; i++) {
      for (int n = 0; n < bitString.length; n++) {
        long m = 1 << n;
        if (bitString[n] == '1') {
          bitMask |= m;
        } else {
          bitMask &= ~m;
        }
      }
    }
    return bitMask;
  }

  /**
   * This benchmark attempts to measure the baseline cost of both
   * {@link #charsToBitSet(int)} and {@link #charsToMask(int)}.
   * It does this by unconditionally summing the character values of the char[].
   * This is as close to a no-op case as we can expect to get without unwanted
   * over-optimization.
   */
  @Benchmark long baselineIteration(int reps) {
    int badHash = 0;
    for (int i = 0; i < reps; i++) {
      for (int n = 0; n < bitString.length; n++) {
        badHash += bitString[n];
      }
    }
    return badHash;
  }
}
