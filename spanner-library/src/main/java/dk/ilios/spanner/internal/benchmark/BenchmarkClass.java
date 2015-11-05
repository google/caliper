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

package dk.ilios.spanner.internal.benchmark;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagateIfInstanceOf;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dk.ilios.spanner.AfterExperiment;
import dk.ilios.spanner.BeforeExperiment;
import dk.ilios.spanner.Benchmark;
import dk.ilios.spanner.BenchmarkConfiguration;
import dk.ilios.spanner.SpannerConfig;
import dk.ilios.spanner.Param;
import dk.ilios.spanner.exception.InvalidCommandException;
import dk.ilios.spanner.internal.InvalidBenchmarkException;
import dk.ilios.spanner.exception.SkipThisScenarioException;
import dk.ilios.spanner.exception.UserCodeException;
import dk.ilios.spanner.util.Reflection;

/**
 * An instance of this type represents a user-provided Benchmark class. It manages creating, setting up and
 * destroying instances of that class.
 */
public final class BenchmarkClass {

    private static final Logger logger = Logger.getLogger(BenchmarkClass.class.getName());
    private final Class<?> classReference;
    private final Object classInstance;
    private final List<Method> benchmarkMethods;
    private final ParameterSet userParameters;

    /**
     * Creates a wrapper around all benchmark methods in the given class.
     *
     * @param benchmarkClass
     * @throws InvalidBenchmarkException
     */
    public BenchmarkClass(Class<?> benchmarkClass) throws InvalidBenchmarkException {
        this(benchmarkClass, (List) null);
    }

    /**
     * Creates a wrapper around the selected benchmark method.
     *
     * @param benchmarkClass
     * @param method
     * @throws InvalidBenchmarkException If class or method isn't a proper benchmark class/method.
     */
    public BenchmarkClass(Class<?> benchmarkClass, Method method) throws InvalidBenchmarkException {
        this(benchmarkClass, Arrays.asList(method));
    }

    /**
     * Creates a wrapper around the selected benchmark methods.
     *
     * @param benchmarkClass
     * @param methods
     * @throws InvalidBenchmarkException If the class or methods are .
     */
    public BenchmarkClass(Class<?> benchmarkClass, Collection<Method> methods) throws InvalidBenchmarkException {

        // Initialize Benchmark class
        this.classReference = checkNotNull(benchmarkClass);
        validateBenchmarkClass(classReference);
        try {
            classInstance = benchmarkClass.newInstance();
        } catch (InstantiationException  e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // Initialize benchmark methods
        if (methods == null) {
            this.benchmarkMethods = findAllBenchmarkMethods(classReference);
        } else {
            this.benchmarkMethods = new ArrayList<>(methods);
        }
        validateBenchmarkMethods(benchmarkMethods);

        // Initialize benchmark parameters
        this.userParameters = ParameterSet.create(classReference, Param.class);
    }

    private void validateBenchmarkMethods(List<Method> methods) throws InvalidBenchmarkException {
        for (Method method : methods) {

            // Verify annotations
            if (!method.isAnnotationPresent(Benchmark.class)) {
                throw new InvalidBenchmarkException(String.format("Method %s isn't a benchmark method. " +
                                "It is missing @Benchmark.", method.getName()));
            }

            // Verify modifiers
            int modifiers = method.getModifiers();
            if (modifiers != Modifier.PUBLIC) {
                throw new InvalidBenchmarkException("Benchmark methods must only be public: " + method.getName());
            }

            // Verify parameter types
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length > 1) {
                throw new InvalidBenchmarkException("Only 1 int or long parameter allowed: " + method.getName());
            }

            for (Class<?> parameterClass : parameterTypes) {
                if (!parameterClass.equals(int.class) && !parameterClass.equals(long.class)) {
                    throw new InvalidBenchmarkException("Only 1 int or long parameter allowed: " + method.getName());
                }
            }

            // Verify return type
            // Right now we just ignore any return type.
            // TODO Check return type when adding back support for ArbitraryMeasurements
        }
    }

