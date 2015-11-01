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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

import java.lang.reflect.Method;
import java.util.Map;

import dk.ilios.spanner.CustomMeasurement;
import dk.ilios.spanner.internal.CommonInstrumentOptions;
import dk.ilios.spanner.model.Measurement;
import dk.ilios.spanner.model.Value;
import dk.ilios.spanner.util.Util;

/**
 * Worker for methods doing their own measurements (custom measurements).
 */
public final class CustomMeasurementWorker extends Worker {
    private final Options options;
    private final String unit;
    private final String description;

    public CustomMeasurementWorker(Object benchmarkClassInstance,
                                   Method benchmarkMethod,
                                   Map<String, String> workerOptions,
                                   ImmutableSortedMap<String, String> userParameters) {
        super(benchmarkClassInstance, benchmarkMethod, userParameters);
        this.options = new Options(workerOptions);
        CustomMeasurement annotation = benchmarkMethod.getAnnotation(CustomMeasurement.class);
        this.unit = annotation.units();
        this.description = annotation.description();
    }

    @Override
    public void preMeasure(boolean inWarmup) throws Exception {
        if (options.gcBeforeEach && !inWarmup) {
            Util.forceGc();
        }
    }

    @Override
    public Iterable<Measurement> measure() throws Exception {
        double measured = (Double) benchmarkMethod.invoke(benchmark);
        return ImmutableSet.of(new Measurement.Builder()
                .value(Value.create(measured, unit))
                .weight(1)
                .description(description)
                .build());
    }

    private static class Options {
        final boolean gcBeforeEach;

        Options(Map<String, String> options) {
            String key = CommonInstrumentOptions.GC_BEFORE_EACH.getKey();
            this.gcBeforeEach = Boolean.parseBoolean(options.get(key));
        }
    }
}
