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

import com.google.caliper.util.InvalidCommandException;

import java.io.IOException;
import java.io.PrintWriter;

final class Wiring {
  private Wiring() {}

  static CaliperRun wireItUp(
      PrintWriter writer, FilesystemFacade filesystem, String rcFilename, String[] args)
          throws InvalidCommandException {

    CaliperRcManager crm = new CaliperRcManager(filesystem);

    CaliperRc caliperRc = null;
    try {
      caliperRc = crm.loadOrCreate(rcFilename);
    } catch (IOException e) {
      throw new InvalidCommandException(e.getMessage()); // TODO?
    }

    CaliperOptions options = ParsedOptions.from(args, filesystem, caliperRc);
    return new CaliperRun(options, writer);
  }
}
