package dk.ilios.spanner;

import android.annotation.SuppressLint;
import android.support.test.InstrumentationRegistry;

import org.junit.runner.RunWith;

import java.io.File;

import dk.ilios.spanner.example.Utils;
import dk.ilios.spanner.config.SpannerConfiguration;
import dk.ilios.spanner.junit.SpannerRunner;

@RunWith(SpannerRunner.class)
public class UnitTestBenchmarks {

    private File filesDir = InstrumentationRegistry.getTargetContext().getFilesDir();
    private File resultsDir = new File(filesDir, "results");
    private File baseLineFile = Utils.copyFromAssets("baseline.json");

    @BenchmarkConfiguration
    public SpannerConfig configuration = new SpannerConfig.Builder()
            .resultsFolder(resultsDir)
            .baseline(baseLineFile)
            .baselineFailure(100.0) // Accept 100% difference, normally should be 10-15%
            .uploadResults()
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
