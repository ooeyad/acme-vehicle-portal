<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%--
  Dealer inventory: search form and results on one screen.
  Struts tags only, no scriptlets, all text through the message bundle.
  <bean:write> escapes by default, so echoed input is not an XSS vector.
--%>
<html:html>
<head>
  <title><bean:message key="dealer.inventory.title"/></title>
</head>
<body>

  <h1><bean:message key="dealer.inventory.title"/></h1>

  <html:errors/>

  <html:form action="/vehicle/inventory">
    <label for="dealerCode"><bean:message key="dealer.inventory.dealerCode"/></label>
    <html:text property="dealerCode" styleId="dealerCode" maxlength="8"/>

    <label for="modelYear"><bean:message key="dealer.inventory.modelYear"/></label>
    <html:text property="modelYear" styleId="modelYear" maxlength="4"/>

    <html:submit><bean:message key="dealer.inventory.submit"/></html:submit>
  </html:form>

  <logic:present name="vehicles">

    <logic:empty name="vehicles">
      <p><bean:message key="dealer.inventory.empty"/></p>
    </logic:empty>

    <logic:notEmpty name="vehicles">
      <p>
        <bean:message key="dealer.inventory.count" arg0="${resultCount}"/>
        <logic:equal name="truncated" value="true">
          <bean:message key="dealer.inventory.truncated"/>
        </logic:equal>
      </p>

      <table>
        <thead>
          <tr>
            <th><bean:message key="vehicle.vin"/></th>
            <th><bean:message key="vehicle.model"/></th>
            <th><bean:message key="vehicle.modelYear"/></th>
          </tr>
        </thead>
        <tbody>
          <logic:iterate id="vehicle" name="vehicles">
            <tr>
              <%-- The VIN is deliberately NOT a link. A link renders
                   /vehicle/lookup.do?vin=... which puts the VIN into the access log, proxy logs,
                   browser history and the Referer header. CLAUDE.md: never log a VIN. --%>
              <td><bean:write name="vehicle" property="vin"/></td>
              <td><bean:write name="vehicle" property="model"/></td>
              <td><bean:write name="vehicle" property="modelYear"/></td>
            </tr>
          </logic:iterate>
        </tbody>
      </table>
    </logic:notEmpty>

  </logic:present>

</body>
</html:html>
