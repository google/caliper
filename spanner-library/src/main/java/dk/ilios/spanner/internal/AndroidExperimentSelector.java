package dk.ilios.spanner.internal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import dk.ilios.spanner.internal.benchmark.BenchmarkClass;
import dk.ilios.spanner.model.Trial;

/**
 * Experiment selector for Android.
 * Based on the annotations in the benchmark class this class creates the Scenarios that needs to run.
 *
 * Scenario = Experiment?
 */
public class AndroidExperimentSelector implements ExperimentSelector {

    private final ImmutableSet<Instrument> instruments;
    private final BenchmarkClass benchmarkClass;
    private final ImmutableSetMultimap<String, String> userParameters;

    public AndroidExperimentSelector(BenchmarkClass benchmarkClass,
                                     ImmutableSet<Instrument> instruments) {
        this.instruments = instruments;
        this.benchmarkClass = benchmarkClass;
        this.userParameters = benchmarkClass.userParameters().fillInDefaultsFor(ImmutableSetMultimap.<String, String>of());
    }

    @Override
    public BenchmarkClass benchmarkClass() {
        return benchmarkClass;
    }

    @Override
    public ImmutableSet<Instrument> instruments() {
        return instruments;
    }

    @Override
    public ImmutableSetMultimap<String, String> userParameters() {
        // TODO Figure out which user parameters to set
        ArrayListMultimap<String, String> multimap = ArrayListMultimap.create();
        return new ImmutableSetMultimap.Builder<String, String>()
                .orderKeysBy(Ordering.natural())
                .putAll(multimap)
                .build();
    }

    @Override
    public ImmutableSet<Experiment> selectExperiments(Trial[] baselineData) {
        try {
            // Create all combinations
            List<Experiment> experiments = new ArrayList<>();
            for (Instrument instrument : instruments) { // of instruments
                for (Method method : benchmarkClass.getMethods()) { // of methods
                    for (List<String> userParamsChoice : cartesian(userParameters)) { // of parameters
                        ImmutableMap<String, String> experimentBenchmarkParameters = zip(userParameters.keySet(), userParamsChoice);
                        Instrument.Instrumentation instrumentation = instrument.createInstrumentation(method);
                        Experiment experiment = new Experiment(instrumentation, experimentBenchmarkParameters);
                        experiment.setBaseline(findBaseline(experiment, baselineData));
                        experiments.add(experiment);
                    }
                }
            }
            return ImmutableSet.copyOf(experiments);
        } catch (InvalidBenchmarkException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find an trial from an old experiment that can be used for a new experiment of the same kind.
     *
     * @param experiment new experiment
     * @param baselineData old trial data
     * @return trial that was run on a previous experiment or null of no trial matches.
     */
    private Trial findBaseline(Experiment experiment, Trial[] baselineData) {
        for (Trial trial : baselineData) {

            // Need same instrumentationSpec: instrumentationClass + parameters
            if (!experiment.instrumentation().instrument().getSpec().equals(trial.instrumentSpec())) {
                continue;
            }

            // Need same benchmarkSpec: class, method and parameters
            if (!experiment.benchmarkSpec().equals(trial.scenario().benchmarkSpec())) {
                continue;
            }

            // Other parameters might differ like number of measurements/trials. These are excluded for the
            // purpose of finding a a previous trial, so if we got this far the trial is sufficiently similar
            // to function as a baseline for the given experiment.
            return trial;
        }

        return null;
    }

    @Override
    public String selectionType() {
        return null;
    }

    private static <K, V> ImmutableMap<K, V> zip(Set<K> keys, Collection<V> values) {
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

        Iterator<K> keyIterator = keys.iterator();
        Iterator<V> valueIterator = values.iterator();

        while (keyIterator.hasNext() && valueIterator.hasNext()) {
            builder.put(keyIterator.next(), valueIterator.next());
        }

        if (keyIterator.hasNext() || valueIterator.hasNext()) {
            throw new AssertionError(); // I really screwed up, then.
        }
        return builder.build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<List<T>> cartesian(SetMultimap<String, T> multimap) {
        ImmutableMap<String, Set<T>> paramsAsMap = (ImmutableMap) multimap.asMap();
        return Sets.cartesianProduct(paramsAsMap.values().asList());
    }
}
