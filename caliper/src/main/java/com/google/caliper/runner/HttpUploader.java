/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.caliper.runner;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import com.google.caliper.api.ResultProcessor;
import com.google.caliper.config.CaliperConfig;
import com.google.caliper.config.InvalidConfigurationException;
import com.google.caliper.util.Stdout;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.client.urlconnection.HttpURLConnectionFactory;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;

/**
 * A {@link ResultProcessor} implementation that publishes results to the webapp using an HTTP
 * request.
 */
public class HttpUploader extends ResultsUploader {
  @Inject HttpUploader(@Stdout PrintWriter stdout, Gson gson, CaliperConfig config)
      throws InvalidConfigurationException {
    super(stdout, gson, createClientUsingProxy(config), config.getResultProcessorConfig(HttpUploader.class));
  }
  
  
  public static Client createClientUsingProxy(final CaliperConfig config) {
  	HttpURLConnectionFactory httpURLConnectionFactory = new HttpURLConnectionFactory() {
			
			public HttpURLConnection getHttpURLConnection(URL url) throws IOException {
				Proxy proxy = null;
				if (config.properties().containsKey("http.proxyHost")) {
					String proxyHost = config.properties().get("http.proxyHost");
					String proxyPortStr = config.properties().get("http.proxyPort");
					int proxyPort = 8080;
					if (proxyPortStr != null) {
						proxyPort = Integer.parseInt(proxyPortStr); 
					}
					proxy = new Proxy(Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
				}
				else {
					try {
						List<Proxy> proxies = ProxySelector.getDefault().select(url.toURI());
						if ((proxies != null) && (! proxies.isEmpty())) {
							proxy = proxies.iterator().next();
						}
					} 
					catch (Exception e) {
					}
				}
				
				//Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
				if (proxy == null) {
					return (HttpURLConnection)url.openConnection();
				}
				return (HttpURLConnection)url.openConnection(proxy);
			}
		};
  	Client result = new Client(new URLConnectionClientHandler(httpURLConnectionFactory));
  	return result;
  }
}
