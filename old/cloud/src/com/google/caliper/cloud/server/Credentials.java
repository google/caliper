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

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import javax.servlet.http.HttpServletRequest;

public class Credentials {

  private final UserService userService;
  private final String requestUrl;
  private final String requestUri;

  public Credentials(HttpServletRequest request) {
    this.userService = UserServiceFactory.getUserService();
    this.requestUrl = request.getRequestURL().toString();
    this.requestUri = request.getRequestURI();
  }

  public boolean isLoggedIn() {
    return userService.isUserLoggedIn();
  }

  public String getEmail() {
    return isLoggedIn() ? userService.getCurrentUser().getEmail() : null;
  }

  public boolean isUserUrl() {
    return requestUri.equals(getUserUrl());
  }
  
  public String getUserUrl() {
    return "/user/" + getEmail();
  }

  public String getLogoutUrl() {
    return userService.createLogoutURL(requestUrl);
  }

  public String getLoginUrl() {
    return userService.createLoginURL(requestUrl);
  }
}
