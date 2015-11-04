package io.ilios.spanner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import dk.ilios.spanner.SpannerConfig;

import static org.junit.Assert.*;

public class ConfigurationTests {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testDefaultConfig() throws MalformedURLException {
        SpannerConfig defaultConfig = new SpannerConfig.Builder().build();
        assertEquals("", defaultConfig.getApiKey());
        assertEquals(null, defaultConfig.getBaseLineFile());
        assertEquals(null, defaultConfig.getResultsFolder());
        assertEquals(0.2f, defaultConfig.getBaselineFailure(), 0);
        assertFalse(defaultConfig.isUploadResults());
        assertTrue(defaultConfig.warnIfWrongTestGranularity());
        assertFalse(defaultConfig.createBaseline());
        assertEquals(new URL("https://microbenchmarks.appspot.com"), defaultConfig.getUploadUrl());
    }

    @Test
    public void testWrongResultsFolders() throws IOException {
        File nullFolder = null;
        File readonlyFolder = tempFolder.newFolder("foo");
        readonlyFolder.setWritable(false);

        SpannerConfig.Builder builder = new SpannerConfig.Builder();
        try {
            builder.resultsFolder(nullFolder);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            builder.resultsFolder(readonlyFolder);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testWrongBaselineFile() throws IOException {
        File nullFile = null;
        File folder = tempFolder.newFolder("foo");

        SpannerConfig.Builder builder = new SpannerConfig.Builder();
        try {
            builder.baselineFile(nullFile);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            builder.resultsFolder(folder);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }
}
