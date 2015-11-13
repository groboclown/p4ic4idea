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

package net.groboclown.idea.p4ic.v2.ui.warning;

import com.intellij.ide.errorTreeView.HotfixData;
import com.intellij.ide.errorTreeView.HotfixGate;
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.MessageView;
import com.intellij.util.Consumer;
import com.intellij.util.ui.ErrorTreeView;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

public class WarningUI {
    private static final Logger LOG = Logger.getInstance(WarningUI.class);


    public static void showWarnings(@NotNull Collection<WarningMessage> warnings) {
        final Map<Project, List<WarningMessage>> sorted = sortWarningsByProject(warnings);
        for (Entry<Project, List<WarningMessage>> entry : sorted.entrySet()) {
            showWarningPanel(entry.getKey(), entry.getValue());
        }
    }

    private static void showWarningPanel(@NotNull final Project project, final List<WarningMessage> warnings) {
        //final AbstractVcsHelper helper = AbstractVcsHelper.getInstance(project);
        //helper.showErrors(getErrorGroups(warnings), VcsBundle.message("message.title.annotate"));

        new WarningViewHandler(project).showWarnings(warnings);
    }


    private static ErrorTreeView getErrorTreeView(@NotNull Project project, @NotNull String tabDisplayName) {
        MessageView messageView = MessageView.SERVICE.getInstance(project);
        Content[] contents = messageView.getContentManager().getContents();
        for (Content content : contents) {
            if (content != null) {
                if (tabDisplayName.equals(content.getDisplayName())) {
                    ErrorTreeView listErrorView = (ErrorTreeView) content.getComponent();
                    if (listErrorView != null) {
                        return listErrorView;
                    }
                }
            }
        }
        // no error view registered.  Create one.
        return new NewErrorTreeViewPanel(project, null);
    }




    @NotNull
    private static Map<Project, List<WarningMessage>> sortWarningsByProject(@NotNull Collection<WarningMessage> warnings) {
        Map<Project, List<WarningMessage>> ret = new HashMap<Project, List<WarningMessage>>();
        for (WarningMessage warning : warnings) {
            List<WarningMessage> list = ret.get(warning.getProject());
            if (list == null) {
                list = new ArrayList<WarningMessage>();
                ret.put(warning.getProject(), list);
            }
            list.add(warning);
        }
        return ret;
    }


    @SuppressWarnings("ThrowableInstanceNeverThrown")
    @NotNull
    private static Map<HotfixData, List<VcsException>> getErrorGroups(@NotNull Collection<WarningMessage> warnings) {
        Map<HotfixData, List<VcsException>> ret = new HashMap<HotfixData, List<VcsException>>();
        for (WarningMessage warning : warnings) {
            final Consumer<HotfixGate> hotfix = warning.getHotfix();
            final HotfixData data;
            if (hotfix != null) {
                data = new HotfixData(warning.getSummary(),
                        warning.getSummary(), warning.getSummary(),
                        hotfix);
            } else {
                data = null;
            }
            List<VcsException> exList = new ArrayList<VcsException>();
            VcsException baseException;
            if (warning.getWarning() != null) {
                if (warning.getWarning() instanceof VcsException) {
                    baseException = (VcsException) warning.getWarning();
                } else {
                    baseException = new P4Exception(warning.getWarning());
                }
            } else {
                baseException = new P4Exception(warning.getMessage());
            }
            if (warning.getAffectedFiles().isEmpty()) {
                if (baseException.getVirtualFile() == null) {
                    // TODO figure out a better file instance.
                    LOG.warn("No file set for exception", baseException);
                    baseException.setVirtualFile(FilePathUtil.getFilePath(new File(".")).getVirtualFile());
                }
                exList.add(baseException);
            } else if (warning.getAffectedFiles().size() == 1) {
                baseException.setVirtualFile(warning.getAffectedFiles().iterator().next());
                exList.add(baseException);
            } else {
                for (VirtualFile file : warning.getAffectedFiles()) {
                    VcsException ex = new VcsException(baseException);
                    ex.setVirtualFile(file);
                    exList.add(ex);
                }
            }
            final List<VcsException> list = ret.get(data);
            if (list == null) {
                ret.put(data, exList);
            } else {
                list.addAll(exList);
            }
        }
        return ret;
    }
}
