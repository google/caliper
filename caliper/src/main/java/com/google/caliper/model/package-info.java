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

/**
 * These classes model the data that is collected by the caliper {@linkplain
 * com.google.caliper.runner runner}: the record of which scenarios were tested on which VMs by
 * which instruments and, most importantly, all the measurements that were observed.
 *
 * <p>The primary goal of these classes is to be as easily convertible back and forth to JSON text
 * as possible. The secondary goal is to be easily persistable in a relational database.
 */
package com.google.caliper.model;