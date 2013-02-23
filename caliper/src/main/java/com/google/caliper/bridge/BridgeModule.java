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

package com.google.caliper.bridge;

import com.google.caliper.util.Parser;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * Bindings for {@link Parser parsers} and {@link Renderer renderers} for
 * {@link com.google.caliper.model model} classes.
 */
public final class BridgeModule extends AbstractModule {
  @Override protected void configure() {
    requireBinding(Gson.class);
  }

  @Provides Parser<LogMessage> provideLogMessageParser(LogMessageParser parser) {
    return parser;
  }

  @Provides Renderer<CaliperControlLogMessage> provideControlLogMessageRenderer(
      ControlLogMessageRenderer renderer) {
    return renderer;
  }
}
