/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.caliper.util;

import static com.google.common.truth.Truth.assertThat;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Uuids}. */
@RunWith(JUnit4.class)
public final class UuidsTest {

  @Test
  public void uuidRoundTrip() throws Exception {
    UUID id = UUID.randomUUID();
    ByteBuffer bytes = Uuids.toBytes(id);
    UUID id2 = Uuids.fromBytes(bytes);
    assertThat(id2).isEqualTo(id);
  }
}
