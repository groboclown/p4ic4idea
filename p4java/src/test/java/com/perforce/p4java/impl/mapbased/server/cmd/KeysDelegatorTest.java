package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.exception.MessageSeverityCode.E_INFO;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.VALUE;
import static com.perforce.p4java.server.CmdSpec.KEYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.server.IOptionsServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.nitorcreations.junit.runners.NestedRunner;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetKeysOptions;

/**
 * @author Sean Shou
 * @since 27/09/2016
 */
@RunWith(NestedRunner.class)
public class KeysDelegatorTest {
    private static final String KEY_NAME = "testKey";
    private static final String KEY_VALUE = "testValue";
    private static final String[] KEYS_ARGUMENTS = {"-emykey-*", "-m10"};
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private KeysDelegator keysDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private GetKeysOptions opts;
    private IOptionsServer server;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        keysDelegator = new KeysDelegator(server);
        // p4ic4idea: just use a hashmap, as it allows the isEmpty and other calls to work as expected.
        resultMap = new HashMap<>();
        resultMaps = newArrayList(resultMap);

        opts = new GetKeysOptions(KEYS_ARGUMENTS);
    }

    private void populateResultMap() {
        resultMap.put(CODE0, MESSAGE_CODE_IN_INFO_RANGE);
        resultMap.put(VALUE, KEY_VALUE);
        resultMap.put("key", KEY_NAME);
    }

    /**
     * Test getKeys()
     *
     * @throws Exception
     */
    public class TestGetKeys {
        /**
         * Test getKeys() by <code>GetKeysOptions</code>.
         * <p>
         * It's expected thrown <code>ConnectionException</code> when inner call thrown 'any' exception.
         *
         * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
         */
        @Test(expected = ConnectionException.class)
        public void shouldThrowConnectionExceptionWhenInnerMethodCallThrowsIt() throws Exception {
            doThrow(ConnectionException.class)
                    .when(server)
                    .execMapCmdList(eq(KEYS.toString()), eq(KEYS_ARGUMENTS), eq(null));
            keysDelegator.getKeys(opts);
            verify(resultMap, never()).get(E_INFO);
        }

        /**
         * It's expected return non empty keys map
         *
         * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
         */
        @Test
        public void shouldReturnNonEmptyKeysMap() throws Exception {
            //given
            populateResultMap();
            when(server.execMapCmdList(eq(KEYS.toString()), eq(KEYS_ARGUMENTS), eq(null)))
                    .thenReturn(resultMaps);
            //when
            Map<String, String> keys = keysDelegator.getKeys(opts);
            //then
            assertThat(keys.size(), is(1));
            assertThat(keys.get(KEY_NAME), is(KEY_VALUE));
        }
    }
}