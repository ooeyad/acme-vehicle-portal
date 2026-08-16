package com.acme.dao.jdbc;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Enforces the ACME DB2 conventions on the SQL this DAO issues, without needing a database.
 *
 * Every DAO gets a test like this. It runs in the normal build, so a query that breaks a
 * convention fails on the developer's machine rather than in a DBA's review queue a week later.
 * It does NOT replace testing against the DEV database - it replaces the review comment.
 *
 * Queries are discovered by REFLECTION rather than listed in an array. The earlier version kept a
 * hardcoded ALL_QUERIES list, which meant adding a query and forgetting to register it left that
 * query silently unchecked - exactly the failure this test exists to prevent. Discovered while
 * adding FIND_BY_DEALER_AND_YEAR for issue #1.
 */
public class JdbcVehicleDaoSqlConventionsTest {

    /** Every static String constant on the DAO whose value looks like a query. */
    private static List<String> allQueries() {
        List<String> queries = new ArrayList<String>();
        for (Field field : JdbcVehicleDao.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) {
                continue;
            }
            field.setAccessible(true);
            try {
                String value = (String) field.get(null);
                if (value != null && value.trim().toUpperCase().startsWith("SELECT")) {
                    queries.add(value);
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError("Could not read " + field.getName(), e);
            }
        }
        return queries;
    }

    /**
     * Guards the reflection itself. Without this, a rename or a refactor that stopped the lookup
     * finding anything would leave every other test in this class passing against an empty list.
     */
    @Test
    public void reflectionFindsTheQueries() {
        assertTrue("No queries discovered on JdbcVehicleDao - the reflection lookup is broken, "
                 + "so every other assertion in this class is meaningless",
                allQueries().size() >= 3);
    }

    @Test
    public void noQueryUsesSelectStar() {
        for (String sql : allQueries()) {
            assertFalse("SELECT * breaks when a column is added or reordered: " + sql,
                    sql.toUpperCase().contains("SELECT *"));
        }
    }

    @Test
    public void everyQueryUsesParameterMarkers() {
        for (String sql : allQueries()) {
            assertTrue("Values must be bound with ?, never concatenated: " + sql,
                    sql.contains("?"));
        }
    }

    @Test
    public void noQueryContainsAStringLiteralWhereAParameterBelongs() {
        for (String sql : allQueries()) {
            assertFalse("A quoted literal in a WHERE clause is usually concatenated input: " + sql,
                    sql.matches("(?s).*WHERE.*'.*"));
        }
    }

    @Test
    public void everyReadIsBounded() {
        for (String sql : allQueries()) {
            assertTrue("Reads reaching the web tier need FETCH FIRST n ROWS ONLY: " + sql,
                    sql.toUpperCase().contains("FETCH FIRST"));
        }
    }

    @Test
    public void readOnlyQueriesDeclareUncommittedRead() {
        for (String sql : allQueries()) {
            assertTrue("Read-only queries use WITH UR to avoid lock escalation: " + sql,
                    sql.toUpperCase().endsWith("WITH UR"));
        }
    }

    @Test
    public void noQueryIssuesDdl() {
        for (String sql : allQueries()) {
            String upper = sql.toUpperCase();
            assertFalse("DDL belongs in a migration file: " + sql,
                    upper.contains("CREATE ") || upper.contains("DROP ") || upper.contains("ALTER "));
        }
    }
}
