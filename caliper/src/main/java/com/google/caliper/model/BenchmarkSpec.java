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

import static com.google.caliper.model.PersistentHashing.getPersistentHashFunction;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import java.io.Serializable;
import java.util.Map;
import java.util.SortedMap;

/**
 * A specification by which a benchmark method invocation can be uniquely identified.
 *
 * @author gak@google.com (Gregory Kick)
 */
public final class BenchmarkSpec implements Serializable {
  private static final long serialVersionUID = 1L;

  static final BenchmarkSpec DEFAULT = new BenchmarkSpec();

  @ExcludeFromJson
  private int id;
  private String className;
  private String methodName;
  private SortedMap<String, String> parameters;
  private int hash;

  private BenchmarkSpec() {
    this.className = "";
    this.methodName = "";
    this.parameters = Maps.newTreeMap();
  }

  private BenchmarkSpec(Builder builder) {
    this.className = builder.className;
    this.methodName = builder.methodName;
    this.parameters = Maps.newTreeMap(builder.parameters);
  }

  public String className() {
    return className;
  }

  public String methodName() {
    return methodName;
  }

  public ImmutableSortedMap<String, String> parameters() {
    return ImmutableSortedMap.copyOf(parameters);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof BenchmarkSpec) {
      BenchmarkSpec that = (BenchmarkSpec) obj;
      return this.className.equals(that.className)
          && this.methodName.equals(that.methodName)
          && this.parameters.equals(that.parameters);
    } else {
      return false;
    }
  }

  private void initHash() {
    if (hash == 0) {
      this.hash =
          getPersistentHashFunction().hashObject(this, BenchmarkSpecFunnel.INSTANCE).asInt();
    }
  }

  @Override
  public int hashCode() {
    initHash();
    return hash;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("className", className)
        .add("methodName", methodName)
        .add("parameters", parameters)
        .toString();
  }

  enum BenchmarkSpecFunnel implements Funnel<BenchmarkSpec> {
    INSTANCE;

    @Override
    public void funnel(BenchmarkSpec from, PrimitiveSink into) {
      into.putUnencodedChars(from.className).putUnencodedChars(from.methodName);
      StringMapFunnel.INSTANCE.funnel(from.parameters, into);
    }
  }

  public static final class Builder {
    private String className;
    private String methodName;
    private final SortedMap<String, String> parameters = Maps.newTreeMap();

    public Builder className(String className) {
      this.className = checkNotNull(className);
      return this;
    }

    public Builder methodName(String methodName) {
      this.methodName = checkNotNull(methodName);
      return this;
    }

    public Builder addParameter(String parameterName, String value) {
      this.parameters.put(checkNotNull(parameterName), checkNotNull(value));
      return this;
    }

    public Builder addAllParameters(Map<String, String> parameters) {
      this.parameters.putAll(parameters);
      return this;
    }

    public BenchmarkSpec build() {
      checkState(className != null);
      checkState(methodName != null);
      return new BenchmarkSpec(this);
    }
  }
}
