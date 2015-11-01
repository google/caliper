/**
 * Copyright (C) 2009 Google Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.ilios.spanner.internal;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.rank.Percentile;

import java.io.Closeable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import dk.ilios.spanner.exception.TrialFailureException;
import dk.ilios.spanner.log.StdOut;
import dk.ilios.spanner.model.BenchmarkSpec;
import dk.ilios.spanner.model.InstrumentSpec;
import dk.ilios.spanner.model.Measurement;
import dk.ilios.spanner.model.Scenario;
import dk.ilios.spanner.model.Trial;

/**
 * Prints a brief summary of the results collected.  It does not contain the measurements themselves
 * as that is the responsibility of the webapp.
 */
final class ConsoleOutput implements Closeable {
    private final StdOut stdout;

    private final Set<InstrumentSpec> instrumentSpecs = Sets.newHashSet();
    private final Set<BenchmarkSpec> benchmarkSpecs = Sets.newHashSet();
    private int numMeasurements = 0;
    private int trialsCompleted = 0;
    private final int numberOfTrials;
    private final Stopwatch stopwatch;

    public ConsoleOutput(StdOut stdout, int numberOfTrials, Stopwatch stopwatch) {
        this.stdout = stdout;
        this.numberOfTrials = numberOfTrials;
        this.stopwatch = stopwatch;
    }

    /**
     * Prints a short message when we observe a trial failure.
     */
    void processFailedTrial(TrialFailureException e) {
        trialsCompleted++;
        // TODO(lukes): it would be nice to print which trial failed.  Consider adding Experiment data
        // to the TrialFailureException.
        stdout.println(
                "ERROR: Trial failed to complete (its results will not be included in the run):\n"
                        + "  " + e.getMessage());
        stdout.flush();
    }

    /**
     * Prints a summary of a successful trial result.
     */
    void processTrial(Trial.Result result) {
        Trial baseline = result.getExperiment().getBaseline();

        trialsCompleted++;

        stdout.printf("Trial Report (%d of %d):%n  Experiment %s%n",
                trialsCompleted, numberOfTrials, result.getExperiment());
        if (!result.getTrialMessages().isEmpty()) {
            stdout.println("  Messages:");
            for (String message : result.getTrialMessages()) {
                stdout.print("    ");
                stdout.println(message);
            }
        }
        Trial trial = result.getTrial();

        // Group measurements by their description
        // TODO Why? All measurements for a single trial should have the same description
        ImmutableListMultimap<String, Measurement> measurementsIndex =
                new ImmutableListMultimap.Builder<String, Measurement>()
                        .orderKeysBy(Ordering.natural())
                        .putAll(Multimaps.index(trial.measurements(), new Function<Measurement, String>() {
                            @Override
                            public String apply(Measurement input) {
                                return input.description();
                            }
                        }))
                        .build();

        stdout.println("  Results:");
        for (Map.Entry<String, Collection<Measurement>> entry : measurementsIndex.asMap().entrySet()) {

            Collection<Measurement> measurements = entry.getValue();
            String unit = measurements.iterator().next().value().unit();

            double[] weightedValues = new double[measurements.size()];
            int i = 0;
            for (Measurement measurement : measurements) {
                weightedValues[i] = measurement.value().magnitude() / measurement.weight();
                i++;
            }
            Percentile percentile = new Percentile();
            percentile.setData(weightedValues);
            DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics(weightedValues);
            stdout.printf(
                    "    %s%s: min=%.2f, 1st qu.=%.2f, median=%.2f (%s), mean=%.2f, 3rd qu.=%.2f, max=%.2f%n",
                    entry.getKey(), unit.isEmpty() ? "" : "(" + unit + ")",
                    descriptiveStatistics.getMin(), percentile.evaluate(25),
                    percentile.evaluate(50),
                    calculateDiff(percentile.evaluate(50), baseline),
                    descriptiveStatistics.getMean(), percentile.evaluate(75),
                    descriptiveStatistics.getMax());
        }

        instrumentSpecs.add(trial.instrumentSpec());
        Scenario scenario = trial.scenario();
        benchmarkSpecs.add(scenario.benchmarkSpec());
        numMeasurements += trial.measurements().size();
    }

    private String calculateDiff(double newMedian, Trial oldValue) {
        if (oldValue == null) return "-";
        double oldMedian = oldValue.getMedian();
        double diff = (oldMedian - newMedian) * 100 / oldMedian;
        return String.format("%s%.2f%%", diff > 0 ? "+" : "", diff);
    }

    @Override
    public void close() {
        if (trialsCompleted == numberOfTrials) {  // if we finished all the trials
            stdout.printf("Collected %d measurements from:%n", numMeasurements);
            stdout.printf("  %d instrument(s)%n", instrumentSpecs.size());
            stdout.printf("  %d benchmark(s)%n", benchmarkSpecs.size());
            stdout.println();
            stdout.format("Execution complete: %s.%n", stopwatch.stop());
            stdout.flush();
        }
    }
}
