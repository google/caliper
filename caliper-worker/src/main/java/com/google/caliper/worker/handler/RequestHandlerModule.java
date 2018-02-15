/*
 * Copyright (C) 2018 Google Inc.
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

package com.google.caliper.worker.handler;

import com.google.caliper.bridge.BenchmarkModelRequest;
import com.google.caliper.bridge.DryRunRequest;
import com.google.caliper.bridge.TrialRequest;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

/**
 * Module that binds the available {@link RequestHandler} implementations and maps them by the type
 * of request they handle.
 *
 * @author Colin Decker
 */
@Module
public abstract class RequestHandlerModule {

  @Binds
  @IntoMap
  @RequestTypeKey(BenchmarkModelRequest.class)
  abstract RequestHandler bindModelHandler(BenchmarkModelHandler handler);

  @Binds
  @IntoMap
  @RequestTypeKey(DryRunRequest.class)
  abstract RequestHandler bindDryRunHandler(DryRunHandler handler);

  @Binds
  @IntoMap
  @RequestTypeKey(TrialRequest.class)
  abstract RequestHandler bindTrialHandler(TrialHandler handler);
}
