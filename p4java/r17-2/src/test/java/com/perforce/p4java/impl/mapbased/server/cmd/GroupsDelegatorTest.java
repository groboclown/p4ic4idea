package com.perforce.p4java.impl.mapbased.server.cmd;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.expectThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.google.common.collect.Lists;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.option.server.GetUserGroupsOptions;
import com.perforce.p4java.option.server.UpdateUserGroupOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * @author Sean Shou
 * @since 13/09/2016
 */
@RunWith(JUnitPlatform.class)
public class GroupsDelegatorTest extends P4JavaTestCase {
    private GroupsDelegator groupsDelegator;
    private String groupName = "grpName1";
    private IUserGroup userGroup1;
    private UpdateUserGroupOptions opts1;
    private HashMap<String, Object> firstResultMap;
    private List<Map<String, Object>> resultMaps;
    private IUserGroup group;

    @BeforeEach
    public void beforeEach() {
        server = mock(IOptionsServer.class);
        groupsDelegator = new GroupsDelegator(server);

        userGroup1 = mock(IUserGroup.class);
        opts1 = mock(UpdateUserGroupOptions.class);
        firstResultMap = mock(HashMap.class);
        resultMaps = Lists.newArrayList(firstResultMap);
        group = mock(IUserGroup.class);
    }