    private List<Method> findAllBenchmarkMethods(Class<?> benchmarkClass) {
        List<Method> benchmarkMethods = new ArrayList<>();
        for (Method method : benchmarkClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Benchmark.class)) {
                benchmarkMethods.add(method);
            }
        }
        return benchmarkMethods;
    }

    private void validateBenchmarkClass(Class<?> benchmarkClass) throws InvalidBenchmarkException {
        if (!benchmarkClass.getSuperclass().equals(Object.class)) {
            throw new InvalidBenchmarkException(
                    "%s must not extend any class other than %s. Prefer composition.",
                    benchmarkClass, Object.class);
        }
    }

    /**
     * Returns the simple name of the class that is being benchmarked.
     */
    public String getSimpleName() {
        return classReference.getSimpleName();
    }

    /**
     * Returns a instance of the Benchmark class.
     */
    public Object getInstance() {
        return classInstance;
    }

    /**
     * Returns the configuration for this class or the default configuration if no configuration is provided.
     */
    public SpannerConfig getConfiguration() {
        for (Field field : classReference.getDeclaredFields()) {
            if (field.isAnnotationPresent(BenchmarkConfiguration.class)) {
                try {
                    field.setAccessible(true);
                    return (SpannerConfig) field.get(classInstance);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return new SpannerConfig.Builder().build();
    }


    ImmutableSet<Method> beforeExperimentMethods() {
        return Reflection.getAnnotatedMethods(classReference, BeforeExperiment.class);
    }

    ImmutableSet<Method> afterExperimentMethods() {
        return Reflection.getAnnotatedMethods(classReference, AfterExperiment.class);
    }

    public ParameterSet userParameters() {
        return userParameters;
    }

    // TODO(gak): use these methods in the worker as well
    public void setUpBenchmark(Object benchmarkInstance) throws UserCodeException {
        boolean setupSuccess = false;
        try {
            callSetUp(benchmarkInstance);
            setupSuccess = true;
        } finally {
            // If setUp fails, we should call tearDown. If this method throws an exception, we
            // need to call tearDown from here, because no one else has the reference to the
            // Benchmark.
            if (!setupSuccess) {
                try {
                    callTearDown(benchmarkInstance);
                } catch (UserCodeException e) {
                    // The exception thrown during setUp shouldn't be lost, as it's probably more
                    // important to the user.
                    logger.log(
                            Level.INFO,
                            "in @AfterExperiment methods called because @BeforeExperiment methods failed",
                            e);
                }
            }
        }
    }

    public void cleanup(Object benchmark) throws UserCodeException {
        callTearDown(benchmark);
    }

    public String name() {
        return classReference.getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof BenchmarkClass) {
            BenchmarkClass that = (BenchmarkClass) obj;
            return this.classReference.equals(that.classReference);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(classReference);
    }

    @Override
    public String toString() {
        return name();
    }

    private void callSetUp(Object benchmark) throws UserCodeException {
        for (Method method : beforeExperimentMethods()) {
            try {
                method.invoke(benchmark);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                propagateIfInstanceOf(e.getCause(), SkipThisScenarioException.class);
                throw new UserCodeException(
                        "Exception thrown from a @BeforeExperiment method", e.getCause());
            }
        }
    }

    private void callTearDown(Object benchmark) throws UserCodeException {
        for (Method method : afterExperimentMethods()) {
            try {
                method.invoke(benchmark);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                propagateIfInstanceOf(e.getCause(), SkipThisScenarioException.class);
                throw new UserCodeException(
                        "Exception thrown from an @AfterExperiment method", e.getCause());
            }
        }
    }

    void validateParameters(ImmutableSetMultimap<String, String> parameters)
            throws InvalidCommandException {
        for (String paramName : parameters.keySet()) {
            Parameter parameter = userParameters.get(paramName);
            if (parameter == null) {
                throw new InvalidCommandException("unrecognized parameter: " + paramName);
            }
            try {
                parameter.validate(parameters.get(paramName));
            } catch (InvalidBenchmarkException e) {
                // TODO(kevinb): this is weird.
                throw new InvalidCommandException(e.getMessage());
            }
        }
    }

    /**
     * Returns the canonical name for the Benchmark class.
     */
    public String getCanonicalName() {
        return classReference.getCanonicalName();
    }

    /**
     * Return all benchmark methods in this class.
     */
    public List<Method> getMethods() {
        return benchmarkMethods;
    }
}
