package com.perforce.p4java.mapapi;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.tests.SimpleServerRule;
import com.perforce.p4java.tests.UnicodeServerRule;
import com.perforce.p4java.tests.dev.unit.P4JavaRshTestCase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

// @Ignore("p4ic4idea: SimpleServerRule crashes for some Linux setups")
public class MapTableBuilderTests extends P4JavaRshTestCase {

    @ClassRule
    public static SimpleServerRule p4d = new SimpleServerRule("r16.1", MapTableBuilderTests.class.getSimpleName());

    static IClient client = null;

    @BeforeClass
    public static void setUp() throws Exception {
        setupServer(p4d.getRSHURL(), userName, password, true, null);
        client = getClient(server);
    }

    @Test
    public void MapBuilderTest() {
        MapTable mt = MapTableBuilder.buildMapTable(client);
        assertTrue(mt.hasMaps);
        assertTrue(mt.get(0).lhs().get().contains("//depot/..."));
        assertTrue(mt.get(0).rhs().get().contains(client.getName()));
    }

}
