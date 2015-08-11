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

package net.groboclown.idea.p4ic.actions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4ApiException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class P4RollbackHistoryAction extends BasicAction {
    private static final Logger LOG = Logger.getInstance(P4RollbackHistoryAction.class);
    public static final String ACTION_NAME = "Rollback";

    @Override
    protected void perform(@NotNull final Project project, @NotNull final P4Vcs vcs,
            @NotNull final List<VcsException> exceptions,
            @NotNull final List<VirtualFile> affectedFiles) {
        if (affectedFiles.isEmpty()) {
            return;
        }
        if (affectedFiles.size() > 1) {
            exceptions.add(new P4ApiException(P4Bundle.message("exception.rollback.history")));
            return;
        }
        LOG.info("perform rollback for " + affectedFiles);

        // TODO get the head revision and the rollback revision.
        exceptions.add(new P4InvalidConfigException("not implemented yet"));
    }

    @NotNull
    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected boolean isEnabled(@NotNull final Project project, @NotNull final P4Vcs vcs,
            @NotNull final VirtualFile... vFiles) {
        // TODO return false if this is the latest version.
        LOG.info("is enabled for " + Arrays.asList(vFiles));

        return true;
    }
}
