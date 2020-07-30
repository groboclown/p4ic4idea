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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.VcsDockedComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.ResourceBundle;

public class ChangelistDetails {
    private JPanel root;
    private JTextArea myDescription;
    private JTable myFiles;
    private JTable myJobs;
    private JPanel myNoFilesPanel;
    private JPanel myFilesPanel;
    private JPanel myJobsPanel;
    private JPanel myNoJobsPanel;
    private JLabel myDate;
    private JLabel myChangelistId;
    private JLabel myAuthor;
    private JLabel myClientname;
    private ListTableModel<P4RemoteChangelist.CommittedFile> filesModel;
    private ListTableModel<P4Job> jobsModel;


    public static void showDocked(@NotNull Project project, @NotNull P4RemoteChangelist changelist) {
        ApplicationManager.getApplication().invokeLater(() ->
                VcsDockedComponent.getInstance(project).addVcsTab(
                        P4Bundle.message("changelist.details.tab-title",
                                // Explicit toString to strip localization number formatting.
                                Integer.toString(changelist.getChangelistId().getChangelistId())),
                        new ChangelistDetails(changelist).getRoot(),
                        true, true));
    }


    public ChangelistDetails(@NotNull P4RemoteChangelist changelist) {
        $$$setupUI$$$();
        this.myDescription.setText(changelist.getSummary().getComment());
        Date date = changelist.getSubmittedDate();
        this.myDate.setText(date == null ? null : DateFormatUtil.formatDateTime(date));
        this.myChangelistId.setText(Integer.toString(changelist.getChangelistId().getChangelistId()));
        this.myAuthor.setText(changelist.getUsername());
        this.myClientname.setText(changelist.getClientname());
        if (changelist.getAttachedJobs().isEmpty()) {
            myNoJobsPanel.setVisible(true);
            myJobsPanel.setVisible(false);
        } else {
            myJobsPanel.setVisible(true);
            myNoJobsPanel.setVisible(false);
            myJobsPanel.add(myJobs.getTableHeader(), BorderLayout.NORTH);
            jobsModel.addRows(changelist.getAttachedJobs());

            // TODO add context menu or toolbar action to view the job details.
        }
        if (changelist.getFiles().isEmpty()) {
            myNoFilesPanel.setVisible(true);
            myFilesPanel.setVisible(false);
        } else {
            myFilesPanel.setVisible(true);
            myNoFilesPanel.setVisible(false);
            myFilesPanel.add(myFiles.getTableHeader(), BorderLayout.NORTH);
            filesModel.addRows(changelist.getFiles());

            // TODO add context menu or toolbar action to view the file revision details.
        }


        root.validate();
        root.doLayout();
    }

    public JPanel getRoot() {
        return root;
    }

