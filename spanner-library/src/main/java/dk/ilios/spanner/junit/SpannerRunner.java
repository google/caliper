package dk.ilios.spanner.junit;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;

import org.apache.commons.math.stat.descriptive.rank.Percentile;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.TestClass;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import dk.ilios.spanner.Benchmark;
import dk.ilios.spanner.BenchmarkConfiguration;
import dk.ilios.spanner.Spanner;
import dk.ilios.spanner.SpannerConfig;
import dk.ilios.spanner.exception.TrialFailureException;
import dk.ilios.spanner.internal.InvalidBenchmarkException;
import dk.ilios.spanner.json.ExcludeFromJson;
import dk.ilios.spanner.model.Measurement;
import dk.ilios.spanner.model.Run;
import dk.ilios.spanner.model.Trial;

/**
 * Runner for handling the individual Benchmarks.
 */
public class SpannerRunner extends Runner {

    private Object testInstance;
    private TestClass testClass;
    private List<Method> testMethods = new ArrayList();
    private SpannerConfig benchmarkConfiguration;

    public SpannerRunner(Class clazz) {
        testClass = new TestClass(clazz);
        try {
            testInstance = testClass.getJavaClass().newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        // Setup config (if any)
        List<FrameworkField> fields = testClass.getAnnotatedFields(BenchmarkConfiguration.class);

        if (fields.size() > 1) {
            throw new IllegalStateException("Only one @BenchmarkConfiguration allowed");
        }
        if (fields.size() > 0) {
            FrameworkField field = fields.get(0);
            try {
                if (!field.getType().equals(SpannerConfig.class)) {
                    throw new IllegalArgumentException("@BenchmarkConfiguration can only be set on " +
                            "GaugeConfiguration fields.");
                }
                benchmarkConfiguration = (SpannerConfig) field.get(testInstance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(field + " is not public", e);
            }
        }

        Method[] classMethods = clazz.getDeclaredMethods();
        for (int i = 0; i < classMethods.length; i++) {
            Method classMethod = classMethods[i];
            Class retClass = classMethod.getReturnType();
            int modifiers = classMethod.getModifiers();
            if (retClass == null || Modifier.isStatic(modifiers)
                    || !Modifier.isPublic(modifiers) || Modifier.isInterface(modifiers)
                    || Modifier.isAbstract(modifiers)) {
                continue;
            }
            if (classMethod.getAnnotation(Benchmark.class) != null) {
                testMethods.add(classMethod);
            }
            if (classMethod.getAnnotation(Ignore.class) != null) {
                testMethods.remove(classMethod);
            }
        }
    }

    @Override
    public Description getDescription() {
        Description spec = Description.createSuiteDescription(
                this.testClass.getName(),
                this.testClass.getJavaClass().getAnnotations()
        );
        return spec;
    }

    /**
     * @return the number of tests to be run by the receiver
     */
    public int testCount() {
        return testMethods.size();
    }

    //
    @Override
    public void run(final RunNotifier runNotifier) {
        // Because Description must have the same value when starting and finishing the unit test, we are introducing
        // a "fake" method called "Running". This acts as a placeholder for all running benchmarks, and we can
        // then determine how the unit test is displayed when it finishes or crashes.
        //
        // Only downside is that the duration of the benchmark test as measured by Junit will be 0s instead of the
        // actual value, but on the upside it is possible to show the value of the benchmark in the title.
        try {
            final Description RUNNING = Description.createTestDescription(testClass.getJavaClass(), "doingStuff");
            runNotifier.fireTestRunStarted(RUNNING);
            runBenchmarks(runNotifier);
        } finally {
            // TODO Notify UI if an exception happened, otherwise it will just report "empty test suite"
            runNotifier.fireTestRunFinished(null);
        }
    }

    private void runBenchmarks(final RunNotifier runNotifier) {
        Spanner.runBenchmarks(testClass.getJavaClass(), testMethods, new Spanner.Callback() {

            public Trial currentTrail;

            @Override
            public void onStart() {
                /* Ignore */
            }

            @Override
            public void trialStarted(Trial trial) {
                currentTrail = trial;
            }

            @Override
            public void trialSuccess(Trial trial, Trial.Result result) {
                double resultMedian = getMedian(result.getTrial().measurements());
                Description spec = getDescription(trial, resultMedian);
                runNotifier.fireTestStarted(spec);
                if (trial.hasBaseline()) {
                    double absChange = Math.abs(trial.getChangeFromBaseline());
                    if (absChange > benchmarkConfiguration.getBaselineFailure()) {
                        runNotifier.fireTestFailure(new Failure(spec,
                                new TrialFailureException(String.format("Change from baseline was to big: %.2f%%. Limit is %.2f%%",
                                        absChange, benchmarkConfiguration.getBaselineFailure()))));
                    }
                }
                runNotifier.fireTestFinished(spec);
            }

            // FIXME Move this to Trial.Result
            private double getMedian(List<Measurement> trialMeasurements) {

                // Group by measurement description
                // TODO Figure out why measurents can have multiple descriptions
                ImmutableListMultimap<String, Measurement> measurementsIndex =
                        new ImmutableListMultimap.Builder<String, Measurement>()
                                .orderKeysBy(Ordering.natural())
                                .putAll(Multimaps.index(trialMeasurements, new Function<Measurement, String>() {
                                    @Override
                                    public String apply(Measurement input) {
                                        return input.description();
                                    }
                                }))
                                .build();

                for (Map.Entry<String, Collection<Measurement>> entry : measurementsIndex.asMap().entrySet()) {
                    Collection<Measurement> measurements = entry.getValue();
                    ImmutableSet<String> units = FluentIterable.from(measurements)
                            .transform(new Function<Measurement, String>() {
                                @Override
                                public String apply(Measurement input) {
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
                    return percentile.evaluate(50);
                }

                return -1;
            }

            @Override
            public void trialFailure(Trial trial, Throwable error) {
                Description spec = getDescription(trial);
                runNotifier.fireTestRunStarted(spec);
                runNotifier.fireTestFailure(new Failure(spec, error));
                runNotifier.fireTestFinished(spec);
            }

            @Override
            public void trialEnded(Trial trial) {
                /* Ignore */
            }

            @Override
            public void onComplete() {
                /* Ignore */
            }

            @Override
            public void onError(Exception error) {
                throw new RuntimeException(error);
            }

            private Description getDescription(Trial trial) {
                Method method = trial.experiment().instrumentation().benchmarkMethod();
                return Description.createTestDescription(testClass.getJavaClass(), method.getName());
            }

            private Description getDescription(Trial trial, double result) {
                Method method = trial.experiment().instrumentation().benchmarkMethod();
                String resultString = String.format(" [%.2f ns.]", result);
                resultString += formatBenchmarkChange(trial);
                return Description.createTestDescription(testClass.getJavaClass(), method.getName() + resultString);
            }
        });
    }

    private String formatBenchmarkChange(Trial trial) {
        Double change = trial.getChangeFromBaseline();
        if (change == null) return "";
        return String.format("[%s%.2f%%]", change > 0 ? "+" : "", change);
    }

    private String getDescription(Trial trial) {
        Method method = trial.experiment().instrumentation().benchmarkMethod();
        return method.getName();
    }

}
