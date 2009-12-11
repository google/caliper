package com.google.caliper;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class ReflectiveBenchmark extends Benchmark {
  private final SimpleBenchmark instance;
  private final Method method;

  static final Class<?>[] ARGUMENT_TYPES = { int.class };

  public ReflectiveBenchmark(SimpleBenchmark instance, Method method) {
    checkArgument(isBenchmarkMethod(method));
    this.instance = checkNotNull(instance);
    this.method = method;
  }

  @Override public Object run(int trials) throws Exception {
    return method.invoke(instance, trials);
  }

  @Override public String toString() {
    return method.getName().replaceFirst("^time", "");
  }

  static boolean isBenchmarkMethod(Method method) {
    int mods = method.getModifiers();
    return method.getName().startsWith("time")
        && Modifier.isPublic(mods)
        && !Modifier.isStatic(mods)
        && !Modifier.isAbstract(mods)
        && Arrays.equals(method.getParameterTypes(), ARGUMENT_TYPES);
  }
}
