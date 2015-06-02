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
import net.groboclown.idea.p4ic.history.P4FileRevision;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PathRevs {
    private static final Logger LOG = Logger.getInstance(PathRevs.class);

    private final String depotPath;
    private final List<P4FileRevision> revs;

    @NotNull
    public static List<PathRevs> getPathRevs(@NotNull final List<P4FileRevision> revs) {
        List<PathRevs> ret = new ArrayList<PathRevs>();
        PathRevs current = null;
        for (P4FileRevision rev : revs) {
            String depotPath = rev.getRevisionDepotPath();
            if (depotPath != null) {
                if (current == null) {
                    LOG.info(":: " + depotPath);
                    current = new PathRevs(depotPath);
                    ret.add(current);
                } else if (! depotPath.equals(current.depotPath)) {
                    LOG.info(":: " + depotPath);
                    current = new PathRevs(depotPath);
                    ret.add(current);
                }
                LOG.info(":: -> " + rev.getRev());
                current.revs.add(rev);
            }
        }

        return ret;
    }



    private PathRevs(final String depotPath) {
        this.depotPath = depotPath;
        this.revs = new ArrayList<P4FileRevision>();
    }

    public String getDepotPath() {
        return depotPath;
    }

    public List<P4FileRevision> getRevisions() {
        return Collections.unmodifiableList(revs);
    }
}
