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
interface AllocationRecorder {

  /** Clears the prior state and starts a new recording. */
  void startRecording();
  
  /**
   * Stops recording allocations and saves all the allocation data recorded since the previous call
   * to {@link #startRecording()} to an {@link AllocationStats} object.
   * 
   * @param reps The number of reps that the previous set of allocation represents.
   */
  AllocationStats stopRecording(int reps);
}
