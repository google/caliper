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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.io.Serializable;

/**
 * A message containing a selection of {@link System#getProperties() system properties} from the
 * worker JVM.
 */
public class VmPropertiesLogMessage extends LogMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  private final ImmutableMap<String, String> properties;

  public VmPropertiesLogMessage() {
    this(ImmutableMap.copyOf(Maps.fromProperties(System.getProperties())));
  }

  public VmPropertiesLogMessage(ImmutableMap<String, String> properties) {
    this.properties = checkNotNull(properties);
  }

  public ImmutableMap<String, String> properties() {
    return properties;
  }

  @Override public void accept(LogMessageVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(properties);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof VmPropertiesLogMessage) {
      VmPropertiesLogMessage that = (VmPropertiesLogMessage) obj;
      return this.properties.equals(that.properties);
    } else {
      return false;
    }
  }
}
