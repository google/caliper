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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;


/**
 * A message containing a selection of {@link System#getProperties() system properties} from the
 * worker JVM.
 */
public class VmPropertiesLogMessage extends CaliperControlLogMessage {
  private static final String MESSAGE_PREFIX = CONTROL_PREFIX + "properties//";

  public static final class Parser implements TryParser<VmPropertiesLogMessage>,
      Renderer<VmPropertiesLogMessage> {
    private final Gson gson;

    @Inject Parser(Gson gson) {
      this.gson = gson;
    }

    @Override public Optional<VmPropertiesLogMessage> tryParse(String text) {
      return text.startsWith(MESSAGE_PREFIX)
          ? Optional.of(new VmPropertiesLogMessage(
              gson.<ImmutableMap<String, String>>fromJson(text.substring(MESSAGE_PREFIX.length()),
                  new TypeToken<ImmutableMap<String, String>>() {}.getType())))
          : Optional.<VmPropertiesLogMessage>absent();
    }

    @Override public String render(VmPropertiesLogMessage message) {
      return MESSAGE_PREFIX + gson.toJson(message.properties);
    }
  }

  private final ImmutableMap<String, String> properties;

  public VmPropertiesLogMessage(ImmutableMap<String, String> properties) {
    this.properties = checkNotNull(properties);
  }

  public ImmutableMap<String, String> properties() {
    return properties;
  }

  @Override public void accept(LogMessageVisitor visitor) {
    visitor.visit(this);
  }
}
