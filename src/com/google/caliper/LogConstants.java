/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.caliper;

public final class LogConstants {
  /**
   * Must be prepended to line of XML that represents normalized scenario.
   */
  public static final String SCENARIO_XML_PREFIX = "[scenario] ";

  /**
   * Must be prepended to any logs that are to be included in the run event log.
   */
  public static final String CALIPER_LOG_PREFIX = "[caliper] ";

  public static final String SCENARIOS_STARTING = "[starting scenarios]";
  public static final String STARTING_SCENARIO_PREFIX = "[starting scenario] ";
  public static final String MEASUREMENT_PREFIX = "[scenario finished] ";
  public static final String SCENARIOS_FINISHED = "[scenarios finished]";

  /**
   * All events will be logged from when {@code TIMED_SECTION_STARTING} is logged until
   * {@code TIMED_SECTION_DONE} is logged.
   */
  public static final String TIMED_SECTION_STARTING = "[starting timed section]";
  public static final String TIMED_SECTION_DONE = "[done timed section]";

  private LogConstants() {}
}
