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

import com.google.common.collect.ImmutableMap;
import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Kevin Bourrillion
 */
public interface FilesystemFacade {

  boolean exists(String filename);

  boolean isDirectory(String filename);

  void copy(InputSupplier<InputStream> in, String rcFileName) throws IOException;

  ImmutableMap<String, String> loadProperties(String propFileName) throws IOException;

  String makeAbsolute(String name, String baseDir);
}
