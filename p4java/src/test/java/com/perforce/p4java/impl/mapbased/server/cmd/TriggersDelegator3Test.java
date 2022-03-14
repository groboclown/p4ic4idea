package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.TRIGGERS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IOptionsServer;
import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.admin.ITriggerEntry;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.impl.mapbased.server.Server;

/**
 * @author Sean Shou
 * @since 6/10/2016
 */
public class TriggersDelegator3Test {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private static final String MESSAGE_CODE_NOT_IN_INFO_RANGE = "168435456";
    private static final String INFO_MESSAGE = "create triggers entries";

    private TriggersDelegator triggersDelegator;
    private Map<String, Object> resultMap;
    private Map<String, Object> resultMap2;
    private List<Map<String, Object>> resultMaps;

    private List<ITriggerEntry> entryList;
    private IOptionsServer server;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach()
            throws AccessException, RequestException, ConnectionException {
        server = mock(Server.class);
        triggersDelegator = new TriggersDelegator(server);
        resultMap = mock(Map.class);
        resultMap2 = mock(Map.class);
        resultMaps = List.of(resultMap, resultMap2);

        entryList = new ArrayList<>();
        ITriggerEntry triggerEntry = mock(ITriggerEntry.class);
        entryList.add(triggerEntry);

        resultMaps = buildCreateTriggerEntries();
        when(server.execMapCmdList(eq(TRIGGERS.toString()), eq(updateCmdArguments), anyMap()))
                .thenReturn(resultMaps);
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

    private final String[] updateCmdArguments = {"-i"};

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