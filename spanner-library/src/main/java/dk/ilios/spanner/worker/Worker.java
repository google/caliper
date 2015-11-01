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
package dk.ilios.spanner.worker;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Set;
import java.util.SortedMap;

import dk.ilios.spanner.AfterExperiment;
import dk.ilios.spanner.BeforeExperiment;
import dk.ilios.spanner.Param;
import dk.ilios.spanner.model.Measurement;
import dk.ilios.spanner.util.Parser;
import dk.ilios.spanner.util.Parsers;
import dk.ilios.spanner.util.Reflection;

/**
 * A {@link Worker} collects measurements on behalf of a particular Instrument.
 */
public abstract class Worker {

    private final SortedMap<String, String> userParameters;
    private Set<Method> beforeExperimentMethods;
    private Set<Method> afterExperimentMethods;

    protected final Method benchmarkMethod;
    protected final Object benchmark;

    protected Worker(Object benchmark, Method method, SortedMap<String, String> userParameters) {
        this.benchmark = benchmark;
        this.benchmarkMethod = method;
        this.beforeExperimentMethods = Reflection.getAnnotatedMethods(benchmark.getClass(), BeforeExperiment.class);
        this.afterExperimentMethods = Reflection.getAnnotatedMethods(benchmark.getClass(), AfterExperiment.class);
        this.userParameters = userParameters;
    }

    /**
     * Injects any configured parameters into the class.
     */
    private void injectParams() {
        for (Field field : benchmark.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Param.class)) {
                String fieldName = field.getName();
                String value = userParameters.get(fieldName);
                field.setAccessible(true);
                try {
                    Parser<?> parser = Parsers.conventionalParser(field.getType());
                    field.set(benchmark, parser.parse(value));
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Initializes the benchmark object.
     */
    public final void setUpBenchmark() throws Exception {
        injectParams();
        for (Method method : beforeExperimentMethods) {
            method.invoke(benchmark);
        }
    }

    /**
     * Called once before all measurements but after benchmark setup.
     */
    public void bootstrap() throws Exception {
    }

    /**
     * Called immediately before {@link #measure()}.
     *
     * @param inWarmup whether we are in warmup, or taking real measurements. Used by
     *                 some implementations to skip forcing GC to make warmup faster.
     */
    public void preMeasure(boolean inWarmup) throws Exception {
    }

    /**
     * Called immediately after {@link #measure()}.
     */
    public void postMeasure() throws Exception {
    }

    /**
     * Template method for workers that produce multiple measurements.
     */
    public abstract Iterable<Measurement> measure() throws Exception;

    /**
     * Tears down the benchmark object.
     */
    public final void tearDownBenchmark() throws Exception {
        for (Method method : afterExperimentMethods) {
            method.invoke(benchmark);
        }
    }
}
