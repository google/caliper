// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that identifies a given method as an "arbitrary measurement" method. The method should
 * take no parameters and return a double, which is the measured value.
 *
 * @author schmoe@google.com (mike nonemacher)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ArbitraryMeasurement {

  /**
   * The units for the value returned by this measurement method.
   */
  String units() default "";

  /**
   * Text description of the quantity measured by this measurement method.
   */
  String description() default "";
}
