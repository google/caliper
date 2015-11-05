/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.caliper.options;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.caliper.util.DisplayUsageException;
import com.google.caliper.util.InvalidCommandException;
import com.google.caliper.util.Parser;
import com.google.caliper.util.Parsers;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.primitives.Primitives;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

// based on r135 of OptionParser.java from vogar
// NOTE: this class is still pretty messy but will be cleaned up further and possibly offered to
// Guava.

/**
 * Parses command line options.
 *
 * Strings in the passed-in String[] are parsed left-to-right. Each String is classified as a short
 * option (such as "-v"), a long option (such as "--verbose"), an argument to an option (such as
 * "out.txt" in "-f out.txt"), or a non-option positional argument.
 *
 * A simple short option is a "-" followed by a short option character. If the option requires an
 * argument (which is true of any non-boolean option), it may be written as a separate parameter,
 * but need not be. That is, "-f out.txt" and "-fout.txt" are both acceptable.
 *
 * It is possible to specify multiple short options after a single "-" as long as all (except
 * possibly the last) do not require arguments.
 *
 * A long option begins with "--" followed by several characters. If the option requires an
 * argument, it may be written directly after the option name, separated by "=", or as the next
 * argument. (That is, "--file=out.txt" or "--file out.txt".)
 *
 * A boolean long option '--name' automatically gets a '--no-name' companion. Given an option
 * "--flag", then, "--flag", "--no-flag", "--flag=true" and "--flag=false" are all valid, though
 * neither "--flag true" nor "--flag false" are allowed (since "--flag" by itself is sufficient, the
 * following "true" or "false" is interpreted separately). You can use "yes" and "no" as synonyms
 * for "true" and "false".
 *
 * Each String not starting with a "-" and not a required argument of a previous option is a
 * non-option positional argument, as are all successive Strings. Each String after a "--" is a
 * non-option positional argument.
 *
 * The fields corresponding to options are updated as their options are processed. Any remaining
 * positional arguments are returned as an ImmutableList<String>.
 *
 * Here's a simple example:
 *
 * // This doesn't need to be a separate class, if your application doesn't warrant it. //
 * Non-@Option fields will be ignored. class Options {
 *
 * @Option(names = { "-q", "--quiet" }) boolean quiet = false;
 *
 * // Boolean options require a long name if it's to be possible to explicitly turn them off. //
 * Here the user can use --no-color.
 * @Option(names = { "--color" }) boolean color = true;
 * @Option(names = { "-m", "--mode" }) String mode = "standard; // Supply a default just by setting
 * the field.
 * @Option(names = { "-p", "--port" }) int portNumber = 8888;
 *
 * // There's no need to offer a short name for rarely-used options.
 * @Option(names = { "--timeout" }) double timeout = 1.0;
 * @Option(names = { "-o", "--output-file" }) String outputFile;
 *
 * }
 *
 * See also:
 *
 * the getopt(1) man page Python's "optparse" module (http://docs.python.org/library/optparse.html)
 * the POSIX "Utility Syntax Guidelines" (http://www.opengroup.org/onlinepubs/000095399/basedefs/xbd_chap12.html#tag_12_02)
 * the GNU "Standards for Command Line Interfaces" (http://www.gnu.org/prep/standards/standards.html#Command_002dLine-Interfaces)
 */
