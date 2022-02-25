package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import org.apache.commons.io.FileUtils;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.perforce.p4java.common.base.OSUtils;

/**
 * @author Sean Shou
 * @since 30/08/2016
 */
@FixMethodOrder
@RunWith(JUnitPlatform.class)
public class RpcSystemFileCommandsHelperTest {
    private static SymbolicLinkHelper rpcSystemFileCommandsHelper;
    private static Path mockFile;
    private static Path symlinkFile;

    // p4ic4idea: get this to work with Windows better.
    private static Path tempDir;
    private static String noExistFileName;
    private static boolean symlinksSupported = true;


    @BeforeAll
    static void beforeAll() throws IOException {
        rpcSystemFileCommandsHelper = OSUtils.isWindows()
                ? new WindowsRpcSystemFileCommandsHelper()
                : new RpcSystemFileCommandsHelper();
        // p4ic4idea: get this working with Windows better, and use more
        //   secure temp file usage.
        tempDir = Files.createTempDirectory("p4-tests");
        noExistFileName = Paths.get(tempDir.toString(), "does-not-exist.data").toString();
        Path mockFilePath = Paths.get(tempDir.toString(), "p4java");
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
        try {
            // Note: this symlinksSupported logic is duplicated in SymlinkUtil.
            Files.createSymbolicLink(symlinkFile, mockFile);
            symlinksSupported = true;
        } catch (FileSystemException e) {
            System.err.println("WARNING: Symlinks are not supported on your operating system; they will not be tested"
                    + " properly here: " + e);
            symlinksSupported = false;
        }
    }

    // p4ic4idea: part of getting this working with Windows better
    @AfterAll
    static void afterAll() throws IOException {
        if (!P4JavaTestCase.deleteDir(tempDir.toFile())) {
            throw new IOException("Could not delete " + tempDir);
        }
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
        if (symlinksSupported) {
            boolean isSymlink = rpcSystemFileCommandsHelper.isSymlink(mockFile.toString());
            assertThat(isSymlink, is(false));

            isSymlink = rpcSystemFileCommandsHelper.isSymlink(symlinkFile.toString());
            assertThat(isSymlink, is(true));
        } else {
            System.err.println("TEST SKIPPED ; SYMLINKS NOT SUPPORTED");
        }
    }

}