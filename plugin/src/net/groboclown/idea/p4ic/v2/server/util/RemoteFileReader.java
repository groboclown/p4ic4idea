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

package net.groboclown.idea.p4ic.v2.server.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;
import net.groboclown.idea.p4ic.v2.server.cache.sync.ClientCacheManager;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.P4Exec2;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnection;
import net.groboclown.idea.p4ic.v2.server.connection.ServerQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class RemoteFileReader {
    private static final Logger LOG = Logger.getInstance(RemoteFileReader.class);

    public static ServerQuery<byte[]> createByteReader(@NotNull FilePath file, @NotNull IFileSpec spec) {
        return new ByteReader(file, spec);
    }


    public static ServerQuery<String> createStringReader(@NotNull FilePath file, @NotNull IFileSpec spec) {
        return new StringReader(file, spec);
    }



    private static final class ByteReader implements ServerQuery<byte[]> {
        private final FilePath file;
        private final IFileSpec spec;

        private ByteReader(@NotNull final FilePath file, @NotNull final IFileSpec spec) {
            this.file = file;
            this.spec = spec;
        }

        @Nullable
        @Override
        public byte[] query(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager cacheManager,
                @NotNull final ServerConnection connection, @NotNull final AlertManager alerts)
                throws InterruptedException {
            try {
                return exec.loadFile(spec);
            } catch (VcsException e) {
                alerts.addWarning(exec.getProject(),
                        P4Bundle.message("exception.load-file.title"),
                        P4Bundle.message("exception.load-file", spec.toString()),
                        e, file);
            } catch (IOException e) {
                alerts.addWarning(exec.getProject(),
                        P4Bundle.message("exception.load-file.title"),
                        P4Bundle.message("exception.load-file", spec.toString()),
                        e, file);
            }
            return null;
        }
    }


    private static final class StringReader implements ServerQuery<String> {
        private final FilePath file;
        private final IFileSpec spec;

        private StringReader(@NotNull final FilePath file, @NotNull final IFileSpec spec) {
            this.file = file;
            this.spec = spec;
        }

        @Nullable
        @Override
        public String query(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager cacheManager,
                @NotNull final ServerConnection connection, @NotNull final AlertManager alerts)
                throws InterruptedException {
            try {
                byte[] bytes = exec.loadFile(spec);
                //if (bytes == null) {
                //    return null;
                //}
                final List<IExtendedFileSpec> es =
                        exec.getFileStatus(Collections.singletonList(spec));
                String encoding = null;
                if (! es.isEmpty()) {
                    encoding = es.get(0).getCharset();
                }
                if (encoding == null) {
                    alerts.addNotice(exec.getProject(),
                            P4Bundle.message("exception.load-file-encoding"),
                            new P4FileException(file),
                            file);
                    return new String(bytes);
                }
                LOG.info("reading " + file + " with encoding " + encoding);
                return new String(bytes, encoding);
            } catch (VcsException e) {
                alerts.addWarning(exec.getProject(),
                        P4Bundle.message("exception.load-file.title"),
                        P4Bundle.message("exception.load-file", spec.toString()),
                        e, file);
            } catch (IOException e) {
                alerts.addWarning(exec.getProject(),
                        P4Bundle.message("exception.load-file.title"),
                        P4Bundle.message("exception.load-file", spec.toString()),
                        e, file);
            }
            return null;
        }
    }
}
