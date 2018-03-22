package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.exception.MessageSeverityCode.E_FAILED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.VALUE;
import static com.perforce.p4java.server.CmdSpec.KEY;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.nitorcreations.junit.runners.NestedRunner;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.KeyOptions;

/**
 * @author Sean Shou
 * @since 29/09/2016
 */
@RunWith(NestedRunner.class)
public class KeyDelegatorTest extends AbstractP4JavaUnitTest {
    private KeyDelegator keyDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;

    private static final String KEY_NAME = "testKey";
    private static final String KEY_VALUE = "testValue";

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        keyDelegator = new KeyDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = newArrayList(resultMap);
    }

    /**
     * Test deleteKey()
     */
    public class TestDeleteKey {
        private final String[] deleteCmdArguments = {"-d", KEY_NAME};

        /**
         * Expected thrown <code>IllegalArgumentException</code> when to delete key name is blank string.
         *
         * @throws Exception
         */
        @Test(expected = IllegalArgumentException.class)
        public void shouldThrowIllegalArgumentExceptionWhenKeyNameIsBlank() throws Exception {
            keyDelegator.deleteKey(EMPTY);
        }

        /**
         * Expect return deleted key name.
         *
         * @throws Exception
         */
        @Test
        public void shouldSuccessfullyDeleteKey() throws Exception {
            //given
            populateResultMaps(KEY_NAME);
            when(server.execMapCmdList(eq(KEY.toString()), eq(deleteCmdArguments), eq(null)))
                    .thenReturn(resultMaps);
            //when
            String deleteKey = keyDelegator.deleteKey(KEY_NAME);
            //then
            assertThat(deleteKey, is(KEY_NAME));
            verify(resultMap, times(1)).get(VALUE);
        }

    }

    /**
     * Test setKey()
     */
    public class TestSetKey {
        private final String[] setCmdArguments = {KEY_NAME, KEY_VALUE};
        private KeyOptions options;

        /**
         * Runs before every test.
         */
        @Before
        public void beforeEach() {
            options = new KeyOptions();
        }

        /**
         * Expected setKey() thrown expception when any inner call throws it
         *
         * @throws Exception
         */
        @Test(expected = AccessException.class)
        public void shouldThrowExceptionWhenInnerCallThrownIt() throws Exception {
            //given
            doThrow(AccessException.class).when(server)
                    .execMapCmdList(eq(KEY.toString()), eq(setCmdArguments), eq(null));
            //then
            keyDelegator.setKey(KEY_NAME, KEY_VALUE, options);
        }

        /**
         * Expected setKey() return created key name
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnCreatedKeyName() throws Exception {
            //given
            populateResultMaps(KEY_NAME);
            when(server.execMapCmdList(eq(KEY.toString()), eq(setCmdArguments), eq(null)))
                    .thenReturn(resultMaps);
            //when
            String actual = keyDelegator.setKey(KEY_NAME, KEY_VALUE, options);
            //then
            assertThat(actual, is(KEY_NAME));
        }
    }

    /**
     * Test getKey()
     */
    public class TestGetKey {
        private final String[] getCmdArguments = {KEY_NAME};

        /**
         * Expected getKey() throws <code>IllegalArgumentException</code> when 'keyName' is blank.
         *
         * @throws Exception
         */
        @Test(expected = IllegalArgumentException.class)
        public void shouldThrowIllegalArgumentExceptionWhenKeyNameIsBlank() throws Exception {
            keyDelegator.getKey(EMPTY);
        }

        /**
         * Expected getKey() return the key value
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnKeyValue() throws Exception {
            //given
            populateResultMaps(KEY_VALUE);
            when(server.execMapCmdList(eq(KEY.toString()), eq(getCmdArguments), eq(null)))
                    .thenReturn(resultMaps);
            //when
            String actual = keyDelegator.getKey(KEY_NAME);
            //then
            assertThat(actual, is(KEY_VALUE));
        }
    }

    private void populateResultMaps(String value) {
        when(resultMap.get(E_FAILED)).thenReturn(EMPTY);
        when(resultMap.containsKey(VALUE)).thenReturn(true);
        when(resultMap.get(VALUE)).thenReturn(value);
    }
}