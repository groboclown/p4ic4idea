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

package net.groboclown.idea.p4ic.v2.file;

import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileExtensions {
    private final P4Vcs vcs;
    private final AlertManager alerts;
    private final Lock vfsLock = new ReentrantLock();


    public FileExtensions(@NotNull P4Vcs vcs, @NotNull AlertManager alerts) {
        this.vcs = vcs;
        this.alerts = alerts;
    }

    @NotNull
    public P4EditFileProvider createEditFileProvider() {
        return new P4EditFileProvider(vcs, vfsLock);
    }


    @NotNull
    public P4RollbackEnvironment createRollbackEnvironment() {
        return new P4RollbackEnvironment(vcs, vfsLock);
    }


    @NotNull
    public P4VFSListener createVcsVFSListener() {
        return new P4VFSListener(vcs, alerts, vfsLock);
    }
}
