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

package net.groboclown.idea.p4ic.v2.ui.alerts;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import net.groboclown.idea.p4ic.v2.server.connection.CriticalErrorHandler;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Date;

public class ConfigPanelErrorHandler implements CriticalErrorHandler {
    private final Project project;
    private final String title;
    private final String message;

    public ConfigPanelErrorHandler(@NotNull Project project,
            @NotNull @Nls final String title,
            @NotNull @Nls final String message) {
        this.project = project;
        this.title = title;
        this.message = message;
    }


    @Override
    public void handleError(@NotNull final Date when) {
        // using ApplicationManager.getApplication().invokeLater
        // will cause the dialog to delay displaying until all
        // other active gui are dismissed.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Messages.showMessageDialog(project,
                        message,
                        title,
                        Messages.getErrorIcon());
            }
        });
    }
}
