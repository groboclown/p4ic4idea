package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.common.base.OSUtils;

/**
 * @author Sean Shou
 * @since 30/08/2016
 */
@FixMethodOrder
@RunWith(JUnitPlatform.class)
public class RpcSystemFileCommandsHelperTest extends AbstractP4JavaUnitTest {
    private static SymbolicLinkHelper rpcSystemFileCommandsHelper;
    private static Path mockFile;
    private static Path symlinkFile;
    private static String noExistFileName = "/tmp/notExistFile.data";

    @BeforeAll
    static void beforeAll() throws IOException {
        rpcSystemFileCommandsHelper = OSUtils.isWindows()
                ? new WindowsRpcSystemFileCommandsHelper()
                : new RpcSystemFileCommandsHelper();
        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"));
        Path mockFilePath = Paths.get(tempPath.toString(), "p4java");
        if (Files.notExists(mockFilePath)) {
            Files.createDirectory(mockFilePath);
        }
        mockFile = mockFilePath.resolve("RpcSystemFileCommandsHelperTest.exe");
        if (!Files.exists(mockFile)) {
            FileUtils.write(new File(mockFile.toString()), "hello world",
                    StandardCharsets.UTF_8.name());
        }   
        symlinkFile = mockFilePath.resolve("symlinkTest");
        Files.deleteIfExists(symlinkFile);
        Files.createSymbolicLink(symlinkFile, mockFile);
    }

    @BeforeEach
    public void beforeEach() {
        rpcSystemFileCommandsHelper.setWritable(mockFile.toString(), true);
    }

    @Test
    public void setWritable() throws Exception {
        boolean setWritable = rpcSystemFileCommandsHelper.setWritable(mockFile.toString(), true);
        assertThat(setWritable, is(true));

        setWritable = rpcSystemFileCommandsHelper.setWritable(mockFile.toString(), false);
        assertThat(setWritable, is(true));

        setWritable = rpcSystemFileCommandsHelper.setWritable(noExistFileName, true);
        assertThat(setWritable, is(false));
    }
    
    @Test
    public void setReadable() throws Exception {
        
        boolean setReadable = rpcSystemFileCommandsHelper.setReadable(mockFile.toString(), true,
                false);
        assertThat(setReadable, is(true));

        setReadable = rpcSystemFileCommandsHelper.setReadable(mockFile.toString(), true, true);
        assertThat(setReadable, is(true));
        
        setReadable = rpcSystemFileCommandsHelper.setReadable(mockFile.toString(), false, true);
        assertThat(setReadable, is(true));
        
        setReadable = rpcSystemFileCommandsHelper.setReadable(noExistFileName, true, false);
        assertThat(setReadable, is(false));
    }

    @Test
    public void setOwnerReadOnly() throws Exception {
        boolean setReadable = rpcSystemFileCommandsHelper.setReadable(mockFile.toString(), false, false);
        assertThat(setReadable, is(true));
        
        setReadable = rpcSystemFileCommandsHelper.setReadable(mockFile.toString(), true, false);
        assertThat(setReadable, is(true));
        
        boolean setOwnerReadOnly = rpcSystemFileCommandsHelper
                .setOwnerReadOnly(mockFile.toString());
        assertThat(setOwnerReadOnly, is(true));

        setOwnerReadOnly = rpcSystemFileCommandsHelper.setOwnerReadOnly(noExistFileName);
        assertThat(setOwnerReadOnly, is(false));
    }

    @Test
    public void setExecutable() throws Exception {
        boolean setExecutable = rpcSystemFileCommandsHelper.setExecutable(mockFile.toString(), true,
                false);
        assertThat(setExecutable, is(true));

        setExecutable = rpcSystemFileCommandsHelper.setExecutable(noExistFileName, true, false);
        // Windows non-existent always returns true, different from nix
        assertThat(setExecutable, is(OSUtils.isWindows()));
    }

    @Test
    public void canExecute() throws Exception {
        boolean canExecute = rpcSystemFileCommandsHelper.canExecute(mockFile.toString());
        assertThat(canExecute, is(true));

        canExecute = rpcSystemFileCommandsHelper.canExecute(noExistFileName);
        assertThat(canExecute, is(false));
    }

    @Test
    public void isSymlink() throws Exception {
        boolean isSymlink = rpcSystemFileCommandsHelper.isSymlink(mockFile.toString());
        assertThat(isSymlink, is(false));

        isSymlink = rpcSystemFileCommandsHelper.isSymlink(symlinkFile.toString());
        assertThat(isSymlink, is(true));
    }

}