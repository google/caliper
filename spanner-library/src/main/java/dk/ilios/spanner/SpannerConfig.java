package dk.ilios.spanner;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Class for adding custom configuration of a Spanner run.
 */
public class SpannerConfig {

    private final File resultsFolder;
    private final File baseLineFile;
    private final boolean warnIfWrongTestGranularity;
    private final boolean createBaseLine;
    private final URL uploadUrl;
    private final String apiKey;
    private final boolean uploadResults;
    private float baselineFailure;

    private SpannerConfig(Builder builder) {
        this.resultsFolder = builder.resultsFolder;
        this.baseLineFile = builder.baseLineFile;
        this.warnIfWrongTestGranularity = builder.warnIfWrongTestGranularity;
        this.createBaseLine = builder.createBaseline;
        this.uploadResults = builder.uploadResults;
        this.uploadUrl = builder.uploadUrl;
        this.apiKey = builder.apiKey;
        this.baselineFailure = builder.baselineFailure;
    }

    public File getResultsFolder() {
        return resultsFolder;
    }

    public File getBaseLineFile() {
        return baseLineFile;
    }

    public boolean warnIfWrongTestGranularity() {
        return warnIfWrongTestGranularity;
    }

    public boolean createBaseline() {
        return createBaseLine;
    }

    public URL getUploadUrl() {
        return uploadUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean isUploadResults() {
        return uploadResults;
    }

    public float getBaselineFailure() {
        return baselineFailure;
    }

    /**
     * Builder for fluent construction of a SpannerConfig object.
     */
    public static class Builder {
        private File resultsFolder = null;
        private File baseLineFile = null;
        private boolean warnIfWrongTestGranularity = true;
        private boolean createBaseline = false;
        private boolean uploadResults = false;
        private String apiKey = "";
        private URL uploadUrl = getUrl("https://microbenchmarks.appspot.com");
        private float baselineFailure = 0.2f; // 20% difference from baseline will fail the experiment.

        public Builder() {
        }

        /**
         * Constructs an instance of {@link SpannerConfig}.
         */
        public SpannerConfig build() {
            return new SpannerConfig(this);
        }

        /**
         * Set the folder where any benchmark results should be stored.
         *
         * @param dir Reference to folder.
         * @return Builder object.
         */
        public Builder resultsFolder(File dir) {
            checkNotNull(dir, "Results folder was null.");
            if (!dir.isDirectory() || !dir.canWrite())
            this.resultsFolder = dir;
            return this;
        }

        // TODO Add support for overriding the filename

        /**
         * Set a baseline for the tests being run.
         *
         * @param file Reference to the baseline file (see .
         * @return Builder object.
         */
        public Builder baselineFile(File file) {
            checkNotNull(file, "Baseline file was null");
            this.baseLineFile = file;
            return this;
        }

        /**
         * Setting this will cause Spanner to verify that the granularity of the tests are set correctly or will
         * @return
         */
        public Builder warnIfWrongTestGranularity() {
            this.warnIfWrongTestGranularity = true;
            return this;
        }

        /**
         * Setting this will cause the benchmarks results to be saved in a new baseline file in the results folder.
         * @return
         */
        public Builder createBaseline() {
            this.createBaseline = true;
            return this;
        }

        public Builder uploadResults() {
            uploadResults = true;
            return this;
        }

        public Builder uploadUrl(String url) {
            this.uploadUrl = getUrl(url);
            return this;
        }

        public Builder apiKey(String apiKey) {
            checkNotNull(apiKey, "Only non-null keys allowed");
            this.apiKey = apiKey;
            return this;
        }

        /**
         * The difference in percent from the baseline allowed before the experiment will be a failure.
         * @param percentage [0-1.0] for [0-100%]
         * @return the Builder.
         */
        public Builder baselineFailure(float percentage) {
            baselineFailure = Math.abs(percentage);
            return this;
        }

        private void checkNotNull(Object obj, String errorMessage) {
            if (obj == null) {
                throw new IllegalArgumentException(errorMessage);
            }
        }

        private URL getUrl(String url) {
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
