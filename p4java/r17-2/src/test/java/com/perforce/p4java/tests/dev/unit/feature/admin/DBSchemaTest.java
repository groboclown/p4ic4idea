/**
 *
 */
package com.perforce.p4java.tests.dev.unit.feature.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.perforce.p4java.admin.IDbSchema;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Simple dbschema tests.
 */

@TestId("DBSchemaTest01")
public class DBSchemaTest extends P4JavaTestCase {
    /**
     * Simple dbschema test. Not intended to be extensive or anything more
     * than basic sanity checks at the moment (dbschema is not a front-line
     * feature, to put it mildly).
     */

    @Test
    public void testDbSchema() throws Exception {
        String[] args = {null, "db.fixrev:1", "xyz"};

        IServer server = getServerAsSuper();
        assertNotNull("Null server returned getServerAsSuper", server);

        for (String arg : args) {
            List<String> argList = null;

            if (arg != null) {
                argList = new ArrayList<String>();
                argList.add(arg);
            }
            String[] argArray = null;
            if (arg != null) {
                argArray = new String[]{"dbschema", arg};
            } else {
                argArray = new String[]{"dbschema"};
            }
            List<IDbSchema> schemaList = server.getDbSchema(argList);
            assertNotNull("Null db schema list returned by getDbSchema()", schemaList);

            List<Map<String, Object>> taggedList = doTaggedP4Cmd(argArray, null, null, true);
            assertNotNull("null schema list returned by p4 command", taggedList);
            assertEquals("p4j schema list size not same as p4 command list",
                    schemaList.size(), taggedList.size());

            for (IDbSchema schema : schemaList) {
                assertNotNull("Null schema in schema list", schema);
                assertNotNull("Null table name", schema.getName());
                for (Map<String, Object> map : taggedList) {
                    boolean found = false;
                    assertNotNull("Null schema detail map returned by p4 command", map);
                    if (map.containsKey("table")) {
                        String name = (String) map.get("table");
                        if ((name != null) && name.equals(schema.getName())) {
                            found = true;
                            compareFields(map, schema);
                        }
                    } else {
                        fail("Map does not contain a table key");
                    }

                    if (found) {
                        break;
                    }
                }
            }
        }
    }

    private void compareFields(Map<String, Object> tagMap, IDbSchema schema) {
        List<Map<String, String>> columnList = schema.getColumnMetadata();
        assertNotNull("Null column metadata list", columnList);

        for (String key : tagMap.keySet()) {
            assertNotNull(key);
            int indx = getIndexSuffix(key);
            String sKey = stripTrailingNums(key);

            if (indx >= 0) {
                assertTrue(indx <= columnList.size());
                Map<String, String> colMap = columnList.get(indx);
                assertNotNull(colMap);
                assertTrue("Missing schema column key: " + key, colMap.containsKey(sKey));
                assertEquals("Non-matching column value; key: " + key,
                        tagMap.get(key), colMap.get(sKey));
            }
        }
    }

    private int getIndexSuffix(String key) {
        if (key != null) {
            int i = 1;
            String retStr = "";
            while (Character.isDigit(key.charAt(key.length() - i))) {
                retStr = key.charAt(key.length() - i) + retStr;
                i++;
            }

            if (i > 1) {
                return new Integer(retStr);
            }
        }
        return -1;
    }

    private String stripTrailingNums(String key) {
        String newKey = key;
        if (newKey != null) {
            // Dumb, dumb, dumb... (and error-prone).
            while (Character.isDigit(newKey.charAt(newKey.length() - 1))) {
                assertTrue(newKey.length() > 0);
                newKey = newKey.substring(0, newKey.length() - 1);
            }
        }
        return newKey;
    }
}
