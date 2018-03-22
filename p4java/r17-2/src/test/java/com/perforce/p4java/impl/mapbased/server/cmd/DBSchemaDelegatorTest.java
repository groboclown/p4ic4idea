package com.perforce.p4java.impl.mapbased.server.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.perforce.p4java.server.CmdSpec.DBSCHEMA;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.admin.IDbSchema;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.delegator.IDBSchemaDelegator;

/**
 * Tests the DBSchemaDelegator.
 */
@RunWith(JUnitPlatform.class)
public class DBSchemaDelegatorTest extends AbstractP4JavaUnitTest {

    /** Example table name. */
    private static final String DB_CONFIG = "db.config";

    /** The dbschemadelegator. */
    private IDBSchemaDelegator dBSchemaDelegator;

    /**
     * Before each.
     */
    @BeforeEach
    public void beforeEach() {
        server = mock(Server.class);
        dBSchemaDelegator = new DBSchemaDelegator(server);
    }

    /**
     * Gets the db schema with a table name.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testGetDbSchemaWithTable() throws Exception {
        CommandLineArgumentMatcher matcher = new CommandLineArgumentMatcher(
                new String[] { DB_CONFIG });
        when(server.execMapCmdList(eq(DBSCHEMA.toString()), argThat(matcher), eq(null)))
                .thenReturn(getMockResultMaps());
        List<IDbSchema> schemas = dBSchemaDelegator
                .getDbSchema(Arrays.asList(new String[] { DB_CONFIG }));
        verify(server).execMapCmdList(eq(DBSCHEMA.toString()), argThat(matcher), eq(null));
        assertResult(schemas);
    }

    /**
     * Gets the db schema without a table name.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    public void testGetDbSchemaWithoutTable() throws Exception {
        CommandLineArgumentMatcher matcher = new CommandLineArgumentMatcher(new String[] {});
        when(server.execMapCmdList(eq(DBSCHEMA.toString()), argThat(matcher), eq(null)))
                .thenReturn(getMockResultMaps());
        List<IDbSchema> schemas = dBSchemaDelegator.getDbSchema(null);
        verify(server).execMapCmdList(eq(DBSCHEMA.toString()), argThat(matcher), eq(null));
        assertResult(schemas);
    }

    /**
     * Assert that the list of IDbSchemas agrees with the mock server response..
     *
     * @param schemas
     *            the schemas
     */
    private void assertResult(final List<IDbSchema> schemas) {
        final int metaSize = 3;
        assertNotNull(schemas);
        assertEquals(1, schemas.size());
        IDbSchema schema = schemas.get(0);
        assertEquals(DB_CONFIG, schema.getName());
        List<Map<String, String>> metadata = schema.getColumnMetadata();
        assertNotNull(metadata);
        assertEquals(metaSize, metadata.size());
        assertTrue(metadata.contains(buildMetaMap("string", "CFsname", "key", "key", "yes")));
        assertTrue(metadata.contains(buildMetaMap("string", "CFName", "key", "key", "yes")));
        assertTrue(metadata.contains(buildMetaMap("string", "CFValue", "value", "text", null)));
    }

    /**
     * Builds a map of expected column metadata in the IDbSchema.
     *
     * @param fmtKind
     *            the fmt kind
     * @param name
     *            the name
     * @param dmType
     *            the dm type
     * @param type
     *            the type
     * @param key
     *            the key
     * @return the map
     */
    private Map<String, Object> buildMetaMap(final String fmtKind, final String name,
            final String dmType, final String type, final String key) {
        Map<String, Object> metaMap = new HashMap<>();
        metaMap.put("fmtkind", fmtKind);
        metaMap.put("name", name);
        metaMap.put("dmtype", dmType);
        metaMap.put("type", type);
        if (key != null) {
            metaMap.put("key", key);
        }
        return metaMap;
    }

    /**
     * Gets a mock result for schema returned by the server. In this case its a
     * simple single entry describing the db.config table.
     *
     * @return the mock result maps
     */
    private List<Map<String, Object>> getMockResultMaps() {
        List<Map<String, Object>> resultMaps = new ArrayList<>();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("key1", "yes");
        resultMap.put("key0", "yes");
        resultMap.put("type2", "text");
        resultMap.put("dmtype1", "key");
        resultMap.put("dmtype2", "value");
        resultMap.put("dmtype0", "key");
        resultMap.put("fmtkind0", "string");
        resultMap.put("type1", "key");
        resultMap.put("version", "1");
        resultMap.put("type0", "key");
        resultMap.put("fmtkind1", "string");
        resultMap.put("fmtkind2", "string");
        resultMap.put("name2", "CFValue");
        resultMap.put("name1", "CFName");
        resultMap.put("table", DB_CONFIG);
        resultMap.put("name0", "CFsname");
        resultMaps.add(resultMap);
        return resultMaps;
    }

}