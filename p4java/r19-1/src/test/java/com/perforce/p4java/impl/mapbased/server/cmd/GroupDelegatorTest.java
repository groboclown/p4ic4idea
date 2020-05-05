package com.perforce.p4java.impl.mapbased.server.cmd;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.IUserGroup;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.impl.generic.core.UserGroup;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.option.server.UpdateUserGroupOptions;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

public class GroupDelegatorTest extends P4JavaTestCase {
    private GroupDelegator groupDelegator;
    private String groupName = "grpName1";
    private HashMap<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private IUserGroup group;

    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(IOptionsServer.class);

        groupDelegator = new GroupDelegator(server);

        resultMap = mock(HashMap.class);
        resultMaps = new ArrayList<Map<String, Object>>();
        resultMaps.add(resultMap);
        group = mock(IUserGroup.class);
    }

    /**
     * Test that when a server gives a null response to a group -o command the
     * delegate returns null to any client.
     * 
     * TODO: Check that null is actually a valid server response, otherwise this
     * should be an exception
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testNullServerResponse() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.GROUP.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-o", groupName })),
                eq((Map<String, Object>) null))).thenReturn(null);

        IUserGroup userGroup = groupDelegator.getUserGroup(groupName);
        assertNull(userGroup);
    }

    /**
     * Test that when a server sends a resultmap containing group data, it gets
     * put into the group object returned from the delegate.
     * 
     * TODO: Use more attributes in the resultmap.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testGetUserGroup() throws P4JavaException {
        when(server.execMapCmdList(eq(CmdSpec.GROUP.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-o", groupName })),
                eq((Map<String, Object>) null))).thenReturn(resultMaps);
        when(resultMap.get(MapKeys.GROUP_KEY)).thenReturn(groupName);

        IUserGroup userGroup = groupDelegator.getUserGroup(groupName);
        assertNotNull(userGroup);
        assertEquals(groupName, userGroup.getName());
    }

    /**
     * Test that creating a user will throw an exception if a null group object
     * is passed and return the name of the created group when creation is
     * successful.
     * 
     * TODO: Setting more fields on the group object will allow better testing
     * of how execmap... is invoked, using a real group object instead of a mock
     * would be better
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test(expected = NullPointerException.class)
    public void testCreateUserGroup() throws P4JavaException {

        // setup
        when(group.getName()).thenReturn(groupName);
        Map<String, Object> inMap = InputMapper.map(group);
        when(server.execMapCmdList(eq(CmdSpec.GROUP.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-i" })), eq(inMap)))
                        .thenReturn(resultMaps);
        when(resultMap.get(RpcFunctionMapKey.CODE0)).thenReturn("268435456");
        when(resultMap.get(RpcFunctionMapKey.FMT0)).thenReturn(groupName);

        // Normal group creation
        String userGroup = groupDelegator.createUserGroup(group);
        assertEquals(groupName, userGroup);

        // null pointer thrown for null group object
        groupDelegator.createUserGroup(null);

    }

    /**
     * Test that updating a group will pass call the server correctly and
     * extract the name of the group updated from the info message in the server
     * response.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testUpdateGroup() throws P4JavaException {
        when(group.getName()).thenReturn(groupName);
        Map<String, Object> inMap = InputMapper.map(group);
        when(server.execMapCmdList(eq(CmdSpec.GROUP.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-i" })), eq(inMap)))
                        .thenReturn(resultMaps);
        when(resultMap.get(RpcFunctionMapKey.CODE0)).thenReturn("268435456");
        when(resultMap.get(RpcFunctionMapKey.FMT0)).thenReturn(groupName);

        String userGroup = groupDelegator.updateUserGroup(group, false);
        assertEquals(groupName, userGroup);
    }

    /**
     * Test that updating a group will pass the updateIfOwner flag through to
     * the server correctly and return the name of the new group.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testOwnerUpdateGroup() throws P4JavaException {
        when(group.getName()).thenReturn(groupName);
        Map<String, Object> inMap = InputMapper.map(group);
        when(server.execMapCmdList(eq(CmdSpec.GROUP.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-a", "-i" })), eq(inMap)))
                        .thenReturn(resultMaps);
        when(resultMap.get(RpcFunctionMapKey.CODE0)).thenReturn("268435456");
        when(resultMap.get(RpcFunctionMapKey.FMT0)).thenReturn(groupName);

        String userGroup = groupDelegator.updateUserGroup(group, true);
        assertEquals(groupName, userGroup);
    }

    /**
     * Test that deleting a user will throw an exception if a null group object
     * is passed and return the name of the created group when creation is
     * successful.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testDeleteUserGroup() throws P4JavaException {
        when(group.getName()).thenReturn(groupName);
        when(server.execMapCmdList(eq(CmdSpec.GROUP.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-d", groupName })),
                eq((Map<String, Object>) null))).thenReturn(resultMaps);
        when(resultMap.get(RpcFunctionMapKey.CODE0)).thenReturn("268435456");
        when(resultMap.get(RpcFunctionMapKey.FMT0)).thenReturn(groupName);

        String userGroup = groupDelegator.deleteUserGroup(group);
        assertEquals(groupName, userGroup);
    }

    /**
     * Test that a connection exception thrown by the underlying server
     * implementation is correctly propagated as a connection exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConnectionException() throws P4JavaException {
        when(group.getName()).thenReturn(groupName);
        when(server.execMapCmdList(eq(CmdSpec.GROUP.toString()), any(String[].class),
                (Map<String, Object>) any())).thenThrow(new ConnectionException("Read timeout!"));
        try {
            groupDelegator.getUserGroup(groupName);
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(ConnectionException.class, e.getClass());
        }
        try {
            groupDelegator.createUserGroup(group, new UpdateUserGroupOptions());
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(ConnectionException.class, e.getClass());
        }
        try {
            groupDelegator.updateUserGroup(group, new UpdateUserGroupOptions());
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(ConnectionException.class, e.getClass());
        }
        try {
            groupDelegator.deleteUserGroup(group, new UpdateUserGroupOptions());
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(ConnectionException.class, e.getClass());
        }
    }

    /**
     * Test that an access exception thrown by the underlying server
     * implementation is correctly propagated as an access exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAccessException() throws P4JavaException {
        when(group.getName()).thenReturn(groupName);
        when(server.execMapCmdList(eq(CmdSpec.GROUP.toString()), any(String[].class),
                (Map<String, Object>) any())).thenThrow(new AccessException("Not allowed!"));
        try {
            groupDelegator.getUserGroup(groupName);
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(AccessException.class, e.getClass());
        }
        try {
            groupDelegator.createUserGroup(group, new UpdateUserGroupOptions());
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(AccessException.class, e.getClass());
        }
        try {
            groupDelegator.updateUserGroup(group, new UpdateUserGroupOptions());
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(AccessException.class, e.getClass());
        }
        try {
            groupDelegator.deleteUserGroup(group, new UpdateUserGroupOptions());
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(AccessException.class, e.getClass());
        }
    }

    /**
     * Test that a request exception thrown by the underlying server
     * implementation is correctly propagated as a request exception.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testRequestException() throws P4JavaException {
        when(group.getName()).thenReturn(groupName);
        when(server.execMapCmdList(eq(CmdSpec.GROUP.toString()), any(String[].class),
                (Map<String, Object>) any())).thenThrow(new RequestException("Perforce (PASSWD)"));
        try {
            groupDelegator.getUserGroup(groupName);
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(RequestException.class, e.getClass());
        }
        try {
            groupDelegator.createUserGroup(group, new UpdateUserGroupOptions());
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(RequestException.class, e.getClass());
        }
        try {
            groupDelegator.updateUserGroup(group, new UpdateUserGroupOptions());
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(RequestException.class, e.getClass());
        }
        try {
            groupDelegator.deleteUserGroup(group, new UpdateUserGroupOptions());
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(RequestException.class, e.getClass());
        }
    }

    /**
     * Test that creating a user will throw an exception if null group and
     * options objects are passed and return the name of the created group when
     * creation is successful. Also verifies that the Admin option is correctly
     * passed down to the server.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testCreatUserGroupWithOptions() throws P4JavaException {
        when(group.getName()).thenReturn(groupName);
        Map<String, Object> inMap = InputMapper.map(group);
        when(server.execMapCmdList(eq(CmdSpec.GROUP.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-A", "-i" })), eq(inMap)))
                        .thenReturn(resultMaps);
        when(resultMap.get(RpcFunctionMapKey.CODE0)).thenReturn("268435456");
        when(resultMap.get(RpcFunctionMapKey.FMT0)).thenReturn(groupName);

        try {
            groupDelegator.createUserGroup(null, null);
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(NullPointerException.class, e.getClass());
        }
        String userGroup = groupDelegator.createUserGroup(group,
                new UpdateUserGroupOptions().setAddIfAdmin(true));
        assertEquals(groupName, userGroup);
    }

    /**
     * Test that creating a user will throw an exception if null group and
     * options objects are passed and return the name of the created group when
     * creation is successful. Also verifies that the Admin option is correctly
     * passed down to the server.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testUpdateUserGroupWithOptions() throws P4JavaException {
        when(group.getName()).thenReturn(groupName);
        Map<String, Object> inMap = InputMapper.map(group);
        when(server.execMapCmdList(eq(CmdSpec.GROUP.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-a", "-i" })), eq(inMap)))
                        .thenReturn(resultMaps);
        when(resultMap.get(RpcFunctionMapKey.CODE0)).thenReturn("268435456");
        when(resultMap.get(RpcFunctionMapKey.FMT0)).thenReturn(groupName);

        try {
            groupDelegator.updateUserGroup(null, null);
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(NullPointerException.class, e.getClass());
        }
        String userGroup = groupDelegator.updateUserGroup(group,
                new UpdateUserGroupOptions().setUpdateIfOwner(true));
        assertEquals(groupName, userGroup);
    }

    /**
     * Test that deleting a user will throw an exception if null group and
     * options objects are passed, an illegal argument exception when a group
     * with a blank name is given, and return the name of the created group when
     * deletion is successful.
     * 
     * TODO: We need to support the -F option, which will need a new Options
     * object
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testDeleteUserGroupWithOptions() throws P4JavaException {
        when(group.getName()).thenReturn(groupName);
        when(server.execMapCmdList(eq(CmdSpec.GROUP.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "-d", groupName })),
                eq((Map<String, Object>) null))).thenReturn(resultMaps);
        when(resultMap.get(RpcFunctionMapKey.CODE0)).thenReturn("268435456");
        when(resultMap.get(RpcFunctionMapKey.FMT0)).thenReturn(groupName);
        try {
            groupDelegator.deleteUserGroup(null, null);
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(NullPointerException.class, e.getClass());
        }
        try {
            UserGroup emptyName = new UserGroup();
            emptyName.setName(EMPTY);
            groupDelegator.deleteUserGroup(emptyName, null);
            fail("Expected exception");
        } catch (Throwable e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }

        String userGroup = groupDelegator.deleteUserGroup(group, new UpdateUserGroupOptions());
        assertEquals(groupName, userGroup);
    }
}