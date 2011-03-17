/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.caliper.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.primitives.Primitives.wrap;
import static java.util.Arrays.asList;

import com.google.common.base.Preconditions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;

public class Parsers {
  public static final Parser<String> IDENTITY = new Parser<String>() {
    @Override public String parse(CharSequence in) {
      return in.toString();
    }
  };

  /**
   * Parser that uses fromString/valueOf.
   */
  public static <T> Parser<T> byConventionParser(Class<T> resultType) {
    if (resultType == String.class) {
      @SuppressWarnings("unchecked") // T == String
      Parser<T> identity = (Parser<T>) IDENTITY;
      return identity;
    }

    resultType = wrap(resultType);
    for (String methodName : asList("fromString", "valueOf")) {
      Method method;
      try {
        method = resultType.getDeclaredMethod(methodName, String.class);
      } catch (Exception e) {
        continue; // we don't care what went wrong, we just try again
      }
      if (Modifier.isStatic(method.getModifiers())) {
        return new MethodCallingParser<T>(resultType, method);
      }
    }
    throw new IllegalArgumentException(
        "No static valueOf(String) or fromString(String) method found in class: " + resultType);
  }

  public static class MethodCallingParser<T> implements Parser<T> {
    private final Class<T> resultType;
    private final Method method;

    public MethodCallingParser(Class<T> resultType, Method method) {
      checkArgument(Modifier.isStatic(method.getModifiers()));
      Preconditions.checkArgument(Util.extendsIgnoringWrapping(method.getReturnType(), resultType));
      this.resultType = resultType;
      this.method = method;
      method.setAccessible(true); // to permit inner enums, etc.
    }

    @Override public T parse(CharSequence cs) throws ParseException {
      String input = cs.toString();
      Object result;
      try {
        result = method.invoke(null, input);
      } catch (IllegalAccessException impossible) {
        throw new AssertionError(impossible);
      } catch (InvocationTargetException e) {
        throw newParseException("Wrong argument format: " + cs, e.getCause());
      }

      // Check round-trip
      if (!result.toString().equals(input)) {
        throw newParseException("blah"); // TODO
      }
      return resultType.cast(result);
    }
  }

  public static ParseException newParseException(String message, Throwable cause) {
    ParseException pe = newParseException(message);
    pe.initCause(cause);
    return pe;
  }

  public static ParseException newParseException(String message) {
    return new ParseException(message, 0);
  }
}
