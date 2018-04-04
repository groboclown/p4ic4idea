package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.UNLOAD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.UnloadOptions;

/**
 * @author Sean Shou
 * @since 5/10/2016
 */
public class UnloadDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private static final String[] CMD_OPTIONS = {"-f", "-a", "-lp4_java_test"};
    private static final String INFO_MESSAGE = "Unload success!!";
    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private UnloadDelegator unloadDelegator;
    private UnloadOptions opts;

    @Before
    public void beforeEach() throws Exception {
        server = mock(Server.class);
        unloadDelegator = new UnloadDelegator(server);
        List<Map<String, Object>> resultMaps = buildUnloadItems();

        opts = new UnloadOptions(CMD_OPTIONS);
        when(server.execMapCmdList(eq(UNLOAD.toString()), eq(CMD_OPTIONS), eq(null)))
                .thenReturn(resultMaps);
    }

    /**
     * Expected throws exception when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsExceptionWhenInnerMethodCallThrowsIt() throws Exception {
        thrown.expect(P4JavaException.class);

        //given
        doThrow(P4JavaException.class).when(server).execMapCmdList(
                eq(UNLOAD.toString()),
                eq(CMD_OPTIONS),
                eq(null));
        //when
        unloadDelegator.unload(opts);
    }

    /**
     * Expected return non blank unload item
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonBlankUnloadItem() throws Exception {
        //when
        String unload = unloadDelegator.unload(opts);
        //then
        assertThat(unload, is(INFO_MESSAGE));
    }

    private List<Map<String, Object>> buildUnloadItems() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put(FMT0, "%unload%");
        map.put(CODE0, MESSAGE_CODE_IN_INFO_RANGE);
        map.put("unload", INFO_MESSAGE);
        list.add(map);
        return list;
    }
}