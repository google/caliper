/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.caliper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CaliperRc {
  public static final CaliperRc INSTANCE = new CaliperRc();

  private final Properties properties = new Properties();

  private CaliperRc() {
    try {
      String caliperRcEnvVar = System.getenv("CALIPERRC");
      File caliperRcFile = (caliperRcEnvVar == null)
          ? new File(System.getProperty("user.home"), ".caliperrc")
          : new File(caliperRcEnvVar);
      if (caliperRcFile.exists()) {
        InputStream in = new FileInputStream(caliperRcFile);
        properties.load(in);
        in.close();
      } else {
        // create it with a template
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String getApiKey() {
    return properties.getProperty("apiKey");
  }

  public String getPostUrl() {
    return properties.getProperty("postUrl");
  }
}
