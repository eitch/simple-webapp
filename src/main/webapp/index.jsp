<%@ page import="java.time.ZonedDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<html>
<body>
<h2>Hello World!</h2>

<p>The time is now <%= ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) %>
</p>
</body>
</html>
