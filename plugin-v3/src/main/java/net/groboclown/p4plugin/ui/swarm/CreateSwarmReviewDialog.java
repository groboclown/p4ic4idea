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
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.ui.SwingUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CreateSwarmReviewDialog extends JDialog {
    public interface OnCompleteListener {
        void create(List<SwarmReviewPanel.Reviewer> reviewers, P4ChangelistId changelistId);
        void cancel();
    }

    public static void show(@NotNull Project project,
            @NotNull final ClientConfig clientConfig,
            @NotNull final P4ChangelistId changelistId,
            @NotNull final OnCompleteListener onCompleteListener) {
        CreateSwarmReviewDialog dialog = new CreateSwarmReviewDialog(project, clientConfig, changelistId,
                onCompleteListener);
        SwingUtil.centerDialog(dialog);
        dialog.setVisible(true);
    }


    private CreateSwarmReviewDialog(final Project project, ClientConfig clientConfig,
            final P4ChangelistId changelistId,
            final OnCompleteListener onCompleteListener) {
        JPanel contentPane = new JPanel(new BorderLayout());
        SwarmReviewPanel reviewPanel = new SwarmReviewPanel(project, clientConfig, changelistId);
        contentPane.add(reviewPanel.getRoot(), BorderLayout.CENTER);
        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        JButton buttonOK = new JButton();
        SwingUtil.loadButtonText(buttonOK, P4Bundle.getString("button.ok"));
        buttonPane.add(buttonOK);
        buttonOK.addActionListener((e) -> {
            onCompleteListener.create(reviewPanel.getReviewers(), changelistId);
            dispose();
        });
        JButton buttonCancel = new JButton();
        SwingUtil.loadButtonText(buttonCancel, P4Bundle.getString("button.cancel"));
        buttonPane.add(buttonCancel);
        buttonCancel.addActionListener((e) -> {
            onCompleteListener.cancel();
            dispose();
        });

        setContentPane(contentPane);
        setModal(false);
        getRootPane().setDefaultButton(buttonOK);
    }
}
