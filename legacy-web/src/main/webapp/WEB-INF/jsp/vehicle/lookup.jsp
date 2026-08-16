<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%--
  REFERENCE JSP. Copy this shape.
  Rules: lives under WEB-INF so it is not directly reachable; Struts tags only,
  no scriptlets; all user-visible text through the message bundle.
--%>
<html:html>
<head>
  <title><bean:message key="vehicle.lookup.title"/></title>
</head>
<body>

  <h1><bean:message key="vehicle.lookup.title"/></h1>

  <html:errors/>

  <html:form action="/vehicle/lookup">
    <label for="vin"><bean:message key="vehicle.lookup.vin"/></label>
    <html:text property="vin" styleId="vin" maxlength="17"/>
    <html:submit><bean:message key="vehicle.lookup.submit"/></html:submit>
  </html:form>

</body>
</html:html>
