package dk.ilios.spanner.example;

import android.annotation.SuppressLint;
import android.os.Environment;

import java.io.File;

import dk.ilios.spanner.AfterExperiment;
import dk.ilios.spanner.BeforeExperiment;
import dk.ilios.spanner.Benchmark;
import dk.ilios.spanner.BenchmarkConfiguration;
import dk.ilios.spanner.Param;
import dk.ilios.spanner.SpannerConfig;
import dk.ilios.spanner.config.SpannerConfiguration;

public class ActivityBenchmarks {

    @SuppressLint("InlinedApi")
    private File externalDir = MyApplication.getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
    private File resultsDir = new File(externalDir, "results");
    //    private File baseLineFile = new File(resultstsDir, "baseline.json");
    private File baselineFile = SpannerConfiguration.getLatestJsonFile(resultsDir);

    @BenchmarkConfiguration
    public SpannerConfig configuration = new SpannerConfig.Builder()
            .resultsFolder(externalDir)
            .build();

    // Public test parameters (value chosen and injected by Experiment)
    @Param(value = {"java.util.Date", "java.lang.Object"})
    public String value;

    // Private fields used by benchmark methods
    private Class testClass;

    @BeforeExperiment
    public void before() {
        try {
            testClass = Class.forName(value);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterExperiment
    public void after() {

    }

    @Benchmark
    public boolean instanceOf(int reps) {
        boolean result = false;
        for (int i = 0; i < reps; i++) {
             result = (testClass instanceof Object);
        }
        return result;
    }

    @Benchmark
    public boolean directComparison(int reps) {
        boolean result = false;
        for (int i = 0; i < reps; i++) {
            result = testClass == Object.class;
        }
        return result;
    }

    @Benchmark
    public boolean equalsTo(int reps) {
        boolean result = false;
        for (int i = 0; i < reps; i++) {
            result = testClass.equals(Object.class);
        }
        return result;
    }
}
