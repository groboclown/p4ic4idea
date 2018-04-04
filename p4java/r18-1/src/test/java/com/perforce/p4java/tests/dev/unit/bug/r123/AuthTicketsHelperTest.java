package com.perforce.p4java.tests.dev.unit.bug.r123;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.common.base.OSUtils;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.RpcSystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.WindowsRpcSystemFileCommandsHelper;
import com.perforce.p4java.server.AuthTicketsHelper;
import com.perforce.p4java.tests.dev.annotations.Jobs;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * The Class AuthTicketsHelperTest.
 */
@RunWith(JUnitPlatform.class)
@Jobs({ "job057711" })
@TestId("Dev123_AuthTicketsHelperTest")
public class AuthTicketsHelperTest extends P4JavaTestCase {
    
    /** The files helper. */
    private static SymbolicLinkHelper filesHelper;
    
    /**
     * Before all.
     */
    @BeforeAll
    public static void beforeAll() {
        filesHelper = OSUtils.isWindows() 
                ? new WindowsRpcSystemFileCommandsHelper() : new RpcSystemFileCommandsHelper();
    }
    
    /**
     * Test saving auth tickets.
     *
     * @throws Exception the exception
     */
    @Test
    public void testSaveAuthTickets() throws Exception {
        int randNo = getRandomInt();

        String address = "server:1666";
        String value = "ABCDEF1231233";
        String user = "bruno1";

        String ticketsFilePath = System.getProperty("user.dir");
        assertThat(ticketsFilePath, notNullValue());
        ticketsFilePath += File.separator + "realticketsfile3" + randNo;
        final int ticketsToWrite = 5;
        try {
            // write 5 tickets
            for (int i = 0; i < ticketsToWrite; i++) {
                address += i;
                value += i;
                user += i;
                AuthTicketsHelper.saveTicket(user, address, value, ticketsFilePath);
            }
        } finally {
            filesHelper.setWritable(ticketsFilePath, true);
            boolean deleted = Files.deleteIfExists(Paths.get(ticketsFilePath + ".lck"));
            assertThat(deleted, is(false));
            deleted = Files.deleteIfExists(Paths.get(ticketsFilePath));
            assertThat(deleted, is(true));
        }
    }
}
