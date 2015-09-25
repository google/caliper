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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.joda.time.Instant;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;

/**
 * Serializes and deserializes {@link Instant} instances.
 */
final class InstantTypeAdapter extends TypeAdapter<Instant> {
  @Override public void write(JsonWriter out, Instant value) throws IOException {
    out.value(ISODateTimeFormat.dateTime().print(value));
  }

  @Override public Instant read(JsonReader in) throws IOException {
    return ISODateTimeFormat.dateTime().parseDateTime(in.nextString()).toInstant();
  }
}
