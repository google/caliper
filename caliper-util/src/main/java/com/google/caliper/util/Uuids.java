/*
 * Copyright (C) 2018 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;

/** Utility for converting {@code UUID}s to bytes and back. */
public final class Uuids {

  /** The number of bytes in a {@code UUID}. */
  private static final int UUID_BYTES = 16;

  private Uuids() {}

  /** Returns a buffer containing the 16 bytes of the given UUID. */
  @SuppressWarnings("RedundantCast")
  public static ByteBuffer toBytes(UUID uuid) {
    ByteBuffer buf = ByteBuffer.allocate(UUID_BYTES);
    buf.putLong(uuid.getMostSignificantBits());
    buf.putLong(uuid.getLeastSignificantBits());
    ((java.nio.Buffer) buf).flip();  // for Java 1.8 compatibility
    return buf;
  }

  /** Writes the bytes of the given UUID to the given channel. */
  public static void writeToChannel(UUID uuid, WritableByteChannel channel) throws IOException {
    ByteBuffer buf = toBytes(uuid);
    while (buf.hasRemaining()) {
      channel.write(buf);
    }
  }

  /** Returns a UUID created from the first 16 bytes of the given buffer. */
  public static UUID fromBytes(ByteBuffer buf) {
    checkArgument(buf.remaining() >= UUID_BYTES);
    return new UUID(buf.getLong(), buf.getLong());
  }

  /** Reads the next 16 bytes from the given channel as a UUID. */
  @SuppressWarnings("RedundantCast")
  public static UUID readFromChannel(ReadableByteChannel channel) throws IOException {
    ByteBuffer buf = ByteBuffer.allocate(UUID_BYTES);
    while (buf.hasRemaining()) {
      if (channel.read(buf) == -1) {
        throw new EOFException("unexpected EOF while reading UUID");
      }
    }
    ((java.nio.Buffer) buf).flip();  // for Java 1.8 compatibility
    return fromBytes(buf);
  }
}
