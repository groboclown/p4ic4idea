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

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RevisionDialog extends DialogWrapper {
    private final RevisionList revisionList;
    private final VirtualFile file;


    public RevisionDialog(@NotNull P4Vcs vcs, @NotNull VirtualFile file) {
        super(vcs.getProject(), true, IdeModalityType.PROJECT);
        this.file = file;
        this.revisionList = new RevisionList(file, vcs);
    }


    @Nullable
    public static VcsRevisionNumber requestRevision(@NotNull P4Vcs vcs, @NotNull VirtualFile file) {
        if (vcs.getProject().isDisposed()) {
            return null;
        }

        final RevisionDialog dialog = new RevisionDialog(vcs, file);
        if (dialog.showAndGet()) {
            return dialog.getSelectedRevision();
        } else {
            return null;
        }
    }

    /**
     * Validates user input and returns <code>null</code> if everything is fine
     * or validation description with component where problem has been found.
     *
     * @return <code>null</code> if everything is OK or validation descriptor
     */
    @Nullable
    protected ValidationInfo doValidate() {
        return revisionList.validate();
    }

    @Nullable
    protected JComponent createTitlePane() {
        return new JLabel(file.getCanonicalPath());
    }


    @Nullable
    @Override
    protected JComponent createNorthPanel() {
        return revisionList.getRootPanel();
    }


    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return null;
    }

    private VcsRevisionNumber getSelectedRevision() {
        return revisionList.getSelectedRevision();
    }
}
