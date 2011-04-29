/*
 * Copyright (C) 2011 Google Inc.
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

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Note: classes in this package are deliberately quick-and-dirty and minimal, and may be upgraded
 * to be a little more robust in the future.
 */
public class CaliperData {
  public List<Environment> environments = Lists.newArrayList();
  public List<VM> vms = Lists.newArrayList();
  public List<Run> runs = Lists.newArrayList();
  public List<Instrument> instruments = Lists.newArrayList();
  public List<Scenario> scenarios = Lists.newArrayList();
  public List<Result> results = Lists.newArrayList();
}
