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

package net.groboclown.p4plugin.ui.swarm;

import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.p4.server.api.commands.user.ListUsersQuery;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4RemoteChangelist;
import net.groboclown.p4.server.api.values.P4User;
import net.groboclown.p4.simpleswarm.model.Review;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.ui.BooleanColumnInfo;
import net.groboclown.p4plugin.ui.SearchSelectPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SwarmReviewPanel {
    private final Project project;
    private final ClientConfig clientConfig;

    private JPanel root;
    private JTextArea reviewDescription;
    private JTable fileSelectionTable;
    private JPanel userSelectionPanel;
    private SearchSelectPanel<Reviewer> reviewersPanel;

    private ListTableModel<FileRow> fileTableModel;

    private int oldReviewId = -1;

    public SwarmReviewPanel(@NotNull final Project project, @NotNull final ClientConfig clientConfig,
            @NotNull P4ChangelistId changelist) {
        this.project = project;
        this.clientConfig = clientConfig;

        $$$setupUI$$$();
        this.fileTableModel.setSortable(true);

        userSelectionPanel.add(reviewersPanel, BorderLayout.CENTER);
    }

    public JComponent getRoot() {
        return root;
    }

    public List<Reviewer> getReviewers() {
        return reviewersPanel.getSelectedItems().collect(Collectors.toList());
    }

    public List<String> getFiles() {
        return fileTableModel.getItems().stream()
                .filter(r -> r.selected)
                .map(r -> r.name)
                .collect(Collectors.toList());
    }

    public void setReview(Review existingReview, List<P4RemoteChangelist> changelists) {
        if (existingReview == null) {
            oldReviewId = -1;
            return;
        }
        // TODO implement
        oldReviewId = existingReview.getId();
        //reviewerTableModel.setItems();
        //fileTableModel.setItems();
    }

    private void createUIComponents() {
        fileTableModel = new ListTableModel<>(
                new ColumnInfo<FileRow, String>(P4Bundle.getString("swarm-review.files.name")) {
                    @Nullable
                    @Override
                    public String valueOf(FileRow o) {
                        return o.name;
                    }

                    @Override
                    public Comparator<FileRow> getComparator() {
                        return FILE_ROW_COMPARATOR;
                    }

                    @Override
                    public String getMaxStringValue() {
                        return "MMMMMMMMMMMMMMMMMMMMMMMMM";
                    }
                },
                new BooleanColumnInfo<FileRow>(P4Bundle.getString("swarm-review.files.selected"), true) {
                    @Override
                    protected boolean booleanValue(FileRow o) {
                        return o.selected;
                    }

                    @Override
                    protected void setBooleanValue(FileRow fileRow, boolean value) {
                        fileRow.selected = value;
                    }
                }
        );
        fileSelectionTable = new JBTable(fileTableModel);

        this.reviewersPanel = new SearchSelectPanel<>(
                P4ServerComponent.query(project, clientConfig.getServerConfig(),
                        new ListUsersQuery(-1))
                .mapQuery(r -> r.getUsers().stream()
                    .map(Reviewer::new)
                    .collect(Collectors.toList())),
                null,
                Arrays.asList(
                    new ColumnInfo<Reviewer, String>(P4Bundle.getString("swarm-review.reviewers.username")) {
                        @Nullable
                        @Override
                        public String valueOf(Reviewer o) {
                            return o.user.getUsername();
                        }

                        @Override
                        public Comparator<Reviewer> getComparator() {
                            return REVIEWER_USERNAME_ROW_COMPARATOR;
                        }

                        @Override
                        public String getMaxStringValue() {
                            return "MMMMMMMMMMMMMMMMMMMMMMMMM";
                        }
                    },
                    new ColumnInfo<Reviewer, String>(P4Bundle.getString("swarm-review.reviewers.fullname")) {
                        @Nullable
                        @Override
                        public String valueOf(Reviewer o) {
                            return o.user.getFullName();
                        }

                        @Override
                        public Comparator<Reviewer> getComparator() {
                            return REVIEWER_FULLNAME_ROW_COMPARATOR;
                        }

                        @Override
                        public String getMaxStringValue() {
                            return "MMMMMMMMMMMMMMMMMMMMMMMMM";
                        }
                    },
                    new BooleanColumnInfo<Reviewer>(P4Bundle.getString("swarm-review.reviewers.selected"), true) {
                        @Override
                        protected boolean booleanValue(Reviewer o) {
                            return o.required;
                        }

                        @Override
                        protected void setBooleanValue(Reviewer reviewer, boolean value) {
                            reviewer.required = value;
                        }
                    }
                )
        );
    }

    public static class Reviewer {
        final P4User user;
        private boolean required;

        private Reviewer(P4User user) {
            this.user = user;
        }
    }

    private static class FileRow {
        private final String name;
        private boolean selected;

        private FileRow(String name) {
            this.name = name;
        }
    }

    private static final Comparator<FileRow> FILE_ROW_COMPARATOR = Comparator.comparing(o -> o.name);
    private static final Comparator<Reviewer> REVIEWER_USERNAME_ROW_COMPARATOR = Comparator.comparing(o -> o.user.getUsername());
    private static final Comparator<Reviewer> REVIEWER_FULLNAME_ROW_COMPARATOR =
           Comparator.comparing(o -> o.user.getFullName());

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
        final JSplitPane splitPane1 = new JSplitPane();
        splitPane1.setOrientation(0);
        root.add(splitPane1, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        splitPane1.setLeftComponent(panel1);
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("swarm-review.create.description"));
        panel1.add(label1, BorderLayout.NORTH);
        reviewDescription = new JTextArea();
        reviewDescription.setLineWrap(true);
        reviewDescription.setRows(4);
        reviewDescription.setToolTipText(ResourceBundle.getBundle("net/groboclown/p4plugin/P4Bundle")
                .getString("swarm-review.create.description.tooltip"));
        reviewDescription.setWrapStyleWord(true);
        panel1.add(reviewDescription, BorderLayout.CENTER);
        final JSplitPane splitPane2 = new JSplitPane();
        splitPane2.setOrientation(1);
        splitPane2.setResizeWeight(0.5);
        splitPane1.setRightComponent(splitPane2);
        final JScrollPane scrollPane1 = new JScrollPane();
        splitPane2.setLeftComponent(scrollPane1);
        scrollPane1.setViewportView(fileSelectionTable);
        userSelectionPanel = new JPanel();
        userSelectionPanel.setLayout(new FormLayout("fill:d:grow", "center:d:grow"));
        splitPane2.setRightComponent(userSelectionPanel);
        label1.setLabelFor(reviewDescription);
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
