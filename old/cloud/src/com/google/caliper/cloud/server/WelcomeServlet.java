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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class WelcomeServlet extends HttpServlet {

  @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    if (!request.getServletPath().equals("/")) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
      return;
    }

    Credentials credentials = new Credentials(request);

    if (credentials.isLoggedIn()) {
      response.sendRedirect("/user/" + credentials.getEmail());
      return;
    }

    request.setAttribute("credentials", credentials);
    getServletContext().getRequestDispatcher("/Welcome.jsp").forward(request, response);
  }
}
