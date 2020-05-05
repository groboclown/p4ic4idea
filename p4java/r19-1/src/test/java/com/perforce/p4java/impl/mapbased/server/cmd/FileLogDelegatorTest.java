package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.core.file.FileSpecOpStatus.ERROR;
import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.FILELOG;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;

public class FileLogDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_ERROR_RANGE = "968435456";
    private FileLogDelegator fileLogDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;

    private List<IFileSpec> mockFileSpecs;
    private IFileSpec mockFileSpec;
    private GetRevisionHistoryOptions mockOpts;
    private Map<IFileSpec, List<IFileRevisionData>> mockFileRevisionDataMap;
    private List<IFileRevisionData> mockFileRevisionData;
    /* max revs number */
    private static final int MOCK_MAX_REVS = 11;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        // fileLogDelegator = spy(new FileLogDelegator(server));
        fileLogDelegator = new FileLogDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = new ArrayList<Map<String, Object>>();
        resultMaps.add(resultMap);

        mockFileSpecs = new ArrayList<IFileSpec>();
        mockFileSpec = mock(IFileSpec.class);
        mockFileSpecs.add(mockFileSpec);

        mockOpts = mock(GetRevisionHistoryOptions.class);

        mockFileRevisionData = mock(List.class);
        mockFileRevisionDataMap = new HashMap<IFileSpec, List<IFileRevisionData>>();
        mockFileRevisionDataMap.put(mockFileSpec, mockFileRevisionData);
    }

    /**
     * Test get revision history by fileSpec, maxRevs, contentHistory,
     * includeInherited, longOutput and truncatedLongOutput arguments. It
     * expected thrown <code>ConnectionException</code>
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @Test(expected = ConnectionException.class)
    public void testGetRevisionHistoryByFileSpecsMaxRevsContentHistoryIncludeInheritedLongOutputAndTruncatedLongOutputExpectedConnectionException()
            throws Exception {
        doThrow(ConnectionException.class).when(server).execMapCmdList(eq(FILELOG.toString()),
                any(String[].class), eq((Map<String, Object>) null));
        fileLogDelegator.getRevisionHistory(mockFileSpecs, MOCK_MAX_REVS, false, false, false,
                false);
    }

    /**
     * Test get revision history by fileSpec, maxRevs, contentHistory,
     * includeInherited, longOutput and truncatedLongOutput arguments. It
     * expected thrown <code>AccessException</code>
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @Test(expected = AccessException.class)
    public void testGetRevisionHistoryByFileSpecsMaxRevsContentHistoryIncludeInheritedLongOutputAndTruncatedLongOutputExpectedAccessException()
            throws Exception {
        doThrow(AccessException.class).when(server).execMapCmdList(eq(FILELOG.toString()),
                any(String[].class), eq((Map<String, Object>) null));
        fileLogDelegator.getRevisionHistory(mockFileSpecs, MOCK_MAX_REVS, false, false, false,
                false);
    }

    /**
     * Test get revision history by fileSpec, maxRevs, contentHistory,
     * includeInherited, longOutput and truncatedLongOutput arguments. It should
     * return empty <code>Map</code> as inner method call thrown
     * <code>P4JavaException</code>.
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @Test
    public void testGetRevisionHistoryByFileSpecsMaxRevsContentHistoryIncludeInheritedLongOutputAndTruncatedLongOutputExpectedEmptyMapAsInnerMethodCallThrownP4JavaException()
            throws Exception {
        // given
        doThrow(P4JavaException.class).when(server).execMapCmdList(eq(FILELOG.toString()),
                any(String[].class), eq((Map<String, Object>) null));
        // when
        Map<IFileSpec, List<IFileRevisionData>> revisionHistory = fileLogDelegator
                .getRevisionHistory(mockFileSpecs, MOCK_MAX_REVS, false, false, false, false);
        // then
        assertThat(revisionHistory.size(), is(0));
    }

    /**
     * Test get revision history by fileSpec, maxRevs, contentHistory,
     * includeInherited, longOutput and truncatedLongOutput arguments. It should
     * return non-empty <code>Map</code>.
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @Test
    public void testGetRevisionHistoryByFileSpecsMaxRevsContentHistoryIncludeInheritedLongOutputAndTruncatedLongOutputExpectedReturnNonEmptyMap()
            throws Exception {
        // given
        String depotPath1 = "//depot/fileLogTest/test1.txt";
        when(resultMap.get(DEPOT_FILE)).thenReturn(depotPath1);
        when(resultMap.get("rev0")).thenReturn("11");
        when(resultMap.get("time0")).thenReturn("20160923");
        when(resultMap.get("change0")).thenReturn("123");
        when(resultMap.get("file0,0")).thenReturn(EMPTY);
        when(resultMap.get("rev1")).thenReturn("12");
        when(resultMap.get("time1")).thenReturn("20160923");
        when(resultMap.get("change1")).thenReturn("123");
        when(resultMap.get("file1,0")).thenReturn(EMPTY);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap2 = mock(Map.class);
        String depotPath2 = "//depot/fileLogTest/test2.txt";
        when(resultMap2.get(DEPOT_FILE)).thenReturn(depotPath2);
        when(resultMap2.get(CODE0)).thenReturn(MESSAGE_CODE_IN_ERROR_RANGE);
        when(resultMap2.get(FMT0)).thenReturn("Error other than Auth fail message");
        resultMaps.add(resultMap2);

        doReturn(resultMaps).when(server).execMapCmdList(eq(FILELOG.toString()),
                eq(new String[] { "-m" + MOCK_MAX_REVS }), eq((Map<String, Object>) null));
        // when
        Map<IFileSpec, List<IFileRevisionData>> revisionHistory = fileLogDelegator
                .getRevisionHistory(mockFileSpecs, MOCK_MAX_REVS, false, false, false, false);
        // then
        assertThat(revisionHistory.size(), is(2));
        for (Map.Entry<IFileSpec, List<IFileRevisionData>> entry : revisionHistory.entrySet()) {
            IFileSpec fileSpec = entry.getKey();
            if (depotPath1.equals(fileSpec.getDepotPathString())) {
                assertThat(fileSpec.getOpStatus(), is(VALID));
                assertThat(entry.getValue().size(), is(2));
            }

            if (depotPath2.equals(fileSpec.getDepotPathString())) {
                assertThat(fileSpec.getOpStatus(), is(ERROR));
                assertThat(entry.getValue(), nullValue());
            }
        }
    }

    /**
     * Test get revision history by fileSpec and
     * <code>GetRevisionHistoryOptions</code>. It should return non-empty
     * <code>List</code>.
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @Test
    public void testGetRevisionHistoryByFileSpecsAndGetRevisionHistoryOptionsExpectedReturnEmptyMapAsResultMapsIsNull()
            throws Exception {
        // given
        when(server.execMapCmdList(eq(FILELOG.toString()), any(String[].class),
                eq((Map<String, Object>) null))).thenReturn(null);
        // when
        Map<IFileSpec, List<IFileRevisionData>> revisionHistory = fileLogDelegator
                .getRevisionHistory(mockFileSpecs, mockOpts);
        // then
        assertThat(revisionHistory.size(), is(0));
    }

    /**
     * Test get revision history by fileSpec and
     * <code>GetRevisionHistoryOptions</code>. It should return non-empty
     * <code>List</code> but don't have revision history as some error message
     * return.
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @Test
    public void testGetRevisionHistoryByFileSpecsAndGetRevisionHistoryOptionsExpectedReturnNonEmptyListButDontHaveRevisionHistoryAsSomeError()
            throws Exception {
        // given
        doReturn(resultMaps).when(server).execMapCmdList(eq(FILELOG.toString()),
                any(String[].class), eq((Map<String, Object>) null));

        givenErrorMessageCode();
        // when
        Map<IFileSpec, List<IFileRevisionData>> revisionHistory = fileLogDelegator
                .getRevisionHistory(mockFileSpecs, mockOpts);
        // then
        assertThat(revisionHistory.size(), is(1));
        assertNull(revisionHistory.entrySet().iterator().next().getValue());
    }

    private void givenErrorMessageCode() {
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_ERROR_RANGE);
        when(resultMap.get(FMT0)).thenReturn("Error other than Auth fail message");
    }

    /**
     * Test get revision history by fileSpec and
     * <code>GetRevisionHistoryOptions</code>. It should return non-empty
     * <code>List</code>.
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @Test
    public void testGetRevisionHistoryByFileSpecsAndGetRevisionHistoryOptionsExpectedReturnNonEmptyListAndNonEmptyRevisionDataList()
            throws Exception {
        // given
        when(server.execMapCmdList(eq(FILELOG.toString()), any(String[].class),
                eq((Map<String, Object>) null))).thenReturn(resultMaps);
        when(resultMap.get("rev0")).thenReturn(MOCK_MAX_REVS);
        when(resultMap.get("time0")).thenReturn("20160923");
        when(resultMap.get("change0")).thenReturn("123");
        when(resultMap.get("file0,0")).thenReturn(EMPTY);
        // when
        Map<IFileSpec, List<IFileRevisionData>> revisionHistory = fileLogDelegator
                .getRevisionHistory(mockFileSpecs, mockOpts);
        // then
        assertThat(revisionHistory.size(), is(1));
        Collection<List<IFileRevisionData>> values = revisionHistory.values();
        List<IFileRevisionData> fileRevisionDataList = new ArrayList<IFileRevisionData>();
        for (List<IFileRevisionData> revs : values) {
            fileRevisionDataList.addAll(revs);
        }
        assertNotNull(fileRevisionDataList.get(0));
    }
}