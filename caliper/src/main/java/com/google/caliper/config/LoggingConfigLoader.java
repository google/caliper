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

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import com.google.caliper.model.Run;
import com.google.caliper.options.CaliperDirectory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.Closer;

import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Loading the logging configuration at {@code ~/.caliper/logging.properties} if present.
 */
@Singleton
final class LoggingConfigLoader {
  private static final Logger logger = Logger.getLogger(LoggingConfigLoader.class.getName());

  private final File caliperDirectory;
  private final LogManager logManager;
  private final Run run;

  @Inject LoggingConfigLoader(@CaliperDirectory File caliperDirectory, LogManager logManager,
      Run run) {
    this.caliperDirectory = caliperDirectory;
    this.logManager = logManager;
    this.run = run;
  }

  @Inject void loadLoggingConfig() {
    File loggingPropertiesFile = new File(caliperDirectory, "logging.properties");
    if (loggingPropertiesFile.isFile()) {
      Closer closer = Closer.create();
      FileInputStream fis = null;
      try {
        fis = closer.register(new FileInputStream(loggingPropertiesFile));
        logManager.readConfiguration(fis);
      } catch (SecurityException e) {
        logConfigurationException(e);
      } catch (IOException e) {
        logConfigurationException(e);
      } finally {
        try {
          closer.close();
        } catch (IOException e) {
          logger.log(SEVERE, "could not close " + loggingPropertiesFile, e);
        }
      }
      logger.info(String.format("Using logging configuration at %s", loggingPropertiesFile));
    } else {
      try {
        maybeLoadDefaultLogConfiguration(LogManager.getLogManager());
      } catch (SecurityException e) {
        logConfigurationException(e);
      } catch (IOException e) {
        logConfigurationException(e);
      }
    }
  }

  @VisibleForTesting void maybeLoadDefaultLogConfiguration(LogManager logManager)
      throws SecurityException, IOException {
    logManager.reset();
    File logDirectory = new File(caliperDirectory, "log");
    logDirectory.mkdirs();
    FileHandler fileHandler = new FileHandler(String.format("%s%c%s.%s.log",
        logDirectory.getAbsolutePath(), File.separatorChar,
        ISODateTimeFormat.basicDateTimeNoMillis().print(run.startTime()), run.id()));
    fileHandler.setEncoding(Charsets.UTF_8.name());
    fileHandler.setFormatter(new SimpleFormatter());
    Logger globalLogger = logManager.getLogger("");
    globalLogger.addHandler(fileHandler);
  }

  private static void logConfigurationException(Exception e) {
    logger.log(WARNING, "Could not apply the logging configuration", e);
  }
}
