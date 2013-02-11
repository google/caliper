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

package com.google.caliper.util;

import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Tests {@link Pipes}.
 */
@RunWith(JUnit4.class)

public class PipesTest {
  @Test public void createPipe() throws IOException {
    List<File> pipeFiles = Lists.newArrayListWithCapacity(10);
    for (int i = 0; i < 10; i++) {
      File pipeFile = Pipes.createPipe();
      assertTrue(pipeFile.exists());
      pipeFiles.add(pipeFile);
    }
    for (File pipeFile : pipeFiles) {
      pipeFile.delete();
    }
  }
}
