package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.NEW_PASSWORD;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.NEW_PASSWORD2;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OLD_PASSWORD;
import static com.perforce.p4java.server.CmdSpec.PASSWD;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.impl.mapbased.server.Server;

/**
 * @author Sean Shou
 * @since 5/10/2016
 */
public class PasswdDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";

    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private PasswdDelegator passwdDelegator;
    private String oldPassword;
    private String newPassword;
    private String userName;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        passwdDelegator = new PasswdDelegator(server);

        oldPassword = "old password";
        newPassword = "my New password";
        userName = "p4javaTestUser";
    }

    /**
     * Test changePassword() when oldPassword is blank
     *
     * @throws Exception
     */
    @Test
    public void testChangePasswordWhenOldPasswordIsBlank() throws Exception {
        //given
        oldPassword = EMPTY;
        //then
        executeAndVerify(newPassword + "\n", oldPassword, newPassword + "\n");
    }

    /**
     * Test changePassword() when both oldPassword and newPassword is blank
     *
     * @throws Exception
     */
    @Test
    public void testChangePasswordWhenOldPasswordIsBlankAndNewPasswordIsBlank() throws Exception {
        //given
        oldPassword = EMPTY;
        newPassword = EMPTY;

        executeAndVerify("\n", oldPassword, "\n");
    }

    /**
     * Test changePassword() when oldPassword is not blank, but newPassword is blank
     *
     * @throws Exception
     */
    @Test
    public void testChangePasswordWhenOldPasswordIsNotBlankButNewPasswordIsBlank() throws Exception {
        //given
        newPassword = EMPTY;

        executeAndVerify("\n", oldPassword + "\n", "\n");
    }

    private void executeAndVerify(
            String expectedNewPassword2String,
            String expectedOldPasswordString,
            String expectedNewPasswordString) throws Exception {

        Map<String, Object> pwdMap = ImmutableMap.of(
                NEW_PASSWORD2,
                expectedNewPassword2String,
                OLD_PASSWORD,
                expectedOldPasswordString,
                NEW_PASSWORD,
                expectedNewPasswordString);

        when(server.execMapCmdList(eq(PASSWD.toString()), eq(new String[]{userName}), eq(pwdMap)))
                .thenReturn(buildChangePasswordList());

        //when
        String changePassword = passwdDelegator.changePassword(oldPassword, newPassword, userName);
        assertThat(changePassword, is(newPassword));
    }

    private List<Map<String, Object>> buildChangePasswordList() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put(FMT0, "%newPassword%");
        map.put(CODE0, MESSAGE_CODE_IN_INFO_RANGE);
        map.put("oldPassword", oldPassword);
        map.put("newPassword", newPassword);
        list.add(map);
        return list;
    }
}