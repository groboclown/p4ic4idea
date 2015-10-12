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

import com.intellij.icons.AllIcons.Actions;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

public class WarningLine {
    private JPanel myDetailsExpanded;
    private JLabel myExpandoTitle;
    private JButton myClearWarningButton;
    private JTextArea myDetails;
    private JPanel myRootPanel;
    private JTextArea myStackTrace;
    private boolean detailsExpanded = false;

    public WarningLine(@NotNull final WarningMessage warningMessage,
            @NotNull final WarningLineRemovedListener listener) {
        // Make the details hidden
        $$$setupUI$$$();
        myDetailsExpanded.setVisible(false);

        myDetails.setText(warningMessage.getMessage());
        if (warningMessage.getWarning() != null) {
            StringWriter out = new StringWriter();
            PrintWriter pw = new PrintWriter(out);
            warningMessage.getWarning().printStackTrace(pw);
            pw.flush();
            myStackTrace.setText(out.toString());
        } else {
            myStackTrace.setVisible(false);
        }


        // Setup the expando title text.
        myExpandoTitle.setText(warningMessage.getSummary());
        myExpandoTitle.setIcon(Actions.Right);
        myExpandoTitle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                detailsExpanded = !detailsExpanded;
                boolean needsLayout = false;
                if (detailsExpanded) {
                    if (!Actions.Down.equals(myExpandoTitle.getIcon())) {
                        myExpandoTitle.setIcon(Actions.Down);
                        myDetailsExpanded.setVisible(true);
                        needsLayout = true;
                    }
                } else {
                    if (!Actions.Right.equals(myExpandoTitle.getIcon())) {
                        myExpandoTitle.setIcon(Actions.Right);
                        myDetailsExpanded.setVisible(false);
                        needsLayout = true;
                    }
                }

                if (needsLayout) {
                    myRootPanel.doLayout();
                }
            }
        });

        myClearWarningButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                listener.warningRemoved(WarningLine.this, warningMessage);
            }
        });
    }


    public JPanel getRootPanel() {
        return myRootPanel;
    }


    private void createUIComponents() {
        // TODO: place custom component creation code here


        myClearWarningButton = new JButton(Actions.Delete);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        myRootPanel = new JPanel();
        myRootPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        myRootPanel.add(panel1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myExpandoTitle = new JLabel();
        myExpandoTitle.setText("(title)");
        panel1.add(myExpandoTitle,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        myClearWarningButton.setText("");
        panel1.add(myClearWarningButton,
                new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myDetailsExpanded = new JPanel();
        myDetailsExpanded.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        myRootPanel.add(myDetailsExpanded,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myDetailsExpanded
                .setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), null));
        myDetails = new JTextArea();
        myDetails.setEditable(false);
        myDetails.setWrapStyleWord(true);
        myDetailsExpanded.add(myDetails,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null,
                        new Dimension(150, 30), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        myDetailsExpanded.add(scrollPane1,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null,
                        0, false));
        myStackTrace = new JTextArea();
        myStackTrace.setEditable(false);
        scrollPane1.setViewportView(myStackTrace);
        final Spacer spacer2 = new Spacer();
        myRootPanel.add(spacer2,
                new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return myRootPanel;
    }
}
