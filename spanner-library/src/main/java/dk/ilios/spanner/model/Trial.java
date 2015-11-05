/*
 * Copyright (C) 2012 Google Inc.
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
 *
 * Original author gak@google.com (Gregory Kick)
 */

package dk.ilios.spanner.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.rank.Percentile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import dk.ilios.spanner.internal.Experiment;
import dk.ilios.spanner.internal.trial.TrialContext;
import dk.ilios.spanner.json.ExcludeFromJson;

/**
 * An invocation of a single {@link Experiment}.
 * <p>
 * Once a trial is completed, call {@link #getResult()} to get the result. From that point, all further modifications
 * to the object Trial object will fail.
 */
public final class Trial {

    private UUID id;
    private Run run;
    private InstrumentSpec instrumentSpec;
    private Scenario scenario;
    private Experiment experiment;
    private List<Measurement> measurements = new ArrayList<>();
    private List<String> messages = new ArrayList<String>();

    @ExcludeFromJson
    private boolean trialComplete;

    @ExcludeFromJson
    private boolean resultsCalculated;

    @ExcludeFromJson
    private Percentile percentile;

    @ExcludeFromJson
    private DescriptiveStatistics descriptiveStatistics;

    private Trial(Builder builder) {
        this.id = builder.id;
        this.run = builder.run;
        this.instrumentSpec = builder.instrumentSpec;
        this.scenario = builder.scenario;
        this.experiment = builder.experiment;
    }

    public UUID id() {
        return id;
    }

    public Run run() {
        return run;
    }

    public InstrumentSpec instrumentSpec() {
        return instrumentSpec;
    }

    public Scenario scenario() {
        return scenario;
    }

    public Experiment experiment() {
        return experiment;
    }

    public List<Measurement> measurements() {
        return measurements;
    }

    public void addMeasurement(Measurement.Builder measurementBuilder) {
        checkIsComplete();
        addMeasurement(measurementBuilder.build());
    }

    public void addMeasurement(Measurement measurement) {
        checkIsComplete();
        this.measurements.add(measurement);
    }

    public void addAllMeasurements(Iterable<Measurement> measurements) {
        checkIsComplete();
        Iterables.addAll(this.measurements, measurements);
    }

    public void addAllMessages(Iterable<String> messages) {
        checkIsComplete();
        Iterables.addAll(this.messages, messages);
    }

    public void addMessage(String message) {
        checkIsComplete();
        messages.add(message);
    }

    /**
     * Mark Trial as done and calculate results.
     */
    public void calculateResults() {
        checkResultsCalculated(false);

        double[] weightedValues = new double[measurements.size()];
        int i = 0;
        for (Measurement measurement : measurements) {
            weightedValues[i] = measurement.value().magnitude() / measurement.weight();
            i++;
        }
        percentile = new Percentile();
        percentile.setData(weightedValues);
        descriptiveStatistics = new DescriptiveStatistics(weightedValues);
        if (experiment.getBaseline() != null) {
            experiment.getBaseline().calculateResults();
        }
        resultsCalculated = true;
    }

    private void checkResultsCalculated(boolean calculated) {
        if (calculated && !resultsCalculated) {
            throw new IllegalStateException("Results have not been calculated.");
        } else if (!calculated && resultsCalculated) {
            throw new IllegalStateException("Results have already been calculated.");
        }
    }

    /**
     * Returns the results. Will calculate results if not already done
     */
    public Result getResult() {
        trialComplete = true;
        calculateResults();
        return new Result(this, experiment, ImmutableList.copyOf(messages));
    }

    public double getMin() {
        checkResultsCalculated(true);
        return descriptiveStatistics.getMin();
    }

    public double getMax() {
        checkResultsCalculated(true);
        return descriptiveStatistics.getMax();
    }

    /**
     * @param percentile [0, 100]
     */
    public double getPercentile(int percentile) {
        checkResultsCalculated(true);
        return this.percentile.evaluate(percentile);
    }

    public double getMedian() {
        checkResultsCalculated(true);
        return this.percentile.evaluate(50);
    }

    public boolean hasBaseline() {
        return experiment.getBaseline() != null;
    }

    /**
     * Returns changes from baseline or {@code null} if no baseline exists.
     * @return Change in percent from baseline. {@code 1.0} is 100%.
     */
    public Double getChangeFromBaseline() {
        checkResultsCalculated(true);
        if (experiment.getBaseline() == null) return null;

        double newMedian = getMedian();
        double oldMedian = experiment.getBaseline().getMedian();

        return (oldMedian - newMedian) / oldMedian;
    }

    public TimeUnit getUnit() {
        return TimeUnit.NANOSECONDS;
    }

    private void checkIsComplete() {
        if (trialComplete) {
            throw new RuntimeException("Trial is complete. No further modifications are allowed");
        }
    }

    private void checkNotIsComplete() {
        if (!trialComplete) {
            throw new RuntimeException("Trial is not complete. Results not yet available");
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Trial) {
            Trial that = (Trial) obj;
            return this.id.equals(that.id)
                    && this.run.equals(that.run)
                    && this.instrumentSpec.equals(that.instrumentSpec)
                    && this.scenario.equals(that.scenario)
                    && this.measurements.equals(that.measurements)
                    && this.experiment.equals(that.experiment)
                    && this.messages.equals(that.messages)
                    && this.trialComplete == that.trialComplete;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, run, instrumentSpec, scenario, experiment, measurements, messages, trialComplete);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("run", run)
                .add("instrumentSpec", instrumentSpec)
                .add("scenario", scenario)
                .add("experiment", experiment)
                .add("measurements", measurements)
                .add("messages", messages)
                .add("trialComplete", trialComplete)
                .toString();
    }

    /**
     * Builder for making Trial objects that are ready to be run.
     */
    public static final class Builder {
        private final UUID id;
        private final int trialNumber;
        private Run run;
        private InstrumentSpec instrumentSpec;
        private Scenario scenario;
        private Experiment experiment;

        public Builder(TrialContext context) {
            checkNotNull(context);
            this.id = context.getTrialId();
            this.experiment = context.getExperiment();
            this.trialNumber = context.getTrialNumber();
        }

        public Builder run(Run.Builder runBuilder) {
            return run(runBuilder.build());
        }

        public Builder run(Run run) {
            this.run = checkNotNull(run);
            return this;
        }

        public Builder instrumentSpec(InstrumentSpec.Builder instrumentSpecBuilder) {
            return instrumentSpec(instrumentSpecBuilder.build());
        }

        public Builder instrumentSpec(InstrumentSpec instrumentSpec) {
            this.instrumentSpec = checkNotNull(instrumentSpec);
            return this;
        }

        public Builder scenario(Scenario.Builder scenarioBuilder) {
            return scenario(scenarioBuilder.build());
        }

        public Builder scenario(Scenario scenario) {
            this.scenario = checkNotNull(scenario);
            return this;
        }

        public Trial build() {
            checkState(run != null);
            checkState(instrumentSpec != null);
            checkState(scenario != null);
            checkState(experiment != null);
            return new Trial(this);
        }
    }

    /**
     * Container for the results of the current trail.
     */
    public class Result {
        private final Trial trial;
        private final Experiment experiment;
        private final ImmutableList<String> trialMessages;

        Result(Trial trial, Experiment experiment, ImmutableList<String> trialMessages) {
            this.trial = trial;
            this.experiment = experiment;
            this.trialMessages = trialMessages;
        }

        public Experiment getExperiment() {
            return experiment;
        }

        public Trial getTrial() {
            return trial;
        }

        public ImmutableList<String> getTrialMessages() {
            return trialMessages;
        }
    }
}
