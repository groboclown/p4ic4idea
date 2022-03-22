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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import net.groboclown.p4.server.api.commands.user.ListUsersQuery;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
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
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SwarmReviewPanel {
    private final Project project;
    private final ClientConfig clientConfig;
    private final ValidChangeListener listener;

    private JPanel root;
    private JTable fileSelectionTable;
    private JPanel userSelectionPanel;
    private JTextArea descriptionTextArea;
    private SearchSelectPanel<Reviewer> reviewersPanel;

    private ListTableModel<FileRow> fileTableModel;

    private int oldReviewId = -1;
    private volatile int userSelectedCount = 0;

    public interface ValidChangeListener {
        void onValidChange(boolean valid);
    }

    SwarmReviewPanel(@NotNull final Project project, @NotNull final ClientConfig clientConfig,
            @NotNull ChangeList changelist,
            @NotNull final ValidChangeListener listener) {
        this.project = project;
        this.clientConfig = clientConfig;
        this.listener = listener;

        $$$setupUI$$$();

        this.fileTableModel.setSortable(true);
        userSelectionPanel.add(reviewersPanel, BorderLayout.CENTER);
        fileTableModel.setItems(
                changelist.getChanges().stream()
                        .flatMap(c -> {
                            List<FilePath> files = new ArrayList<>(2);
                            if (c.getBeforeRevision() != null) {
                                files.add(c.getBeforeRevision().getFile());
                            }
                            if (c.getAfterRevision() != null) {
                                files.add(c.getAfterRevision().getFile());
                            }
                            return files.stream();
                        })
                        .map(FileRow::new)
                        .collect(Collectors.toList())
        );
        descriptionTextArea.setText(changelist.getComment());
        descriptionTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fireValidChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fireValidChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fireValidChange();
            }
        });
        SwingUtilities.invokeLater(this::fireValidChange);
    }

    public JComponent getRoot() {
        return root;
    }

    public List<Reviewer> getReviewers() {
        return reviewersPanel.getSelectedItems().collect(Collectors.toList());
    }

    @NotNull
    public String getDescription() {
        String ret = descriptionTextArea.getText();
        return ret == null ? "" : ret;
    }

    public List<FilePath> getFiles() {
        return fileTableModel.getItems().stream()
                .filter(r -> r.selected)
                .map(r -> r.name)
                .collect(Collectors.toList());
    }

    public void setReview(@NotNull Review existingReview, @NotNull List<P4RemoteChangelist> changelists) {
        if (changelists.isEmpty()) {
            oldReviewId = -1;
            return;
        }
        oldReviewId = existingReview.getId();
        descriptionTextArea.setText(existingReview.getDescription());
        // TODO the reviewers needs proper query of the perforce users
        // reviewersPanel.setItems(existingReview.getParticipants());
        // TODO the files will probably need to be contained in another way.
        //fileTableModel.setItems(changelists.stream()
        //    .flatMap(rc -> rc.getFiles().stream())
        //    .map(c -> new FileRow(c.getDepotPath().getDepotPath()))
        //    .collect(Collectors.toList()));
    }

    private void fireValidChange() {
        listener.onValidChange(
                userSelectedCount > 0
                        && !getDescription().isEmpty()
                        && !getReviewers().isEmpty()
                        && !getFiles().isEmpty());
    }

    private void createUIComponents() {
        fileTableModel = new ListTableModel<>(
                new ColumnInfo<FileRow, String>(P4Bundle.getString("swarm-review.files.name")) {
                    @Override
                    public String valueOf(FileRow o) {
                        return o.name.getName();
                    }

                    @Override
                    public Comparator<FileRow> getComparator() {
                        return FILE_ROW_NAME_COMPARATOR;
                    }

                    @Override
                    public String getMaxStringValue() {
                        return "MMMMMMMMMMMMMMMMMMMMMMMMM";
                    }
                },
                new ColumnInfo<FileRow, String>(P4Bundle.getString("swarm-review.files.directory")) {
                    @Override
                    public String valueOf(FileRow o) {
                        return o.name.getParentPath() != null
                                ? o.name.getParentPath().getPath()
                                : "";
                    }

                    @Override
                    public Comparator<FileRow> getComparator() {
                        return FILE_ROW_COMPARATOR;
                    }

                    @Override
                    public String getMaxStringValue() {
                        return "MMMMMMMMMMMMMMMMMMMMMMMMM";
                    }
                }
                /* For now, leave this out.  It implies a bunch of additional overhead that the plugin
                isn't ready to support yet.  Specifically, removing shelved files and shelving others.
                , new BooleanColumnInfo<FileRow>(P4Bundle.getString("swarm-review.files.selected"), true) {
                    @Override
                    protected boolean booleanValue(FileRow o) {
                        return o.selected;
                    }

                    @Override
                    protected void setBooleanValue(FileRow fileRow, boolean value) {
                        fileRow.selected = value;
                        fireValidChange();
                    }
                }
                */
        );
        fileSelectionTable = new JBTable(fileTableModel);

        this.reviewersPanel = new SearchSelectPanel<>(
                P4ServerComponent.query(project, new OptionalClientServerConfig(clientConfig),
                                new ListUsersQuery(-1))
                        .mapQuery(r -> r.getUsers().stream()
                                .map(Reviewer::new)
                                .collect(Collectors.toList())),
                count -> {
                    userSelectedCount = count;
                    fireValidChange();
                },
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

    static class Reviewer {
        final P4User user;
        boolean required;

        private Reviewer(P4User user) {
            this.user = user;
        }
    }

    private static class FileRow {
        private final FilePath name;
        private boolean selected = true;

        private FileRow(FilePath name) {
            this.name = name;
        }
    }

    private static final Comparator<FileRow> FILE_ROW_NAME_COMPARATOR = Comparator.comparing(o -> o.name.getName());
    private static final Comparator<FileRow> FILE_ROW_COMPARATOR = Comparator.comparing(o -> o.name.getPath());
    private static final Comparator<Reviewer> REVIEWER_USERNAME_ROW_COMPARATOR =
            Comparator.comparing(o -> o.user.getUsername());
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
        splitPane1.setOrientation(1);
        splitPane1.setResizeWeight(0.5);
        root.add(splitPane1, BorderLayout.CENTER);
        final JScrollPane scrollPane1 = new JScrollPane();
        splitPane1.setLeftComponent(scrollPane1);
        scrollPane1.setViewportView(fileSelectionTable);
        userSelectionPanel = new JPanel();
        userSelectionPanel.setLayout(new BorderLayout(0, 0));
        splitPane1.setRightComponent(userSelectionPanel);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        root.add(panel1, BorderLayout.SOUTH);
        panel1.setBorder(BorderFactory.createTitledBorder(null,
                this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle", "swarm.create.description"),
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel1.add(scrollPane2, BorderLayout.CENTER);
        descriptionTextArea = new JTextArea();
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setRows(4);
        descriptionTextArea.setToolTipText(this.$$$getMessageFromBundle$$$("net/groboclown/p4plugin/P4Bundle",
                "swarm.create.description.tooltip"));
        descriptionTextArea.setWrapStyleWord(true);
        scrollPane2.setViewportView(descriptionTextArea);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.jgoodies.forms.layout.FormLayout("fill:d:noGrow", "center:d:noGrow"));
        root.add(panel2, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        label1.setHorizontalAlignment(0);
        label1.setHorizontalTextPosition(0);
        label1.setText("Swarm Review Creation is in Beta");
        com.jgoodies.forms.layout.CellConstraints cc = new com.jgoodies.forms.layout.CellConstraints();
        panel2.add(label1, cc.xy(1, 1));
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
    public JComponent $$$getRootComponent$$$() {
        return root;
    }

}
