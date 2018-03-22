package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.RENAMEUSER;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.runner.RunWith;

import com.nitorcreations.junit.runners.NestedRunner;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.impl.mapbased.server.Server;

/**
 * @author Sean Shou
 * @since 16/09/2016
 */
@RunWith(NestedRunner.class)
public class RenameUserDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private RenameUserDelegator renameUserDelegator;
    private List<Map<String, Object>> resultMaps;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        renameUserDelegator = new RenameUserDelegator(server);
    }

    /**
     * Test renameUser()
     */
    public class TestRenameUser {
        /**
         * Rule for expected exception verification
         */
        @Rule
        public ExpectedException thrown = ExpectedException.none();

        private String oldUserName;
        private String newUserName;
        private String[] cmdArguments;

        /**
         * Runs before every test.
         */
        @SuppressWarnings("unchecked")
        @Before
        public void beforeEach() {
            oldUserName = "testUser";
            newUserName = "newTestUser";

            cmdArguments = new String[]{"--from=" + oldUserName, "--to=" + newUserName};
        }

        /**
         * Expected throws <code>IllegalArgumentException</code> when 'oldUserName' is blank.
         *
         * @throws Exception
         */
        @Test
        public void shouldThrownIllegalArgumentExceptionWhenOldUserNameIsBlank() throws Exception {
            thrown.expect(IllegalArgumentException.class);
            //given
            oldUserName = EMPTY;

            //when
            renameUserDelegator.renameUser(oldUserName, newUserName);
        }

        /**
         * Expected throws <code>IllegalArgumentException</code> when 'newUserName' is blank.
         *
         * @throws Exception
         */
        @Test
        public void shouldThrownIllegalArgumentExceptionWhenNewUserNameIsBlank() throws Exception {
            thrown.expect(IllegalArgumentException.class);
            //given
            newUserName = EMPTY;
            //when
            renameUserDelegator.renameUser(oldUserName, newUserName);
        }

        /**
         * Expected return non blank rename user name
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnNonBlankRenamedUserName() throws Exception {
            //given
            resultMaps = buildRenameUserResultMaps();
            when(server.execMapCmdList(eq(RENAMEUSER.toString()), eq(cmdArguments), any()))
                    .thenReturn(resultMaps);

            //when
            String renameUser = renameUserDelegator.renameUser(oldUserName, newUserName);

            //then
            assertThat(renameUser, is(newUserName));
        }

        private List<Map<String, Object>> buildRenameUserResultMaps() {
            List<Map<String, Object>> list = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            map.put(FMT0, "%renameUserTo%");
            map.put(CODE0, MESSAGE_CODE_IN_INFO_RANGE);
            map.put("renameUserTo", newUserName);
            list.add(map);
            return list;
        }
    }
}