/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.groboclown.idea.p4ic.ui.checkin;

import com.intellij.icons.AllIcons.Actions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListJob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

public class P4SubmitPanel {
    private static final Logger LOG = Logger.getInstance(P4SubmitPanel.class);

    private JTable myJobTable;
    private JButton myAddJobButton;
    private JButton myBrowseButton;
    private JButton myRemoveButton;
    private JTextField myJobIdField;
    private JobTableModel jobTableModel;
    // JDK 1.6 does not have the generic form of the combo box.
    private JComboBox/*<String>*/ myJobStatus;
    private JPanel myRootPanel;
    private JLabel myAssociateJobExpander;
    private JPanel myExpandedPanel;
    private JLabel myJobsDisabledLabel;
    private DefaultComboBoxModel/*<String>*/ jobStatusModel;

    private final SubmitContext context;

    @NotNull
    private Set<P4ChangeListJob> lastJobList = Collections.emptySet();

    private boolean expandState = false;


    public P4SubmitPanel(final SubmitContext context) {
        this.context = context;


        // UI setup code - compiler will inject the initialization from the
        // form.

        $$$setupUI$$$();
        // Set the visibility of the expanded panel first.
        myExpandedPanel.setVisible(false);

        myJobTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myJobTable.setRowSelectionAllowed(true);
        myJobTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    // Something in this call is disabling
                    // the currently selected item in the table.
                    updateStatus();
                }
            }
        });
        myAddJobButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                String jobId = getJobIdFieldText();
                if (jobId != null) {
                    if (context.addJobId(jobId) != null) {
                        // job was added successfully
                        myJobIdField.setText("");
                        jobTableModel.fireTableDataChanged();
                    } else {
                        Messages.showMessageDialog(context.getProject(),
                                P4Bundle.message("submit.job.error.notfound.message", jobId),
                                P4Bundle.getString("submit.job.error.notfound.title"),
                                Messages.getErrorIcon());
                    }
                }
            }
        });
        myRemoveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                int rowId = myJobTable.getSelectedRow();
                if (rowId >= 0 && rowId < context.getJobs().size()) {
                    context.removeJob(context.getJobs().get(rowId));
                    jobTableModel.fireTableDataChanged();
                }
            }
        });
        myBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                // TODO replace with real action
                Messages.showMessageDialog(context.getProject(),
                        "Browsing for jobs is not yet implemented",
                        "Not implemented",
                        Messages.getErrorIcon());
            }
        });
        myJobIdField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                updateStatus();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                updateStatus();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                updateStatus();
            }
        });
        myJobStatus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Object selected = myJobStatus.getSelectedItem();
                if (selected != null) {
                    context.setSubmitStatus(selected.toString());
                }
            }
        });
        myAssociateJobExpander.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                expandState = !expandState;
                updateStatus();
            }
        });
        myAssociateJobExpander.setIcon(Actions.Right);
        myJobsDisabledLabel.setVisible(false);
    }

    @NotNull
    public JPanel getRootPanel() {
        return myRootPanel;
    }


    public void updateStatus() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean needsLayout = false;

                final boolean jobsUpdated = !(
                        lastJobList.containsAll(context.getJobs()) &&
                                context.getJobs().containsAll(lastJobList));

                if (jobsUpdated) {
                    final int jobSelectionIndex = myJobTable.getSelectedRow();
                    final P4ChangeListJob jobTableSelection;
                    if (jobSelectionIndex >= 0 && jobSelectionIndex < context.getJobs().size()) {
                        jobTableSelection = context.getJobs().get(jobSelectionIndex);
                    } else {
                        jobTableSelection = null;
                    }
                    jobTableModel.fireTableDataChanged();
                    if (jobTableSelection != null) {
                        int rowId = context.getJobs().indexOf(jobTableSelection);
                        if (rowId >= 0 && rowId != jobSelectionIndex) {
                            myJobTable.setRowSelectionInterval(rowId, rowId);
                        }
                    }
                    lastJobList = new HashSet<P4ChangeListJob>(context.getJobs());
                }

                if (context.isJobAssociationValid()) {
                    myJobTable.setEnabled(true);
                    myAddJobButton.setEnabled(getJobIdFieldText() != null);
                    myRemoveButton.setEnabled(myJobTable.getSelectedRow() >= 0);

                    // TODO change to "true" when implemented
                    myBrowseButton.setEnabled(false);

                    myJobStatus.setEnabled(true);
                    final Object selectedJob = myJobStatus.getSelectedItem();
                    jobStatusModel.removeAllElements();
                    boolean foundSelected = false;
                    for (String status : context.getJobStatuses()) {
                        jobStatusModel.addElement(status);
                        if (status.equals(selectedJob)) {
                            foundSelected = true;
                        }
                    }
                    if (foundSelected) {
                        myJobStatus.setSelectedItem(selectedJob);
                    } else {
                        // The P4 default job status.
                        // if "closed" is not in the list, it will simply
                        // be rejected, and no error is thrown.
                        myJobStatus.setSelectedItem("closed");
                    }
                    if (myJobsDisabledLabel.isVisible()) {
                        myJobsDisabledLabel.setVisible(false);
                        needsLayout = true;
                    }
                } else {
                    myJobTable.setEnabled(false);
                    myAddJobButton.setEnabled(false);
                    myRemoveButton.setEnabled(false);
                    myBrowseButton.setEnabled(false);
                    myJobIdField.setEnabled(false);
                    myJobStatus.setEnabled(false);
                    if (!myJobsDisabledLabel.isVisible()) {
                        myJobsDisabledLabel.setVisible(true);
                        needsLayout = true;
                    }
                }

                if (expandState) {
                    if (!Actions.Down.equals(myAssociateJobExpander.getIcon())) {
                        myAssociateJobExpander.setIcon(Actions.Down);
                        myExpandedPanel.setVisible(true);
                        needsLayout = true;
                    }
                } else {
                    if (!Actions.Right.equals(myAssociateJobExpander.getIcon())) {
                        myAssociateJobExpander.setIcon(Actions.Right);
                        myExpandedPanel.setVisible(false);
                        needsLayout = true;
                    }
                }

                if (needsLayout) {
                    myRootPanel.doLayout();
                }
            }
        });
    }


    private void createUIComponents() {
        // place custom component creation code here
        jobTableModel = new JobTableModel();
        myJobTable = new JBTable(jobTableModel);

        jobStatusModel = new DefaultComboBoxModel();
        myJobStatus = new ComboBox(jobStatusModel);
    }


    @Nullable
    private String getJobIdFieldText() {
        String text = myJobIdField.getText();
        if (text == null || text.length() <= 0) {
            return null;
        }
        return text;
    }


    private static final String[] COLUMN_NAMES = {
            P4Bundle.getString("submit.job.table.column.name"),
            //P4Bundle.getString("submit.job.table.column.assignee"),
            P4Bundle.getString("submit.job.table.column.description"),
    };

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
        myRootPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        myExpandedPanel = new JPanel();
        myExpandedPanel.setLayout(new GridLayoutManager(3, 4, new Insets(0, 0, 0, 0), -1, -1));
        myExpandedPanel.setVisible(true);
        myRootPanel.add(myExpandedPanel,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myExpandedPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), null));
        final JScrollPane scrollPane1 = new JScrollPane();
        myExpandedPanel.add(scrollPane1,
                new GridConstraints(0, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null,
                        0, false));
        myJobTable.setCellSelectionEnabled(false);
        myJobTable.setColumnSelectionAllowed(false);
        myJobTable.setFillsViewportHeight(true);
        myJobTable.setPreferredScrollableViewportSize(new Dimension(450, 200));
        myJobTable.setShowVerticalLines(false);
        myJobTable.setSurrendersFocusOnKeystroke(true);
        scrollPane1.setViewportView(myJobTable);
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("submit.job.id"));
        myExpandedPanel.add(label1,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        myJobIdField = new JTextField();
        myJobIdField.setToolTipText(ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("job.id"));
        myExpandedPanel.add(myJobIdField,
                new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                        new Dimension(150, -1), null, 0, false));
        myAddJobButton = new JButton();
        myAddJobButton.setEnabled(false);
        this.$$$loadButtonText$$$(myAddJobButton,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("submit.job.add.button"));
        myAddJobButton.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("job.panel.add"));
        myExpandedPanel.add(myAddJobButton,
                new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myBrowseButton = new JButton();
        myBrowseButton.setEnabled(false);
        this.$$$loadButtonText$$$(myBrowseButton,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("submit.job.browse.button"));
        myBrowseButton.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("job.panel.browse"));
        myExpandedPanel.add(myBrowseButton,
                new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myRemoveButton = new JButton();
        myRemoveButton.setEnabled(false);
        this.$$$loadButtonText$$$(myRemoveButton,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("submit.job.remove.button"));
        myRemoveButton.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("job.panel.remove"));
        myExpandedPanel.add(myRemoveButton,
                new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myExpandedPanel.add(myJobStatus,
                new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("submit.job.status"));
        myExpandedPanel.add(label2,
                new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        myAssociateJobExpander = new JLabel();
        this.$$$loadLabelText$$$(myAssociateJobExpander,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("submit.job.title"));
        myAssociateJobExpander.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("associate.jobs.expand"));
        myRootPanel.add(myAssociateJobExpander,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final Spacer spacer1 = new Spacer();
        myRootPanel.add(spacer1,
                new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myJobsDisabledLabel = new JLabel();
        myJobsDisabledLabel.setForeground(new Color(-65536));
        this.$$$loadLabelText$$$(myJobsDisabledLabel,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("submit.job.not-enabled"));
        myRootPanel.add(myJobsDisabledLabel,
                new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        label1.setLabelFor(myJobIdField);
        label2.setLabelFor(myJobStatus);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return myRootPanel;
    }


    private class JobTableModel extends AbstractTableModel {
        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            final P4ChangeListJob job = context.getJobs().get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return job.getJobId();
                case 1:
                    return job.getDescription() == null ? "" : job.getDescription();
                default:
                    return "";
            }
        }

        @Override
        public int getRowCount() {
            return context.getJobs().size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }
    }
}
