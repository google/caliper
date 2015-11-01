/*
 * Copyright (C) 2013 Google Inc.
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

import com.sun.management.HotSpotDiagnosticMXBean;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

/**
 * A simple class that invokes {@link System#gc()} over and over to generate some GC log messages.
 */
public final class GcLogMessageGenerator {
  public static void main(String[] args) throws IOException {
    checkGcLogging();
    for (int i = 0; i < 100; i++) {
      System.gc();
    }
  }

  private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

  private static void checkGcLogging() throws IOException {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    HotSpotDiagnosticMXBean bean = ManagementFactory.newPlatformMXBeanProxy(
        server, HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class);
    if (!bean.getVMOption("PrintGC").getValue().equals(Boolean.TRUE.toString())) {
      System.err.println("This is only useful if you run with -XX:+PrintGC");
      System.exit(1);
    }
  }
}
