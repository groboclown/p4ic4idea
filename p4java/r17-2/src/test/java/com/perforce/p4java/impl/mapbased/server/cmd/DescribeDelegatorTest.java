package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.google.common.collect.Lists.newArrayList;
import static com.perforce.p4java.server.CmdSpec.DESCRIBE;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.CommandLineArgumentMatcher;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetChangelistDiffsOptions;
import com.perforce.p4java.server.IOptionsServer;

/**
 * Exercise the p4java support for p4 describe commands and their associated
 * options. TODO: This is inadequate, -d<flags> -m -s -f -O -I are not tested
 * and -S does not look at any actual differences.
 */
@RunWith(JUnitPlatform.class)
public class DescribeDelegatorTest extends AbstractP4JavaUnitTest {
    private DescribeDelegator describeDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;

    private List<IFileSpec> mockFileSpecs;
    private IFileSpec mockFileSpec;
    private int mockChangelistId = 10;

    @BeforeEach
    public void beforeEach() {
        server = mock(IOptionsServer.class);
        describeDelegator = new DescribeDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = newArrayList(resultMap);

        mockFileSpecs = newArrayList();
        mockFileSpec = mock(IFileSpec.class);
        mockFileSpecs.add(mockFileSpec);
    }

    /**
     * Test that when a streaming describe command returns an input stream, that
     * input stream is faithfully returned from the delegator.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testInputStreamDiffs() throws P4JavaException {
        // given
        InputStream mockInputStream = mock(InputStream.class);
        when(server.execStreamCmd(eq(DESCRIBE.toString()),
                argThat(new CommandLineArgumentMatcher(new String[] { "10" }))))
                        .thenReturn(mockInputStream);
        // when
        InputStream changelistInputStream = describeDelegator.getChangelistDiffs(10,
                new GetChangelistDiffsOptions());
        // then
        assertEquals(mockInputStream, changelistInputStream);
    }

    /**
     * Test that a describe command which asks for shelved files correctly
     * returns a list of shelved files that are associated with this changelist.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void testShelvedFilesList() throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(DESCRIBE.toString()),
                argThat(new CommandLineArgumentMatcher(
                        new String[] { "-s", "-S", String.valueOf(mockChangelistId) })),
                eq(null))).thenReturn(resultMaps);
        when(resultMap.get("id")).thenReturn(String.valueOf(mockChangelistId));
        when(resultMap.get("rev0")).thenReturn("present");
        // when
        List<IFileSpec> shelvedFiles = describeDelegator.getShelvedFiles(mockChangelistId);
        // then
        assertEquals(mockChangelistId, shelvedFiles.get(0).getChangelistId());
    }

    /**
     * Test that a describe command which asks for shelved files correctly
     * returns a list of files that are associated with this changelist.
     * 
     * @throws P4JavaException
     *             exception superclass from server commands
     */
    @Test
    public void getChangelistFiles() throws P4JavaException {
        // given
        when(server.execMapCmdList(eq(DESCRIBE.toString()),
                argThat(new CommandLineArgumentMatcher(
                        new String[] { "-s", String.valueOf(mockChangelistId) })),
                eq(null))).thenReturn(resultMaps);
        when(resultMap.get("id")).thenReturn(String.valueOf(mockChangelistId));
        when(resultMap.get("rev0")).thenReturn("present");

        // when
        List<IFileSpec> changelistFiles = describeDelegator.getChangelistFiles(mockChangelistId);

        // then
        assertEquals(mockChangelistId, changelistFiles.get(0).getChangelistId());
    }
}