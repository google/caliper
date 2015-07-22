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

package com.google.caliper.runner;

import static com.google.common.base.Preconditions.checkState;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.model.Trial;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

/**
 * A {@link ResultProcessor} that collects all trials in a static list for easy inspection by tests.
 */
public class InMemoryResultsUploader implements ResultProcessor {
  static ImmutableList<Trial> trials() {
    return ImmutableList.copyOf(trials);
  }
  
  private static List<Trial> trials;
  private boolean isClosed;

  @Inject public InMemoryResultsUploader() {
    trials = Lists.newArrayList();
  }

  @Override public void close() throws IOException {
    checkState(!isClosed);
    isClosed = true;
  }

  @Override public void processTrial(Trial trial) {
    checkState(!isClosed);
    trials.add(trial); 
  }
}