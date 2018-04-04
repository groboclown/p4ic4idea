package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.exception.MessageSeverityCode.E_INFO;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DESC;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.server.CmdSpec.VERIFY;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.VerifyFilesOptions;
import com.perforce.p4java.tests.UnitTestGiven;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Sean Shou
 * @since 23/09/2016
 */
public class VerifyDelegatorTest extends AbstractP4JavaUnitTest {
    private static final String MESSAGE_CODE_IN_ERROR_RANGE = "968435456";
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";

    private static final String TEST_FILE_DEPOT_PATH = "//depot/dev/test.txt";
    private static final String[] CMD_OPTIONS = {"-t", "-m20"};
    private static final String[] CMD_ARGUMENTS = ArrayUtils.add(CMD_OPTIONS, TEST_FILE_DEPOT_PATH);

    private VerifyDelegator verifyDelegator;
    private Map<String, Object> resultMap;

    private List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(TEST_FILE_DEPOT_PATH);
    private VerifyFilesOptions opts;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() throws Exception {
        server = mock(Server.class);
        verifyDelegator = new VerifyDelegator(server);

        resultMap = mock(Map.class);
        when(resultMap.get(E_INFO)).thenReturn(EMPTY);

        List<Map<String, Object>> resultMaps = newArrayList(resultMap);
        opts = new VerifyFilesOptions(CMD_OPTIONS);

        when(server.execMapCmdList(eq(VERIFY.toString()), eq(CMD_ARGUMENTS), eq(null)))
                .thenReturn(resultMaps);
    }

    /**
     * Expected return empty verified fileSpecs when no depot file
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnEmptyListWhenNoErrorNoDepotPath()
            throws Exception {

        shouldReturnEmptyList(
                new UnitTestGiven() {
                    @Override
                    public void given() throws P4JavaException {
                        when(resultMap.containsKey(DEPOT_FILE)).thenReturn(false);
                    }
                });
    }

    /**
     * Expected return empty verified fileSpecs when has depot file but is contains desc
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnEmptyListWhenNoErrorHasDepotPathButContainsDescField() throws Exception {
        shouldReturnEmptyList(() -> {
            when(resultMap.containsKey(DEPOT_FILE)).thenReturn(true);
            when(resultMap.containsKey(DESC)).thenReturn(true);
        });
    }

    /**
     * Expected return empty verified fileSpecs when command result maps is null
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnEmptyListWhenResultMapsIsNull() throws Exception {
        shouldReturnEmptyList(
                new UnitTestGiven() {
                    @Override
                    public void given() throws P4JavaException {
                        when(server.execMapCmdList(eq(VERIFY.toString()), eq(CMD_ARGUMENTS), eq(null)))
                                .thenReturn(null);
                    }
                });
    }

    private void shouldReturnEmptyList(UnitTestGiven unitTestGiven) throws Exception {
        //given
        unitTestGiven.given();
        //when
        List<IExtendedFileSpec> extendedFileSpecs = verifyDelegator.verifyFiles(fileSpecs, opts);
        //then
        assertThat(extendedFileSpecs.size(), is(0));
    }

    /**
     * Expected return non empty verified fileSpecs when has depot file and is contain desc
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonEmptyListThatDontHaveAnyErrorDontHaveDepotPathAndIsNotContainsDescField()
            throws Exception {

        shouldReturnNonEmptyList(
                new UnitTestGiven() {
                    @Override
                    public void given() throws P4JavaException {
                        when(resultMap.containsKey(DEPOT_FILE)).thenReturn(true);
                        when(resultMap.containsKey(DESC)).thenReturn(false);
                    }
                });
    }

    private void shouldReturnNonEmptyList(UnitTestGiven unitTestGiven) throws Exception {
        unitTestGiven.given();
        //when
        List<IExtendedFileSpec> extendedFileSpecs = verifyDelegator.verifyFiles(fileSpecs, opts);
        //then
        assertThat(extendedFileSpecs.size(), is(1));
    }

    /**
     * Expected return non empty verified 'error' fileSpecs when error message is real error message
     * FIXME: server.handleFileErrorStr()
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonEmptyListWhenErrorMessageIsRealErrorMessage() throws Exception {
        givenErrorMessageCode();
        when(server.handleFileErrorStr(eq(resultMap))).thenReturn("not blank");

        List<IExtendedFileSpec> extendedFileSpecs = verifyDelegator.verifyFiles(fileSpecs, opts);
        assertThat(extendedFileSpecs.size(), is(1));
        assertThat(extendedFileSpecs.get(0).getOpStatus(), is(FileSpecOpStatus.ERROR));
    }

    private void givenErrorMessageCode() {
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_ERROR_RANGE);
        when(resultMap.get(FMT0)).thenReturn("not blank message");
    }

    /**
     * Expected return non empty verified 'info' fileSpecs when error message is info message
     * FIXME: server.handleFileErrorStr()
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonEmptyListWhenErrorMessageIsInfoMessage() throws Exception {
        givenInfoMessageCode();
        when(server.handleFileErrorStr(eq(resultMap))).thenReturn("not blank");

        List<IExtendedFileSpec> extendedFileSpecs = verifyDelegator.verifyFiles(fileSpecs, opts);
        assertThat(extendedFileSpecs.size(), is(1));
        assertThat(extendedFileSpecs.get(0).getOpStatus(), is(FileSpecOpStatus.INFO));
    }

    private void givenInfoMessageCode() {
        when(resultMap.get(FMT0)).thenReturn("%verify%");
        when(resultMap.get(CODE0)).thenReturn(MESSAGE_CODE_IN_INFO_RANGE);
        when(resultMap.get("verify")).thenReturn("Verify success");
    }
}