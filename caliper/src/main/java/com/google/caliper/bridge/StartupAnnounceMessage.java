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

package com.google.caliper.bridge;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * A message sent from the worker to the runner immediately after startup to identify the trial
 * that it is performing.
 */
public final class StartupAnnounceMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  private final UUID trialId;

  public StartupAnnounceMessage(UUID trialId) {
    this.trialId = checkNotNull(trialId);
  }

  public UUID trialId() {
    return trialId;
  }

  @Override public int hashCode() {
    return trialId.hashCode();
  }

  @Override public boolean equals(Object obj) {
    return obj instanceof StartupAnnounceMessage
        && trialId.equals(((StartupAnnounceMessage) obj).trialId);
  }
}
