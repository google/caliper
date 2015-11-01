package dk.ilios.spanner;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Apply this annotation to the field that holds the {@link SpannerConfig} for Gauge if it is needed to modify the
 * default configuration.
 */
@Retention(RUNTIME)
@Target(ElementType.FIELD)
public @interface BenchmarkConfiguration {}
