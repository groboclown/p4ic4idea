package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.server.CmdSpec.PROTECTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetProtectionEntriesOptions;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Sean Shou
 * @since 5/10/2016
 */
public class ProtectsDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String TEST_FILE_DEPOT_PATH = "//depot/dev/test.txt";
    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private String[] cmdArguments;
    private ProtectsDelegator protectsDelegator;
    private List<Map<String, Object>> resultMaps;

    private List<IFileSpec> fileSpecs;
    private GetProtectionEntriesOptions opts;

    private boolean allUsers;
    private String hostName;
    private String userName;
    private String groupName;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        protectsDelegator = new ProtectsDelegator(server);
        Map<String, Object> resultMap = mock(Map.class);
        Map<String, Object> resultMap2 = mock(Map.class);
        resultMaps = newArrayList(resultMap, resultMap2);

        fileSpecs = FileSpecBuilder.makeFileSpecList(TEST_FILE_DEPOT_PATH);

        allUsers = true;
        hostName = "host name";
        userName = "user name";
        groupName = "group name";

        String[] cmdOptions = {
                "-a",
                "-g" + groupName,
                "-u" + userName,
                "-h" + hostName};

        cmdArguments = ArrayUtils.add(cmdOptions, TEST_FILE_DEPOT_PATH);

        opts = new GetProtectionEntriesOptions(cmdOptions);
    }

    /**
     * Expected throws <code>ConnectionException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void testGetProtectionEntriesShouldThrownConnectionExceptionWhenInnerMethodThrowsIt()
            throws Exception {

        getProtectionEntriesThrowsExceptions(
                ConnectionException.class,
                ConnectionException.class);
    }

    /**
     * Expected throws <code>AccessException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void testGetProtectionEntriesShouldThrownRAccessExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {

        getProtectionEntriesThrowsExceptions(
                AccessException.class,
                AccessException.class);
    }

    /**
     * Expected throws <code>RequestException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void testGetProtectionEntriesShouldThrownRequestExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {

        getProtectionEntriesThrowsExceptions(
                RequestException.class,
                RequestException.class);
    }

    /**
     * Expected throws <code>RequestException</code> when inner method call throws <code>P4JavaException</code>
     *
     * @throws Exception
     */
    @Test
    public void testGetProtectionEntriesShouldThrownRequestExceptionWhenInnerMethodCallThrownP4JavaException()
            throws Exception {

        getProtectionEntriesThrowsExceptions(
                P4JavaException.class,
                RequestException.class);
    }

    private void getProtectionEntriesThrowsExceptions(
            Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {

        thrown.expect(expectedThrows);

        doThrow(thrownException).when(server)
                .execMapCmdList(eq(PROTECTS.toString()), eq(cmdArguments), eq(null));
        protectsDelegator.getProtectionEntries(allUsers, hostName, userName, groupName, fileSpecs);
    }

    /**
     * Expected return non empty protections entries
     *
     * @throws Exception
     */
    @Test
    public void testGetProtectionEntriesShouldReturnNonEmptyList()
            throws Exception {

        //given
        when(server.execMapCmdList(eq(PROTECTS.toString()), eq(cmdArguments), eq(null)))
                .thenReturn(resultMaps);
        //when
        List<IProtectionEntry> protectionEntries = protectsDelegator.getProtectionEntries(
                allUsers,
                hostName,
                userName,
                groupName,
                fileSpecs);
        //then
        assertThat(protectionEntries.size(), is(2));
        assertThat(protectionEntries.get(0).getOrder(), is(0));
        assertThat(protectionEntries.get(1).getOrder(), is(1));
    }

    /**
     * Expected return non empty protections entries
     *
     * @throws Exception
     */
    @Test
    public void testGetProtectionEntriesByOptionsShouldReturnNonEmptyList() throws Exception {
        //given
        when(server.execMapCmdList(eq(PROTECTS.toString()), eq(cmdArguments), eq(null)))
                .thenReturn(resultMaps);
        //when
        List<IProtectionEntry> protectionEntries = protectsDelegator.getProtectionEntries(
                fileSpecs,
                opts);
        //then
        assertThat(protectionEntries.size(), is(2));
        assertThat(protectionEntries.get(0).getOrder(), is(0));
        assertThat(protectionEntries.get(1).getOrder(), is(1));
    }
}