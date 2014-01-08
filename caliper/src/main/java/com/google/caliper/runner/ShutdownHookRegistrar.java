package com.google.caliper.runner;

import com.google.inject.ImplementedBy;

/**
 * A simple interface for registering and deregistering shutdown hooks.
 */
@ImplementedBy(RuntimeShutdownHookRegistrar.class)
interface ShutdownHookRegistrar {
  /**
   * Adds a hook to run at process shutdown.
   * 
   * <p>See {@link Runtime#addShutdownHook(Thread)}.
   */
  void addShutdownHook(Thread hook);
  /**
   * Removes a shutdown hook that was previously registered via {@link #addShutdownHook(Thread)}.
   * 
   * <p>See {@link Runtime#removeShutdownHook(Thread)}.
   */
  boolean removeShutdownHook(Thread hook);
}

