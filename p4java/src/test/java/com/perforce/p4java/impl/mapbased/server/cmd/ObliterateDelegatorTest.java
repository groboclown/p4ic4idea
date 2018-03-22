package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static com.perforce.p4java.exception.MessageSeverityCode.E_INFO;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CLIENT_REC_DELETED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.INTEGRATION_REC_ADDED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.INTEGRATION_REC_DELETED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.LABEL_REC_DELETED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.PURGE_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.PURGE_REV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REPORT_ONLY;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REVISION_REC_DELETED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.WORKING_REC_DELETED;
import static com.perforce.p4java.server.CmdSpec.OBLITERATE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IObliterateResult;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.ObliterateFilesOptions;
import com.perforce.p4java.tests.UnitTestGiven;
import com.perforce.p4java.tests.UnitTestThen;

/**
 * @author Sean Shou
 * @since 4/10/2016
 */
public class ObliterateDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private static final String EXECUTE_OBLITERATE = "-y";
    private static final String FILE_DEPOT_PATH = "//depot/dev/test.txt";
    private static final String[] CMD_ARGUMENTS = {EXECUTE_OBLITERATE, FILE_DEPOT_PATH};
    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private ObliterateDelegator obliterateDelegator;
    private Map<String, Object> resultMap;
    private Map<String, Object> resultMap2;
    private List<Map<String, Object>> resultMaps;
    private List<IFileSpec> fileSpecs;
    private ObliterateFilesOptions opts;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() throws P4JavaException {
        server = mock(Server.class);
        obliterateDelegator = new ObliterateDelegator(server);

        resultMap = mock(Map.class);
        resultMap2 = mock(Map.class);
        resultMaps = newArrayList(resultMap, resultMap2);

        fileSpecs = newArrayList();
        IFileSpec fileSpec = mock(IFileSpec.class);
        when(fileSpec.getOpStatus()).thenReturn(VALID);
        when(fileSpec.getAnnotatedPreferredPathString()).thenReturn(FILE_DEPOT_PATH);

        fileSpecs.add(fileSpec);

        opts = new ObliterateFilesOptions(EXECUTE_OBLITERATE);
    }

    /**
     * Expected throws <code>NullPointerException</code> when fileSpecs is null.
     *
     * @throws Exception
     */
    @Test
    public void shouldThrownNullPointerExceptionWhenFileSpecsIsNull() throws Exception {
        thrown.expect(NullPointerException.class);

        fileSpecs = null;
        obliterateDelegator.obliterateFiles(fileSpecs, opts);
    }

    /**
     * Expected return empty obliterate files when command return null result maps.
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnEmptyListWhenResultMapsIsNull() throws Exception {
        expectedReturnEmptyList(new UnitTestGiven() {
            @Override
            public void given() throws P4JavaException {
                when(server.execMapCmdList(
                        eq(OBLITERATE.toString()),
                        eq(CMD_ARGUMENTS),
                        eq(null))).thenReturn(null);
            }
        });
    }

    /**
     * Expected return empty obliterate files when command return non-null result maps,
     * but it's only include purge files
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnEmptyListWhenAllResultMapsArePurgeFile() throws Exception {
        expectedReturnEmptyList(new UnitTestGiven() {
            @Override
            public void given() throws P4JavaException {
                when(resultMap.containsKey(PURGE_FILE)).thenReturn(true);
                when(resultMap2.containsKey(PURGE_FILE)).thenReturn(true);
            }
        });
    }

    /**
     * Expected return empty obliterate files when command return non-null result maps,
     * but it's not include any non purge files or non deleted revision record files
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnEmptyListWhenAllResultMapIsNotPurgeFileAndIsNotContainsDeletedRevisionRecord() throws Exception {
        expectedReturnEmptyList(
                new UnitTestGiven() {
                    @Override
                    public void given() throws P4JavaException {
                        when(resultMap.containsKey(PURGE_FILE)).thenReturn(false);
                        when(resultMap2.containsKey(PURGE_FILE)).thenReturn(false);
                        when(resultMap.containsKey(REVISION_REC_DELETED)).thenReturn(false);
                        when(resultMap2.containsKey(REVISION_REC_DELETED)).thenReturn(false);
                    }
                });

    }

    private void expectedReturnEmptyList(UnitTestGiven unitTestGiven) throws Exception {
        //given
        unitTestGiven.given();
        when(server.execMapCmdList(eq(OBLITERATE.toString()), eq(CMD_ARGUMENTS), eq(null))).thenReturn(resultMaps);
        //when
        List<IObliterateResult> obliterateResults = obliterateDelegator.obliterateFiles(fileSpecs, opts);
        //then
        assertThat(obliterateResults.size(), is(0));
    }

    /**
     * Expectet return 'deleted revison records'
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnOneDeletedRevisionRecord() throws Exception {
        shouldReturnNonEmptyList(
                new UnitTestGiven() {
                    @Override
                    public void given() throws P4JavaException {
                        when(resultMap.containsKey(PURGE_FILE)).thenReturn(true);
                        when(resultMap2.containsKey(PURGE_FILE)).thenReturn(false);
                        when(resultMap.containsKey(REVISION_REC_DELETED)).thenReturn(false);
                        when(resultMap2.containsKey(REVISION_REC_DELETED)).thenReturn(true);
                        mockingPureFileMap(resultMap);
                        mockingRevisionRecDeletedMap(resultMap2);
                    }
                },
                new UnitTestThen<List<IObliterateResult>>() {
                    @Override
                    public void then(List<IObliterateResult> resultList) throws P4JavaException {
                        assertThat(resultList.size(), is(1));
                        verify(resultMap).get(PURGE_FILE);
                        verify(resultMap2).get(INTEGRATION_REC_ADDED);
                        IObliterateResult obliterateResult = resultList.get(0);
                        assertThat(obliterateResult.getLabelRecDeleted(), is(2));
                        assertThat(obliterateResult.isReportOnly(), is(true));
                    }
                });
    }

    /**
     * Expected return one 'deleted reviison record' and one 'info or error obliterate'
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnOneDeletedRevisionRecordAndOneInfoOrErrorObliterates() throws Exception {
        shouldReturnNonEmptyList(
                new UnitTestGiven() {
                    @Override
                    public void given() throws P4JavaException {
                        when(resultMap.get(E_INFO)).thenReturn(EMPTY);
                        when(resultMap.containsKey(PURGE_FILE)).thenReturn(false);
                        when(resultMap.containsKey(REVISION_REC_DELETED)).thenReturn(true);
                        mockingRevisionRecDeletedMap(resultMap);

                        when(resultMap2.get(E_INFO)).thenReturn("not blank");
                        when(resultMap2.get(CODE0)).thenReturn(MESSAGE_CODE_IN_INFO_RANGE);
                        when(resultMap2.get(FMT0)).thenReturn(FILE_DEPOT_PATH);
                    }
                },
                new UnitTestThen<List<IObliterateResult>>() {
                    @Override
                    public void then(List<IObliterateResult> resultList) throws P4JavaException {
                        assertThat(resultList.size(), is(2));
                        IObliterateResult revisionRecDeleteObliterateResult = resultList.get(0);
                        assertThat(revisionRecDeleteObliterateResult.getLabelRecDeleted(), is(2));
                        assertThat(revisionRecDeleteObliterateResult.isReportOnly(), is(false));

                        IObliterateResult infoObliterateResult = resultList.get(1);
                        assertThat(infoObliterateResult.getLabelRecDeleted(), is(0));
                        assertThat(infoObliterateResult.getFileSpecs().size(), is(1));
                    }
                });
    }


    private void shouldReturnNonEmptyList(
            UnitTestGiven unitTestGiven,
            UnitTestThen<List<IObliterateResult>> unitTestThen) throws Exception {
        //given
        when(server.execMapCmdList(eq(OBLITERATE.toString()), eq(CMD_ARGUMENTS), eq(null)))
                .thenReturn(resultMaps);
        unitTestGiven.given();
        //when
        List<IObliterateResult> obliterateResults = obliterateDelegator.obliterateFiles(
                fileSpecs,
                opts);
        //then
        assertThat(obliterateResults.size() > 0, is(true));
        unitTestThen.then(obliterateResults);
    }

    private void mockingRevisionRecDeletedMap(Map<String, Object> map) {
        when(map.containsKey(REPORT_ONLY)).thenReturn(true);
        when(map.get(INTEGRATION_REC_ADDED)).thenReturn(1);
        when(map.get(LABEL_REC_DELETED)).thenReturn(2);
        when(map.get(CLIENT_REC_DELETED)).thenReturn(3);
        when(map.get(INTEGRATION_REC_DELETED)).thenReturn(4);
        when(map.get(WORKING_REC_DELETED)).thenReturn(5);
        when(map.get(REVISION_REC_DELETED)).thenReturn(6);
    }

    private void mockingPureFileMap(Map<String, Object> map) {
        when(map.get(PURGE_FILE)).thenReturn("pure file");
        when(map.get(PURGE_REV)).thenReturn(12);
    }
}