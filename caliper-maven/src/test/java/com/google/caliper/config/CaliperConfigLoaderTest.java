/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.caliper.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.caliper.options.CaliperOptions;
import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Tests {@link CaliperConfigLoader}.
 */
@RunWith(MockitoJUnitRunner.class)

public class CaliperConfigLoaderTest {
  @Mock CaliperOptions optionsMock;

  private File tempConfigFile;

  @Before public void createTempUserProperties() throws IOException {
    tempConfigFile = File.createTempFile("caliper-config-test", "properties");
    tempConfigFile.deleteOnExit();
    Properties userProperties = new Properties();
    userProperties.put("some.property", "franklin");
    FileOutputStream fs = new FileOutputStream(tempConfigFile);
    userProperties.store(fs, null);
    fs.close();
  }

  @After public void deleteTempUserProperties() {
    tempConfigFile.delete();
  }

  @Test public void loadOrCreate_configFileExistsNoOverride() throws Exception {
    when(optionsMock.caliperConfigFile()).thenReturn(tempConfigFile);
    when(optionsMock.configProperties()).thenReturn(ImmutableMap.<String, String>of());
    CaliperConfigLoader loader = new CaliperConfigLoader(optionsMock);
    CaliperConfig config = loader.loadOrCreate();
    assertEquals("franklin", config.properties.get("some.property"));
  }

  @Test public void loadOrCreate_configFileExistsWithOverride() throws Exception {
    when(optionsMock.caliperConfigFile()).thenReturn(tempConfigFile);
    when(optionsMock.configProperties()).thenReturn(ImmutableMap.of(
        "some.property", "tacos"));
    CaliperConfigLoader loader = new CaliperConfigLoader(optionsMock);
    CaliperConfig config = loader.loadOrCreate();
    assertEquals("tacos", config.properties.get("some.property"));
  }
}
