package com.google.caliper.memory;

import com.google.caliper.memory.ObjectVisitor.Traversal;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * A depth-first object graph explorer. The traversal starts at a root (an 
 * {@code Object}) and explores any other reachable object (recursively) or 
 * primitive value, excluding static fields from the traversal. The traversal 
 * is controlled by a user-supplied {@link ObjectVisitor}, which decides for 
 * each explored path whether to continue exploration of that path, and it can 
 * also return a value at the end of the traversal.
 */
public final class ObjectExplorer {
  private ObjectExplorer() { }

  /**
   * Explores an object graph (defined by a root object and whatever is 
   * reachable through it, following non-static fields) while using an 
   * {@link ObjectVisitor} to both control the traversal and return a value.
   *
   * <p>Equivalent to {@code exploreObject(rootObject, visitor, 
   * EnumSet.noneOf(Feature.class))}.
   *
   * @param <T> the type of the value obtained (after the traversal) by the 
   * ObjectVisitor
   * @param rootObject an object to be recursively explored
   * @param visitor a visitor that is notified for each explored path and
   * decides whether to continue exploration of that path, and constructs a 
   * return value at the end of the exploration
   * @return whatever value is returned by the visitor at the end of the 
   * traversal
   * @see ObjectVisitor
   */
  public static <T> T exploreObject(Object rootObject, ObjectVisitor<T> visitor) {
    return exploreObject(rootObject, visitor, EnumSet.noneOf(Feature.class));
  }

  /**
   * Explores an object graph (defined by a root object and whatever is 
   * reachable through it, following non-static fields) while using an 
   * {@link ObjectVisitor} to both control the traversal and return a value.
   *
   * <p>The {@code features} further customizes the exploration behavior. 
   * In particular:
   * <ul>
   * <li>If {@link Feature#VISIT_PRIMITIVES} is contained in features, 
   * the visitor will also be notified about exploration of primitive values.
   * <li>If {@link Feature#VISIT_NULL} is contained in features, the visitor 
   * will also be notified about exploration of {@code null} values.
   * </ul>
   * In both cases above, the return value of 
   * {@link ObjectVisitor#visit(Chain)} is ignored, since neither primitive 
   * values or {@code null} can be further explored.
   *
   * @param <T> the type of the value obtained (after the traversal) by the 
   * ObjectVisitor
   * @param rootObject an object to be recursively explored
   * @param visitor a visitor that is notified for each explored path 
   * and decides whether to continue exploration of that path, and constructs 
   * a return value at the end of the exploration
   * @param features a set of desired features that the object exploration should have
   * @return whatever value is returned by the visitor at the end of the traversal
   * @see ObjectVisitor
   */
  public static <T> T exploreObject(Object rootObject,
      ObjectVisitor<T> visitor, EnumSet<Feature> features) {
    Deque<Chain> stack = new ArrayDeque<Chain>(32);
    if (rootObject != null) stack.push(Chain.root(rootObject));

    while (!stack.isEmpty()) {
      Chain chain = stack.pop();
      //the only place where the return value of visit() is considered
      Traversal traversal = visitor.visit(chain);
      switch (traversal) {
        case SKIP: continue;
        case EXPLORE: break;
        default: throw new AssertionError();
      }

      //only nonnull values pushed in the stack
      @Nonnull Object value = chain.getValue();
      Class<?> valueClass = value.getClass();
      if (valueClass.isArray()) {
        boolean isPrimitive = valueClass.getComponentType().isPrimitive();
        /*
         * Since we push paths to explore in a stack, we push references found in the array in
         * reverse order, so when we pop them, they will be in the array's order. 
         */
        for (int i = Array.getLength(value) - 1; i >= 0; i--) {
          Object childValue = Array.get(value, i);
          if (isPrimitive) {
            if (features.contains(Feature.VISIT_PRIMITIVES)) {
              visitor.visit(chain.appendArrayIndex(i, childValue));
            }
          } else if (childValue == null) {
            if (features.contains(Feature.VISIT_NULL)) {
              visitor.visit(chain.appendArrayIndex(i, childValue));
            }
          } else {
            stack.push(chain.appendArrayIndex(i, childValue));
          }
        }
      } else {
        /*
         * Reflection usually provides fields in declaration order. As above in arrays, we push
         * them to the stack in reverse order, so when we pop them, we get them in the original 
         * (declaration) order. 
         */
        for (Field field : Lists.reverse(getAllFields(value))) {
          Object childValue = null;
          try {
            childValue = field.get(value);
          } catch (Exception e) {
            throw new AssertionError(e);
          }
          if (childValue == null) { // handling nulls
            if (features.contains(Feature.VISIT_NULL)) {
              visitor.visit(chain.appendField(field, childValue));
            }
          } else { // handling primitives or references
            boolean isPrimitive = field.getType().isPrimitive();
            Chain extendedChain = chain.appendField(field, childValue);
            if (isPrimitive) {
              if (features.contains(Feature.VISIT_PRIMITIVES)) {
                visitor.visit(extendedChain);
              }
            } else {
              stack.push(extendedChain);
            }
          }
        }
      }
    }
    return visitor.result();
  }

  /**
   * A stateful predicate that allows exploring an object (the tail of the chain) only once.
   */
  static class AtMostOncePredicate implements Predicate<Chain> {
    private final Set<Object> seen = Collections.newSetFromMap(
        new IdentityHashMap<Object, Boolean>());

    public boolean apply(Chain chain) {
      return seen.add(chain.getValue());
    }
  }

  static final Predicate<Chain> notEnumFieldsOrClasses = new Predicate<Chain>() {
    public boolean apply(Chain chain) {
      return !(Enum.class.isAssignableFrom(chain.getValueType())
          || chain.getValue() instanceof Class<?>);
    }
  };

  static final Function<Chain, Object> chainToObject =
      new Function<Chain, Object>() {
    public Object apply(Chain chain) {
      return chain.getValue();
    }
  };

  private static List<Field> getAllFields(Object o) {
    List<Field> fields = Lists.newArrayListWithCapacity(8);
    Class<?> clazz = o.getClass();
    while (clazz != null) {
      for (Field field : Arrays.asList(clazz.getDeclaredFields())) {
        // add only non-static fields
        if (!Modifier.isStatic(field.getModifiers())) {
          fields.add(field); 
        }
      }
      clazz = clazz.getSuperclass();
    }

    //all together so there is only one security check
    AccessibleObject.setAccessible(fields.toArray(new AccessibleObject[fields.size()]), true);
    return fields;
  }

  /**
   * Enumeration of features that may be optionally requested for an object 
   * traversal.
   *
   * @see ObjectExplorer#exploreObject(Object, ObjectVisitor, EnumSet)
   */
  public enum Feature {
    /**
     * Null references should be visited.
     */
    VISIT_NULL,

    /**
     * Primitive values should be visited.
     */
    VISIT_PRIMITIVES
  }
}
