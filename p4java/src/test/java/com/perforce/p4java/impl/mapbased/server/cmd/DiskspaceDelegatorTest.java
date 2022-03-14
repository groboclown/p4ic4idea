package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.server.CmdSpec.DISKSPACE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.server.IOptionsServer;
import org.junit.Before;
import org.junit.Test;

import com.perforce.p4java.admin.IDiskSpace;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.mapbased.server.Server;

/**
 * @author Sean Shou
 * @since 27/09/2016
 */
public class DiskspaceDelegatorTest {
    private DiskspaceDelegator diskspaceDelegator;
    private Map<String, Object> resultMap;
    private List<Map<String, Object>> resultMaps;

    private List<IFileSpec> mockFileSpecs;
    private IFileSpec mockFileSpec;

    IOptionsServer server;

    /**
     * Runs before every test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void beforeEach() {
        server = mock(Server.class);
        diskspaceDelegator = new DiskspaceDelegator(server);
        resultMap = mock(Map.class);
        resultMaps = List.of(resultMap);

        mockFileSpecs = new ArrayList<>();
        mockFileSpec = mock(IFileSpec.class);
        mockFileSpecs.add(mockFileSpec);
    }

    /**
     * Test get disk space
     *
     * @throws Exception if the <code>Exception</code> is thrown, it's mean an unexpected error occurs
     */
    @Test
    public void testGetDiskSpace() throws Exception {
        //given
        String[] cmdArgs = {"-f", "-s"};
        List<String> filesystems = Arrays.asList(cmdArgs);
        when(server.execMapCmdList(eq(DISKSPACE.toString()), eq(cmdArgs), eq(null))).thenReturn(
                resultMaps);
        //when
        List<IDiskSpace> diskSpaces = diskspaceDelegator.getDiskSpace(filesystems);
        //then
        assertThat(diskSpaces.size(), is(1));
    }
}