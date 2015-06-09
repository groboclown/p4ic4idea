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

package net.groboclown.idea.p4ic.ui.revision;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PathRevsSet {
    private static final Logger LOG = Logger.getInstance(PathRevsSet.class);


    // TODO make configurable
    private static final int REVISION_PAGE_SIZE = 1000;


    private final List<PathRevs> revs;
    private final String error;

    public static PathRevsSet create(final @NotNull P4Vcs vcs, final @NotNull VirtualFile file) {
        final Client client = vcs.getClientFor(file);
        if (client == null || client.isWorkingOffline()) {
            return new PathRevsSet(P4Bundle.getString("revision.list.notconnected"));
        } else {
            try {
                final List<P4FileInfo> p4infoList = client.getServer().getVirtualFileInfo(Collections.singleton(file));
                if (p4infoList.isEmpty()) {
                    // can't find file
                    return new PathRevsSet(P4Bundle.getString("revision.list.nosuchfile"));
                } else {
                    final P4FileInfo fileInfo = p4infoList.get(0);
                    LOG.info("diff file depot: " + fileInfo.getDepotPath());

                    final List<PathRevs> revisions =
                            PathRevs.getPathRevs(client.getServer().getRevisionHistory(fileInfo, REVISION_PAGE_SIZE));

                    if (revisions.isEmpty()) {
                        return new PathRevsSet(P4Bundle.message("revision.list.no-revs", file));
                    }

                    return new PathRevsSet(revisions);
                }
            } catch (VcsException e) {
                LOG.warn(e);
                return new PathRevsSet(e.getMessage());
            }
        }
    }

    PathRevsSet(@NotNull final List<PathRevs> revs) {
        this.revs = Collections.unmodifiableList(revs);
        this.error = null;
    }

    PathRevsSet(@NotNull final String errs) {
        this.revs = Collections.emptyList();
        this.error = errs;
    }


    public boolean isError() {
        return error != null;
    }

    public List<PathRevs> getPathRevs() {
        return revs;
    }

    public String getError() {
        return error;
    }
}
