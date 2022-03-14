package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CODE0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.server.CmdSpec.TAG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.server.IOptionsServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.TagFilesOptions;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Sean Shou
 * @since 6/10/2016
 */
public class TagDelegatorTest {
    private static final String MESSAGE_CODE_IN_INFO_RANGE = "268435456";
    private static final String TEST_FILE_DEPOT_PATH = "//depot/dev/test.txt";
    private static final String[] CMD_OPTIONS = {"-n", "-d"};
    private static final String LABEL_NAME = "p4_java_test";

    private static final String[] CMD_ARGUMENTS = ArrayUtils.addAll(
            CMD_OPTIONS,
            "-l" + LABEL_NAME,
            TEST_FILE_DEPOT_PATH);

    private TagDelegator tagDelegator;
    private Map<String, Object> resultMap;

    private List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(TEST_FILE_DEPOT_PATH);
    private TagFilesOptions opts;
    private IOptionsServer server;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() throws Exception {
        server = mock(Server.class);
        tagDelegator = new TagDelegator(server);

        resultMap = new HashMap<>();
        List<Map<String, Object>> resultMaps = List.of(resultMap);

        opts = new TagFilesOptions(CMD_OPTIONS);

        when(server.execMapCmdList(
                eq(TAG.toString()),
                eq(CMD_ARGUMENTS),
                eq(null))).thenReturn(resultMaps);
    }

    private final boolean listOnly1 = true;
    private final boolean delete1 = true;
    /**
     * Rule for expected exception verification
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Expected throws <code>ConnectionException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrowsConnectionExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {

        executeAndExpectThrowsExceptions(
                ConnectionException.class,
                ConnectionException.class
        );
    }

    /**
     * Expected throws <code>AccessException</code> when inner method call throws it
     *
     * @throws Exception
     */
    @Test
    public void shouldThrownAccessExceptionWhenInnerMethodCallThrowsIt()
            throws Exception {

        // p4ic4idea: use a public, non-abstract class with default constructor
        executeAndExpectThrowsExceptions(
                AccessException.AccessExceptionForTests.class,
                AccessException.class
        );
    }

    private void executeAndExpectThrowsExceptions(
            Class<? extends P4JavaException> thrownException,
            Class<? extends P4JavaException> expectedThrows) throws P4JavaException {

        thrown.expect(expectedThrows);

        doThrow(thrownException).when(server).execMapCmdList(
                eq(TAG.toString()),
                eq(CMD_ARGUMENTS),
                eq(null));
        tagDelegator.tagFiles(fileSpecs, LABEL_NAME, listOnly1, delete1);
    }

    /**
     * Expected return empty tag files when inner method call throws <code>P4JavaException</code>
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnEmptyTagFilesWhenInnerMethodCallThrowsP4JavaException()
            throws Exception {

        //given
        doThrow(P4JavaException.class).when(server).execMapCmdList(
                eq(TAG.toString()),
                eq(CMD_ARGUMENTS),
                eq(null));
        //when
        List<IFileSpec> tagFiles = tagDelegator.tagFiles(
                fileSpecs,
                LABEL_NAME,
                listOnly1,
                delete1);
        //then
        assertThat(tagFiles.size(), is(0));
    }

    /**
     * Expected return non empty tag files
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnNonEmptyTagFiles() throws Exception {
        //given
        givenInfoMessageCode();
        //when
        List<IFileSpec> tagFiles = tagDelegator.tagFiles(
                fileSpecs,
                LABEL_NAME,
                listOnly1,
                delete1);
        //then
        assertThat(tagFiles.size(), is(1));
        assertThat(tagFiles.get(0).getDepotPathString(), is(TEST_FILE_DEPOT_PATH));
    }

    /**
     * Expected return non empty tag files
     *
     * @throws Exception
     */
    @Test
    public void tagFilesShouldReturnNonEmptyTagFiles() throws Exception {
        //given
        givenInfoMessageCode();

        //when
        List<IFileSpec> tagFiles = tagDelegator.tagFiles(fileSpecs, LABEL_NAME, opts);
        //then
        assertThat(tagFiles.size(), is(1));
        assertThat(tagFiles.get(0).getDepotPathString(), is(TEST_FILE_DEPOT_PATH));
    }

    private void givenInfoMessageCode() {
        resultMap.clear();
        resultMap.put(CODE0, MESSAGE_CODE_IN_INFO_RANGE);
        resultMap.put(DEPOT_FILE, TEST_FILE_DEPOT_PATH);
    }
}