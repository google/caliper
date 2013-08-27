/**
 * Copyright (C) 2009 Google Inc.
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
import com.google.caliper.model.BenchmarkSpec;
import com.google.caliper.model.InstrumentSpec;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Scenario;
import com.google.caliper.model.Trial;
import com.google.caliper.model.VmSpec;
import com.google.caliper.util.Stdout;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.rank.Percentile;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Prints a brief summary of the results collected.  It does not contain the measurements themselves
 * as that is the responsibility of the webapp.
 */
final class ConsoleResultProcessor implements ResultProcessor {
  private final PrintWriter stdout;

  private Set<InstrumentSpec> instrumentSpecs = Sets.newHashSet();
  private Set<VmSpec> vmSpecs = Sets.newHashSet();
  private Set<BenchmarkSpec> benchmarkSpecs = Sets.newHashSet();
  private int numMeasurements = 0;

  @Inject ConsoleResultProcessor(@Stdout PrintWriter stdout) {
    this.stdout = stdout;
  }

  @Override public void processTrial(Trial trial) {
    ImmutableListMultimap<String, Measurement> measurementsIndex =
        new ImmutableListMultimap.Builder<String, Measurement>()
            .orderKeysBy(Ordering.natural())
            .putAll(Multimaps.index(trial.measurements(), new Function<Measurement, String>() {
              @Override public String apply(Measurement input) {
                return input.description();
              }
            }))
            .build();
    for (Entry<String, Collection<Measurement>> entry : measurementsIndex.asMap().entrySet()) {
      Collection<Measurement> measurements = entry.getValue();
      ImmutableSet<String> units = FluentIterable.from(measurements)
          .transform(new Function<Measurement, String>() {
            @Override public String apply(Measurement input) {
              return input.value().unit();
            }
          }).toSet();
      double[] weightedValues = new double[measurements.size()];
      int i = 0;
      for (Measurement measurement : measurements) {
        weightedValues[i] = measurement.value().magnitude() / measurement.weight();
        i++;
      }
      Percentile percentile = new Percentile();
      percentile.setData(weightedValues);
      DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics(weightedValues);
      String unit = Iterables.getOnlyElement(units);
      stdout.printf(
          "  %s%s: min=%.2f, 1st qu.=%.2f, median=%.2f, mean=%.2f, 3rd qu.=%.2f, max=%.2f%n",
          entry.getKey(), unit.isEmpty() ? "" : "(" + unit + ")",
          descriptiveStatistics.getMin(), percentile.evaluate(25),
          percentile.evaluate(50), descriptiveStatistics.getMean(),
          percentile.evaluate(75), descriptiveStatistics.getMax());
    }

    instrumentSpecs.add(trial.instrumentSpec());
    Scenario scenario = trial.scenario();
    vmSpecs.add(scenario.vmSpec());
    benchmarkSpecs.add(scenario.benchmarkSpec());
    numMeasurements += trial.measurements().size();
  }

  @Override public void close() {
    stdout.printf("Collected %d measurements from:%n", numMeasurements);
    stdout.printf("  %d instrument(s)%n", instrumentSpecs.size());
    stdout.printf("  %d virtual machine(s)%n", vmSpecs.size());
    stdout.printf("  %d benchmark(s)%n", benchmarkSpecs.size());
    stdout.flush();
  }
}
