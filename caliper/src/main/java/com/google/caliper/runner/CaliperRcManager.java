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
import com.google.common.collect.ImmutableMap;
import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;

class CaliperRcManager {
  private final FilesystemFacade fsi;

  CaliperRcManager(FilesystemFacade fsi) {
    this.fsi = fsi;
  }

  CaliperRc loadOrCreate(String rcFileName) throws IOException {
    // TODO(kevinb): deal with migration issue from old-style .caliperrc
    if (!fsi.exists(rcFileName)) {
      InputSupplier<InputStream> supplier = Util.resourceSupplier(getClass(), "default.caliperrc");
      fsi.copy(supplier, rcFileName);
    }
    ImmutableMap<String,String> props = fsi.loadProperties(rcFileName);
    CaliperRc caliperRc = new CaliperRc(props);

    String dir = caliperRc.vmBaseDirectory();
    if (dir != null && !fsi.isDirectory(dir)) {
      throw new RuntimeException("No such dir: " + dir); // TODO
    }
    return caliperRc;
  }
}
