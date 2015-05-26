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

import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.history.P4FileRevision;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.AbstractTableModel;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class RevisionModel extends AbstractTableModel {
    private static final int COL_REV = 0;
    private static final int COL_DATE = 1;
    private static final int COL_AUTH = 2;
    private static final int COL_COMMENT = 3;
    private static final int COL_COUNT = 4;

    private boolean hasRevisions = false;
    private final Vector<P4FileRevision> revisions = new Vector<P4FileRevision>();

    // TODO make configurable
    private static final int REVISION_PAGE_SIZE = 30;


    public boolean isConnected() {
        return hasRevisions;
    }


    @Nullable
    public String initialize(final @NotNull P4Vcs vcs, final @NotNull VirtualFile file) {
        final Client client = vcs.getClientFor(file);
        if (client == null || client.isWorkingOffline()) {
            hasRevisions = false;
            return P4Bundle.getString("revision.list.notconnected");
        }
        try {
            final List<P4FileInfo> p4infoList = client.getServer().getVirtualFileInfo(Collections.singleton(file));
            if (p4infoList.isEmpty()) {
                // can't find file
                hasRevisions = false;
                return P4Bundle.getString("revision.list.nosuchfile");
            }
            final P4FileInfo fileInfo = p4infoList.get(0);
            revisions.addAll(client.getServer().getRevisionHistory(fileInfo, REVISION_PAGE_SIZE));
            hasRevisions = true;
            return null;
        } catch (VcsException e) {
            e.printStackTrace();
            hasRevisions = false;
            return e.getMessage();
        }
    }

    @Nullable
    public VcsRevisionNumber getRevisionAt(int index) {
        if (index == revisions.size()) {
            return new VcsRevisionNumber.Int(0);
        }
        if (index < 0 || index > revisions.size()) {
            return null;
        }
        final P4FileRevision rev = revisions.get(index);
        if (rev == null) {
            return null;
        }
        return rev.getRevisionNumber();
    }


    @Override
    public String getColumnName(int column) {
        switch (column) {
            case COL_REV:
                return P4Bundle.getString("revision.list.rev");
            case COL_DATE:
                return P4Bundle.getString("revision.list.datetime");
            case COL_AUTH:
                return P4Bundle.getString("revision.list.author");
            case COL_COMMENT:
                return P4Bundle.getString("revision.list.comment");
            default:
                throw new IllegalArgumentException("invalid column " + column);
        }
    }


    @Override
    public int getColumnCount() {
        return COL_COUNT;
    }


    @Override
    public int getRowCount() {
        return revisions.size() + 1;
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        if (rowIndex < 0 || rowIndex > revisions.size()) {
            return "";
        }

        // TODO if not all revisions were loaded, add an extra "..." row.

        if (rowIndex == revisions.size()) {
            // last revision
            switch (columnIndex) {
                case COL_REV:
                    return 0;
                case COL_DATE:
                    return "";
                case COL_AUTH:
                    return "";
                case COL_COMMENT:
                    return "<before checkin>";
                default:
                    return "";
            }
        }

        P4FileRevision rev = revisions.get(rowIndex);
        if (rev == null) {
            return "";
        }

        switch (columnIndex) {
            case COL_REV:
                return rev.getRevisionNumber();
            case COL_DATE:
                return rev.getRevisionDate();
            case COL_AUTH:
                return rev.getAuthor();
            case COL_COMMENT:
                return rev.getCommitMessage();
            default:
                return "";
        }
    }
}
