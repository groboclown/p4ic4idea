/* *************************************************************************
 * (c) Copyright 2015 Zilliant Inc. All rights reserved.                   *
 * *************************************************************************
 *                                                                         *
 * THIS MATERIAL IS PROVIDED "AS IS." ZILLIANT INC. DISCLAIMS ALL          *
 * WARRANTIES OF ANY KIND WITH REGARD TO THIS MATERIAL, INCLUDING,         *
 * BUT NOT LIMITED TO ANY IMPLIED WARRANTIES OF NONINFRINGEMENT,           *
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.                   *
 *                                                                         *
 * Zilliant Inc. shall not be liable for errors contained herein           *
 * or for incidental or consequential damages in connection with the       *
 * furnishing, performance, or use of this material.                       *
 *                                                                         *
 * Zilliant Inc. assumes no responsibility for the use or reliability      *
 * of interconnected equipment that is not furnished by Zilliant Inc,      *
 * or the use of Zilliant software with such equipment.                    *
 *                                                                         *
 * This document or software contains trade secrets of Zilliant Inc. as    *
 * well as proprietary information which is protected by copyright.        *
 * All rights are reserved.  No part of this document or software may be   *
 * photocopied, reproduced, modified or translated to another language     *
 * prior written consent of Zilliant Inc.                                  *
 *                                                                         *
 * ANY USE OF THIS SOFTWARE IS SUBJECT TO THE TERMS AND CONDITIONS         *
 * OF A SEPARATE LICENSE AGREEMENT.                                        *
 *                                                                         *
 * The information contained herein has been prepared by Zilliant Inc.     *
 * solely for use by Zilliant Inc., its employees, agents and customers.   *
 * Dissemination of the information and/or concepts contained herein to    *
 * other parties is prohibited without the prior written consent of        *
 * Zilliant Inc..                                                          *
 *                                                                         *
 * (c) Copyright 2015 Zilliant Inc. All rights reserved.                   *
 *                                                                         *
 * *************************************************************************/

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
