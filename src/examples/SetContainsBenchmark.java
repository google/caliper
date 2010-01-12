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
import com.google.common.collect.ImmutableSet;
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
  @Param private Impl impl;

  // So far, this is the best way to test various implementations
  public enum Impl {
    Hash {
      @Override Set<Integer> create(Collection<Integer> contents) {
        return new HashSet<Integer>(contents);
      }
    },
    LinkedHash {
      @Override Set<Integer> create(Collection<Integer> contents) {
        return new LinkedHashSet<Integer>(contents);
      }
    },
    UnmodHS {
      @Override Set<Integer> create(Collection<Integer> contents) {
        return Collections.unmodifiableSet(new HashSet<Integer>(contents));
      }
    },
    SyncHS {
      @Override Set<Integer> create(Collection<Integer> contents) {
        return Collections.synchronizedSet(new HashSet<Integer>(contents));
      }
    },

    // Kind of cheating here -- Caliper just happens to bundle Google Collections so I'm testing
    // this from it; this might not work at the command line since GC are jarjar'd for caliper.jar
    Immutable {
      @Override Set<Integer> create(Collection<Integer> contents) {
        return ImmutableSet.copyOf(contents);
      }
    };

    abstract Set<Integer> create(Collection<Integer> contents);
  }

  @Param private int size;
  public static final Collection<Integer> sizeValues = Arrays.asList(
      (1<<2) - 1,
      (1<<2),
      (1<<6) - 1,
      (1<<6),
      (1<<10) - 1,
      (1<<10),
      (1<<14) - 1,
      (1<<14),
      (1<<18) - 1,
      (1<<18)
  );

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
    // Paranoia: acting on hearsay that accessing fields might be slow
    // Should write a benchmark to test that!
    Set<Integer> set = setToTest;
    Integer[] queries = this.queries;

    // Allows us to use & instead of %, acting on hearsay that division operators (/%) are
    // disproportionately expensive; should test this too!
    int mask = Integer.highestOneBit(size * 2) - 1;

    boolean dummy = false;
    for (int i = 0; i < reps; i++) {
      dummy ^= set.contains(queries[i & mask]);
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
