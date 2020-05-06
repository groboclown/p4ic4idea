/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perforce.test;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// Renamed from "FileUtils" so that it can easily be used with Apache commons-io FileUtils
public class P4ExtFileUtils {
    public static void createDirectory(String toPath) {
        createDirectory(new File(toPath));
    }

    public static void createDirectory(File toPath) {
        if (toPath.exists()) {
            if (! toPath.isDirectory()) {
                throw new IllegalStateException("Exists but is not a directory: " + toPath);
            }
        } else if (!toPath.mkdirs()) {
            throw new IllegalStateException("Could not create directory " + toPath);
        }
    }

    public static void extractResource(@Nonnull ClassLoader cl, @Nonnull String resourceLocation,
            @Nonnull File outputFile, boolean uncompress)
            throws IOException {
        extractResource(cl, null, resourceLocation, outputFile, uncompress);
    }

    public static void extractResource(@Nonnull Object parentObject, @Nonnull String resourceLocation,
            @Nonnull File outputFile, boolean uncompress)
            throws IOException {
        extractResource(null, parentObject, resourceLocation, outputFile, uncompress);
    }

    public static void extractResource(@Nullable ClassLoader cl, @Nullable Object parentObject,
            @Nonnull String resourceLocation, @Nonnull File outputFile, boolean uncompress)
            throws IOException {
        // if (outputFile.exists()) {
        //     throw new IOException("Cannot overwrite existing file: " + outputFile);
        // }
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("Could not create directory " + parent);
            }
        }
        InputStream inp = new BufferedInputStream(getStream(cl, parentObject, resourceLocation));
        if (uncompress) {
            if (resourceLocation.endsWith(".tar.bz2")) {
                extractArchive(new TarArchiveInputStream(new BZip2CompressorInputStream(inp)), outputFile);
                return;
            }
            if (resourceLocation.endsWith(".tar.xz")) {
                extractArchive(new TarArchiveInputStream(new XZCompressorInputStream(inp)), outputFile);
                return;
            }
            if (resourceLocation.endsWith(".tar.gz") || resourceLocation.endsWith(".tgz")) {
                extractArchive(new TarArchiveInputStream(new GzipCompressorInputStream(inp)), outputFile);
                return;
            }
            if (resourceLocation.endsWith(".tar")) {
                extractArchive(new TarArchiveInputStream(inp), outputFile);
                return;
            }
            if (resourceLocation.endsWith(".zip")) {
                extractArchive(new ZipArchiveInputStream(inp), outputFile);
                return;
            }
        }
        extractFile(inp, outputFile);
    }


    private static void extractArchive(ArchiveInputStream archiveInputStream, File outputDir)
            throws IOException {
        createDirectory(outputDir);
        try {
            ArchiveEntry entry = archiveInputStream.getNextEntry();
            while (entry != null) {
                File node = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!node.mkdirs()) {
                        throw new IOException("Could not create directory " + node);
                    }
                } else {
                    extractFile(archiveInputStream, node);
                }
                entry = archiveInputStream.getNextEntry();
            }
        } finally {
            archiveInputStream.close();
        }
    }

    public static void extractFile(InputStream inp, File outputFile)
            throws IOException {
        byte[] buff = new byte[4096];
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            int len;
            while ((len = inp.read(buff, 0, 4096)) > 0) {
                out.write(buff, 0, len);
            }
        }
    }


    public static void readFile(InputStream inp, OutputStream out)
            throws IOException {
        byte[] buff = new byte[4096];
        int len;
        while ((len = inp.read(buff, 0, 4096)) > 0) {
            out.write(buff, 0, len);
        }
    }

    @Nonnull
    public static InputStream getStream(@Nonnull ClassLoader cl, @Nonnull String resource) {
        return getStream(cl, null, resource);
    }

    @Nonnull
    public static InputStream getStream(@Nonnull Object parentObject, @Nonnull String resource) {
        return getStream(null, parentObject, resource);
    }

    @Nonnull
    public static InputStream getStream(@Nullable ClassLoader cl, @Nullable Object parentObject, @Nonnull String resource) {
        resource = resource.replace('\\', '/');
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
            if (cl == null && parentObject != null) {
                if (parentObject instanceof Class<?>) {
                    cl = ((Class<?>) parentObject).getClassLoader();
                } else {
                    cl = parentObject.getClass().getClassLoader();
                }
            }
        }
        if (resource.startsWith("/")) {
            resource = resource.substring(1);
        }
        InputStream inp;
        if (cl == null) {
            inp = ClassLoader.getSystemResourceAsStream(resource);
        } else {
            inp = cl.getResourceAsStream(resource);
        }
        if (inp == null) {
            if (!resource.startsWith("/")) {
                resource = "/" + resource;
            }
            if (cl == null) {
                inp = ClassLoader.getSystemResourceAsStream(resource);
            } else {
                inp = cl.getResourceAsStream(resource);
            }
            if (inp == null) {
                throw new IllegalStateException("Could not find resource " + resource);
            }
        }
        return inp;
    }

    public static String getP4dPath(String version)
            throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "bin/" + version + "/bin.ntx64/p4d.exe";
        }
        if (os.contains("mac")) {
            return "bin/" + version + "/bin.darwin90x86_64/p4d";
        }
        if (os.contains("nix") || os.contains("nux")) {
            return "bin/" + version + "/bin.linux26x86_64/p4d";
        }
        throw new IOException("No p4d registered for OS " + os);
    }

    public static File getP4dOutput(File outdir) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new File(outdir, "p4d.exe");
        }
        return new File(outdir, "p4d");
    }

    public static File extractP4d(File outdir, String version)
            throws IOException {
        File outP4d = getP4dOutput(outdir);
        if (!outP4d.exists()) {
            if (!outdir.exists() && !outdir.mkdirs()) {
                throw new IOException("could not create output dir " + outdir);
            }
            String osP4d = getP4dPath(version);
            extractResource(P4ExtFileUtils.class.getClassLoader(), osP4d, outP4d, false);
            if (!outP4d.setExecutable(true)) {
                throw new IOException("Could not make executable: " + outP4d);
            }
        }
        return outP4d;
    }
}
