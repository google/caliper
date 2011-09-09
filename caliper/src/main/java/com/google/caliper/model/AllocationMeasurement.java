// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that identifies a given method as an "allocation measurement" method. The method
 * should take a single int parameter; its return value may be any type. Methods with this
 * annotation may be used with the {@link com.google.caliper.runner.AllocationInstrument}
 * or the {@link com.google.caliper.runner.AllocationSizeInstrument}.
 *
 * @author schmoe@google.com (mike nonemacher)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AllocationMeasurement {}
