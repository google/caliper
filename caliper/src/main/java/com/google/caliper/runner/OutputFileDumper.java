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

import static java.util.logging.Level.SEVERE;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.config.CaliperConfig;
import com.google.caliper.config.InvalidConfigurationException;
import com.google.caliper.config.ResultProcessorConfig;
import com.google.caliper.model.Run;
import com.google.caliper.model.Trial;
import com.google.caliper.options.CaliperDirectory;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * {@link ResultProcessor} that dumps the output data to a file in JSON format. By default, the
 * output will be dumped to a file called
 * {@code ~/.caliper/results/[benchmark classname].[timestamp].json}; if it exists and is a file,
 * the file will be overwritten.  The location can be overridden as either a file or a directory
 * using either the {@code file} or {@code dir} options respectively.
 */
final class OutputFileDumper implements ResultProcessor {
  private static final Logger logger = Logger.getLogger(OutputFileDumper.class.getName());

  private final Run run;
  private final Gson gson;
  private final File resultFile;
  private final File workFile;

  private Optional<JsonWriter> writer = Optional.absent();

  @Inject OutputFileDumper(Run run,
      BenchmarkClass benchmarkClass,
      Gson gson,
      CaliperConfig caliperConfig,
      @CaliperDirectory File caliperDirectory) throws InvalidConfigurationException {
    this.run = run;
    ResultProcessorConfig config = caliperConfig.getResultProcessorConfig(OutputFileDumper.class);
    if (config.options().containsKey("file")) {
      this.resultFile = new File(config.options().get("file"));
      logger.finer("found an output file in the configuration");
    } else if (config.options().containsKey("dir")) {
      File dir = new File(config.options().get("dir"));
      if (dir.isFile()) {
        throw new InvalidConfigurationException("specified a directory, but it's a file");
      }
      this.resultFile = new File(dir, createFileName(benchmarkClass.name()));
      logger.finer("found an output directory in the configuration");
    } else {
      this.resultFile =
          new File(new File(caliperDirectory, "results"), createFileName(benchmarkClass.name()));
      logger.fine("found no configuration");
    }
    logger.fine(String.format("using %s for results", resultFile));
    this.gson = gson;
    this.workFile = new File(resultFile.getPath() + ".tmp");
  }

  private String createFileName(String benchmarkName) {
    return String.format("%s.%s.json", benchmarkName, createTimestamp());
  }

  private String createTimestamp() {
    return ISODateTimeFormat.dateTimeNoMillis().print(run.startTime());
  }

  @Override public void processTrial(Trial trial) {
    if (!writer.isPresent()) {
      try {
        Files.createParentDirs(workFile);
        JsonWriter writer =
            new JsonWriter(new OutputStreamWriter(new FileOutputStream(workFile), Charsets.UTF_8));
        writer.setIndent("  ");  // always pretty print
        writer.beginArray();
        this.writer = Optional.of(writer);
      } catch (IOException e) {
        logger.log(SEVERE, String.format(
            "An error occured writing trial %s. Results in %s will be incomplete.", trial.id(),
            resultFile), e);
      }
    }
    if (writer.isPresent()) {
      gson.toJson(trial, Trial.class, writer.get());
    }
  }

  @Override public void close() throws IOException {
    if (writer.isPresent()) {
      writer.get().endArray().close();
    }
    if (workFile.exists()) {
      Files.move(workFile, resultFile);
    }
  }
}
