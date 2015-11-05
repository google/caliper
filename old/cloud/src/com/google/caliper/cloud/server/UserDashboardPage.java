/**
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

package com.google.caliper.cloud.server;

public final class UserDashboardPage {

  private final String benchmarkOwner;

  UserDashboardPage(String benchmarkOwner) {
    this.benchmarkOwner = benchmarkOwner;
  }

  public String getBenchmarkOwner() {
    return benchmarkOwner;
  }

  /**
   * Returns benchmark owner's email with everything after and including the @ stripped off.
   */
  public String getBenchmarkOwnerName() {
    int atPosition = benchmarkOwner.indexOf('@');
    if (atPosition != -1) {
      return benchmarkOwner.substring(0, atPosition);
    }
    return benchmarkOwner;
  }
}
