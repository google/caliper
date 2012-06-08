// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.caliper.config;

/**
 * Thrown when an invalid configuration has been specified by the user.
 *
 * @author gak@google.com (Gregory Kick)
 */
public final class InvalidConfigurationException extends Exception {
  InvalidConfigurationException() {
    super();
  }

  InvalidConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  InvalidConfigurationException(String message) {
    super(message);
  }

  InvalidConfigurationException(Throwable cause) {
    super(cause);
  }
}
