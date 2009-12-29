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

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

/**
 * A microbenchmark that tests the performance of contains() on various Set
 * implementations.
 *
 * @author Kevin Bourrillion
 */
public class SetContainsBenchmark extends SimpleBenchmark {

  // So far, this is the best way to test various implementations

  @Param private Impl impl;

  public enum Impl {
    HashSet {
      @Override Set<Integer> create(Collection<Integer> contents) {
        return new HashSet<Integer>(contents);
      }
    },
    LinkedHashSet {
      @Override Set<Integer> create(Collection<Integer> contents) {
        return new LinkedHashSet<Integer>(contents);
      }
    },
    UnmodifiableHashSet {
      @Override Set<Integer> create(Collection<Integer> contents) {
        return Collections.unmodifiableSet(new HashSet<Integer>(contents));
      }
    },
    SynchronizedHashSet {
      @Override Set<Integer> create(Collection<Integer> contents) {
        return Collections.synchronizedSet(new HashSet<Integer>(contents));
      }
    },
    ;

    abstract Set<Integer> create(Collection<Integer> contents);
  }

  // a range of sizes that are different multiples of their nearest power of 2
  @Param({"2", "18", "160", "1400", "12600" /*, "112000" */})
  private int size;

  // "" means no fixed seed
  @Param("") private SpecialRandom random;

  // the following must be set during setUp
  private Integer[] queries;
  private Set<Integer> setToTest;

  // Queries are just sequential integers. Since the contents of the set were
  // chosen randomly, this shouldn't cause any undue bias.
  @Override public void setUp() {
    this.queries = new Integer[size * 2];
    for (int i = 0; i < size * 2; i++) {
      queries[i] = i;
    }
    Collections.shuffle(Arrays.asList(queries), random);

    setToTest = impl.create(createData());
  }

  private Collection<Integer> createData() {
    Set<Integer> tempSet = new HashSet<Integer>(size * 3 / 2);

    // Choose 50% of the numbers between 0 and max to be in the set; thus we
    // are measuring performance of contains() when there is a 50% hit rate
    int max = size * 2;
    while (tempSet.size() < size) {
      tempSet.add(random.nextInt(max));
    }
    return tempSet;
  }

  public boolean timeContains(int reps) {
    boolean dummy = false;
    int numQueries = size * 2;
    for (int i = 0; i < reps; i++) {
      // TODO: this % may be too expensive...
      dummy ^= setToTest.contains(queries[i % numQueries]);
    }
    return dummy;
  }

  // TODO: remove this from all examples when IDE plugins are ready
  public static void main(String[] args) throws Exception {
    Runner.main(SetContainsBenchmark.class, args);
  }

  // Just an experiment with a slightly nicer way to create Randoms for benchies

  public static class SpecialRandom extends Random {
    public static SpecialRandom valueOf(String s) {
      return (s.length() == 0)
          ? new SpecialRandom()
          : new SpecialRandom(Long.parseLong(s));
    }

    private final boolean hasSeed;
    private final long seed;

    public SpecialRandom() {
      this.hasSeed = false;
      this.seed = 0;
    }

    public SpecialRandom(long seed) {
      super(seed);
      this.hasSeed = true;
      this.seed = seed;
    }

    @Override public String toString() {
      return hasSeed ? "(seed:" + seed : "(default seed)";
    }

    private static final long serialVersionUID = 0;
  }
}
