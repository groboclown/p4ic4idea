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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.JBScrollPane;
import net.groboclown.idea.p4ic.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Shows the list of warnings caused by Perforce operations.
 * It should be
 */
public class WarningDialog extends JDialog {
    private JPanel contentPane;
    private Box warningList;
    private boolean disposed = false;
    private WarningLineListener listener = new WarningLineListener();


    public WarningDialog(@Nullable Frame owner) {
        super(owner, P4Bundle.message("warning-dialog.title"));

        contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());

        contentPane.add(new JLabel(P4Bundle.message("warning-dialog.description")), BorderLayout.NORTH);

        final JPanel buttonPanel = new JPanel();
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton buttonOK = new JButton();
        buttonPanel.add(buttonOK);
        buttonOK.setText(P4Bundle.getString("ui.OK"));
        buttonOK.setEnabled(false);
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        // TODO this should really be a JBList for performance reasons, but that
        // would involve some complicated expand logic handling.
        warningList = Box.createVerticalBox();
        contentPane.add(new JBScrollPane(warningList), BorderLayout.CENTER);
    }


    public void addWarningMessage(@NotNull final WarningMessage message) {
        if (isDisposed()) {
            throw new IllegalStateException("dialog is disposed");
        }
        ApplicationManager.getApplication().assertIsDispatchThread();
        warningList.add(new WarningLine(message, listener).getRootPanel());
        warningList.doLayout();
    }


    public void showDialog() {
        pack();
        //final Dimension bounds = getSize();
        //final Rectangle parentBounds = getOwner().getBounds();
        //setLocation(parentBounds.x + (parentBounds.width - bounds.width) / 2,
        //        parentBounds.y + (parentBounds.height - bounds.height) / 2);
        setVisible(true);

    }


    public boolean isDisposed() {
        return disposed;
    }


    @Override
    public void dispose() {
        this.disposed = true;
        super.dispose();
    }


    private class WarningLineListener implements WarningLineRemovedListener {
        @Override
        public void warningRemoved(@NotNull final WarningLine warningLine, @NotNull final WarningMessage message) {
            warningList.remove(warningLine.getRootPanel());
            warningList.doLayout();
        }
    }
}
