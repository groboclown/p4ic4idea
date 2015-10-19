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

package net.groboclown.idea.p4ic.v2.ui.warning;

import com.intellij.ide.errorTreeView.HotfixGate;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class WarningMessage {
    private final Project project;
    private final String summary;
    private final String message;
    private final Exception warning;
    private final Collection<VirtualFile> affectedFiles;
    private final Consumer<HotfixGate> hotfix;
    private final Date when = new Date();

    public WarningMessage(@NotNull Project project, @Nls @NotNull String summary,
            @Nls @NotNull final String message,
            @Nullable final Exception warning, @Nullable Collection<VirtualFile> affectedFiles) {
        this(project, summary, message, warning, affectedFiles, null);
    }

    /**
     *
     * @param project project
     * @param summary display summary
     * @param message display message
     * @param warning error source
     * @param affectedFiles files that generated error
     * @param hotfix if the error is automatically solvable, add this in;
     *               it will display the error as "fixable", and give the
     *               user a chance to click it and resolve the issue.
     */
    public WarningMessage(@NotNull Project project, @Nls @NotNull String summary,
            @Nls @NotNull final String message,
            @Nullable final Exception warning, @Nullable Collection<VirtualFile> affectedFiles,
            @Nullable Consumer<HotfixGate> hotfix) {
        this.project = project;
        this.summary = summary;
        this.message = message;
        this.warning = warning;
        this.affectedFiles = affectedFiles == null
                ? Collections.<VirtualFile>emptyList()
                : Collections.unmodifiableCollection(affectedFiles);
        this.hotfix = hotfix;
    }

    public WarningMessage(@NotNull Project project, @Nls @NotNull String summary,
            @Nls @NotNull final String message,
            @Nullable final Exception warning, VirtualFile... affectedFiles) {
        this(project, summary, message, warning, Arrays.asList(affectedFiles));
    }

    @NotNull
    public String getSummary() {
        return summary;
    }

    @NotNull
    public String getMessage() {
        return message;
    }

    @Nullable
    public Exception getWarning() {
        return warning;
    }

    @NotNull
    public Date getWhen() {
        return when;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public Collection<VirtualFile> getAffectedFiles() {
        return affectedFiles;
    }

    @Nullable
    public Consumer<HotfixGate> getHotfix() {
        return hotfix;
    }


    /*
    public void addToMessages() {
        // See AbstractVcsHelperImpl and AbstractVcsHelper
        // tab name VcsBundle.message("message.title.annotate")
        // this requires storing a project with the warning.

        AbstractVcsHelper.getInstance(project).showError(x, VcsBundle.message("message.title.annotate"));
    }
    */
}
