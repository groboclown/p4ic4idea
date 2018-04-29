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

package net.groboclown.p4plugin.ui.history;

import com.intellij.openapi.vcs.FileStatus;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListJob;
import net.groboclown.idea.p4ic.v2.history.P4CommittedChangeListDetails;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;

public class ChangelistDetails {
    private JPanel root;
    private JTextPane descriptionText;
    private JLabel changelistTitle;
    private ListTableModel<P4ChangeListJob> jobsTableModel;
    private JTable jobsTable;
    private JPanel jobsPanel;
    private JTextField authorField;
    private JTextField dateField;
    private ListTableModel<P4CommittedChangeListDetails.FileChange> filesTableModel;
    private JTable filesTable;
    private JPanel filesPanel;

    ChangelistDetails(@NotNull P4CommittedChangeListDetails value) {
        $$$setupUI$$$();
        changelistTitle.setText(P4Bundle.message("changelist.details.title",
                value.getChangeListId() == null ? "" : value.getChangeListId().asString()));
        descriptionText.setText(value.getComment());
        authorField.setText(value.getAuthor());
        dateField.setText(DateFormatUtil.formatDateTime(value.getDate()));
        Collection<P4ChangeListJob> jobs = value.getJobs();
        if (jobs.isEmpty()) {
            jobsPanel.setVisible(false);
        } else {
            jobsTableModel.setItems(new ArrayList<P4ChangeListJob>(jobs));
            jobsPanel.add(jobsTable.getTableHeader(), BorderLayout.NORTH);
        }
        filesTableModel.setItems(new ArrayList<P4CommittedChangeListDetails.FileChange>(value.getChanges()));
        filesPanel.add(filesTable.getTableHeader(), BorderLayout.NORTH);
        root.doLayout();
    }

    @NotNull
    public JPanel getRoot() {
        return root;
    }

    private void createUIComponents() {
        jobsTableModel = new ListTableModel<P4ChangeListJob>(
                new ColumnInfo(P4Bundle.getString("changelist.details.jobs.id.title")) {
                    @Nullable
                    @Override
                    public Object valueOf(Object o) {
                        return o == null
                                ? null
                                : o instanceof P4ChangeListJob
                                        ? ((P4ChangeListJob) o).getJobId()
                                        : "";
                    }
                },
                new ColumnInfo(P4Bundle.getString("changelist.details.jobs.desc.title")) {
                    @Nullable
                    @Override
                    public Object valueOf(Object o) {
                        return o == null
                                ? null
                                : o instanceof P4ChangeListJob
                                        ? ((P4ChangeListJob) o).getDescription()
                                        : "";
                    }
                },
                new ColumnInfo(P4Bundle.getString("changelist.details.jobs.status.title")) {
                    @Nullable
                    @Override
                    public Object valueOf(Object o) {
                        return o == null
                                ? null
                                : o instanceof P4ChangeListJob
                                        ? ((P4ChangeListJob) o).getStatus()
                                        : "";
                    }
                }
        );
        jobsTable = new JBTable(jobsTableModel);
        jobsTable.setAutoCreateRowSorter(true);

        filesTableModel = new ListTableModel<P4CommittedChangeListDetails.FileChange>(
                new ColumnInfo(P4Bundle.getString("changelist.details.file.action.title")) {
                    @Nullable
                    @Override
                    public Object valueOf(Object o) {
                        return o == null
                                ? null
                                : o instanceof P4CommittedChangeListDetails.FileChange
                                        ? ((P4CommittedChangeListDetails.FileChange) o).getStatus()
                                        : "";
                    }

                    @Override
                    public TableCellRenderer getCustomizedRenderer(final Object o, final TableCellRenderer
                            renderer) {
                        return new TableCellRenderer() {
                            @Override
                            public Component getTableCellRendererComponent(JTable table, Object value,
                                    boolean isSelected, boolean hasFocus, int row, int column) {
                                Component ret = renderer.getTableCellRendererComponent(table, value, isSelected,
                                        hasFocus, row, column);
                                final FileStatus status = value == null
                                        ? null
                                        : value instanceof P4CommittedChangeListDetails.FileChange
                                                ? ((P4CommittedChangeListDetails.FileChange) value).getStatus()
                                                : null;
                                if (status == null || ret == null) {
                                    return ret;
                                }
                                ret.setForeground(status.getColor());
                                return ret;
                            }
                        };
                    }
                },
                new ColumnInfo(P4Bundle.getString("changelist.details.file.path.title")) {
                    @Nullable
                    @Override
                    public Object valueOf(Object o) {
                        return o == null
                                ? null
                                : o instanceof P4CommittedChangeListDetails.FileChange
                                        ? ((P4CommittedChangeListDetails.FileChange) o).getPrimaryPath()
                                        : "";
                    }
                }
        );
        filesTable = new JBTable(filesTableModel);
        filesTable.setAutoCreateRowSorter(true);
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
        root = new JPanel();
        root.setLayout(new BorderLayout(0, 0));
        final JScrollPane scrollPane1 = new JScrollPane();
        root.add(scrollPane1, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FormLayout("fill:d:grow",
                "center:p:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:d:grow"));
        scrollPane1.setViewportView(panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        CellConstraints cc = new CellConstraints();
        panel1.add(panel2, cc.xy(1, 5));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), null));
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setHorizontalScrollBarPolicy(31);
        panel2.add(scrollPane2, BorderLayout.CENTER);
        descriptionText = new JTextPane();
        Font descriptionTextFont = UIManager.getFont("Label.font");
        if (descriptionTextFont != null) {
            descriptionText.setFont(descriptionTextFont);
        }
        scrollPane2.setViewportView(descriptionText);
        changelistTitle = new JLabel();
        Font changelistTitleFont = UIManager.getFont("InternalFrame.titleFont");
        if (changelistTitleFont != null) {
            changelistTitle.setFont(changelistTitleFont);
        }
        this.$$$loadLabelText$$$(changelistTitle,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("changelist.details.title"));
        panel1.add(changelistTitle, new CellConstraints(1, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT,
                new Insets(4, 4, 4, 4)));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:d:grow",
                "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel1.add(panel3, cc.xy(1, 3));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("changelist.details.author"));
        panel3.add(label1, cc.xy(1, 1));
        authorField = new JTextField();
        authorField.setEditable(false);
        panel3.add(authorField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2,
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("changelist.details.date"));
        panel3.add(label2, cc.xy(1, 3));
        dateField = new JTextField();
        dateField.setEditable(false);
        panel3.add(dateField, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        jobsPanel = new JPanel();
        jobsPanel.setLayout(new BorderLayout(0, 0));
        panel1.add(jobsPanel, cc.xy(1, 9));
        jobsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("changelist.details.jobs")));
        jobsTable.setAutoResizeMode(4);
        jobsPanel.add(jobsTable, BorderLayout.CENTER);
        filesPanel = new JPanel();
        filesPanel.setLayout(new BorderLayout(0, 0));
        panel1.add(filesPanel, cc.xy(1, 7));
        filesPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                ResourceBundle.getBundle("net/groboclown/idea/p4ic/P4Bundle").getString("changelist.details.files")));
        filesTable.setAutoResizeMode(4);
        filesPanel.add(filesTable, BorderLayout.CENTER);
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, cc.xy(1, 11, CellConstraints.DEFAULT, CellConstraints.FILL));
        label1.setLabelFor(authorField);
        label2.setLabelFor(dateField);
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
    public JComponent $$$getRootComponent$$$() {
        return root;
    }
}
