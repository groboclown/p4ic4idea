package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.RELOAD;
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
import com.perforce.p4java.option.server.ReloadOptions;

/**
 * @author Sean Shou
 * @since 5/10/2016
 */
public class ReloadDelegatorTest extends AbstractP4JavaUnitTest {
    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private static final String CLIENT_NAME = "p4javaTest";
    private static final String[] CMD_OPTIONS = {"-f", "-c" + CLIENT_NAME};
    private ReloadDelegator reloadDelegator;
    private List<Map<String, Object>> resultMaps;

    private ReloadOptions opts = new ReloadOptions(CMD_OPTIONS);

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        reloadDelegator = new ReloadDelegator(server);
    }

    /**
     * Expected throws <code>P4JavaException</code> when inner method call throws it.
     *
     * @throws Exception
     */
    @Test
    public void testReloadShouldThrowsExceptionWhenInnerMethodCallThrowsIt() throws Exception {
        thrown.expect(P4JavaException.class);

        doThrow(P4JavaException.class).when(server).execMapCmdList(eq(RELOAD.toString()), eq(CMD_OPTIONS), eq(null));
        reloadDelegator.reload(opts);
    }

    /**
     * Expected return not blank reload 'client' or 'lable' or 'task stream'
     *
     * @throws Exception
     */
    @Test
    public void testReloadShouldReturnNonBlankClientOrLabelOrTaskStreamName() throws Exception {
        //given
        resultMaps = buildReloadClientList();
        when(server.execMapCmdList(eq(RELOAD.toString()), eq(CMD_OPTIONS), eq(null))).thenReturn(resultMaps);
        //when
        String reloadClient = reloadDelegator.reload(opts);
        //then
        assertThat(reloadClient, is(CLIENT_NAME));
    }

    private List<Map<String, Object>> buildReloadClientList() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put(FMT0, "%client%");
        map.put(CODE0, MESSAGE_CODE_IN_INFO_RANGE);
        map.put("client", CLIENT_NAME);
        list.add(map);
        return list;
    }
}