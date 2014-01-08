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

package com.google.caliper.model;

import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

import java.util.Map;
import java.util.Map.Entry;

/**
 * A simple funnel that inserts string map entries into a funnel in iteration order.
 */
enum StringMapFunnel implements Funnel<Map<String, String>> {
  INSTANCE;

  @Override
  public void funnel(Map<String, String> from, PrimitiveSink into) {
    for (Entry<String, String> entry : from.entrySet()) {
      into.putUnencodedChars(entry.getKey())
          .putByte((byte) -1) // separate key and value
          .putUnencodedChars(entry.getValue());
    }
  }
}
