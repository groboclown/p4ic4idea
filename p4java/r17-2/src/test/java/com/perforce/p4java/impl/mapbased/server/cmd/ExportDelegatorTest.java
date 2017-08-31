package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FUNCTION;
import static com.perforce.p4java.server.CmdSpec.EXPORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.expectThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Executable;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.ExportRecordsOptions;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.tests.UnitTestGivenThatWillThrowException;

/**
 * @author Sean Shou
 * @since 27/09/2016
 */
@RunWith(JUnitPlatform.class)
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
    @BeforeEach
    public void beforeEach() {
        server = mock(Server.class);
        exportDelegator = new ExportDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = newArrayList(resultMap);

        mockFileSpecs = newArrayList();
        mockFileSpec = mock(IFileSpec.class);
        mockFileSpecs.add(mockFileSpec);

        mockOpts = mock(ExportRecordsOptions.class);
        mockCallback = mock(IStreamingCallback.class);
    }

    /**
     * Test get export records by useJournal, maxRecs, sourceNum, offset, formatJournalPrefix and filter arguments.
     * It expected thrown  <code>ConnectionException</code>
     *
     * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
     */
    @Test
    public void testGetExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownConnectionExceptionThatWasThrownFromInnerMethodCall() throws Exception {
        getExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownExceptions(ConnectionException.class, ConnectionException.class);
    }

    /**
     * Test get export records by useJournal, maxRecs, sourceNum, offset, formatJournalPrefix and filter arguments.
     * It expected thrown  <code>AccessException</code>
     *
     * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
     */
    @Test
    public void testGetExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownRAccessExceptionThatWasThrownFromInnerMethodCall() throws Exception {
        getExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownExceptions(AccessException.class, AccessException.class);
    }

    /**
     * Test get export records by useJournal, maxRecs, sourceNum, offset, formatJournalPrefix and filter arguments.
     * It expected thrown  <code>RequestException</code>
     *
     * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
     */
    @Test
    public void testGetExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownRequestExceptionThatWasThrownFromInnerMethodCall() throws Exception {
        getExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownExceptions(RequestException.class, RequestException.class);
    }


    /**
     * Test get export records by useJournal, maxRecs, sourceNum, offset, formatJournalPrefix and filter arguments.
     * It expected thrown  <code>RequestException</code>, it's wrap from <code>P4JavaException</code>
     *
     * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
     */
    @Test
    public void testGetExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownRequestExceptionWhenInnerMethodCallThrownP4JavaException() throws Exception {
        getExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownExceptions(P4JavaException.class, RequestException.class);
    }

    private void getExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedThrownExceptions(Class<? extends P4JavaException> thrownException, Class<? extends P4JavaException> expectedThrows) throws P4JavaException {
        final int maxRecs = 10;
        final int sourceNum = 2;
        final int offset = 3;
        Executable executable = () -> {
            exportDelegator.getExportRecords(true, maxRecs, sourceNum, offset, true, "prefix", "add");
        };
        UnitTestGivenThatWillThrowException unitTestGiven = (originalException) -> {
            doThrow(originalException).when(server).execMapCmdList(eq(EXPORT.toString()), any(String[].class), any(Map.class));
        };

        testIfGivenExceptionWasThrown(thrownException, expectedThrows, executable, unitTestGiven);
    }

    /**
     * Test get export records by useJournal, maxRecs, sourceNum, offset, formatJournalPrefix and filter arguments.
     * It expected return non-empty export record list.
     *
     * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
     */
    @Test
    public void testGetExportRecordsByUseJournalMaxRecsSourceNumOffsetFormatJournalPrefixAndFilterExpectedReturnNonEmptyList() throws Exception {
        //given
//        doReturn(resultMaps).when(exportDelegator).getExportRecords(any(ExportRecordsOptions.class));
        givenWorkingResultMaps();

        final int maxRecs = 10;
        final int sourceNum = 2;
        final int offset = 3;
        //when
        List<Map<String, Object>> exportRecords = exportDelegator.getExportRecords(true, maxRecs, sourceNum, offset, true, "prefix", "add");
        //then
        assertThat(exportRecords, is(resultMaps));
    }

    private void givenWorkingResultMaps() throws ConnectionException, AccessException, RequestException {
        Map<String, Object> resultMap2 = newHashMap();
        resultMap2.put(FUNCTION, "fileLog");
        resultMap2.put("any other", "test");
        resultMaps.add(resultMap2);
        when(server.execMapCmdList(eq(EXPORT.toString()), any(String[].class), any(Map.class))).thenReturn(resultMaps);
    }

    /**
     * Test get export records by <code>ExportRecordsOptions</code>.
     * It expected return non-empty export record list.
     *
     * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
     */
    @Test
    public void testGetExportRecordsByExportRecordsOptions() throws Exception {
        //given
        when(mockOpts.processFieldRules()).thenReturn(resultMap);
        givenWorkingResultMaps();
        //when
        List<Map<String, Object>> exportRecords = exportDelegator.getExportRecords(mockOpts);
        //then
        assertThat(exportRecords.size(), is(2));
        Map<String, Object> secondResultMap = exportRecords.get(1);
        assertThat(secondResultMap.get("any other"), is("test"));
        assertThat(secondResultMap.containsKey(FUNCTION), is(false));
    }

    /**
     * Test get streaming export records by <code>ExportRecordsOptions</code>.
     * It expected thrown  <code>NullPointerException</code> as command return null result maps.
     *
     * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
     */
    @Test
    public void testGetStreamingExportRecordsExpectedThrownNullPointerExceptionWhenCallBackIsNull() throws Exception {
        //then
        expectThrows(NullPointerException.class, () -> {
            final int key = 10;
            exportDelegator.getStreamingExportRecords(mockOpts, null, key);
        });
    }

    /**
     * Test get streaming export records by <code>ExportRecordsOptions</code>.
     *
     * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
     */
    @Test
    public void testGetStreamingExportRecords() throws Exception {
        //given
        when(mockOpts.processFieldRules()).thenReturn(resultMap);
        when(mockOpts.getSourceNum()).thenReturn(2);
        when(mockOpts.isImmutable()).thenReturn(true);
        String mockOptionsValue = "-jtoken123";
        when(mockOpts.getOptions()).thenReturn(Arrays.asList(mockOptionsValue));
        //when
        final int key = 10;
        exportDelegator.getStreamingExportRecords(mockOpts, mockCallback, key);
        //then
        verify(mockOpts).processFieldRules();
        verify(server).execStreamingMapCommand(eq(EXPORT.toString()), eq(new String[]{mockOptionsValue}), eq(resultMap), eq(mockCallback), eq(key));
    }
}