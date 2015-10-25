<%@ page contentType ="text/html; charset=UTF-8" %>
<%@ page import="static com.google.caliper.cloud.server.JspEscaping.html" %>
<%@ page import="static com.google.caliper.cloud.server.JspEscaping.js" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- The HTML 4.01 Transitional DOCTYPE declaration-->
<!-- above set at the top of the file will set     -->
<!-- the browser's rendering engine into           -->
<!-- "Quirks Mode". Replacing this declaration     -->
<!-- with a "Standards Mode" doctype is supported, -->
<!-- but may lead to some differences in layout.   -->

<html>
  <head>
    <jsp:useBean id="userPage" scope="request" type="com.google.caliper.cloud.server.UserDashboardPage" />
    <jsp:useBean id="apiDetails" scope="request" type="com.google.caliper.cloud.server.ApiDetails" />
    <jsp:useBean id="credentials" scope="request" type="com.google.caliper.cloud.server.Credentials" />

    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <link type="text/css" rel="stylesheet" href="/static/main.css">

    <title><%= html(userPage.getBenchmarkOwner()) %>'s benchmarks</title>

    <script type="text/javascript">
      var pageGlobals = {
        user: "<%= js(credentials.getEmail()) %>",
        benchmarkOwner: "<%= js(userPage.getBenchmarkOwner()) %>"
      };
    </script>

    <script type="text/javascript" language="javascript" src="/caliper/caliper.nocache.js"></script>
  </head>
  <body><div class="body">
    <div class="content" style="position:relative;">
      <!-- for history support (not currently used) -->
      <iframe src="javascript:''" id="__gwt_historyFrame" tabIndex='-1' style="position:absolute;width:0;height:0;border:0"></iframe>

      <jsp:include page="CredentialsHeader.jsp" />

      <h1><%= html(userPage.getBenchmarkOwnerName()) %>'s benchmarks</h1>

      <h3>Benchmarks</h3>
      <div id="benchmarks"></div>

      <% if (credentials.isLoggedIn() && userPage.getBenchmarkOwner().equals(credentials.getEmail())) { %>

      <h3 id="api">Using the API</h3>

      <p>Caliper uses an API key to link your local installation to your Google account. To enable
         online access, copy and paste this API key into your <code>.caliperrc</code> file:</p>

      <div class="clearfix">
        <div class="apikeydivcontainer">
          <div class="apikeydiv"><pre class="apikey"># Caliper API key for <%= html(credentials.getEmail()) %>
postUrl: <%= html(apiDetails.getPostUrl()) %>
apiKey: <%= html(apiDetails.getApiKey()) %></pre>
          </div>
          <div class="clearfix">
            <table class="caliperrcpath">
              <tr>
                <td align="right" style="padding-right: 10px">on Mac OS X, Linux and UNIX:</td>
                <td align="right"><code>~/.caliperrc</code></td>
              </tr>
              <tr>
                <td align="right" style="padding-right: 10px">on Windows:</td>
                <td align="right"><code>C:\.caliperrc</code></td>
              </tr>
            </table>
          </div>
        </div>
      </div>

      <% } %>

      <!-- so users get something when JavaScript is disabled -->
      <noscript>
        <div style="width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif">
          Your web browser must have JavaScript enabled in order for this application to display correctly.
        </div>
      </noscript>


    </div>
    <div class="footer"><a href="http://code.google.com/p/caliper/"><img src="http://code.google.com/p/caliper/logo?logo_id=1261251646" alt="caliper logo"></a><p><a href="http://code.google.com/p/caliper/">Caliper Microbenchmarking Framework</a></div>
  </div></body>
</html>
