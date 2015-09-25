<%@ page contentType ="text/html; charset=UTF-8" %>
<%@ page import="static com.google.caliper.cloud.server.JspEscaping.html" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <jsp:useBean id="credentials" scope="request" type="com.google.caliper.cloud.server.Credentials" />

    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <link type="text/css" rel="stylesheet" href="/static/main.css">

    <title>Caliper</title>

    <script type="text/javascript">
      var pageGlobals = {
      };
    </script>
  </head>

  <body><div class="body"><div class="content">
    <div class="credentials">
      <a href="<%= html(credentials.getLoginUrl()) %>">Sign in</a>
    </div>
    </div>
    <div class="splashpage">

    <table class="splashtable"><tr><td valign="middle" style="padding-right: 20px">
        <img src="/static/frontpage-symbol.png" alt="Caliper" width="179" height="301">
    </td><td valign="middle">

        <h3>Caliper Microbenchmarking Framework</h3>
        <h1>Save, analyze and share your Caliper results online.</h1>

        <p>Caliper uses an API key to link your local installation to your Google account. To see
        your API key, you must <a href="<%= html(credentials.getLoginUrl()) %>">sign in</a> to your
        Google account.
    </td></tr></table>

  </div>
  </div></body>
</html>
