---
paths:
  - "**/*Action.java"
  - "**/*Form.java"
  - "**/struts-config*.xml"
  - "**/*.jsp"
---

# Struts conventions

- Extend `BaseAction`, never `Action` directly. `BaseAction.execute` is final; implement `doExecute`.
- An Action does four things: read the form, call one service, set request attributes, return a
  forward. Anything else belongs in a service.
- Forwards are `success` and `failure`, declared in `struts-config.xml`. JSPs reference forward
  names — renaming one without updating every JSP breaks the screen silently.
- Form beans hold `String` fields. `validate()` does structural checks only (required, length,
  character set). Business validation lives in the service.
- `reset()` must clear every field, or a stale value leaks into the next request.
- JSPs live under `WEB-INF/`, use Struts tags only, and contain no scriptlets.
- Register every new form bean in `<form-beans>` and every new action in `<action-mappings>`.
