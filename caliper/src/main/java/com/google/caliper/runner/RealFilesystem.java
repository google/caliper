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
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Since we don't have a real file system API in Java, I'm going to pretend we do. This is NOT
 * trying to be full-featured, just whatever Caliper happens to need.
 */
public class RealFilesystem implements FilesystemFacade {
  @Override
  public boolean exists(String filename) {
    return new File(filename).exists();
  }

  @Override
  public boolean isDirectory(String filename) {
    return new File(filename).isDirectory();
  }

  @Override
  public void copy(InputSupplier<InputStream> in, String rcFileName) throws IOException {
    Files.copy(in, new File(rcFileName));
  }

  @Override
  public ImmutableMap<String, String> loadProperties(String propFileName) throws IOException {
    Properties props = new Properties();
    InputStream is = new FileInputStream(propFileName);
    try {
      props.load(is);
    } finally {
      Closeables.closeQuietly(is);
    }
    return Maps.fromProperties(props);
  }

  @Override
  public String makeAbsolute(String name, String baseDir) {
    return new File(name).isAbsolute() ? name : new File(baseDir, name).getPath();
  }
}
