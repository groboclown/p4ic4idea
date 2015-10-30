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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class FileUpdateStateList implements Iterable<P4FileUpdateState> {
    private static final Logger LOG = Logger.getInstance(FileUpdateStateList.class);

    private final Set<P4FileUpdateState> updatedFiles;
    private final Object sync = new Object();

    public FileUpdateStateList() {
        this.updatedFiles = new HashSet<P4FileUpdateState>();
    }

    @NotNull
    @Override
    public Iterator<P4FileUpdateState> iterator() {
        return copy().iterator();
    }


    @NotNull
    public Set<P4FileUpdateState> copy() {
        synchronized (sync) {
            return new HashSet<P4FileUpdateState>(updatedFiles);
        }
    }


    public void replaceWith(@NotNull Collection<P4FileUpdateState> newValues) {
        synchronized (sync) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Replacing update state files with " + newValues + "; was " + updatedFiles);
            }
            updatedFiles.clear();
            updatedFiles.addAll(newValues);
        }
    }


    public void add(@NotNull P4FileUpdateState state) {
        synchronized (sync) {
            updatedFiles.add(state);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Adding state file with " + state + "; now " + updatedFiles);
            }
        }
    }


    public boolean remove(@NotNull P4FileUpdateState state) {
        synchronized (sync) {
            final boolean ret = updatedFiles.remove(state);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Removing state file " + state + "; now " + updatedFiles);
            }
            return ret;
        }
    }


    @Nullable
    public P4FileUpdateState getUpdateStateFor(@NotNull final FilePath file) {
        for (P4FileUpdateState updatedFile : copy()) {
            if (file.equals(updatedFile.getLocalFilePath())) {
                return updatedFile;
            }
        }
        return null;
    }


    @Override
    public String toString() {
        return updatedFiles.toString();
    }
}

