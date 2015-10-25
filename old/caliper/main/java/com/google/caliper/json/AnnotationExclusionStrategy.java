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

package com.google.caliper.json;

import com.google.caliper.model.ExcludeFromJson;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * An exclusion strategy that excludes elements annotated with {@link ExcludeFromJson}.
 */
final class AnnotationExclusionStrategy implements ExclusionStrategy {
  @Override public boolean shouldSkipField(FieldAttributes f) {
    return f.getAnnotation(ExcludeFromJson.class) != null;
  }

  @Override public boolean shouldSkipClass(Class<?> clazz) {
    return clazz.getAnnotation(ExcludeFromJson.class) != null;
  }
}
