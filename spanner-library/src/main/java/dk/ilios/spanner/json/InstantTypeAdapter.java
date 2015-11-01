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

package dk.ilios.spanner.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;

/**
 * Serializes and deserializes {@code Instant} instances.
 */
public final class InstantTypeAdapter extends TypeAdapter<Instant> {

  @Override public void write(JsonWriter out, Instant value) throws IOException {
    out.value(DateTimeFormatter.ISO_INSTANT.format(value));
  }

  @Override public Instant read(JsonReader in) throws IOException {
    return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(in.nextString()));
  }
}
