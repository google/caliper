package dk.ilios.spanner;

import android.support.test.InstrumentationRegistry;

import org.junit.runner.RunWith;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.HashMap;

import dk.ilios.spanner.example.Utils;
import dk.ilios.spanner.junit.SpannerRunner;

@RunWith(SpannerRunner.class)
public class Maps {

    private File filesDir = InstrumentationRegistry.getTargetContext().getFilesDir();
    private File resultsDir = new File(filesDir, "results");
    private File baseLineFile = Utils.copyFromAssets("baseline.json");

    @BenchmarkConfiguration
    public SpannerConfig configuration = new SpannerConfig.Builder()
//            .resultsFolder(resultsDir)
//            .baselineFile(baseLineFile)
//            .baselineFailure(1.0f) // Accept 100% difference, normally should be 10-15%
            .uploadResults()
            .build();

    // Public test parameters (value chosen and injected by Experiment)
    @Param(value = {"java.util.Date", "java.net.URLClassLoader", "java.lang.String"})
    public String strClass;

    @Param(value = {"Date", "URLClassLoader", "String"})
    public String str;

    // Private fields used by benchmark methods
    private Class testClass;
    private String testString;
    private HashMap<Class<?>, String> classMap;
    private HashMap<String, String> stringMap;

    @BeforeExperiment
    public void before() {
        classMap = new HashMap<Class<?>, String>();
        stringMap = new HashMap<String, String>();

        classMap.put(Date.class, "foo");
        classMap.put(URLClassLoader.class, "foo");
        classMap.put(String.class, "foo");
        stringMap.put("Date", "foo");
        stringMap.put("URLClassLoader", "foo");
        stringMap.put("String", "foo");

        try {
            testClass = Class.forName(strClass);
            testString = str;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public void mapClasses(int reps) {
        for (int i = 0; i < reps; i++) {
             String result = classMap.get(testClass);
        }
    }

    @Benchmark
    public void mapStrings(int reps) {
        for (int i = 0; i < reps; i++) {
            String result = stringMap.get(testString);
        }
    }
}
