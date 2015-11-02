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

import com.intellij.ide.errorTreeView.HotfixData;
import com.intellij.ide.errorTreeView.HotfixGate;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

public class WarningUI {
    private static final Logger LOG = Logger.getInstance(WarningUI.class);


    public static void showWarnings(@NotNull Collection<WarningMessage> warnings) {
        final Map<Project, List<WarningMessage>> sorted = sortWarningsByProject(warnings);
        for (Entry<Project, List<WarningMessage>> entry : sorted.entrySet()) {
            showWarningPanel(entry.getKey(), entry.getValue());
        }



        // The old crappy UI
        /*
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (warningDialog == null || warningDialog.isDisposed()) {
                    warningDialog = new WarningDialog(null);
                    warningDialog.showDialog();
                }
                LOG.info("loading warnings " + warnings);
                for (WarningMessage warning : warnings) {
                    warningDialog.addWarningMessage(warning);
                }
            }
        });
        */
    }

    private static void showWarningPanel(@NotNull Project project, final List<WarningMessage> warnings) {
        final AbstractVcsHelper helper = AbstractVcsHelper.getInstance(project);

        helper.showErrors(getErrorGroups(warnings), VcsBundle.message("message.title.annotate"));
    }




    @NotNull
    private static Map<Project, List<WarningMessage>> sortWarningsByProject(@NotNull Collection<WarningMessage> warnings) {
        Map<Project, List<WarningMessage>> ret = new HashMap<Project, List<WarningMessage>>();
        for (WarningMessage warning : warnings) {
            List<WarningMessage> list = ret.get(warning.getProject());
            if (list == null) {
                list = new ArrayList<WarningMessage>();
                ret.put(warning.getProject(), list);
            }
            list.add(warning);
        }
        return ret;
    }


    @SuppressWarnings("ThrowableInstanceNeverThrown")
    @NotNull
    private static Map<HotfixData, List<VcsException>> getErrorGroups(@NotNull Collection<WarningMessage> warnings) {
        Map<HotfixData, List<VcsException>> ret = new HashMap<HotfixData, List<VcsException>>();
        for (WarningMessage warning : warnings) {
            final Consumer<HotfixGate> hotfix = warning.getHotfix();
            final HotfixData data;
            if (hotfix != null) {
                data = new HotfixData(warning.getSummary(),
                        warning.getSummary(), warning.getSummary(),
                        hotfix);
            } else {
                data = null;
            }
            List<VcsException> exList = new ArrayList<VcsException>();
            VcsException baseException;
            if (warning.getWarning() != null) {
                if (warning.getWarning() instanceof VcsException) {
                    baseException = (VcsException) warning.getWarning();
                } else {
                    baseException = new P4Exception(warning.getWarning());
                }
            } else {
                baseException = new P4Exception(warning.getMessage());
            }
            if (warning.getAffectedFiles().isEmpty()) {
                if (baseException.getVirtualFile() == null) {
                    // TODO figure out a better file instance.
                    LOG.warn("No file set for exception", baseException);
                    baseException.setVirtualFile(FilePathUtil.getFilePath(new File(".")).getVirtualFile());
                }
                exList.add(baseException);
            } else if (warning.getAffectedFiles().size() == 1) {
                baseException.setVirtualFile(warning.getAffectedFiles().iterator().next());
                exList.add(baseException);
            } else {
                for (VirtualFile file : warning.getAffectedFiles()) {
                    VcsException ex = new VcsException(baseException);
                    ex.setVirtualFile(file);
                    exList.add(ex);
                }
            }
            final List<VcsException> list = ret.get(data);
            if (list == null) {
                ret.put(data, exList);
            } else {
                list.addAll(exList);
            }
        }
        return ret;
    }
}
