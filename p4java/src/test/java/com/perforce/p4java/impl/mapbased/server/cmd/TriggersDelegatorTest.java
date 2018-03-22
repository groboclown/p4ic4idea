package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.exception.MessageSeverityCode.E_FAILED;
import static com.perforce.p4java.impl.mapbased.MapKeys.TRIGGERS_KEY;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.TRIGGERS;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
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
import com.perforce.p4java.admin.ITriggerEntry;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.tests.UnitTestGiven;

/**
 * @author Sean Shou
 * @since 6/10/2016
 */
@RunWith(NestedRunner.class)
public class TriggersDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private static final String MESSAGE_CODE_NOT_IN_INFO_RANGE = "168435456";
    private static final String INFO_MESSAGE = "create triggers entries";

    private TriggersDelegator triggersDelegator;
    private Map<String, Object> resultMap;
    private Map<String, Object> resultMap2;
    private List<Map<String, Object>> resultMaps;

    private List<ITriggerEntry> entryList;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        triggersDelegator = new TriggersDelegator(server);
        resultMap = mock(Map.class);
        resultMap2 = mock(Map.class);
        resultMaps = newArrayList(resultMap, resultMap2);

        entryList = newArrayList();
        ITriggerEntry triggerEntry = mock(ITriggerEntry.class);
        entryList.add(triggerEntry);
    }

    private List<Map<String, Object>> buildCreateTriggerEntries() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put(FMT0, "%createTriggerEntries%");
        map.put(CODE0, MESSAGE_CODE_IN_INFO_RANGE);
        map.put("createTriggerEntries", INFO_MESSAGE);
        list.add(map);
        return list;
    }

    /**
     * Test createTriggerEntries(entryList)
     */
    public class TestCreateTriggerEntries {
        private final String[] createCmdArguments = {"-i"};
        /**
         * Rule for expected exception verification
         */
        @Rule
        public ExpectedException thrown = ExpectedException.none();

        /**
         * Runs before every test.
         */
        @SuppressWarnings("unchecked")
        @Before
        public void beforeEach() throws Exception {
            resultMaps = buildCreateTriggerEntries();
            when(server.execMapCmdList(eq(TRIGGERS.toString()), eq(createCmdArguments), anyMap()))
                    .thenReturn(resultMaps);
        }

        /**
         * Expected throws <code>NullPointerException</code> when entryList is null
         *
         * @throws Exception
         */
        @Test
        public void shouldThrowsNullPointerExceptionWhenEntryListIsNull() throws Exception {
            thrown.expect(NullPointerException.class);
            //given
            entryList = null;
            //then
            triggersDelegator.createTriggerEntries(entryList);
        }

        /**
         * Expected return non blank create trigger entries
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnNonBlankCreatedTriggerEntries() throws Exception {
            //when
            String triggerEntries = triggersDelegator.createTriggerEntries(entryList);
            //then
            assertThat(triggerEntries, is(INFO_MESSAGE));
        }
    }

    /**
     * Test getTriggerEntries()
     */
    public class TestGetTriggerEntries {
        private final String[] getCmdArguments = {"-o"};

        /**
         * Rule for expected exception verification
         */
        @Rule
        public ExpectedException thrown = ExpectedException.none();

        /**
         * Runs before every test.
         */
        @SuppressWarnings("unchecked")
        @Before
        public void beforeEach() throws Exception {
            when(server.execMapCmdList(eq(TRIGGERS.toString()), eq(getCmdArguments), eq(null)))
                    .thenReturn(resultMaps);
        }

        /**
         * Expected return empty trigger entries when command result maps is null
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnEmptyTriggerEntriesWhenResultMapsIsNull() throws Exception {
            shouldReturnEmptyTriggerEntries(new UnitTestGiven() {
                @Override
                public void given() throws P4JavaException {
                    when(server.execMapCmdList(
                            eq(TRIGGERS.toString()),
                            eq(getCmdArguments),
                            eq(null))).thenReturn(null);
                }
            });
        }

        /**
         * Expected return empty trigger entries when command result maps is empty list
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnEmptyTriggerEntriesWhenResultMapsIsEmptyList() throws Exception {
            shouldReturnEmptyTriggerEntries(new UnitTestGiven() {
                @Override
                public void given() throws P4JavaException {
                    resultMaps = newArrayList();
                    when(server.execMapCmdList(
                            eq(TRIGGERS.toString()),
                            eq(getCmdArguments),
                            eq(null))).thenReturn(resultMaps);
                }
            });
        }

        /**
         * Expected return empty trigger entries
         * when command result maps isn't contains trigger key with index
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnEmptyTriggerEntriesWhenResultMapIsNotContainsTriggerKeyWithIndex()
                throws Exception {

            shouldReturnEmptyTriggerEntries(new UnitTestGiven() {
                @Override
                public void given() throws P4JavaException {
                    when(resultMap.get(anyString())).thenReturn(EMPTY);
                    when(resultMap2.get(anyString())).thenReturn(EMPTY);
                }
            });
        }

        private void shouldReturnEmptyTriggerEntries(UnitTestGiven unitTestGiven) throws Exception {
            //given
            unitTestGiven.given();
            //when
            List<ITriggerEntry> triggerEntries = triggersDelegator.getTriggerEntries();
            //then
            assertThat(triggerEntries.size(), is(0));
        }

        /**
         * Expected return non empty trigger entries
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnNonEmptyTriggerEntries() throws Exception {
            //given
            when(resultMap.get(TRIGGERS_KEY + 0)).thenReturn("my entry value 0");
            when(resultMap.get(TRIGGERS_KEY + 1)).thenReturn("my entry value 1");
            givenNoHandleErrorMap(resultMap);
            when(resultMap2.get(TRIGGERS_KEY + 0)).thenReturn("my entry2 value 0");
            when(resultMap2.get(TRIGGERS_KEY + 1)).thenReturn("my entry2 value 1");
            when(resultMap2.get(TRIGGERS_KEY + 2)).thenReturn("my entry2 value 2");
            givenNoHandleErrorMap(resultMap2);
            //when
            List<ITriggerEntry> triggerEntries = triggersDelegator.getTriggerEntries();
            //then
            assertThat(triggerEntries.size(), is(5));
            assertThat(triggerEntries.get(1).getOrder(), is(1));
            assertThat(triggerEntries.get(4).getOrder(), is(2));
        }

        private void givenNoHandleErrorMap(Map<String, Object> map) {
            when(map.get(E_FAILED)).thenReturn(EMPTY);
            when(map.get(CODE0)).thenReturn(MESSAGE_CODE_NOT_IN_INFO_RANGE);
        }

        /**
         * Expected throws <code>AccessException</code> when inner method call throws it
         *
         * @throws Exception
         */
        @Test
        public void shouldThrowsExceptionWhenInnerMethodCallThrowsIt() throws Exception {
            thrown.expect(AccessException.class);
            //given
            doThrow(AccessException.class).when(server).execMapCmdList(
                    eq(TRIGGERS.toString()),
                    eq(getCmdArguments),
                    eq(null));
            //then
            triggersDelegator.getTriggerEntries();
        }
    }

    /**
     * Test updateTriggerEntries()
     */
    public class TestUpdateTriggerEntries {
        private final String[] updateCmdArguments = {"-i"};

        /**
         * Runs before every test.
         */
        @SuppressWarnings("unchecked")
        @Before
        public void beforeEach() throws Exception {
            resultMaps = buildCreateTriggerEntries();
            when(server.execMapCmdList(eq(TRIGGERS.toString()), eq(updateCmdArguments), anyMap()))
                    .thenReturn(resultMaps);
        }

        /**
         * Expected return non blank updated trigger entries
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnNonBlankUpdatesTriggerEntries() throws Exception {
            //when
            String updateTriggerEntries = triggersDelegator.updateTriggerEntries(entryList);
            //then
            assertThat(updateTriggerEntries, is(INFO_MESSAGE));
        }
    }

    /**
     * Test getTriggersTable()
     */
    public class TestGetTriggersTable {
        private final String[] getTableCmdArguments = {"-o"};
        private InputStream mockInputStream = mock(InputStream.class);

        /**
         * Runs before every test.
         */
        @SuppressWarnings("unchecked")
        @Before
        public void beforeEach() throws Exception {
            resultMaps = buildCreateTriggerEntries();
            when(server.execStreamCmd(eq(TRIGGERS.toString()), eq(getTableCmdArguments)))
                    .thenReturn(mockInputStream);
        }

        /**
         * Expected return non null <code>InputStream</code>
         *
         * @throws Exception
         */
        @Test
        public void shouldReturnNonNullInputStream() throws Exception {
            //when
            InputStream inputStream = triggersDelegator.getTriggersTable();
            //then
            assertThat(inputStream, is(mockInputStream));
        }
    }
}