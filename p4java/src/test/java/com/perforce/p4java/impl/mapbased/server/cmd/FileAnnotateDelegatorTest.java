package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.exception.MessageSeverityCode.E_FAILED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.server.CmdSpec.ANNOTATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.Lists;
import com.nitorcreations.junit.runners.NestedRunner;
import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;

/**
 * @author Sean Shou
 * @since 21/09/2016
 */
@RunWith(NestedRunner.class)
public class FileAnnotateDelegatorTest extends AbstractP4JavaUnitTest {
    private FileAnnotateDelegator fileAnnotateDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;
    private List<IFileSpec> fileSpecs;

    private static final String DEPOT_FILE_KEY = "depotFile";

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        fileAnnotateDelegator = new FileAnnotateDelegator(server);

        resultMap = mock(Map.class);
        resultMaps = Lists.newArrayList(resultMap);

        fileSpecs = Lists.newArrayList();
        IFileSpec fileSpec = mock(IFileSpec.class);
        fileSpecs.add(fileSpec);
    }

    /**
     * test getFileAnnotations()
     */
    public class TestGetFileAnnotations {
        /**
         * test getFileAnnotations() by 'fileSpecs, diffType, allResult, useChangeNumber, followBranch' arguments
         */
        public class WhenFileSpecsDiffTypeAllResultsUseChangeNumberAndFollowBranch {
            private DiffType nonWhitespaceDiffType = DiffType.UNIFIED_DIFF;
            private DiffType whitespaceDiffType = DiffType.IGNORE_WS;

            /**
             * Expected thrown <code>RequestException</code> when "non whitespace diff option" of <code>DiffType</code> given.
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test(expected = RequestException.class)
            public void shouldThrowRequestExceptionWhenNonWhitespaceDiffTypeGiven() throws Exception {
                executeGetFileAnnotations(nonWhitespaceDiffType);
                verify(server, never()).execMapCmdList(eq(ANNOTATE.toString()), any(String[].class), eq(null));
            }

            /**
             * Expected thrown <code>RequestException</code> when inner getFileAnnotations() throws the <code>P4JavaException</code>
             *
             * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
             */
            @Test(expected = RequestException.class)
            public void shouldThrowRequestExceptionWhenInnerGetFileAnnotationsThrowsP4JavaException() throws Exception {
                doThrow(P4JavaException.class).when(server).execMapCmdList(eq(ANNOTATE.toString()), any(String[].class), eq(null));

                executeGetFileAnnotations(whitespaceDiffType);
            }

            /**
             * Expected thrown <code>ConnectionException</code> when inner getFileAnnotations() throws the exception.
             *
             * @throws Exception
             */
            @Test(expected = ConnectionException.class)
            public void shouldThrowExceptionWhenInnerGetFileAnnotationsThrowsIt() throws Exception {
                doThrow(ConnectionException.class).when(server).execMapCmdList(eq(ANNOTATE.toString()), any(String[].class), eq(null));
                executeGetFileAnnotations(whitespaceDiffType);
            }

            /**
             * Return non-empty list when non new depot files include in file specs
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonEmptyFileAnnotations() throws Exception {
                mockResultMaps();
                doReturn(resultMaps).when(server).execMapCmdList(eq(ANNOTATE.toString()), any(String[].class), eq(null));

                //when
                List<IFileAnnotation> fileAnnotations = executeGetFileAnnotations(whitespaceDiffType);
                //then
                assertThat(fileAnnotations.size(), is(2));
            }

            private List<IFileAnnotation> executeGetFileAnnotations(DiffType diffType) throws Exception {
                boolean allResults = false;
                boolean useChangeNumbers = false;
                boolean followBranches = false;
                return fileAnnotateDelegator.getFileAnnotations(
                        fileSpecs,
                        diffType,
                        allResults,
                        useChangeNumbers,
                        followBranches);
            }
        }

        /**
         * test getFileAnnotations() by 'fileSpecs, GetFileAnnotationsOptions'
         */
        public class WhenFileSpecsAndGetFileAnnotationsOptions {
            private GetFileAnnotationsOptions fileAnnotationsOptions;

            /**
             * Runs before every test.
             */
            @Before
            public void beforeEach() {
                fileAnnotationsOptions = mock(GetFileAnnotationsOptions.class);
            }

            /**
             * Expected get empty file annotations when command return null result maps.
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnEmptyFileAnnotatesWhenResultMapIsNull() throws Exception {
                when(server.execMapCmdList(eq(ANNOTATE.toString()), any(String[].class), eq(null))).thenReturn(null);

                executeThenVerifyEmptyFileAnnotations();
            }

            /**
             * Expected get empty file annotations when only new depot files.
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnEmptyFileAnnotationsWhenOnlyNewDepotFiles() throws Exception {
                when(resultMap.containsKey(DEPOT_FILE_KEY)).thenReturn(true);
                when(server.execMapCmdList(eq(ANNOTATE.toString()), any(String[].class), eq(null))).thenReturn(resultMaps);

                executeThenVerifyEmptyFileAnnotations();
                verify(resultMap, never()).get(E_FAILED);
            }

            private void executeThenVerifyEmptyFileAnnotations() throws P4JavaException {
                List<IFileAnnotation> fileAnnotations = fileAnnotateDelegator.getFileAnnotations(
                        fileSpecs,
                        fileAnnotationsOptions);
                assertThat(fileAnnotations.size(), is(0));
            }

            /**
             * Return non-empty list when non new depot file include in file specs.
             *
             * @throws Exception
             */
            @Test
            public void shouldReturnNonEmptyFileAnnotationsWhenExistDepotFileGiven() throws Exception {
                mockResultMaps();
                when(server.execMapCmdList(eq(ANNOTATE.toString()), any(String[].class), eq(null))).thenReturn(resultMaps);

                List<IFileAnnotation> fileAnnotations = fileAnnotateDelegator.getFileAnnotations(fileSpecs, fileAnnotationsOptions);
                assertThat(fileAnnotations.size(), is(2));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void mockResultMaps() {
        Map<String, Object> resultMap0 = mock(Map.class);
        when(resultMap0.containsKey(DEPOT_FILE)).thenReturn(false);
        when(resultMap0.get(DEPOT_FILE_KEY + "0")).thenReturn("dev");
        when(resultMap0.containsKey(DEPOT_FILE_KEY + "0")).thenReturn(true);

        final int upper = 10;
        when(resultMap0.get("upper0")).thenReturn(upper);
        when(resultMap0.containsKey("upper0")).thenReturn(true);
        when(resultMap0.get("lower0")).thenReturn("5");
        when(resultMap0.containsKey("lower0")).thenReturn(true);

        Map<String, Object> resultMap1 = mock(Map.class);
        when(resultMap1.containsKey(DEPOT_FILE)).thenReturn(false);
        when(resultMap1.get(DEPOT_FILE_KEY + "1")).thenReturn("dev");
        when(resultMap1.containsKey(DEPOT_FILE_KEY + "1")).thenReturn(true);
        when(resultMap1.get("upper1")).thenReturn(upper);
        when(resultMap1.containsKey("upper1")).thenReturn(true);
        when(resultMap1.get("lower1")).thenReturn("5");
        when(resultMap1.containsKey("lower1")).thenReturn(true);

        //new depot file
        Map<String, Object> newDepotFileResultMap1 = mock(Map.class);
        when(newDepotFileResultMap1.containsKey(DEPOT_FILE)).thenReturn(true);
        when(newDepotFileResultMap1.get(DEPOT_FILE_KEY + "1")).thenReturn("dev");
        when(newDepotFileResultMap1.get("upper1")).thenReturn(upper);
        when(newDepotFileResultMap1.get("lower1")).thenReturn("not valid number");

        resultMaps = newArrayList(resultMap0, resultMap1, newDepotFileResultMap1);
    }
}