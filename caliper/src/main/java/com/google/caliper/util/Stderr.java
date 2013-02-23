package com.google.caliper.util;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** A binding annotation for standard err. */
@Retention(RUNTIME)
@Target({FIELD, PARAMETER, METHOD})
@BindingAnnotation
public @interface Stderr {}
