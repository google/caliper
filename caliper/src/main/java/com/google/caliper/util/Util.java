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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.io.InputSupplier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Kevin Bourrillion
 */
public class Util {
  public static ImmutableMap<String, String> loadProperties(File propsFile) throws IOException {
    Properties props = new Properties();
    InputStream is = new FileInputStream(propsFile);
    try {
      props.load(is);
    } finally {
      Closeables.closeQuietly(is);
    }
    return Maps.fromProperties(props);
  }

  // TODO: this is similar to Resources.getResource
  public static InputSupplier<InputStream> resourceSupplier(final Class<?> c, final String name) {
    return new InputSupplier<InputStream>() {
      @Override public InputStream getInput() {
        return c.getResourceAsStream(name);
      }
    };
  }
}
