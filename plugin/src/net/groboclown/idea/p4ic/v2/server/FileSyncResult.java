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

package net.groboclown.idea.p4ic.v2.server;

import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.core.file.FileAction;
import net.groboclown.idea.p4ic.v2.history.P4RevisionNumber;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ClientFileMapping;
import org.jetbrains.annotations.NotNull;

public class FileSyncResult {
    private final P4ClientFileMapping file;
    private final FileAction fileAction;
    private final P4RevisionNumber rev;

    public FileSyncResult(@NotNull P4ClientFileMapping file,
            @NotNull FileAction clientAction, final int revision) {
        assert file.getLocalFilePath() != null;
        this.file = file;
        this.fileAction = clientAction;
        this.rev = new P4RevisionNumber(file.getDepotPath(), file.getDepotPath(), revision);
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    public FilePath getFilePath() {
        return file.getLocalFilePath();
    }



    @NotNull
    public FileAction getFileAction() {
        return fileAction;
    }


    @NotNull
    public P4RevisionNumber getRevisionNumber() {
        return rev;
    }
}
