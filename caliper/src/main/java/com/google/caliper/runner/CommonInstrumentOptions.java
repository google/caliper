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

package com.google.caliper.runner;

/**
 * Instrument option identifiers shared between instruments.
 */
final class CommonInstrumentOptions {
  private CommonInstrumentOptions() {}

  static final String MEASUREMENTS_OPTION = "measurements";
  static final String GC_BEFORE_EACH_OPTION = "gcBeforeEach";
  static final String WARMUP_OPTION = "warmup";
  static final String MAX_WARMUP_WALL_TIME_OPTION = "maxWarmupWallTime";
}
