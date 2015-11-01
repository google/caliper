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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import dk.ilios.spanner.internal.InvalidBenchmarkException;

/**
 * Represents all the injectable parameter fields of a single kind (@Param) found in a
 * benchmark class. Has nothing to do with particular choices of <i>values</i> for these parameters
 * (except that it knows how to find the <i>default</i> values).
 */
public final class ParameterSet {

    /**
     * Returns the set of all parameters and their possible values.
     * @param benchmarkClass    Benchmark class.
     * @param annotationClass   Annotation specifying a parameter.
     * @return
     * @throws InvalidBenchmarkException
     */
    public static ParameterSet create(Class<?> benchmarkClass, Class<? extends Annotation> annotationClass) throws InvalidBenchmarkException {
        // deterministic order, not reflection order
        ImmutableMap.Builder<String, Parameter> parametersBuilder =
                ImmutableSortedMap.naturalOrder();

        for (Field field : benchmarkClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(annotationClass)) {
                Parameter parameter = Parameter.create(field);
                parametersBuilder.put(field.getName(), parameter);
            }
        }
        return new ParameterSet(parametersBuilder.build());
    }

    final ImmutableMap<String, Parameter> map;

    private ParameterSet(ImmutableMap<String, Parameter> map) {
        this.map = map;
    }

    public Set<String> names() {
        return map.keySet();
    }

    public Parameter get(String name) {
        return map.get(name);
    }

    /**
     * Create the combined set of Parameters set in code in the benchmark class and any potential overrides.
     * NOTE: Overrides not used currently. Legacy from Gauge where commandline params took precedence.
     */
    public ImmutableSetMultimap<String, String> fillInDefaultsFor(ImmutableSetMultimap<String, String> explicitValues) {
        ImmutableSetMultimap.Builder<String, String> combined = ImmutableSetMultimap.builder();

        // For user parameters, this'll actually be the same as fromClass.keySet(), since any extras
        // given at the command line are treated as errors; for VM parameters this is not the case.
        for (String name : Sets.union(map.keySet(), explicitValues.keySet())) {
            Parameter parameter = map.get(name);
            ImmutableCollection<String> values = explicitValues.containsKey(name)
                    ? explicitValues.get(name)
                    : parameter.defaults();

            combined.putAll(name, values);
            if (values.isEmpty()) {
                throw new IllegalArgumentException("ERROR: No default value provided for " + name);
            }
        }
        return combined.orderKeysBy(Ordering.natural()).build();
    }

    public void injectAll(Object benchmark, Map<String, String> actualValues) {
        for (Parameter parameter : map.values()) {
            String value = actualValues.get(parameter.name());
            parameter.inject(benchmark, value);
        }
    }
}
