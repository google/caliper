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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A microbenchmark that tests the performance of contains() on various Set
 * implementations.
 *
 * @author Kevin Bourrillion
 */
public class SetContainsBenchmark extends SimpleBenchmark {
  @Param({"Hash", "Immutable"}) private Impl impl;

  // So far, this is the best way to test various implementations
  public enum Impl {
    Hash {
      @Override Set<Element> create(Collection<Element> contents) {
        return new HashSet<Element>(contents);
      }
    },
    LinkedHash {
      @Override Set<Element> create(Collection<Element> contents) {
        return new LinkedHashSet<Element>(contents);
      }
    },
    UnmodHS {
      @Override Set<Element> create(Collection<Element> contents) {
        return Collections.unmodifiableSet(new HashSet<Element>(contents));
      }
    },
    SyncHS {
      @Override Set<Element> create(Collection<Element> contents) {
        return Collections.synchronizedSet(new HashSet<Element>(contents));
      }
    },

    // Kind of cheating here -- Caliper just happens to bundle Google Collections so I'm testing
    // this from it; this might not work at the command line since GC are jarjar'd for caliper.jar
    Immutable {
      @Override Set<Element> create(Collection<Element> contents) {
        return ImmutableSet.copyOf(contents);
      }
    };

    abstract Set<Element> create(Collection<Element> contents);
  }

  @Param private int size;
  public static final Collection<Integer> sizeValues = Arrays.asList(
      (1<<3) - 1, // 7
      (1<<3),
      (1<<6) - 1,
      (1<<6),
      (1<<10) - 1,
      (1<<10),
      (1<<15) - 1,
      (1<<15)
  );

  @Param({"0.2", "0.8"}) private double hitRate;

  @Param({"true", "false"}) // TODO: that should be assumed
  private boolean isUserTypeFast;

  // "" means no fixed seed
  @Param("") private SpecialRandom random;

  // the following must be set during setUp
  private Element[] queries;
  private Set<Element> setToTest;

  @Override public void setUp() {
    Set<Element> valuesInSet = createData();
    this.queries = createQueries(valuesInSet, 1024);
    this.setToTest = impl.create(valuesInSet);
  }

  private Element[] createQueries(Set<Element> elementsInSet, int numQueries) {
    List<Element> queryList = Lists.newArrayListWithCapacity(numQueries);

    int numGoodQueries = (int) (numQueries * hitRate + 0.5);

    // add good queries
    int size = elementsInSet.size();
    int minCopiesOfEachGoodQuery = numGoodQueries / size;
    int extras = numGoodQueries % size;

    for (int i = 0; i < minCopiesOfEachGoodQuery; i++) {
      queryList.addAll(elementsInSet);
    }
    List<Element> tmp = Lists.newArrayList(elementsInSet);
    Collections.shuffle(tmp, random);
    queryList.addAll(tmp.subList(0, extras));

    // now add bad queries
    while (queryList.size() < numQueries) {
      Element candidate = newElement();
      if (!elementsInSet.contains(candidate)) {
        queryList.add(candidate);
      }
    }
    Collections.shuffle(queryList, random);
    return queryList.toArray(new Element[0]);
  }

  private Set<Element> createData() {
    Set<Element> set = Sets.newHashSetWithExpectedSize(size);
    while (set.size() < size) {
      set.add(newElement());
    }
    return set;
  }

  private Element newElement() {
    int value = random.nextInt();
    return isUserTypeFast
        ? new Element(value)
        : new SlowElement(value);
  }

  public boolean timeContains(int reps) {
    // Paranoia: acting on hearsay that accessing fields might be slow
    // Should write a benchmark to test that!
    Set<Element> set = setToTest;
    Element[] queries = this.queries;

    // Allows us to use & instead of %, acting on hearsay that division operators (/%) are
    // disproportionately expensive; should test this too!
    int mask = queries.length - 1;

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

  private static class Element implements Comparable<Element> {
    final int hash;
    Element(int hash) {
      this.hash = hash;
    }
    @Override public boolean equals(Object obj) {
      return this == obj
          || (obj instanceof Element && ((Element) obj).hash == hash);
    }
    @Override public int hashCode() {
      return hash;
    }
    public int compareTo(Element that) {
      return (hash < that.hash) ? -1 : (hash > that.hash) ? 1 : 0;
    }
    @Override public String toString() {
      return String.valueOf(hash);
    }
  }

  private static class SlowElement extends Element {
    private SlowElement(int hash) {
      super(hash);
    }
    @Override public boolean equals(Object obj) {
      return slowItDown() != 1 && super.equals(obj);
    }
    @Override public int hashCode() {
      return slowItDown() + hash;
    }
    static int slowItDown() {
      int result = 0;
      for (int i = 1; i <= 1000; i++) {
        result += i;
      }
      return result;
    }
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
