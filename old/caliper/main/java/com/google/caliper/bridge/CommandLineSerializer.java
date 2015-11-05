/*
 * Copyright (C) 2014 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.io.BaseEncoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Serializes and deserializes WorkerSpecs as base64 encoded strings so they can be passed on the
 * command line to the worker.
 *
 * <p>Java serialization is a appropriate in this usecase because there are no compatibility
 * concerns.  The messages encoded/decoded by this class are only used to communicate with another
 * JVM that is running with the same version of the java classes.  Also, it should be lighter weight
 * and faster than other common serialization solutions.
 */
public final class CommandLineSerializer {
  private CommandLineSerializer() {}

  /** Returns the given serializable object as a base64 encoded String. */
  public static String render(WorkerSpec message) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      ObjectOutputStream out = new ObjectOutputStream(bytes);
      out.writeObject(message);
      out.close();
      return BaseEncoding.base64().encode(bytes.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Parses the given base64 encoded string as an object of the given type. */
  public static WorkerSpec parse(String arg) {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(BaseEncoding.base64().decode(arg));
      ObjectInputStream in = new ObjectInputStream(bais);
      WorkerSpec instance = (WorkerSpec) in.readObject();
      in.close();
      checkState(bais.read() == -1,
          "Message %s contains more than one object.", arg);
      return instance;
    } catch (IOException e) {
      throw new RuntimeException(e);  // assertion error?
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("cannot decode message", e);
    }
  }
}
