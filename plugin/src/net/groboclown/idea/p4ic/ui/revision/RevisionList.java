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

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.table.JBTable;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RevisionList {
    private final VirtualFile file;
    private JTable myRevisions;
    private JTextField myManualRev;
    private JPanel myRootPanel;
    private RevisionModel revisionModel;
    private ValidationInfo initializationError;


    public RevisionList(@NotNull final VirtualFile file, @NotNull final P4Vcs vcs) {
        this.file = file;

        myRevisions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myRevisions.setRowSelectionAllowed(true);
        myRevisions.setColumnSelectionAllowed(false);

        String error = revisionModel.initialize(vcs, file);
        if (error != null) {
            initializationError = new ValidationInfo(error, myRootPanel);
        }
    }


    @Nullable
    public ValidationInfo validate() {
        if (initializationError != null) {
            return initializationError;
        }
        String manual = myManualRev.getText();
        if (manual != null) {
            manual = manual.trim();
            if (manual.length() > 0) {
                try {
                    Integer.parseInt(manual);
                } catch (NumberFormatException e) {
                    return new ValidationInfo(P4Bundle.message("revision.list.invalid-rev", manual));
                }
            }
        }
        if (myRevisions.getSelectedRow() < 0) {
            // nothing selected, and manual entry is empty
            return new ValidationInfo(P4Bundle.message("revision.list.nothing"));
        }
        return null;
    }


    public JPanel getRootPanel() {
        return myRootPanel;
    }


    @Nullable
    public VcsRevisionNumber getSelectedRevision() {
        if (validate() != null) {
            return null;
        }
        String manual = myManualRev.getText();
        if (manual != null) {
            manual = manual.trim();
            if (manual.length() > 0) {
                try {
                    return new VcsRevisionNumber.Int(Integer.parseInt(manual));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return revisionModel.getRevisionAt(myRevisions.getSelectedRow());
    }


    private void createUIComponents() {
        // place custom component creation code here
        revisionModel = new RevisionModel();
        myRevisions = new JBTable(revisionModel);
    }
}