final class CommandLineParser<T> {
  /**
   * Annotates a field or method in an options class to signify that parsed values should be
   * injected.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.METHOD})
  public @interface Option {
    /**
     * The names for this option, such as { "-h", "--help" }. Names must start with one or two '-'s.
     * An option must have at least one name.
     */
    String[] value();
  }

  /**
   * Annotates a single method in an options class to receive any "leftover" arguments. The method
   * must accept {@code ImmutableList<String>} or a supertype. The method will be invoked even if
   * the list is empty.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.METHOD})
  public @interface Leftovers {}

  public static <T> CommandLineParser<T> forClass(Class<? extends T> c) {
    return new CommandLineParser<T>(c);
  }

  private final InjectionMap injectionMap;
  private T injectee;

  // TODO(kevinb): make a helper object that can be mutated during processing
  private final List<PendingInjection> pendingInjections = Lists.newArrayList();

  /**
   * Constructs a new command-line parser that will inject values into {@code injectee}.
   *
   * @throws IllegalArgumentException if {@code injectee} contains multiple options using the same
   *     name
   */
  private CommandLineParser(Class<? extends T> c) {
    this.injectionMap = InjectionMap.forClass(c);
  }

  /**
   * Parses the command-line arguments 'args', setting the @Option fields of the 'optionSource'
   * provided to the constructor. Returns a list of the positional arguments left over after
   * processing all options.
   */
  public void parseAndInject(String[] args, T injectee) throws InvalidCommandException {
    this.injectee = injectee;
    pendingInjections.clear();
    Iterator<String> argsIter = Iterators.forArray(args);
    ImmutableList.Builder<String> builder = ImmutableList.builder();

    while (argsIter.hasNext()) {
      String arg = argsIter.next();
      if (arg.equals("--")) {
        break; // "--" marks the end of options and the beginning of positional arguments.
      } else if (arg.startsWith("--")) {
        parseLongOption(arg, argsIter);
      } else if (arg.startsWith("-")) {
        parseShortOptions(arg, argsIter);
      } else {
        builder.add(arg);
        // allow positional arguments to mix with options since many linux commands do
      }
    }

    for (PendingInjection pi : pendingInjections) {
      pi.injectableOption.inject(pi.value, injectee);
    }

    ImmutableList<String> leftovers = builder.addAll(argsIter).build();
    invokeMethod(injectee, injectionMap.leftoversMethod, leftovers);
  }

  // Private stuff from here on down

  private abstract static class InjectableOption {
    abstract boolean isBoolean();
    abstract void inject(String valueText, Object injectee) throws InvalidCommandException;
    boolean delayedInjection() {
      return false;
    }
  }

  private static class InjectionMap {
    public static InjectionMap forClass(Class<?> injectedClass) {
      ImmutableMap.Builder<String, InjectableOption> builder = ImmutableMap.builder();

      InjectableOption helpOption = new InjectableOption() {
        @Override boolean isBoolean() {
          return true;
        }
        @Override void inject(String valueText, Object injectee) throws DisplayUsageException {
          throw new DisplayUsageException();
        }
      };
      builder.put("-h", helpOption);
      builder.put("--help", helpOption);

      Method leftoverMethod = null;

      for (Field field : injectedClass.getDeclaredFields()) {
        checkArgument(!field.isAnnotationPresent(Leftovers.class),
            "Sorry, @Leftovers only works for methods at present"); // TODO(kevinb)
        Option option = field.getAnnotation(Option.class);
        if (option != null) {
          InjectableOption injectable = FieldOption.create(field);
          for (String optionName : option.value()) {
            builder.put(optionName, injectable);
          }
        }
      }
      for (Method method : injectedClass.getDeclaredMethods()) {
        if (method.isAnnotationPresent(Leftovers.class)) {
          checkArgument(!isStaticOrAbstract(method),
              "@Leftovers method cannot be static or abstract");
          checkArgument(!method.isAnnotationPresent(Option.class),
              "method has both @Option and @Leftovers");
          checkArgument(leftoverMethod == null, "Two methods have @Leftovers");

          method.setAccessible(true);
          leftoverMethod = method;

          // TODO: check type is a supertype of ImmutableList<String>
        }
        Option option = method.getAnnotation(Option.class);
        if (option != null) {
          InjectableOption injectable = MethodOption.create(method);
          for (String optionName : option.value()) {
            builder.put(optionName, injectable);
          }
        }
      }

      ImmutableMap<String, InjectableOption> optionMap = builder.build();
      return new InjectionMap(optionMap, leftoverMethod);
    }

    final ImmutableMap<String, InjectableOption> optionMap;
    final Method leftoversMethod;

    InjectionMap(ImmutableMap<String, InjectableOption> optionMap, Method leftoversMethod) {
      this.optionMap = optionMap;
      this.leftoversMethod = leftoversMethod;
    }

    InjectableOption getInjectableOption(String optionName) throws InvalidCommandException {
      InjectableOption injectable = optionMap.get(optionName);
      if (injectable == null) {
        throw new InvalidCommandException("Invalid option: %s", optionName);
      }
      return injectable;
    }
  }

  private static class FieldOption extends InjectableOption {
    private static InjectableOption create(Field field) {
      field.setAccessible(true);
      Type type = field.getGenericType();

      if (type instanceof Class) {
        return new FieldOption(field, (Class<?>) type);
      }
      throw new IllegalArgumentException("can't inject parameterized types etc.");
    }

    private Field field;
    private boolean isBoolean;
    private Parser<?> parser;

    private FieldOption(Field field, Class<?> c) {
      this.field = field;
      this.isBoolean = c == boolean.class || c == Boolean.class;
      try {
        this.parser = Parsers.conventionalParser(Primitives.wrap(c));
      } catch (NoSuchMethodException e) {
        throw new IllegalArgumentException("No suitable String-conversion method");
      }
    }

    @Override boolean isBoolean() {
      return isBoolean;
    }

    @Override void inject(String valueText, Object injectee) throws InvalidCommandException {
      Object value = convert(parser, valueText);
      try {
        field.set(injectee, value);
      } catch (IllegalAccessException impossible) {
        throw new AssertionError(impossible);
      }
    }
  }

  private static class MethodOption extends InjectableOption {
    private static InjectableOption create(Method method) {
      checkArgument(!isStaticOrAbstract(method),
          "@Option methods cannot be static or abstract");
      Class<?>[] classes = method.getParameterTypes();
      checkArgument(classes.length == 1, "Method does not have exactly one argument: " + method);
      return new MethodOption(method, classes[0]);
    }

    private Method method;
    private boolean isBoolean;
    private Parser<?> parser;

    private MethodOption(Method method, Class<?> c) {
      this.method = method;
      this.isBoolean = c == boolean.class || c == Boolean.class;
      try {
        this.parser = Parsers.conventionalParser(Primitives.wrap(c));
      } catch (NoSuchMethodException e) {
        throw new IllegalArgumentException("No suitable String-conversion method");
      }

      method.setAccessible(true);
    }

    @Override boolean isBoolean() {
      return isBoolean;
    }

    @Override boolean delayedInjection() {
      return true;
    }

    @Override void inject(String valueText, Object injectee) throws InvalidCommandException {
      invokeMethod(injectee, method, convert(parser, valueText));
    }
  }

  private static Object convert(Parser<?> parser, String valueText) throws InvalidCommandException {
    Object value;
    try {
      value = parser.parse(valueText);
    } catch (ParseException e) {
      throw new InvalidCommandException("wrong datatype: " + e.getMessage());
    }
    return value;
  }

  private void parseLongOption(String arg, Iterator<String> args) throws InvalidCommandException {
    String name = arg.replaceFirst("^--no-", "--");
    String value = null;

    // Support "--name=value" as well as "--name value".
    int equalsIndex = name.indexOf('=');
    if (equalsIndex != -1) {
      value = name.substring(equalsIndex + 1);
      name = name.substring(0, equalsIndex);
    }

    InjectableOption injectable = injectionMap.getInjectableOption(name);

    if (value == null) {
      value = injectable.isBoolean()
          ? Boolean.toString(!arg.startsWith("--no-"))
          : grabNextValue(args, name);
    }
    injectNowOrLater(injectable, value);
  }

  private void injectNowOrLater(InjectableOption injectable, String value)
      throws InvalidCommandException {
    if (injectable.delayedInjection()) {
      pendingInjections.add(new PendingInjection(injectable, value));
    } else {
      injectable.inject(value, injectee);
    }
  }

  private static class PendingInjection {
    InjectableOption injectableOption;
    String value;

    private PendingInjection(InjectableOption injectableOption, String value) {
      this.injectableOption = injectableOption;
      this.value = value;
    }
  }

  // Given boolean options a and b, and non-boolean option f, we want to allow:
  // -ab
  // -abf out.txt
  // -abfout.txt
  // (But not -abf=out.txt --- POSIX doesn't mention that either way, but GNU expressly forbids it.)

  private void parseShortOptions(String arg, Iterator<String> args) throws InvalidCommandException {
    for (int i = 1; i < arg.length(); ++i) {
      String name = "-" + arg.charAt(i);
      InjectableOption injectable = injectionMap.getInjectableOption(name);

      String value;
      if (injectable.isBoolean()) {
        value = "true";
      } else {
        // We need a value. If there's anything left, we take the rest of this "short option".
        if (i + 1 < arg.length()) {
          value = arg.substring(i + 1);
          i = arg.length() - 1; // delayed "break"

        // otherwise the next arg
        } else {
          value = grabNextValue(args, name);
        }
      }
      injectNowOrLater(injectable, value);
    }
  }

  private static void invokeMethod(Object injectee, Method method, Object value)
      throws InvalidCommandException {
    try {
      method.invoke(injectee, value);
    } catch (IllegalAccessException impossible) {
      throw new AssertionError(impossible);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      Throwables.propagateIfPossible(cause, InvalidCommandException.class);
      throw new RuntimeException(e);
    }
  }

  private String grabNextValue(Iterator<String> args, String name)
      throws InvalidCommandException {
    if (args.hasNext()) {
      return args.next();
    } else {
      throw new InvalidCommandException("option '" + name + "' requires an argument");
    }
  }

  private static boolean isStaticOrAbstract(Method method) {
    int modifiers = method.getModifiers();
    return Modifier.isStatic(modifiers) || Modifier.isAbstract(modifiers);
  }
}
