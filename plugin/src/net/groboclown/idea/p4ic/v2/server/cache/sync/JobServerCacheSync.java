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

package net.groboclown.idea.p4ic.v2.server.cache.sync;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListJob;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import net.groboclown.idea.p4ic.v2.server.cache.state.CachedState;
import net.groboclown.idea.p4ic.v2.server.cache.state.JobStateList;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4JobState;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import net.groboclown.idea.p4ic.v2.server.connection.P4Exec2;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnection;
import net.groboclown.idea.p4ic.v2.server.connection.ServerQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JobServerCacheSync extends CacheFrontEnd {
    // FIXME jobs need to be cached per server

    private final JobStateList jobs;
    private Date lastRefreshed;

    public JobServerCacheSync(@NotNull JobStateList jobs) {
        this.jobs = jobs;
        lastRefreshed = CachedState.NEVER_LOADED;
        for (P4JobState state : jobs) {
            if (state.getLastUpdated().after(lastRefreshed)) {
                lastRefreshed = state.getLastUpdated();
            }
        }
    }


    @Override
    protected void innerLoadServerCache(@NotNull final P4Exec2 exec,
            @NotNull final AlertManager alerts) {
        // Only reload the job status for the jobs we're asked to
        // care about.

        for (String jobId: jobs.copy().keySet()) {
            try {
                final P4JobState job = exec.getJobForId(jobId);
                if (job != null) {
                    jobs.add(job);
                }
            } catch (VcsException e) {
                alerts.addWarning(exec.getProject(),
                        P4Bundle.message("error.job-refresh.title", jobId),
                        P4Bundle.message("error.job-refresh", jobId),
                        e, new FilePath[0]);
            }
        }
    }

    @NotNull
    @Override
    protected Date getLastRefreshDate() {
        return lastRefreshed;
    }

    @NotNull
    Collection<P4JobState> loadServerCache(@NotNull P4Exec2 exec, @NotNull AlertManager alerts,
            @NotNull Collection<String> jobIds) {
        Set<P4JobState> ret = new HashSet<P4JobState>(jobIds.size());
        for (String jobId : jobIds) {
            try {
                final P4JobState job = exec.getJobForId(jobId);
                if (job != null) {
                    jobs.add(job);
                    ret.add(job);
                }
            } catch (VcsException e) {
                alerts.addWarning(exec.getProject(),
                        P4Bundle.message("error.job-refresh.title", jobId),
                        P4Bundle.message("error.job-refresh", jobId),
                        e, new FilePath[0]);
            }
        }
        return ret;
    }

    Map<String, P4ChangeListJob> getCachedJobIds(@NotNull ClientServerId clientServerId,
            @NotNull Collection<String> jobIds) {
        Map<String, P4ChangeListJob> ret = new HashMap<String, P4ChangeListJob>();
        for (String jobId : jobIds) {
            P4JobState job = jobs.get(jobId);
            if (job == null) {
                ret.put(jobId, null);
            } else {
                ret.put(jobId, new P4ChangeListJob(clientServerId, job));
            }
        }
        return ret;
    }

    @NotNull
    ServerQuery createRefreshQuery(@NotNull Collection<String> jobIds) {
        Date needsRefreshTime = new Date(System.currentTimeMillis() - MIN_REFRESH_INTERVAL_MS);
        final List<String> toRefresh = new ArrayList<String>(jobIds);
        Iterator<String> iter = toRefresh.iterator();
        while (iter.hasNext()) {
            final String next = iter.next();
            final P4JobState job = jobs.get(next);
            if (job != null && job.getLastUpdated().after(needsRefreshTime)) {
                iter.remove();
            }
        }

        return new ServerQuery() {
            @Nullable
            @Override
            public Object query(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager cacheManager,
                    @NotNull final ServerConnection connection, @NotNull final AlertManager alerts)
                    throws InterruptedException {
                for (String jobId : toRefresh) {
                    try {
                        final P4JobState job = exec.getJobForId(jobId);
                        if (job != null) {
                            jobs.add(job);
                        } else {
                            jobs.remove(jobId);
                        }
                    } catch (VcsException e) {
                        alerts.addWarning(exec.getProject(),
                                P4Bundle.message("error.job-refresh.title", jobId),
                                P4Bundle.message("error.job-refresh", jobId),
                                e, new FilePath[0]);
                    }
                }
                return null;
            }
        };
    }
}
