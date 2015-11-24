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


import java.util.Map;
import java.util.SortedMap;


/**
 * A specification by which the application of an instrument can be uniquely identified.
 *
 * @author gak@google.com (Gregory Kick)
 */
public final class InstrumentSpec {
  static final InstrumentSpec DEFAULT = new InstrumentSpec();

  @ExcludeFromJson
  private int id;
  private String className;
  private SortedMap<String, String> options;
  @ExcludeFromJson
  private int hash;

  private InstrumentSpec() {
    this.className = "";
    this.options = Maps.newTreeMap();
  }

  private InstrumentSpec(Builder builder) {
    this.className = builder.className;
    this.options = Maps.newTreeMap(builder.options);
  }

  public String className() {
    return className;
  }

  public ImmutableSortedMap<String, String> options() {
    return ImmutableSortedMap.copyOf(options);
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof InstrumentSpec) {
      InstrumentSpec that = (InstrumentSpec) obj;
      return this.className.equals(that.className)
          && this.options.equals(that.options);
    } else {
      return false;
    }
  }

  private void initHash() {
    if (hash == 0) {
      this.hash = getPersistentHashFunction()
          .newHasher()
          .putUnencodedChars(className)
          .putObject(options, StringMapFunnel.INSTANCE)
          .hash().asInt();
    }
  }

  @Override public int hashCode() {
    initHash();
    return hash;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("className", className)
        .add("options", options)
        .toString();
  }

  public static final class Builder {
    private String className;
    private final SortedMap<String, String> options = Maps.newTreeMap();

    public Builder className(String className) {
      this.className = checkNotNull(className);
      return this;
    }

    public Builder instrumentClass(Class<?> insturmentClass) {
      return className(insturmentClass.getName());
    }

    public Builder addOption(String option, String value) {
      this.options.put(option, value);
      return this;
    }

    public Builder addAllOptions(Map<String, String> options) {
      this.options.putAll(options);
      return this;
    }

    public InstrumentSpec build() {
      checkState(className != null);
      return new InstrumentSpec(this);
    }
  }
}
