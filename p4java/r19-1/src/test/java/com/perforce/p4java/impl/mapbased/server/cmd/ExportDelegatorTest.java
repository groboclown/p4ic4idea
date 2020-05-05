package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FUNCTION;
import static com.perforce.p4java.server.CmdSpec.EXPORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.ExportRecordsOptions;
import com.perforce.p4java.server.callback.IStreamingCallback;

public class ExportDelegatorTest extends AbstractP4JavaUnitTest {
    private ExportDelegator exportDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;

    private List<IFileSpec> mockFileSpecs;
    private IFileSpec mockFileSpec;
    private ExportRecordsOptions mockOpts;
    private IStreamingCallback mockCallback;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        exportDelegator = new ExportDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = new ArrayList<Map<String, Object>>();
        resultMaps.add(resultMap);

        mockFileSpecs = new ArrayList<IFileSpec>();
        mockFileSpec = mock(IFileSpec.class);
        mockFileSpecs.add(mockFileSpec);

        mockOpts = mock(ExportRecordsOptions.class);
        mockCallback = mock(IStreamingCallback.class);
    }

    /**
     * Test get export records by useJournal, maxRecs, sourceNum, offset,
     * formatJournalPrefix and filter arguments. It expected thrown
     * <code>ConnectionException</code>
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @SuppressWarnings("unchecked")
    @Test(expected=ConnectionException.class)
    public void testGetExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownConnectionExceptionThatWasThrownFromInnerMethodCall()
            throws Exception {
        final int maxRecs = 10;
        final int sourceNum = 2;
        final int offset = 3;
            doThrow(ConnectionException.class).when(server).execMapCmdList(eq(EXPORT.toString()),
                    any(String[].class), any(Map.class));
        exportDelegator.getExportRecords(true, maxRecs, sourceNum, offset, true, "prefix",
                    "add");
    }

    /**
     * Test get export records by useJournal, maxRecs, sourceNum, offset,
     * formatJournalPrefix and filter arguments. It expected thrown
     * <code>AccessException</code>
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @SuppressWarnings("unchecked")
    @Test(expected=AccessException.class)
    public void testGetExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownRAccessExceptionThatWasThrownFromInnerMethodCall()
            throws Exception {
        final int maxRecs = 10;
        final int sourceNum = 2;
        final int offset = 3;
            doThrow(AccessException.class).when(server).execMapCmdList(eq(EXPORT.toString()),
                    any(String[].class), any(Map.class));
        exportDelegator.getExportRecords(true, maxRecs, sourceNum, offset, true, "prefix",
                    "add");
    }

    /**
     * Test get export records by useJournal, maxRecs, sourceNum, offset,
     * formatJournalPrefix and filter arguments. It expected thrown
     * <code>RequestException</code>
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @SuppressWarnings("unchecked")
    @Test(expected=RequestException.class)
    public void testGetExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownRequestExceptionThatWasThrownFromInnerMethodCall()
            throws Exception {
        final int maxRecs = 10;
        final int sourceNum = 2;
        final int offset = 3;
            doThrow(RequestException.class).when(server).execMapCmdList(eq(EXPORT.toString()),
                    any(String[].class), any(Map.class));
        exportDelegator.getExportRecords(true, maxRecs, sourceNum, offset, true, "prefix",
                    "add");
    }

    /**
     * Test get export records by useJournal, maxRecs, sourceNum, offset,
     * formatJournalPrefix and filter arguments. It expected thrown
     * <code>RequestException</code>, it's wrap from
     * <code>P4JavaException</code>
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @SuppressWarnings("unchecked")
    @Test(expected=RequestException.class)
    public void testGetExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownRequestExceptionWhenInnerMethodCallThrownP4JavaException()
            throws Exception {
        final int maxRecs = 10;
        final int sourceNum = 2;
        final int offset = 3;
            doThrow(P4JavaException.class).when(server).execMapCmdList(eq(EXPORT.toString()),
                    any(String[].class), any(Map.class));
        exportDelegator.getExportRecords(true, maxRecs, sourceNum, offset, true, "prefix",
                    "add");
    }


    /**
     * Test get export records by useJournal, maxRecs, sourceNum, offset,
     * formatJournalPrefix and filter arguments. It expected return non-empty
     * export record list.
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @Test
    public void testGetExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedReturnNonEmptyList()
            throws Exception {
        // given
        // doReturn(resultMaps).when(exportDelegator).getExportRecords(any(ExportRecordsOptions.class));
        givenWorkingResultMaps();

        final int maxRecs = 10;
        final int sourceNum = 2;
        final int offset = 3;
        // when
        List<Map<String, Object>> exportRecords = exportDelegator.getExportRecords(true, maxRecs,
                sourceNum, offset, true, "prefix", "add");
        // then
        assertThat(exportRecords, is(resultMaps));
    }

    @SuppressWarnings("unchecked")
    private void givenWorkingResultMaps()
            throws ConnectionException, AccessException, RequestException {
        Map<String, Object> resultMap2 = new HashMap<String, Object>();
        resultMap2.put(FUNCTION, "fileLog");
        resultMap2.put("any other", "test");
        resultMaps.add(resultMap2);
        when(server.execMapCmdList(eq(EXPORT.toString()), any(String[].class), any(Map.class)))
                .thenReturn(resultMaps);
    }

    /**
     * Test get export records by <code>ExportRecordsOptions</code>. It expected
     * return non-empty export record list.
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @Test
    public void testGetExportRecordsByExportRecordsOptions() throws Exception {
        // given
        when(mockOpts.processFieldRules()).thenReturn(resultMap);
        givenWorkingResultMaps();
        // when
        List<Map<String, Object>> exportRecords = exportDelegator.getExportRecords(mockOpts);
        // then
        assertThat(exportRecords.size(), is(2));
        Map<String, Object> secondResultMap = exportRecords.get(1);
        assertEquals("test", secondResultMap.get("any other"));
        assertFalse(secondResultMap.containsKey(FUNCTION));
    }

    /**
     * Test get streaming export records by <code>ExportRecordsOptions</code>.
     * It expected thrown <code>NullPointerException</code> as command return
     * null result maps.
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @Test(expected = NullPointerException.class)
    public void testGetStreamingExportRecordsExpectedThrownNullPointerExceptionWhenCallBackIsNull()
            throws Exception {
        // then

        final int key = 10;
        exportDelegator.getStreamingExportRecords(mockOpts, null, key);
    }

    /**
     * Test get streaming export records by <code>ExportRecordsOptions</code>.
     *
     * @throws Exception
     *             if the <code>Exception</code> is thrown, it's mean an
     *             unexpected error occurs
     */
    @Test
    public void testGetStreamingExportRecords() throws Exception {
        // given
        when(mockOpts.processFieldRules()).thenReturn(resultMap);
        when(mockOpts.getSourceNum()).thenReturn(2);
        when(mockOpts.isImmutable()).thenReturn(true);
        String mockOptionsValue = "-jtoken123";
        when(mockOpts.getOptions()).thenReturn(Arrays.asList(mockOptionsValue));
        // when
        final int key = 10;
        exportDelegator.getStreamingExportRecords(mockOpts, mockCallback, key);
        // then
        verify(mockOpts).processFieldRules();
        verify(server).execStreamingMapCommand(eq(EXPORT.toString()),
                eq(new String[] { mockOptionsValue }), eq(resultMap), eq(mockCallback), eq(key));
    }
}