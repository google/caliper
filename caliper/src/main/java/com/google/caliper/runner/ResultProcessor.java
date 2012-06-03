// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.runner;

import com.google.caliper.model.NewTrial;
import com.google.caliper.model.Run;

import java.io.Closeable;

/**
 * Interface for processing results as they complete. Callers must invoke {@link #close()} after
 * all trials have been {@linkplain #processTrial processed}.
 */
public interface ResultProcessor extends Closeable {
  // TODO(gak): remove this method once processTrial is implemented
  void processRun(Run run);

  void processTrial(NewTrial trial);
}
