/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.caliper.memory;

import com.google.caliper.memory.ObjectExplorer.Feature;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;

import java.util.EnumSet;

/**
 * A tool that can qualitatively measure the footprint
 * ({@literal e.g.}, number of objects, references,
 * primitives) of a graph structure.
 */
public final class ObjectGraphMeasurer {
  /**
   * The footprint of an object graph.
   */
  public final static class Footprint {
    private final int objects;
    private final int nonNullRefs;
    private final int nullRefs;
    private final ImmutableMultiset<Class<?>> primitives;

    private static final ImmutableSet<Class<?>> primitiveTypes = ImmutableSet.<Class<?>>of(
        boolean.class, byte.class, char.class, short.class,
        int.class, float.class, long.class, double.class);

    /**
     * Constructs a Footprint, by specifying the number of objects,
     * references, and primitives (represented as a {@link Multiset}).
     *
     * @param objects the number of objects
     * @param nonNullRefs the number of non-null references
     * @param nullRefs the number of null references
     * @param primitives the number of primitives (represented by the
     * respective primitive classes, e.g. {@code int.class} etc)
     */
    public Footprint(int objects, int nonNullRefs, int nullRefs,
        Multiset<Class<?>> primitives) {
      Preconditions.checkArgument(objects >= 0, "Negative number of objects");
      Preconditions.checkArgument(nonNullRefs >= 0, "Negative number of references");
      Preconditions.checkArgument(nullRefs >= 0, "Negative number of references");
      Preconditions.checkArgument(primitiveTypes.containsAll(primitives.elementSet()),
          "Unexpected primitive type");
      this.objects = objects;
      this.nonNullRefs = nonNullRefs;
      this.nullRefs = nullRefs;
      this.primitives = ImmutableMultiset.copyOf(primitives);
    }

    /**
     * Returns the number of objects of this footprint.
     */
    public int getObjects() {
      return objects;
    }

    /**
     * Returns the number of non-null references of this footprint.
     */
    public int getNonNullReferences() {
      return nonNullRefs;
    }

    /**
     * Returns the number of null references of this footprint.
     */
    public int getNullReferences() {
      return nullRefs;
    }

    /**
     * Returns the number of all references (null and non-null) of this footprint.
     */
    public int getAllReferences() {
      return nonNullRefs + nullRefs;
    }

    /**
     * Returns the number of primitives of this footprint
     * (represented by the respective primitive classes,
     * {@literal e.g.} {@code int.class} etc).
     */
    public ImmutableMultiset<Class<?>> getPrimitives() {
      return primitives;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getClass().getName(),
          objects, nonNullRefs, nullRefs, primitives);
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Footprint) {
        Footprint that = (Footprint) o;
        return this.objects == that.objects
            && this.nonNullRefs == that.nonNullRefs
            && this.nullRefs == that.nullRefs
            && this.primitives.equals(that.primitives);
      }
      return false;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("Objects", objects)
          .add("NonNullRefs", nonNullRefs)
          .add("NullRefs", nullRefs)
          .add("Primitives", primitives)
          .toString();
    }
  }

  /**
   * Measures the footprint of the specified object graph.
   * The object graph is defined by a root object and whatever object can be
   * reached through that, excluding static fields, {@code Class} objects,
   * and fields defined in {@code enum}s (all these are considered shared
   * values, which should not contribute to the cost of any single object
   * graph).
   *
   * <p>Equivalent to {@code measure(rootObject, Predicates.alwaysTrue())}.
   *
   * @param rootObject the root object of the object graph
   * @return the footprint of the object graph
   */
  public static Footprint measure(Object rootObject) {
    return measure(rootObject, Predicates.alwaysTrue());
  }

  /**
   * Measures the footprint of the specified object graph.
   * The object graph is defined by a root object and whatever object can be
   * reached through that, excluding static fields, {@code Class} objects,
   * and fields defined in {@code enum}s (all these are considered shared
   * values, which should not contribute to the cost of any single object
   * graph), and any object for which the user-provided predicate returns
   * {@code false}.
   *
   * @param rootObject the root object of the object graph
   * @param objectAcceptor a predicate that returns {@code true} for objects
   * to be explored (and treated as part of the footprint), or {@code false}
   * to forbid the traversal to traverse the given object
   * @return the footprint of the object graph
   */
  public static Footprint measure(Object rootObject, Predicate<Object> objectAcceptor) {
    Preconditions.checkNotNull(objectAcceptor, "predicate");

    Predicate<Chain> completePredicate = Predicates.and(ImmutableList.of(
        ObjectExplorer.notEnumFieldsOrClasses,
        new ObjectExplorer.AtMostOncePredicate(),
        Predicates.compose(objectAcceptor, ObjectExplorer.chainToObject)
    ));

    return ObjectExplorer.exploreObject(rootObject, new ObjectGraphVisitor(completePredicate),
        EnumSet.of(Feature.VISIT_PRIMITIVES, Feature.VISIT_NULL));
  }

  private static class ObjectGraphVisitor implements ObjectVisitor<Footprint> {
    private int objects;
    // -1 to account for the root, which has no reference leading to it
    private int nonNullReferences = -1;
    private int nullReferences = 0;
    private final Multiset<Class<?>> primitives = HashMultiset.create();
    private final Predicate<Chain> predicate;

    ObjectGraphVisitor(Predicate<Chain> predicate) {
      this.predicate = predicate;
    }

    @Override public Traversal visit(Chain chain) {
      if (chain.isPrimitive()) {
        primitives.add(chain.getValueType());
        return Traversal.SKIP;
      } else {
        if (chain.getValue() == null) {
          nullReferences++;
        } else {
          nonNullReferences++;
        }
      }
      if (predicate.apply(chain) && chain.getValue() != null) {
        objects++;
        return Traversal.EXPLORE;
      }
      return Traversal.SKIP;
    }

    @Override public Footprint result() {
      return new Footprint(objects, nonNullReferences, nullReferences,
          ImmutableMultiset.copyOf(primitives));
    }
  }

  private ObjectGraphMeasurer() {}
}
