// Copyright (C) Zilliant, Inc.
package com.perforce.p4java;

import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

// p4ic4idea: better Windows support.
public class SymlinkUtil {
    private static SymlinkUtil INSTANCE;

    private final boolean isSupported;

    public boolean isSupported() {
        return isSupported;
    }

    private SymlinkUtil(boolean isSupported) {
        this.isSupported = isSupported;
    }

    public synchronized static SymlinkUtil getInstance() throws IOException {
        if (INSTANCE == null) {
            INSTANCE = new SymlinkUtil(determineIfSymlinksSupported());
            // First time usage.
            if (! INSTANCE.isSupported()) {
                System.err.println("WARNING: Symbolic Links are not supported on your operating system; they will not"
                        + " be tested.");
            }
        }
        return INSTANCE;
    }

    private static boolean determineIfSymlinksSupported() throws IOException {
        final Path tempDir = Files.createTempDirectory("p4-tests");
        final Path mockFile = tempDir.resolve("SymbolicLinkHelperTest.txt");
        FileUtils.write(new File(mockFile.toString()), "hello world",
                StandardCharsets.UTF_8.name());
        final Path symlinkFile = tempDir.resolve("symlinkTest");
        Files.deleteIfExists(symlinkFile);
        boolean ret;
        try {
            Files.createSymbolicLink(symlinkFile, mockFile);
            ret = true;
        } catch (FileSystemException e) {
            System.err.println("WARNING: Symlinks are not supported on your operating system; they will not be tested"
                    + " properly here: " + e);
            ret = false;
        }
        if (Files.isDirectory(tempDir)) {
            if (!P4JavaTestCase.deleteDir(tempDir.toFile())) {
                throw new IOException("Could not delete " + tempDir);
            }
        }
        return ret;
    }
}
