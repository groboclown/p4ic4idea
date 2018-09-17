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
package net.groboclown.p4.server.impl.tasks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ConcurrencyUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A background task that monitors the temporary files created by
 * the P4 java API.
 */
public class TempFileWatchDog {
    private static final Logger LOG = Logger.getInstance(TempFileWatchDog.class);
    private static final ScheduledExecutorService SCHEDULER =
            ConcurrencyUtil.newSingleScheduledThreadExecutor("P4 Temp File Cleanup");
    private static final FilenameFilter TEMP_FILE_FILTER = new TempFileFilter();
    private static final long INTERVAL_SECONDS = 10L;
    private static final String PREFIX = "p4j";
    private static final String SUFFIX = ".tmp";

    private final File tempDir;
    private ScheduledFuture<?> cleanup;


    private static File getDefaultTempDir() {
        try {
            return Files.createTempDirectory("tmp").toFile();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    public TempFileWatchDog() {
        this(getDefaultTempDir());
    }


    public TempFileWatchDog(@NotNull File tempDir) {
        if (! tempDir.isDirectory() || ! tempDir.canRead()) {
            throw new IllegalArgumentException(tempDir.getAbsolutePath());
        }
        this.tempDir = tempDir;
    }


    @NotNull
    public File getTempDir() {
        return tempDir;
    }


    public synchronized void start() {
        if (cleanup == null) {
            cleanup = SCHEDULER.scheduleWithFixedDelay(new Watcher(), INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }


    public synchronized void stop() {
        if (cleanup != null) {
            cleanup.cancel(false);
            cleanup = null;
        }
    }


    private void cleanUpFiles() {
        File[] contents = tempDir.listFiles(TEMP_FILE_FILTER);
        if (contents != null) {
            for (File file : contents) {
                if (!file.delete()) {
                    LOG.info("Could not delete temporary Perforce file " + file);
                }
            }
        }
    }


    public void cleanUpTempDir() {
        File[] contents = tempDir.listFiles();
        if (contents != null) {
            for (File file: contents) {
                // Ignore result.  We do the final check by trying to
                // delete the directory, which will fail if it's not
                // empty.
                file.delete();
            }
        }
        if (! tempDir.delete()) {
            LOG.info("Could not delete temporary directory " + tempDir);
        }
    }



    private class Watcher implements Runnable {
        @Override
        public void run() {
            try {
                cleanUpFiles();
            } catch (Exception ex) {
                LOG.warn(ex);
            }
        }
    }


    private static class TempFileFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            if (name == null) {
                return false;
            }
            String lName = name.toLowerCase();
            return lName.startsWith(PREFIX) && lName.endsWith(SUFFIX);
        }
    }
}
