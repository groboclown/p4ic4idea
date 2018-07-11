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

package net.groboclown.p4plugin.ui.submit;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.p4.server.api.values.JobStatus;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.impl.values.P4JobImpl;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SubmitPanel {
    private SubmitModel model;
    private JPanel myRoot;
    private JTable myJobTable;
    private JTextField myJobIdField;
    private JButton myRemoveButton;
    private JButton myBrowseButton;
    private JButton myAddButton;
    private JComboBox<JobStatus> myResolveState;

    public SubmitPanel(SubmitModel model) {
        this.model = model;
        $$$setupUI$$$();
        model.addListener(this::loadModelState);

        myAddButton.setEnabled(false);
        myAddButton.addActionListener((e) -> {
            final String text = myJobIdField.getText();
            if (text != null && !text.trim().isEmpty()) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    P4Job job = model.findJob(text.trim());
                    if (job == null) {
                        job = new P4JobImpl(text.trim(), P4Bundle.getString("job.panel.job-not-on-server"), null);
                    }
                    model.addJob(job);
                });
            }
        });

        myRemoveButton.setEnabled(false);
        myRemoveButton.addActionListener((e) -> {
            List<P4Job> jobs = model.getListedJobs();
            List<P4Job> removed = new ArrayList<>(jobs.size());
            for (int i = 0; i < jobs.size(); i++) {
                if (myJobTable.getSelectionModel().isSelectedIndex(i)) {
                    removed.add(jobs.get(i));
                }
            }
            model.removeJobs(removed);
        });

        myBrowseButton.setEnabled(false);
        myBrowseButton.addActionListener((e) -> {
            final String text = myJobIdField.getText();
            if (text != null && !text.trim().isEmpty()) {
                browseJobs(text);
            }
        });

        myJobIdField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                boolean enabled = myJobIdField.getText() != null && !myJobIdField.getText().isEmpty();
                myAddButton.setEnabled(enabled);
                myBrowseButton.setEnabled(enabled);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                boolean enabled = myJobIdField.getText() != null && !myJobIdField.getText().isEmpty();
                myAddButton.setEnabled(enabled);
                myBrowseButton.setEnabled(enabled);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                boolean enabled = myJobIdField.getText() != null && !myJobIdField.getText().isEmpty();
                myAddButton.setEnabled(enabled);
                myBrowseButton.setEnabled(enabled);
            }
        });

        myJobTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        myJobTable.getSelectionModel().addListSelectionListener(
                e -> myRemoveButton.setEnabled(!myJobTable.getSelectionModel().isSelectionEmpty()));

        myResolveState.addItemListener(e -> {
            Object item = myResolveState.getSelectedItem();
            if (item instanceof JobStatus) {
                model.setStatus((JobStatus) item);
            }
        });
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            model.loadJobStatusNames()
                    .whenCompleted((names) -> {
                        myResolveState.removeAllItems();
                        for (JobStatus jobStatusName : names.getJobStatusNames()) {
                            myResolveState.addItem(jobStatusName);
                        }
                    });

        });
    }

    private void browseJobs(@NotNull String text) {
        BrowseJobsDialog.show(model.getProject(), model.searchJobs(text, 100), (jobs) -> {
            for (P4Job job : jobs) {
                model.addJob(job);
            }
        });
    }

    public JPanel getRoot() {
        return myRoot;
    }

    private void loadModelState() {
        // FIXME - is there anything to do here?
    }

    private void createUIComponents() {
        ListTableModel<P4Job> jobModel = new ListTableModel<>(
                new ColumnInfo<P4Job, String>(P4Bundle.getString("submit.job.table.column.name")) {
                    @Nullable
                    @Override
                    public String valueOf(P4Job p4Job) {
                        return p4Job == null ? null : p4Job.getJobId();
                    }
                },
                new ColumnInfo<P4Job, String>(P4Bundle.getString("submit.job.table.column.description")) {
                    @Nullable
                    @Override
                    public String valueOf(P4Job p4Job) {
                        return p4Job == null ? null : p4Job.getDescription();
                    }
                }
        );
        model.setJobModel(jobModel);
        myJobTable = new JBTable(jobModel);
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
        myRoot = new JPanel();
        myRoot.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1, true, false));
        myRoot.setBorder(BorderFactory.createTitledBorder(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("submit.job.title")));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(31);
        myRoot.add(scrollPane1,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        myJobTable.setAutoResizeMode(3);
        myJobTable.setFillsViewportHeight(false);
        scrollPane1.setViewportView(myJobTable);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FormLayout(
                "fill:d:noGrow,left:4dlu:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow",
                "center:d:grow,top:4dlu:noGrow,center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        myRoot.add(panel1,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("job.panel.id"));
        label1.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("job.panel.id.tooltip"));
        CellConstraints cc = new CellConstraints();
        panel1.add(label1, cc.xy(1, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        myJobIdField = new JTextField();
        myJobIdField.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("job.panel.id.tooltip"));
        panel1.add(myJobIdField, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        myAddButton = new JButton();
        this.$$$loadButtonText$$$(myAddButton,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("job.panel.add"));
        myAddButton.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("job.panel.add.tooltip"));
        panel1.add(myAddButton, cc.xy(5, 3));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("job.panel.resolve"));
        label2.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("job.panel.resolve.tooltip"));
        panel1.add(label2, cc.xy(1, 5));
        myResolveState = new JComboBox();
        myResolveState.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("job.panel.resolve.tooltip"));
        panel1.add(myResolveState, cc.xy(3, 5));
        myBrowseButton = new JButton();
        this.$$$loadButtonText$$$(myBrowseButton,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("job.panel.browse"));
        myBrowseButton.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("job.panel.browse.tooltip"));
        panel1.add(myBrowseButton, cc.xy(7, 3));
        myRemoveButton = new JButton();
        this.$$$loadButtonText$$$(myRemoveButton,
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("job.panel.remove"));
        myRemoveButton.setToolTipText(
                ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle").getString("job.panel.remove.tooltip"));
        panel1.add(myRemoveButton, cc.xy(7, 5));
        final Spacer spacer1 = new Spacer();
        myRoot.add(spacer1,
                new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        label1.setLabelFor(myJobIdField);
        label2.setLabelFor(myResolveState);
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
        return myRoot;
    }
}
