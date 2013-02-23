package com.google.caliper.util;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.io.PrintWriter;

/**
 * A module that binds {@link PrintWriter} instances for {@link Stdout} and {@link Stderr}.
 */
public final class OutputModule extends AbstractModule {
  public static OutputModule system() {
    return new OutputModule(new PrintWriter(System.out, true), new PrintWriter(System.err, true));
  }

  private final PrintWriter stdout;
  private final PrintWriter stderr;

  public OutputModule(PrintWriter stdout, PrintWriter stderr) {
    this.stdout = checkNotNull(stdout);
    this.stderr = checkNotNull(stderr);
  }

  @Override protected void configure() {}

  @Provides @Stdout PrintWriter provideStdoutWriter() {
    return stdout;
  }

  @Provides @Stderr PrintWriter provideStderr() {
    return stderr;
  }
}
