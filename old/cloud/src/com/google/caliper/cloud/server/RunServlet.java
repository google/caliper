/**
 * Copyright (C) 2009 Google Inc.
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
 */

package com.google.caliper.cloud.server;

import com.google.appengine.api.datastore.Entity;
import com.google.caliper.Environment;
import com.google.caliper.Result;
import com.google.caliper.ResultsReader;
import com.google.caliper.Run;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles both upload and display of benchmark runs.
 */
public class RunServlet extends HttpServlet {

  @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Result result = new ResultsReader().getResult(request.getInputStream());
    Run run = result.getRun();
    Environment environment = result.getEnvironment();

    URI uri;
    try {
       uri = new URI(request.getRequestURL().toString());
    } catch (URISyntaxException e) {
      throw new AssertionError(e.getMessage());
    }

    // uri.getPath() should be in the form /run/<apiKey>/<benchmarkName>
    Iterable<String> urlStrings = Splitter.on('/').split(uri.getPath());
    Iterator<String> urlIterator = urlStrings.iterator();
    urlIterator.next();
    urlIterator.next();
    String apiKey = urlIterator.next();
    String emailAddress = new UserStore().lookupEmailAddress(apiKey);

    Entity environmentEntity = new EnvironmentStore().getOrCreateEnvironment(environment, emailAddress);
    new RunStore().createRun(run, environmentEntity, emailAddress);

    response.getOutputStream().println(
        uri.resolve("/run/" + emailAddress + "/" + run.getBenchmarkName()).toString());
  }

  @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String path = request.getPathInfo();
    String[] parts = path != null ? path.split("/") : new String[0];

    if ((parts.length != 3 && parts.length != 4) || parts[0].length() != 0) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
      return;
    }

    String benchmarkOwner = parts[1];
    String benchmarkName = parts[2];
    String snapshotId = "";

    if (parts.length == 4) {
      snapshotId = parts[3];
    }

    request.setAttribute("benchmarkPage", new BenchmarkPage(benchmarkOwner, benchmarkName));
    request.setAttribute("credentials", new Credentials(request));
    request.setAttribute("snapshot", snapshotId);
    getServletContext().getRequestDispatcher("/BenchmarkPage.jsp").forward(request, response);
  }
}
