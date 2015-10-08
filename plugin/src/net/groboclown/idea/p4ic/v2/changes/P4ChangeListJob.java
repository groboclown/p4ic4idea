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

package net.groboclown.idea.p4ic.v2.changes;

import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import net.groboclown.idea.p4ic.v2.server.cache.P4ChangeListValue;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4JobState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * A Perforce Job associated with a changelist.  Immutable.
 */
public final class P4ChangeListJob implements Comparable<P4ChangeListJob> {
    // Default list of status, in case of a problem.
    public static final List<String> DEFAULT_JOB_STATUS = Arrays.asList(
            "open", "suspended", "closed"
    );

    private final ClientServerId clientServerId;
    private final P4JobState job;

    public P4ChangeListJob(@NotNull P4ChangeListValue change, @NotNull P4JobState job) {
        this.clientServerId = change.getClientServerId();
        this.job = job;
    }


    @NotNull
    public String getJobId() {
        return job.getId();
    }


    @Nullable
    public String getDescription() {
        return job.getDescription();
    }


    @Override
    public int compareTo(final P4ChangeListJob o) {
        return job.getId().compareTo(o.job.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof P4ChangeListJob) {
            P4ChangeListJob that = (P4ChangeListJob) o;
            return clientServerId.equals(that.clientServerId) &&
                    job.getId().equals(that.job.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (clientServerId.hashCode() << 2) +
                job.getId().hashCode();
    }
}
