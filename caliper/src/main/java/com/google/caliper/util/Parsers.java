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

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.primitives.Primitives.wrap;
import static java.util.Arrays.asList;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import java.lang.reflect.Constructor;
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
   * Parser that tries, in this order:
   * <ul>
   * <li>static fromString(String)
   * <li>static valueOf(String)
   * <li>new ResultType(String)
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
      if (Util.isStatic(method)) {
        return new MethodInvokingParser<T>(resultType, method);
      }
    }

    try {
      Constructor<T> constr = resultType.getDeclaredConstructor(String.class);
      return new ConstructorInvokingParser<T>(resultType, constr);
    } catch (NoSuchMethodException keepGoing) {
    }

    throw new IllegalArgumentException(
        "No static valueOf(String) or fromString(String) method found in class: " + resultType);
  }

  abstract static class InvokingParser<T> implements Parser<T> {
    @Override public T parse(CharSequence cs) throws ParseException {
      String input = cs.toString();
      T result;
      try {
        result = invoke(input);
      } catch (InvocationTargetException e) {
        Throwable cause = e.getCause();
        String desc = firstNonNull(cause.getMessage(), cause.getClass().getSimpleName());
        throw newParseException(desc, cause);
      } catch (Exception e) {
        throw newParseException("Unknown parsing problem", e);
      }

      // Check round-trip -- things will be mighty confusing if we don't have this
      // TODO(kevinb): we could try normalizing the strings one time using a single round-trip, which
      // ought to mostly remove this failure mode
      if (!result.toString().equals(input)) {
        throw newParseException("blah"); // TODO(kevinb): fix (or nuke)
      }
      return result;
    }

    protected abstract T invoke(String input) throws Exception;
  }

  public static class MethodInvokingParser<T> extends InvokingParser<T> {
    private final Method method;
    private final Class<T> resultType;

    public MethodInvokingParser(Class<T> resultType, Method method) {
      checkArgument(Util.isStatic(method));
      checkArgument(Util.extendsIgnoringWrapping(method.getReturnType(), resultType));
      this.resultType = resultType;
      this.method = method;
      method.setAccessible(true); // to permit inner enums, etc.
    }

    @Override
    protected T invoke(String input) throws IllegalAccessException, InvocationTargetException {
      return resultType.cast(method.invoke(null, input));
    }
  }

  public static class ConstructorInvokingParser<T> extends InvokingParser<T> {
    private final Constructor<T> constr;

    public ConstructorInvokingParser(Class<T> resultType, Constructor<T> constr) {
      this.constr = constr;
      constr.setAccessible(true);
    }

    @Override
    protected T invoke(String input)
        throws IllegalAccessException, InvocationTargetException, InstantiationException {
      return constr.newInstance(input);
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
