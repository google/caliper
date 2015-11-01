/**
 * Copyright (C) 2010 Google Inc.
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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class UserDashboardServlet extends HttpServlet {
  @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Credentials credentials = new Credentials(request);

    String path = request.getPathInfo();
    String[] parts = path != null ? path.split("/") : new String[0];

    
    if (parts.length != 2 || parts[0].length() != 0) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
      return;
    }

    String benchmarkOwner = parts[1];

    String apiKey = credentials.isLoggedIn()
            ? new UserStore().getOrCreateApiKey(credentials.getEmail())
            : null;

    String postUrl = request.getScheme() + "://" + request.getServerName()
        + ":" + request.getServerPort() + "/run/";

    // render
    request.setAttribute("userPage", new UserDashboardPage(benchmarkOwner));
    request.setAttribute("apiDetails", new ApiDetails(apiKey, postUrl));
    request.setAttribute("credentials", credentials);
    getServletContext().getRequestDispatcher("/UserDashboardPage.jsp").forward(request, response);
  }
}