    /**
     * Test that the delegator calls the server with the correct parameters and
     * returns data with predicatable values.
     * 
     * TODO: check that -i and -v are the correct parameters to always pass,
     * particularly -i as Swarm found some difficulties with this.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testGetGroups() throws P4JavaException {

        when(server.execMapCmdList(eq(CmdSpec.GROUPS.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-i", "-v", groupName })),
                eq(null))).thenReturn(resultMaps);
        when(firstResultMap.get(MapKeys.GROUP_LC_KEY)).thenReturn(groupName);

        List<IUserGroup> groups = groupsDelegator.getUserGroups(groupName, true, true, -1);
        verify(server).execMapCmdList(eq(CmdSpec.GROUPS.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-i", "-v", groupName })),
                eq(null));
        assertEquals(1, groups.size());
        assertEquals(groups.get(0).getName(), groupName);
    }

    /**
     * Test that a connection exception thrown by the underlying server
     * implementation is correctly propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testConnectionException() throws Exception {
        when(server.execMapCmdList(eq(CmdSpec.GROUPS.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-i", "-v", groupName })),
                eq(null))).thenThrow(new ConnectionException("Read timed out."));
        expectThrows(ConnectionException.class,
                () -> groupsDelegator.getUserGroups(groupName, true, true, -1));
    }

    /**
     * Test that an access exception thrown by the underlying server
     * implementation is correctly propagated as an access exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testAccessException() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.GROUPS.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-i", "-v", groupName })),
                eq(null))).thenThrow(new AccessException("Denied."));
        expectThrows(AccessException.class,
                () -> groupsDelegator.getUserGroups(groupName, true, true, -1));
    }

    /**
     * Test that a request exception thrown by the underlying server
     * implementation is correctly propagated as a request exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testRequestException() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.GROUPS.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-i", "-v", groupName })),
                eq(null))).thenThrow(new RequestException("Invalid"));
        expectThrows(RequestException.class,
                () -> groupsDelegator.getUserGroups(groupName, true, true, -1));
    }

    /**
     * Test that when the server returns a null response, then an empty list is
     * returned by the delegator.
     * 
     * TODO: Check that a null response from the server is ok, it seems like it
     * would indicate that something had gone badly wrong
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testNullResponse() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.GROUPS.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-i", "-v", groupName })),
                eq(null))).thenReturn(null);
        List<IUserGroup> userGroups = groupsDelegator.getUserGroups(groupName,
                new GetUserGroupsOptions());
        assertEquals(0, userGroups.size());
    }

    /**
     * Test that when the server returns an empty response, then an empty list
     * is returned by the delegator.
     * 
     * TODO: Check that an empty response from the server is ok, it seems like
     * it would indicate that something had gone badly wrong
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testEmptyResponse() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.GROUPS.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-i", "-v", groupName })),
                eq(null))).thenReturn(new ArrayList<Map<String, Object>>());
        List<IUserGroup> userGroups = groupsDelegator.getUserGroups(groupName,
                new GetUserGroupsOptions());
        assertEquals(0, userGroups.size());
    }

    /**
     * Test that when the server returns data from many groups the resulting
     * list has the equivalent dataset.
     *
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testGetMany() throws P4JavaException {
        List<Map<String, Object>> populatedResultMaps = new ArrayList<>(4);
        HashMap<String, Object> resultMap1 = createMockResultMap("grpName1", "Sean", "3", "10", "5",
                "6", "100", "1", "0");
        resultMaps.add(resultMap1);

        HashMap<String, Object> resultMap2 = createMockResultMap("grpName1", "subGrp1", "3", "10",
                "5", "6", "100", "0", "1");
        populatedResultMaps.add(resultMap2);

        HashMap<String, Object> resultMap3 = createMockResultMap("grpName1", "subGrp2", "15", "10",
                "5", "6", "100", "0", "1");
        populatedResultMaps.add(resultMap3);

        HashMap<String, Object> resultMap4 = createMockResultMap("grpName2", "Tim", "3", "10", "5",
                "6", "100", "1", "0");
        populatedResultMaps.add(resultMap4);

        when(server.execMapCmdList(eq(CmdSpec.GROUPS.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] {})), eq(null)))
                        .thenReturn(populatedResultMaps);
        when(server.getErrorStr(any())).thenReturn(EMPTY);
        List<IUserGroup> userGroups = groupsDelegator.getUserGroups("", new GetUserGroupsOptions());
        assertEquals(2, userGroups.size());

        for (IUserGroup userGroup : userGroups) {
            if ("grpName1".equals(userGroup.getName())) {
                assertEquals(2, userGroup.getSubgroups().size());
            } else {
                assertEquals("grpName2", userGroup.getName());
                assertTrue(userGroup.getOwners().contains("Tim"));
            }
        }
    }

    /**
     * Test that a null group name means that no group name parameter is passed
     * down to the server and that a single row is returned when the resultmap
     * has an unlimited ticket timeout.
     * 
     * TODO: Add a check to make sure that unlimited becomes -1 and that the
     * resulting group object has all of the relevant attributes populated.
     * There is an A bug related to this.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testUnlimitedTimeout()
            throws P4JavaException {

        List<Map<String, Object>> populatedResultMaps = new ArrayList<>(4);
        HashMap<String, Object> resultMap1 = createMockResultMap("grpName1", "Sean", "3", "10",
                "unlimited", "6", "100", "1", "0");
        populatedResultMaps.add(resultMap1);

        when(server.execMapCmdList(eq(CmdSpec.GROUPS.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] {})), eq(null)))
                        .thenReturn(populatedResultMaps);
        when(firstResultMap.get(MapKeys.GROUP_LC_KEY)).thenReturn(groupName);
        when(server.getErrorStr(any())).thenReturn(EMPTY);
        List<IUserGroup> userGroups = groupsDelegator.getUserGroups(null,
                mock(GetUserGroupsOptions.class));
        assertEquals(1, userGroups.size());
/* when the a bug is fixed
        assertEquals(IUserGroup.UNLIMITED, userGroups.get(0).getTimeout());
*/
    }

    /**
     * Build a resultmap entry for a group spec, populated with the calues
     * provided
     * 
     * @param groupName
     *            The name of the group
     * @param userName
     *            The name of the user
     * @param maxScanRows
     *            The max size of the dataset to process
     * @param maxLockTime
     *            The max time to hold a lock
     * @param timeout
     *            The ticket timeout value
     * @param passwordTimeout
     *            The maximum age of a user's password
     * @param maxResults
     *            The max results to allow this command to process
     * @param isOwner
     *            Whether this user is the group owner
     * @param isSubGroup
     *            Whether
     * @return
     */
    private HashMap<String, Object> createMockResultMap(String groupName, String userName,
            String maxScanRows, String maxLockTime, String timeout, String passwordTimeout,
            String maxResults, String isOwner, String isSubGroup) {

        HashMap<String, Object> resultMap = mock(HashMap.class);
        when(resultMap.get(MapKeys.GROUP_LC_KEY)).thenReturn(groupName);

        when(resultMap.get(MapKeys.USER_LC_KEY)).thenReturn(userName);
        when(resultMap.get(MapKeys.MAXSCANROWS_LC_KEY)).thenReturn(maxScanRows);
        when(resultMap.get(MapKeys.MAXLOCKTIME_LC_KEY)).thenReturn(maxLockTime);
        when(resultMap.get(MapKeys.TIMEOUT_LC_KEY)).thenReturn(timeout);
        when(resultMap.get(MapKeys.PASSWORD_TIMEOUT_LC_KEY)).thenReturn(passwordTimeout);
        when(resultMap.get(MapKeys.MAXRESULTS_LC_KEY)).thenReturn(maxResults);
        when(resultMap.get(MapKeys.ISOWNER_LC_KEY)).thenReturn(isOwner);
        when(resultMap.get(MapKeys.ISSUBGROUP_LC_KEY)).thenReturn(isSubGroup);

        return resultMap;
    }
}