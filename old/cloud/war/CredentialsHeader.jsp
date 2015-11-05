<%@ page import="static com.google.caliper.cloud.server.JspEscaping.html" %>
<%@ page import="static com.google.caliper.cloud.server.JspEscaping.js" %>

<jsp:useBean id="credentials" scope="request" type="com.google.caliper.cloud.server.Credentials" />
<div class="credentials">
    <% if (credentials.isLoggedIn()) { %>
      <% if (credentials.isUserUrl()) { %>
        <%= html(credentials.getEmail()) %> |
      <% } else { %>
        <a class="nameLink" href="<%= html(credentials.getUserUrl()) %>"><%= html(credentials.getEmail()) %></a> |
      <% } %>
    <% } %>
    <span id="refresh"></span> |
    <a href="http://code.google.com/p/caliper/wiki/OnlineResults?show=content" target="_blank">Help</a> |
    <% if (credentials.isLoggedIn()) { %>
      <a href="<%= html(credentials.getLogoutUrl()) %>">Sign out</a>
    <% } else { %>
      <a href="<%= html(credentials.getLoginUrl()) %>">Sign in</a>
    <% } %>
</div>
