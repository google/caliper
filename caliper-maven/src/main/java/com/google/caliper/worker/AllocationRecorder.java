/*
 * Copyright (C) 2013 Google Inc.
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
package com.google.caliper.worker;

/**
 * An object that records all allocations that occur between {@link #startRecording()} 
 * and {@link #stopRecording(int)}.
 * 
 * <p>This object can accurately track allocations made from multiple threads but is only 
 * expected to have {@link #startRecording()} and {@link #stopRecording(int)} called by a single 
 * thread.
 */
abstract class AllocationRecorder {
  private boolean firstTime = true;
  
  /** 
   * Clears the prior state and starts a new recording.
   * 
   * @throws IllegalStateException if the recording infrastructure is misconfigured.
   */
  final void startRecording() {
    if (firstTime) {
      Object obj;
      doStartRecording();
      obj = new Object();
      AllocationStats stats = stopRecording(1);
      if (stats.getAllocationCount() != 1 || stats.getAllocationSize() < 1) {
        throw new IllegalStateException(
            String.format("The allocation recording infrastructure appears to be broken. "
                + "Expected to find exactly one allocation of a java/lang/Object instead found %s",
                stats));
      }
      firstTime = false;
    }
    doStartRecording();
  }
  
  /** Clears the prior state and starts a new recording. */
  protected abstract void doStartRecording();
  
  /**
   * Stops recording allocations and saves all the allocation data recorded since the previous call
   * to {@link #startRecording()} to an {@link AllocationStats} object.
   * 
   * @param reps The number of reps that the previous set of allocation represents.
   */
  abstract AllocationStats stopRecording(int reps);
}
