---
name: characterization-test
description: Write a test that pins the current behaviour of untested legacy code before it is changed. Use when modifying legacy Java that has no test coverage, or when the user asks how to safely refactor old code.
argument-hint: "[class or method to pin]"
---

# Characterization test

Goal: capture what the code **does today**, not what it *should* do. A characterization test
that fails because the current behaviour is odd is a bug in the test, not in the code.

Target: **$ARGUMENTS**

## Steps

1. **Read the target and its collaborators.** Identify every input: method parameters, instance state,
   `ActionForm` fields, request attributes, session attributes, system properties, and database reads.
2. **Choose the seam.** In this codebase, in order of preference:
   - Pure method → call it directly.
   - Service class with a JDBC dependency → constructor-inject or set a test `DataSource` pointing at
     the in-memory fixture; follow the pattern in the nearest existing `*ServiceTest`.
   - Struts Action → drive it through the existing `MockActionHelper` (or the equivalent already in this
     repo — find it, do not invent a new one) with a stubbed request and form bean.
3. **Record actual output.** Run the code with representative inputs and capture what it returns,
   what it writes, and which forward it picks. Include the ugly cases: null input, empty string,
   zero rows, a value that trips a legacy special case.
4. **Assert on what you observed.** Even when it looks wrong. Add a comment marking behaviour that
   appears to be a bug:
   ```java
   // CHARACTERIZATION: returns "" rather than null for an unknown VIN.
   // Preserved deliberately; callers depend on it. See ACME-1234.
   ```
5. **Verify the test is real.** Temporarily break the code under test and confirm the test fails.
   Show me that it failed, then restore the code. A characterization test that passes against
   broken code is worse than none.
6. **Commit the test on its own**, before the behavioural change. Two commits: pin, then change.

## Do not
- Do not "fix" behaviour while writing the pin.
- Do not mock the class under test.
- Do not assert on log output or on timing.
