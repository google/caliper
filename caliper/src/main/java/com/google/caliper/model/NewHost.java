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

package com.google.caliper.model;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

/**
 * The performance-informing properties of the host on which a benchmark is run.
 *
 * @author gak@google.com (Gregory Kick)
 */
@Immutable
public final class NewHost {
  private final ImmutableMap<String, String> properties;

  private NewHost(Builder builder) {
    this.properties = builder.propertiesBuilder.build();
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof NewHost) {
      NewHost that = (NewHost) obj;
      return this.properties.equals(that.properties);
    } else {
      return false;
    }
  }

  @Override public int hashCode() {
    return Objects.hashCode(properties);
  }

  @Override public String toString() {
    return Objects.toStringHelper(this)
        .add("properties", properties)
        .toString();
  }

  public static final class Builder {
    private final ImmutableMap.Builder<String, String> propertiesBuilder = ImmutableMap.builder();

    public Builder addProperty(String key, String value) {
      propertiesBuilder.put(key, value);
      return this;
    }

    public Builder addAllProperies(Map<String, String> properties) {
      propertiesBuilder.putAll(properties);
      return this;
    }

    public NewHost build() {
      return new NewHost(this);
    }
  }
}
