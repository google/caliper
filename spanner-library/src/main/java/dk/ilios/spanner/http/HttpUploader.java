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

package dk.ilios.spanner.http;

import com.google.gson.Gson;

import com.sun.jersey.api.client.Client;

import dk.ilios.spanner.SpannerConfig;
import dk.ilios.spanner.config.InvalidConfigurationException;
import dk.ilios.spanner.config.ResultProcessorConfig;
import dk.ilios.spanner.log.StdOut;
import dk.ilios.spanner.output.ResultProcessor;

/**
 * A {@link ResultProcessor} implementation that publishes results to the webapp using an HTTP
 * request.
 */
public class HttpUploader extends CaliperResultsUploader {

    public HttpUploader(StdOut stdout, Gson gson, SpannerConfig config) throws InvalidConfigurationException {
        super(stdout, gson, Client.create(), getConfig(config));
    }

    private static ResultProcessorConfig getConfig(SpannerConfig config) {
        return new ResultProcessorConfig.Builder()
                .addOption("key", config.getApiKey())
                .addOption("url", config.getUploadUrl().toString())
                .className(HttpUploader.class.getName())
                .build();


    }
}
