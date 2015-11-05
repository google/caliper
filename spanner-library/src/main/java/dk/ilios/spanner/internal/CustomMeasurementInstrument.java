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

package dk.ilios.spanner.internal;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dk.ilios.spanner.CustomMeasurement;
import dk.ilios.spanner.bridge.AbstractLogMessageVisitor;
import dk.ilios.spanner.bridge.StopMeasurementLogMessage;
import dk.ilios.spanner.exception.SkipThisScenarioException;
import dk.ilios.spanner.exception.UserCodeException;
import dk.ilios.spanner.internal.trial.TrialSchedulingPolicy;
import dk.ilios.spanner.model.Measurement;
import dk.ilios.spanner.util.Util;
import dk.ilios.spanner.worker.CustomMeasurementWorker;
import dk.ilios.spanner.worker.Worker;

import static com.google.common.base.Throwables.propagateIfInstanceOf;

/**
 * Instrument for measuring how long it takes for a method to execute. When using this instrument, the benchmark code
 * itself returns the value.
 */
public final class CustomMeasurementInstrument extends Instrument {

    @Override
    public boolean isBenchmarkMethod(Method method) {
        return method.isAnnotationPresent(CustomMeasurement.class);
    }

    @Override
    public Instrumentation createInstrumentation(Method benchmarkMethod)
            throws InvalidBenchmarkException {
        if (benchmarkMethod.getParameterTypes().length != 0) {
            throw new InvalidBenchmarkException(
                    "Arbitrary measurement methods should take no parameters: " + benchmarkMethod.getName());
        }

        if (benchmarkMethod.getReturnType() != double.class) {
            throw new InvalidBenchmarkException(
                    "Arbitrary measurement methods must have a return type of double: "
                            + benchmarkMethod.getName());
        }

        // Static technically doesn't hurt anything, but it's just the completely wrong idea
        if (Util.isStatic(benchmarkMethod)) {
            throw new InvalidBenchmarkException(
                    "Arbitrary measurement methods must not be static: " + benchmarkMethod.getName());
        }

        if (!Util.isPublic(benchmarkMethod)) {
            throw new InvalidBenchmarkException(
                    "Arbitrary measurement methods must be public: " + benchmarkMethod.getName());
        }

        return new ArbitraryMeasurementInstrumentation(benchmarkMethod);
    }

    @Override
    public TrialSchedulingPolicy schedulingPolicy() {
        // We could allow it here but in general it would depend on the particular measurement so it
        // should probably be configured by the user.  For now we just disable it.
        return TrialSchedulingPolicy.SERIAL;
    }

    private final class ArbitraryMeasurementInstrumentation extends Instrumentation {
        protected ArbitraryMeasurementInstrumentation(Method benchmarkMethod) {
            super(benchmarkMethod);
        }

        @Override
        public void dryRun(Object benchmark) throws InvalidBenchmarkException {
            try {
                benchmarkMethod.invoke(benchmark);
            } catch (IllegalAccessException impossible) {
                throw new AssertionError(impossible);
            } catch (InvocationTargetException e) {
                Throwable userException = e.getCause();
                propagateIfInstanceOf(userException, SkipThisScenarioException.class);
                throw new UserCodeException(userException);
            }
        }

        @Override
        public Class<? extends Worker> workerClass() {
            return CustomMeasurementWorker.class;
        }

        @Override
        public ImmutableMap<String, String> workerOptions() {
            String key = CommonInstrumentOptions.GC_BEFORE_EACH.getKey();
            return ImmutableMap.of(key, options.get(key));
        }

        @Override
        MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
            return new SingleMeasurementCollectingVisitor();
        }
    }

    @Override
    public ImmutableSet<String> instrumentOptions() {
        return ImmutableSet.of(CommonInstrumentOptions.GC_BEFORE_EACH.getKey());
    }

    private static final class SingleMeasurementCollectingVisitor extends AbstractLogMessageVisitor
            implements MeasurementCollectingVisitor {
        Optional<Measurement> measurement = Optional.absent();

        @Override
        public boolean isDoneCollecting() {
            return measurement.isPresent();
        }

        @Override
        public boolean isWarmupComplete() {
            return true;
        }

        @Override
        public ImmutableList<Measurement> getMeasurements() {
            return ImmutableList.copyOf(measurement.asSet());
        }

        @Override
        public void visit(StopMeasurementLogMessage logMessage) {
            this.measurement = Optional.of(Iterables.getOnlyElement(logMessage.measurements()));
        }

        @Override
        public ImmutableList<String> getMessages() {
            return ImmutableList.of();
        }
    }
}
