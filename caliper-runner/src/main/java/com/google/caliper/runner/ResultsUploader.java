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

package com.google.caliper.runner;

import static java.util.logging.Level.SEVERE;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.model.Trial;
import com.google.caliper.runner.config.InvalidConfigurationException;
import com.google.caliper.runner.config.ResultProcessorConfig;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * {@link ResultProcessor} implementation that uploads the JSON-serialized results to the Caliper
 * webapp.
 */
abstract class ResultsUploader implements ResultProcessor {
  static final Logger logger = Logger.getLogger(ResultsUploader.class.getName());
  private static final String POST_PATH = "/data/trials";
  private static final String RESULTS_PATH_PATTERN = "/runs/%s";

  private final PrintWriter stdout;
  private final UploadHandler handler;
  private final Gson gson;
  private final Optional<UUID> apiKey;
  private final Optional<URI> uploadUri;
  private Optional<UUID> runId = Optional.absent();
  private boolean failure = false;

  ResultsUploader(
      PrintWriter stdout,
      Gson gson,
      UploadHandler handler,
      ResultProcessorConfig resultProcessorConfig)
      throws InvalidConfigurationException {
    this.stdout = stdout;
    this.handler = handler;
    this.gson = gson;
    @Nullable String apiKeyString = resultProcessorConfig.options().get("key");
    Optional<UUID> apiKey = Optional.absent();
    if (Strings.isNullOrEmpty(apiKeyString)) {
      logger.info("No api key specified. Uploading results anonymously.");
    } else {
      try {
        apiKey = Optional.of(UUID.fromString(apiKeyString));
      } catch (IllegalArgumentException e) {
        throw new InvalidConfigurationException(
            String.format(
                "The specified API key (%s) is not valid. API keys are UUIDs and should look like %s.",
                apiKeyString, new UUID(0L, 0L)));
      }
    }
    this.apiKey = apiKey;

    @Nullable String urlString = resultProcessorConfig.options().get("url");
    if (Strings.isNullOrEmpty(urlString)) {
      logger.info("No upload URL was specified. Results will not be uploaded.");
      this.uploadUri = Optional.absent();
    } else {
      try {
        this.uploadUri = Optional.of(new URI(urlString).resolve(POST_PATH));
      } catch (URISyntaxException e) {
        throw new InvalidConfigurationException(urlString + " is an invalid upload url", e);
      }
    }
  }

  @Override
  public final void processTrial(Trial trial) {
    if (uploadUri.isPresent()) {
      boolean succeeded = false;
      try {
        String json = gson.toJson(ImmutableList.of(trial));
        succeeded =
            handler.upload(uploadUri.get(), json, MediaType.JSON_UTF_8.toString(), apiKey, trial);
      } finally {
        failure |= !succeeded;
      }

      if (succeeded) {
        // only set the run id if a result has been successfully uploaded
        runId = Optional.of(trial.run().id());
      }
    }
  }

  static void logUploadFailure(Trial trial, Exception e) {
    logger.log(
        SEVERE,
        String.format("Could not upload trial %s. Consider uploading it manually.", trial.id()),
        e);
  }

  @Override
  public final void close() {
    if (uploadUri.isPresent()) {
      if (runId.isPresent()) {
        stdout.printf(
            "Results have been uploaded. View them at: %s%n",
            uploadUri.get().resolve(String.format(RESULTS_PATH_PATTERN, runId.get())));
      }
      if (failure) {
        // TODO(gak): implement some retry
        stdout.println("Some trials failed to upload. Consider uploading them manually.");
      }
    } else {
      logger.fine("No upload URL was provided, so results were not uploaded.");
    }
  }

  /** Simple handler for uploading data to a given URI. */
  public interface UploadHandler {
    /**
     * Uploads the given content with the given media type to the given URI. May log errors with the
     * given logger.
     *
     * @return {@code true} if the upload succeeded; {@code false} otherwise.
     */
    boolean upload(URI uri, String content, String mediaType, Optional<UUID> apiKey, Trial trial);
  }
}
