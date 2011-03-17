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

package com.google.caliper.runner;

import com.google.caliper.util.Util;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

// TODO(kevinb): move these methods to CaliperRc class?
public final class CaliperRcManager {
  public static CaliperRc loadOrCreate(File rcFile) {
    // TODO(kevinb): deal with migration issue from old-style .caliperrc

    if (rcFile.exists()) {
      try {
        return loadFrom(Files.newInputStreamSupplier(rcFile));
      } catch (IOException keepGoing) {
      }
    }

    InputSupplier<InputStream> supplier =
        Util.resourceSupplier(CaliperRc.class, "default.caliperrc");
    tryCopyIfNeeded(supplier, rcFile);

    try {
      return loadFrom(supplier);
    } catch (IOException e) {
      throw new AssertionError(e); // class path must be messed up
    }
  }

  private static CaliperRc loadFrom(InputSupplier<? extends InputStream> supplier)
      throws IOException {
    return new CaliperRc(Util.loadProperties(supplier));
  }

  private static void tryCopyIfNeeded(InputSupplier<? extends InputStream> supplier, File rcFile) {
    if (!rcFile.exists()) {
      try {
        Files.copy(supplier, rcFile);
      } catch (IOException e) {
        rcFile.delete();
      }
    }
  }
}
