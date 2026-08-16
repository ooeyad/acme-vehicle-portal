<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<html:html>
<head>
  <title><bean:message key="vehicle.result.title"/></title>
</head>
<body>

  <h1><bean:message key="vehicle.result.title"/></h1>

  <dl>
    <dt><bean:message key="vehicle.vin"/></dt>
    <dd><bean:write name="vehicle" property="vin"/></dd>

    <dt><bean:message key="vehicle.model"/></dt>
    <dd><bean:write name="vehicle" property="model"/></dd>

    <dt><bean:message key="vehicle.modelYear"/></dt>
    <dd><bean:write name="vehicle" property="modelYear"/></dd>

    <dt><bean:message key="vehicle.dealerCode"/></dt>
    <dd><bean:write name="vehicle" property="dealerCode"/></dd>
  </dl>

  <html:link action="/vehicle/lookup"><bean:message key="vehicle.result.back"/></html:link>

</body>
</html:html>
