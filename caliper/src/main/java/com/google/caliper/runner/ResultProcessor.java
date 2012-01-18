// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.runner;

import com.google.caliper.model.CaliperData;

/**
 * Interface for objects that process a set of results once it is complete. Generally, the results 
 */
public interface ResultProcessor {
  void handleResults(CaliperData results);
}
