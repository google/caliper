// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.caliper.runner;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.model.Run;
import com.google.caliper.util.Util;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import javax.annotation.Nullable;

/**
 * {@link ResultProcessor} implementation that uploads the JSON-serialized results to the Caliper
 * webapp.
 */
final class WebappUploader implements ResultProcessor {
  @Nullable private final String postUrl;
  @Nullable private final String apiKey;
  private final String benchmarkName;
  private final Proxy proxy;

  private WebappUploader(
      @Nullable String postUrl, @Nullable String apiKey, String benchmarkName, Proxy proxy) {
    this.postUrl = postUrl;
    this.apiKey = apiKey;
    this.benchmarkName = checkNotNull(benchmarkName);
    this.proxy = checkNotNull(proxy);

    if (apiKeySpecified()) {
      System.out.println(
          "\nYou specified a Caliper API key, but web uploads are not yet available in new"
          + " Caliper.\nIf you require use of the web application then please use old Caliper.\n");
    }
  }

  private boolean apiKeySpecified() {
    return (postUrl != null) && !CharMatcher.WHITESPACE.matchesAllOf(postUrl)
        && (apiKey != null) && !CharMatcher.WHITESPACE.matchesAllOf(apiKey);
  }

  /**
   * Returns a {@code WebappUploader} configured according to the given {@code CaliperRc} and
   * benchmarkName. If the required configuration properties are not specified, this method will
   * still return a {@code WebappUploader}, but it won't actually upload to the webapp: it will
   * simply display a message describing why the results were not uploaded.
   */
  public static WebappUploader create(String benchmarkName, CaliperRc rc) {
    String postUrl = rc.getProperty("results.upload.url");
    String apiKey = rc.getProperty("results.upload.key");
    return new WebappUploader(postUrl, apiKey, benchmarkName, getProxy(rc));
  }

  private static Proxy getProxy(CaliperRc rc) {
    Proxy proxy;
    String proxyAddress = rc.getProperty("results.upload.proxy");
    if ((proxyAddress == null) || CharMatcher.WHITESPACE.matchesAllOf(proxyAddress)) {
      proxy = Proxy.NO_PROXY;
    } else {
      String[] proxyHostAndPort = proxyAddress.trim().split(":");
      proxy = new Proxy(Proxy.Type.HTTP,
          new InetSocketAddress(proxyHostAndPort[0], Integer.parseInt(proxyHostAndPort[1])));
    }
    return proxy;
  }

  @Override public void handleResults(Run results) {
    if (apiKeySpecified()) {
      String jsonResults = Util.GSON.toJson(results);
      uploadResults(jsonResults);
    } else {
      System.out.println(
          "To upload results to the Caliper web application, specify your Caliper API key\n"
              + "in your caliper configuration file (typically ~/.caliperrc)");
    }
  }

  private void uploadResults(String json) {
    try {
      URL url = new URL(postUrl + apiKey + "/" + benchmarkName);
      // TODO: should we have an identifiable URL pattern to distinguish between old/new caliper?
      HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection(proxy);
      urlConnection.setDoOutput(true);
      urlConnection.getOutputStream().write(json.getBytes(Charsets.UTF_8));
      if (urlConnection.getResponseCode() == 200) {
        System.out.println("");
        System.out.println("View current and previous benchmark results online:");
      } else {
        System.out.println("Posting to " + postUrl + " failed: "
            + urlConnection.getResponseMessage());
      }

      BufferedReader in =
          new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
      System.out.println("  " + in.readLine());
      in.close();
    } catch (IOException e) {
      System.out.println("Posting to " + postUrl + " failed: " + e);
    }
  }
}
