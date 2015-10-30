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

package net.groboclown.idea.p4ic.v2.server.cache.state;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JobStateList implements Iterable<P4JobState> {
    private final Map<String, P4JobState> jobs;
    private final Object sync = new Object();

    public JobStateList(@NotNull Collection<P4JobState> jobs) {
        this.jobs = new HashMap<String, P4JobState>();
        for (P4JobState job : jobs) {
            this.jobs.put(job.getId(), job);
        }
    }

    public JobStateList() {
        this.jobs = new HashMap<String, P4JobState>();
    }

    @NotNull
    @Override
    public Iterator<P4JobState> iterator() {
        return copy().values().iterator();
    }

    @NotNull
    public Map<String, P4JobState> copy() {
        synchronized (sync) {
            return new HashMap<String, P4JobState>(jobs);
        }
    }

    public void add(@NotNull P4JobState job) {
        synchronized (sync) {
            jobs.put(job.getId(), job);
        }
    }

    @Nullable
    public P4JobState get(final String jobId) {
        synchronized (sync) {
            return jobs.get(jobId);
        }
    }

    public boolean remove(@NotNull final String jobId) {
        synchronized (sync) {
            return jobs.remove(jobId) != null;
        }
    }
}
