/*
 * Copyright (C) 2016 Google Inc.
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

package com.google.caliper.runner.resultprocessor;

import com.google.caliper.model.Trial;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import java.net.URI;
import java.util.UUID;

/** Upload handler that uses a Jackson client. */
final class JacksonUploadHandler implements ResultsUploader.UploadHandler {
  private final Client client;

  JacksonUploadHandler(Client client) {
    this.client = client;
  }

  @Override
  public boolean upload(
      URI uri, String content, String mediaType, Optional<UUID> apiKey, Trial trial) {
    WebResource resource = client.resource(uri);
    if (apiKey.isPresent()) {
      resource = resource.queryParam("key", apiKey.get().toString());
    }
    try {
      resource.type(mediaType).post(content);
      return true;
    } catch (ClientHandlerException e) {
      ResultsUploader.logUploadFailure(trial, e);
    } catch (UniformInterfaceException e) {
      ResultsUploader.logUploadFailure(trial, e);
      ResultsUploader.logger.fine("Failed upload response: " + e.getResponse().getStatus());
    }
    return false;
  }
}
