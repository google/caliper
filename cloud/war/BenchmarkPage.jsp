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
    <jsp:useBean id="benchmarkPage" scope="request" type="com.google.caliper.cloud.server.BenchmarkPage" />
    <jsp:useBean id="credentials" scope="request" type="com.google.caliper.cloud.server.Credentials" />
    <jsp:useBean id="snapshot" scope="request" type="java.lang.String" />

    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <link type="text/css" rel="stylesheet" href="/static/main.css">

    <title><%= html(benchmarkPage.getBenchmarkName()) %></title>
    
    <script type="text/javascript">
    var pageGlobals = {
      user: "<%= js(credentials.getEmail()) %>",
      benchmarkOwner: "<%= js(benchmarkPage.getBenchmarkOwner()) %>",
      benchmarkName: "<%= js(benchmarkPage.getBenchmarkName()) %>",
      snapshot: "<%= js(snapshot) %>"
    };
    </script>
    <script type="text/javascript" language="javascript" src="/caliper/caliper.nocache.js"></script>
  </head>

  <body><div class="body"><div class="content">

    <!-- for history support (not currently used) -->
    <iframe src="javascript:''" id="__gwt_historyFrame" tabIndex='-1' style="position:absolute;width:0;height:0;border:0"></iframe>

    <jsp:include page="CredentialsHeader.jsp" />

    <h1>
      <span class="package">
        <% if (benchmarkPage.getPackageName().isEmpty()) {%>
        <span class="placeholder">(default package)</span>
        <% } else { %>
        <%= html(benchmarkPage.getPackageName()) %>
        <% } %></span><br>
      <%= html(benchmarkPage.getClassName()) %><br>
      <span class="owner">run by <a href="/user/<%= html(benchmarkPage.getBenchmarkOwner()) %>"><%= html(benchmarkPage.getBenchmarkOwner()) %></a></span>
    </h1>

    <div id="snapshotDisclaimer"></div>

    <h3>Results</h3>
    <div id="results"></div>

    <h3>Runs</h3>

    <div id="runs"></div>

    <h3>Variables</h3>
    <div id="variables"></div>

    <h3>Environments</h3>
    <div id="environments"></div>
    
    <h3>Snapshots</h3>
    <div id="snapshots"></div>

    <h3>Caveats</h3>
    <div id="caveats">
      Not all benchmarks accurately reflect real world performance. See:
      <ul class="linkList">
        <li><a href="http://code.google.com/p/caliper/wiki/JavaMicrobenchmarks">Java Microbenchmarks</a></li>
        <li><a href="http://code.google.com/p/caliper/wiki/JavaMicrobenchmarkReviewCriteria">Microbenchmark Review Criteria</a></li>
      </ul>
    </div>

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
