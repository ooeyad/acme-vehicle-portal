---
name: add-struts-action
description: Add a new Struts action end to end - action class, config entry, form bean, JSP, and test. Use when adding a new screen, form submission, or URL-reachable feature to the legacy web application.
argument-hint: "[action path and purpose]"
disable-model-invocation: true
---

# Add a Struts action

Request: **$ARGUMENTS**

<!-- Replace package names and paths below with your real ones before rollout. -->

## Before you write anything
Find the closest existing analogue and read it fully. Copy its structure. If you cannot name the
analogue, ask me which screen this resembles — do not invent a new pattern.

## The five pieces, in this order

**1. Form bean** — `com.acme.web.form.<Name>Form extends BaseActionForm`
- Fields are `String` for anything that comes off a request. Convert in the service, not the form.
- Implement `validate()` for structural checks only (required, length, format).
- Business validation belongs in the service and returns a domain error, not an `ActionError`.

**2. Action** — `com.acme.web.action.<Name>Action extends BaseAction`
- `execute()` does exactly four things: read the form, call one `*Service` method, put results in
  request scope, return a forward.
- **No** business logic. **No** SQL. **No** `new` on a DAO.
- Errors: catch the service's checked exception, add an `ActionMessage`, forward to `"failure"`.

**3. `struts-config.xml`**
```xml
<action path="/<path>"
        type="com.acme.web.action.<Name>Action"
        name="<name>Form"
        scope="request"
        validate="true"
        input="/WEB-INF/jsp/<area>/<name>.jsp">
  <forward name="success" path="/WEB-INF/jsp/<area>/<name>Result.jsp"/>
  <forward name="failure" path="/WEB-INF/jsp/<area>/<name>.jsp"/>
</action>
```
Register the form bean in `<form-beans>` too. Forward names are referenced by JSPs — keep them
consistent with the neighbouring actions.

**4. JSP** — `/WEB-INF/jsp/<area>/<name>.jsp`
- Under `WEB-INF` so it is not directly reachable.
- Struts tags only (`<html:form>`, `<html:text>`, `<bean:write>`). No scriptlets.
- All user-visible text through `<bean:message key="..."/>` and the resource bundle.

**5. Test**
- An action test driven through the repo's existing mock helper, asserting the chosen forward and the
  request attributes set. Follow the nearest existing `*ActionTest`.

## Security
If this action exposes customer, dealer, or VIN data, confirm it is covered by the security filter
mapping in `web.xml` and that the service performs the ownership check. State explicitly in your
summary which check applies.

## Finish
Build, run the test, then run the `code-reviewer` subagent on the diff.
