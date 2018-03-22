package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.PROTECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
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

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.impl.mapbased.server.Server;

/**
 * @author Sean Shou
 * @since 5/10/2016
 */
public class ProtectDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private static final String[] CREATE_CMD_ARGUMENTS = {"-i"};
    private static final String[] GET_CMD_ARGUMENTS = {"-o"};
    private final String infoMessage = "info message!";
    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private ProtectDelegator protectDelegator;
    private List<Map<String, Object>> resultMaps;

    private List<IProtectionEntry> entryList;

    @Before
    public void beforeEach() {
        server = mock(Server.class);
        protectDelegator = new ProtectDelegator(server);
        entryList = newArrayList();
    }

    @Test
    public void testCreateProtectionEntriesShouldThrownNullPointerExceptionWhenEntryListIsNull()
            throws Exception {

        thrown.expect(NullPointerException.class);

        //when
        protectDelegator.createProtectionEntries(null);
    }

    @Test
    public void testCreateProtectionEntriesShouldReturnNonBlankString() throws Exception {
        //given
        resultMaps = buildProtectionEntries();
        when(server.execMapCmdList(eq(PROTECT.toString()), eq(CREATE_CMD_ARGUMENTS), anyMap()))
                .thenReturn(resultMaps);
        //when
        String protectionEntries = protectDelegator.createProtectionEntries(entryList);
        //then
        assertThat(protectionEntries, is(infoMessage));
    }

    @Test
    public void updateProtectionEntries() throws Exception {
        //given
        resultMaps = buildProtectionEntries();
        when(server.execMapCmdList(eq(PROTECT.toString()), eq(CREATE_CMD_ARGUMENTS), anyMap()))
                .thenReturn(resultMaps);
        //when
        String protectionEntries = protectDelegator.updateProtectionEntries(entryList);
        //then
        assertThat(protectionEntries, is(infoMessage));
    }

    @Test
    public void getProtectionsTable() throws Exception {
        //given
        InputStream mockedInputStream = mock(InputStream.class);
        when(server.execStreamCmd(eq(PROTECT.toString()), eq(GET_CMD_ARGUMENTS)))
                .thenReturn(mockedInputStream);
        //when
        InputStream protectionsTable = protectDelegator.getProtectionsTable();
        //then
        assertThat(protectionsTable, is(mockedInputStream));
    }

    private List<Map<String, Object>> buildProtectionEntries() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put(FMT0, "%createProtectionEntries%");
        map.put(CODE0, MESSAGE_CODE_IN_INFO_RANGE);
        map.put("createProtectionEntries", infoMessage);
        list.add(map);
        return list;
    }
}