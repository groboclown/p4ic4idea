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
import com.intellij.util.ui.ColumnInfo;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.SearchSelectPanel;
import net.groboclown.p4plugin.ui.SwingUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BrowseJobsDialog extends JDialog {
    public interface SelectedJobsListener {
        void onDialogClosed(List<P4Job> jobsSelected);
    }


    public static void show(@NotNull Project project,
            @NotNull P4CommandRunner.QueryAnswer<List<P4Job>> listQueryAnswer,
            @NotNull SelectedJobsListener onCompleteListener) {
        BrowseJobsDialog dialog = new BrowseJobsDialog(project, listQueryAnswer, onCompleteListener);
        SwingUtil.centerDialog(dialog);
        dialog.setVisible(true);
    }

    private BrowseJobsDialog(@NotNull Project project,
            @NotNull P4CommandRunner.QueryAnswer<List<P4Job>> listQueryAnswer,
            @NotNull SelectedJobsListener onCompleteListener) {
        super(WindowManager.getInstance().suggestParentWindow(project),
                P4Bundle.getString("job.search.title"));

        JPanel contentPane = new JPanel(new BorderLayout());

        final SearchSelectPanel<P4Job> searchPanel = new SearchSelectPanel<>(listQueryAnswer,
                count -> {},
                Arrays.asList(
                    new ColumnInfo<P4Job, String>(P4Bundle.getString("job.panel.id")) {
                        @Nullable
                        @Override
                        public String valueOf(P4Job job) {
                            return job == null ? null : job.getJobId();
                        }
                    }, new ColumnInfo<P4Job, String>(P4Bundle.getString("submit.job.table.column.description")) {
                        @Nullable
                        @Override
                        public String valueOf(P4Job job) {
                            return job == null ? null : job.getDescription();
                        }
                    })
        );

        contentPane.add(searchPanel, BorderLayout.CENTER);

        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        JButton buttonOK = new JButton();
        SwingUtil.loadButtonText(buttonOK, P4Bundle.getString("button.ok"));
        buttonPane.add(buttonOK);
        buttonOK.addActionListener((e) -> {
            onCompleteListener.onDialogClosed(searchPanel.getSelectedItems().collect(Collectors.toList()));
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
    }
}
