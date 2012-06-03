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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

/**
 * A specification by which a benchmark method invocation can be uniquely identified.
 *
 * @author gak@google.com (Gregory Kick)
 */
@Immutable
public final class NewBenchmarkSpec {
  private final String className;
  // TODO(gak): possibly worry about overloads
  private final String methodName;
  private final ImmutableMap<String, String> parameters;

  private NewBenchmarkSpec(Builder builder) {
    this.className = builder.className;
    this.methodName = builder.methodName;
    this.parameters = builder.parametersBuilder.build();
  }

  public String className() {
    return className;
  }

  public String methodName() {
    return methodName;
  }

  public ImmutableMap<String, String> parameters() {
    return parameters;
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof NewBenchmarkSpec) {
      NewBenchmarkSpec that = (NewBenchmarkSpec) obj;
      return this.className.equals(that.className)
          && this.methodName.equals(that.methodName)
          && this.parameters.equals(parameters);
    } else {
      return false;
    }
  }

  @Override public int hashCode() {
    return Objects.hashCode(className, methodName, parameters);
  }

  public static final class Builder {
    private String className;
    private String methodName;
    private final ImmutableMap.Builder<String, String> parametersBuilder = ImmutableMap.builder();

    public Builder className(String className) {
      this.className = checkNotNull(className);
      return this;
    }

    public Builder methodName(String methodName) {
      this.methodName = checkNotNull(methodName);
      return this;
    }

    public Builder addParameter(String parameterName, String value) {
      this.parametersBuilder.put(checkNotNull(parameterName), checkNotNull(value));
      return this;
    }

    public Builder addAllParameters(Map<String, String> parameters) {
      this.parametersBuilder.putAll(parameters);
      return this;
    }

    public NewBenchmarkSpec build() {
      checkState(className != null);
      checkState(methodName != null);
      return new NewBenchmarkSpec(this);
    }
  }
}
