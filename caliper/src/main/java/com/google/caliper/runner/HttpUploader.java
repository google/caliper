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

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.config.CaliperConfig;
import com.google.caliper.config.InvalidConfigurationException;
import com.google.caliper.util.Stdout;
import com.google.gson.Gson;

import com.sun.jersey.api.client.Client;

import java.io.PrintWriter;

import javax.inject.Inject;

/**
 * A {@link ResultProcessor} implementation that publishes results to the webapp using an HTTP
 * request.
 */
public class HttpUploader extends ResultsUploader {
  @Inject HttpUploader(@Stdout PrintWriter stdout, Gson gson, CaliperConfig config)
      throws InvalidConfigurationException {
    super(stdout, gson, Client.create(), config.getResultProcessorConfig(HttpUploader.class));
  }
}
