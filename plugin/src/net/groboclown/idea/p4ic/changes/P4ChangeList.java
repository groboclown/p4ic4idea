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

package net.groboclown.idea.p4ic.changes;

import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.P4Job;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A (possibly cached) representation of a Perforce changelist, with the
 * IntelliJ version of the files.  The class is immutable, as it reflects
 * the <em>system of record</em> Perforce server values.  To change the
 * contents or comment, the Server invocation must be used.
 */
public class P4ChangeList {
    private final P4ChangeListId id;
    private final Set<P4FileInfo> files;
    private final String comment;
    private final String owner;
    private final List<P4Job> jobIds;
    private final Date lastUpdateTime = new Date();

    public P4ChangeList(@NotNull final P4ChangeListId id, @NotNull final Collection<P4FileInfo> files,
                        @Nullable final String comment, @Nullable final String owner,
                        @Nullable Collection<P4Job> jobIds) {
        this.id = id;
        this.files = Collections.unmodifiableSet(new HashSet<P4FileInfo>(files));
        this.comment = comment;
        this.owner = owner;
        if (jobIds == null || jobIds.isEmpty()) {
            this.jobIds = Collections.emptyList();
        } else {
            this.jobIds = Collections.unmodifiableList(new ArrayList<P4Job>(jobIds));
        }
    }

    @NotNull
    public P4ChangeListId getId() {
        return id;
    }

    @NotNull
    public Set<P4FileInfo> getFiles() {
        return files;
    }

    @Nullable
    public String getComment() {
        return comment;
    }

    @Nullable
    public String getOwner() {
        return owner;
    }

    @NotNull
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    @NotNull
    public List<P4Job> getJobs() {
        return jobIds;
    }

    @Override
    public String toString() {
        return "[" + id + ": jobs " + jobIds + ", files " + files + "]";
    }
}
