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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.SwingUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BrowseJobsDialog extends JDialog {
    private final ListTableModel<SelectedJob> tableModel;


    public interface SelectedJobsListener {
        void onDialogClosed(java.util.List<P4Job> jobsSelected);
    }


    public static void show(@NotNull Project project,
            @NotNull P4CommandRunner.QueryAnswer<List<P4Job>> listQueryAnswer,
            @NotNull SelectedJobsListener onCompleteListener) {
        BrowseJobsDialog dialog = new BrowseJobsDialog(project, listQueryAnswer, onCompleteListener);
        dialog.pack();
        final Dimension bounds = dialog.getSize();
        final Rectangle parentBounds = dialog.getOwner().getBounds();
        dialog.setLocation(parentBounds.x + (parentBounds.width - bounds.width) / 2,
                parentBounds.y + (parentBounds.height - bounds.height) / 2);
        dialog.setVisible(true);
    }

    private BrowseJobsDialog(@NotNull Project project,
            @NotNull P4CommandRunner.QueryAnswer<List<P4Job>> listQueryAnswer,
            @NotNull SelectedJobsListener onCompleteListener) {
        super(WindowManager.getInstance().suggestParentWindow(project),
                P4Bundle.getString("job.search.title"));
        this.tableModel =
                new ListTableModel<>(new ColumnInfo<SelectedJob, Boolean>(P4Bundle.getString("job.search.check")) {
            @Nullable
            @Override
            public Boolean valueOf(SelectedJob o) {
                return o == null ? null : o.selected;
            }

            public Class<?> getColumnClass() {
                return Boolean.class;
            }

            @Override
            public boolean isCellEditable(SelectedJob item) {
                return true;
            }

            @Override
            public void setValue(SelectedJob item, Boolean value) {
                if (item != null) {
                    item.selected = value == null ? false : value;
                }
            }
            // Cell renderer and editor are not supported here :(
        }, new ColumnInfo<SelectedJob, String>(P4Bundle.getString("job.panel.id")) {
            @Nullable
            @Override
            public String valueOf(SelectedJob selectedJob) {
                return selectedJob == null ? null : selectedJob.job.getJobId();
            }
        }, new ColumnInfo<SelectedJob, String>(P4Bundle.getString("submit.job.table.column.description")) {
            @Nullable
            @Override
            public String valueOf(SelectedJob selectedJob) {
                return selectedJob == null ? null : selectedJob.job.getDescription();
            }
        });


        JPanel contentPane = new JPanel(new BorderLayout());

        JBTable table = new JBTable(tableModel);
        table.getColumnModel().getColumn(0).setCellEditor(new BooleanTableCellEditor(false, SwingConstants.CENTER));
        table.getColumnModel().getColumn(0).setCellRenderer(new BooleanTableCellRenderer(SwingConstants.CENTER));
        contentPane.add(new JBScrollPane(table), BorderLayout.CENTER);
        contentPane.add(table.getTableHeader(), BorderLayout.NORTH);

        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        JButton buttonOK = new JButton();
        SwingUtil.loadButtonText(buttonOK, P4Bundle.getString("button.ok"));
        buttonPane.add(buttonOK);
        buttonOK.addActionListener((e) -> {
            onCompleteListener.onDialogClosed(getSelectedJobs());
            dispose();
        });
        JButton buttonCancel = new JButton();
        SwingUtil.loadButtonText(buttonCancel, P4Bundle.getString("button.cancel"));
        buttonPane.add(buttonCancel);
        buttonCancel.addActionListener((e) -> {
            onCompleteListener.onDialogClosed(Collections.emptyList());
            dispose();
        });

        setContentPane(contentPane);
        setModal(false);
        getRootPane().setDefaultButton(buttonOK);

        // TODO add indicator that shows when loading, and when error.
        listQueryAnswer.whenCompleted((jobs) -> {
            final List<SelectedJob> selected = jobs.stream()
                    .map(SelectedJob::new)
                    .collect(Collectors.toList());
            SwingUtilities.invokeLater(() -> tableModel.addRows(selected));
        });
    }

    private List<P4Job> getSelectedJobs() {
        return tableModel.getItems().stream()
                .filter((j) -> j.selected)
                .map((j) -> j.job)
                .collect(Collectors.toList());
    }

    private static class SelectedJob {
        boolean selected = false;
        final P4Job job;

        private SelectedJob(P4Job job) {
            this.job = job;
        }
    }
}
