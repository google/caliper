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

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * A utility class for standardizing the hash function that we're using for persistence.
 */
final class PersistentHashing {
  private PersistentHashing() {}

  static HashFunction getPersistentHashFunction() {
    return Hashing.murmur3_32();
  }
}
