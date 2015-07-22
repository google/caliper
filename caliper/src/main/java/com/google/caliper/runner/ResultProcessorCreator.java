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

package com.google.caliper.runner;

import com.google.caliper.api.ResultProcessor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Responsible for creating instances of configured {@link ResultProcessor}.
 */
final class ResultProcessorCreator {

  public static final String NO_PUBLIC_DEFAULT_CONSTRUCTOR =
      "ResultProcessor %s not supported as it does not have a public default constructor";

  private ResultProcessorCreator() {
  }

  static ResultProcessor createResultProcessor(Class<? extends ResultProcessor> processorClass) {
    ResultProcessor resultProcessor;

    try {
      Constructor<? extends ResultProcessor> constructor = processorClass.getConstructor();
      resultProcessor = constructor.newInstance();
    } catch (NoSuchMethodException e) {
      throw new UserCodeException(String.format(NO_PUBLIC_DEFAULT_CONSTRUCTOR, processorClass), e);
    } catch (InvocationTargetException e) {
      throw new UserCodeException("ResultProcessor %s could not be instantiated", e.getCause());
    } catch (InstantiationException e) {
      throw new UserCodeException("ResultProcessor %s could not be instantiated", e);
    } catch (IllegalAccessException e) {
      throw new UserCodeException("ResultProcessor %s could not be instantiated", e);
    }

    return resultProcessor;
  }
}
