/*
 * Copyright (C) 2015 Google Inc.
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

package com.google.caliper.runner.resultprocessor;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.core.UserCodeException;
import com.google.caliper.runner.config.ResultProcessorConfig;
import com.google.common.base.Optional;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/** Responsible for creating instances of configured {@link ResultProcessor}. */
final class ResultProcessorCreator {

  public static final String NO_VALID_CONSTRUCTOR =
      "ResultProcessor %s not supported: it needs a public constructor with either no parameters or"
          + " one ResultProcessorConfig parameter";

  private ResultProcessorCreator() {}

  static ResultProcessor createResultProcessor(
      Class<? extends ResultProcessor> processorClass, ResultProcessorConfig config) {
    try {
      Optional<ResultProcessor> result = tryInstantiate(processorClass, config);
      if (!result.isPresent()) {
        result = tryInstantiate(processorClass);
        if (!result.isPresent()) {
          throw new UserCodeException(String.format(NO_VALID_CONSTRUCTOR, processorClass));
        }
      }
      return result.get();
    } catch (InvocationTargetException e) {
      throw new UserCodeException(
          String.format("ResultProcessor %s could not be instantiated", processorClass),
          e.getCause());
    } catch (InstantiationException | IllegalAccessException e) {
      throw new UserCodeException(
          String.format("ResultProcessor %s could not be instantiated", processorClass), e);
    }
  }

  private static Optional<ResultProcessor> tryInstantiate(
      Class<? extends ResultProcessor> processorClass, Object... args)
      throws InvocationTargetException, InstantiationException, IllegalAccessException {
    Class<?>[] argClasses = new Class<?>[args.length];
    for (int i = 0; i < args.length; i++) {
      argClasses[i] = args[i].getClass();
    }

    try {
      // relies on the fact that we're only doing this for no-arg and ResultProcessorConfig args
      Constructor<? extends ResultProcessor> constructor =
          processorClass.getConstructor(argClasses);
      return Optional.of(constructor.newInstance(args));
    } catch (NoSuchMethodException e) {
      return Optional.absent();
    }
  }
}
