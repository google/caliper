package com.google.caliper.runner;

/**
 * A {@link ShutdownHookRegistrar} that delegates to {@link Runtime}.
 */
class RuntimeShutdownHookRegistrar implements ShutdownHookRegistrar {
  @Override public void addShutdownHook(Thread hook) {
    Runtime.getRuntime().addShutdownHook(hook);
  }
  
  @Override public boolean removeShutdownHook(Thread hook) {
    return Runtime.getRuntime().removeShutdownHook(hook);
  }
}