    private void createUIComponents() {
        filesModel = new ListTableModel<>(
                new ColumnInfo<P4RemoteChangelist.CommittedFile, String>(
                        P4Bundle.getString("changelist.details.file.action.title")) {
                    @Nullable
                    @Override
                    public String valueOf(P4RemoteChangelist.CommittedFile f) {
                        // TODO the displayed text should be localized.
                        return f == null ? null :
                                StringUtil.capitalize(f.getAction().toString().toLowerCase().replace('_', ' '));
                    }

                    // Unfortunately, this call seems to be ignored.
                    /*
                    @Override
                    public int getWidth(JTable table) {
                        // TODO should be a constant
                        double max = table.getFont().getStringBounds(getName(), null).getWidth();
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Width of title: " + max);
                        }
                        for (P4FileAction value : P4FileAction.values()) {
                            String k = StringUtil.capitalize(value.toString().toLowerCase());
                            max = Math.max(max, table.getFont().getStringBounds(k, null).getWidth());
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Width of " + value + ": " + table.getFont().getStringBounds(k, null)
                                        .getWidth());
                            }
                        }
                        return (int) Math.ceil(max);
                    }
                    */

                    /* TODO think about setting the color
                    @Override
                    public TableCellRenderer getCustomizedRenderer(final P4CommittedChangelist.CommittedFile f,
                            final TableCellRenderer renderer) {
                        return (table, value, isSelected, hasFocus, row, column) -> {
                            final Component ret = renderer.getTableCellRendererComponent(table, value,
                                    isSelected, hasFocus, row, column);
                            if (ret == null) {
                                return ret;
                            }
                            final P4FileAction action =
                                    value == null ? P4FileAction.UNKNOWN :
                                    ((P4CommittedChangelist.CommittedFile) value).getAction();
                            ret.setForeground(action.getColor());
                            return ret;
                        };
                    }
                    */
                },
                new ColumnInfo<P4RemoteChangelist.CommittedFile, String>(
                        P4Bundle.getString("changelist.details.file.path.title")) {
                    @Nullable
                    @Override
                    public String valueOf(P4RemoteChangelist.CommittedFile f) {
                        return f == null ? null : f.getDepotPath().getDisplayName();
                    }
                }
                /*, TODO get the right revision number; as it stands now, this is always -1.
                new ColumnInfo<P4RemoteChangelist.CommittedFile, Integer>(P4Bundle.getString(
                        "changelist.details.file.rev.title")) {
                    @Nullable
                    @Override
                    public Integer valueOf(P4RemoteChangelist.CommittedFile committedFile) {
                        return committedFile == null ? null : committedFile.getRevision();
                    }
                }
                */
        );
        myFiles = new JBTable(filesModel);
        myFiles.setAutoCreateRowSorter(true);


        jobsModel = new ListTableModel<>(
                new ColumnInfo<P4Job, String>(P4Bundle.getString("changelist.details.jobs.id.title")) {
                    @Nullable
                    @Override
                    public String valueOf(P4Job j) {
                        return j == null ? null : j.getJobId();
                    }
                },
                new ColumnInfo<P4Job, String>(P4Bundle.getString("changelist.details.jobs.desc.title")) {
                    @Nullable
                    @Override
                    public String valueOf(P4Job j) {
                        return j == null ? null : j.getDescription();
                    }
                }
                /*, Think about adding job status.
                new ColumnInfo<P4Job, String>(P4Bundle.getString("changelist.details.jobs.status.title")) {
                    @Nullable
                    @Override
                    public String valueOf(P4Job j) {
                        return j == null ? null : j.;
                    }
                }
                */
        );
        myJobs = new JBTable(jobsModel);
        myJobs.setAutoCreateRowSorter(true);
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
        root.setLayout(new FormLayout("fill:d:grow",
                "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:d:grow"));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FormLayout(
                "fill:d:noGrow,left:4dlu:noGrow,fill:max(d;4px):grow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:max(d;4px):grow",
                "center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        CellConstraints cc = new CellConstraints();
        root.add(panel1, cc.xy(1, 1));
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, Font.BOLD, -1, label1.getFont());
        if (label1Font != null) {
            label1.setFont(label1Font);
        }
        this.$$$loadLabelText$$$(label1,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "changelist.details.title"));
        panel1.add(label1, cc.xy(1, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        myDate = new JLabel();
        myDate.setText("Label");
        panel1.add(myDate, cc.xy(3, 5));
        myChangelistId = new JLabel();
        myChangelistId.setText("Label");
        panel1.add(myChangelistId, cc.xy(3, 3));
        myClientname = new JLabel();
        myClientname.setText("Label");
        panel1.add(myClientname, cc.xy(7, 5));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "changelist.details.author"));
        label2.setToolTipText(this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "changelist.details.author.tooltip"));
        panel1.add(label2, cc.xy(5, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "changelist.details.date"));
        panel1.add(label3, cc.xy(1, 5, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        myAuthor = new JLabel();
        myAuthor.setText("Label");
        panel1.add(myAuthor, cc.xy(7, 3));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "changelist.details.client"));
        panel1.add(label4, cc.xy(5, 5, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        root.add(panel2, cc.xy(1, 3));
        myDescription = new JTextArea();
        panel2.add(myDescription, BorderLayout.CENTER);
        myFilesPanel = new JPanel();
        myFilesPanel.setLayout(new BorderLayout(0, 0));
        root.add(myFilesPanel, cc.xy(1, 5));
        myFilesPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "changelist.details.files"),
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JScrollPane scrollPane1 = new JScrollPane();
        myFilesPanel.add(scrollPane1, BorderLayout.CENTER);
        scrollPane1.setViewportView(myFiles);
        myJobsPanel = new JPanel();
        myJobsPanel.setLayout(new BorderLayout(0, 0));
        root.add(myJobsPanel, cc.xy(1, 9));
        myJobsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "changelist.details.jobs"),
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JScrollPane scrollPane2 = new JScrollPane();
        myJobsPanel.add(scrollPane2, BorderLayout.CENTER);
        scrollPane2.setViewportView(myJobs);
        final Spacer spacer1 = new Spacer();
        root.add(spacer1, cc.xy(1, 13, CellConstraints.DEFAULT, CellConstraints.FILL));
        myNoFilesPanel = new JPanel();
        myNoFilesPanel
                .setLayout(new FormLayout("fill:d:grow", "center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        root.add(myNoFilesPanel, cc.xy(1, 7));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "changelist.details.files.none"));
        myNoFilesPanel.add(label5, cc.xy(1, 3));
        myNoJobsPanel = new JPanel();
        myNoJobsPanel.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow"));
        root.add(myNoJobsPanel, cc.xy(1, 11));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "changelist.details.jobs.none"));
        myNoJobsPanel.add(label6, cc.xy(1, 1));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) {
            return null;
        }
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(),
                size >= 0 ? size : currentFont.getSize());
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
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
