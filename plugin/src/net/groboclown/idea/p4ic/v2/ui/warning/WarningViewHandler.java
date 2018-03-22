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

import com.intellij.ide.errorTreeView.GroupingElement;
import com.intellij.ide.errorTreeView.HotfixData;
import com.intellij.ide.errorTreeView.HotfixGate;
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.ide.errorTreeView.SimpleErrorData;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.MessageView;
import com.intellij.util.Consumer;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * Lifecycle: intended to be created just to show the warnings.  It is not intended to be
 * kept around.
 */
public class WarningViewHandler {
    private final Project project;


    public WarningViewHandler(@NotNull final Project project) {
        this.project = project;
    }

    void showWarnings(@NotNull final Collection<WarningMessage> warnings) {
        showWarningsPanel(new Consumer<P4WarningViewPanel>() {
            @Override
            public void consume(@NotNull final P4WarningViewPanel errorViewStructure) {
                addWarningsInto(errorViewStructure, warnings);
            }
        });
    }

    private void addWarningsInto(@NotNull final P4WarningViewPanel panel,
            @NotNull final Collection<WarningMessage> warnings) {
        final Map<HotfixData, List<WarningMessage>> byHotfix = sortWarningsByHotfix(warnings);
        for (Entry<HotfixData, List<WarningMessage>> entry : byHotfix.entrySet()) {
            if (entry.getKey() == null) {
                for (P4MessageElement message : createMessageElementsFor(entry.getValue())) {
                    panel.getErrorViewStructure().addNavigatableMessage(message.getGroupName(), message);
                }
            } else {
                panel.addHotfixGroup(entry.getKey(), createSimpleErrorData(entry.getValue()));
            }
        }
    }


    private void showWarningsPanel(@NotNull final Consumer<P4WarningViewPanel> viewFiller) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                if (project.isDisposed()) {
                    return;
                }
                final String tabDisplayName = getTabName();
                final P4WarningViewPanel errorTreeView = getErrorTreeView(tabDisplayName);

                viewFiller.consume(errorTreeView);
            }
        });
    }


    /**
     * Get the error tree view.  If there is already one in the tab list, use that one.
     * Otherwise, create a new one.  Finally, show the new tab.
     *
     * @param tabDisplayName name of the tab to grab
     * @return the warning panel
     */
    private P4WarningViewPanel getErrorTreeView(@NotNull final String tabDisplayName) {
        final CommandProcessor commandProcessor = CommandProcessor.getInstance();
        final MessageView messageView = MessageView.SERVICE.getInstance(project);
        final Content[] contents = messageView.getContentManager().getContents();
        Content foundContent = null;
        P4WarningViewPanel errorTreeView = null;
        for (Content content : contents) {
            if (content != null) {
                if (tabDisplayName.equals(content.getDisplayName()) &&
                        content.getComponent() != null) {
                    // ensure the view is of the correct type.
                    if (content.getComponent() instanceof P4WarningViewPanel) {
                        foundContent = content;
                        errorTreeView = (P4WarningViewPanel) content.getComponent();
                        break;
                    } else {
                        // remove the existing content
                        if (messageView.getContentManager().removeContent(content, true)) {
                            content.release();
                        }
                        // and create a replacement
                        errorTreeView = new P4WarningViewPanel(project);
                        break;
                    }
                }
            }
        }

        if (errorTreeView == null) {
            errorTreeView = new P4WarningViewPanel(project);
        }

        final Content finalFoundContent = foundContent;
        final NewErrorTreeViewPanel finalErrorTreeView = errorTreeView;

        // switch to the view
        commandProcessor.executeCommand(project, new Runnable() {
            public void run() {
                messageView.runWhenInitialized(new Runnable() {
                    public void run() {
                        final Content content;
                        if (finalFoundContent == null) {
                            content = ContentFactory.SERVICE.getInstance()
                                            .createContent(finalErrorTreeView, tabDisplayName, true);
                            messageView.getContentManager().addContent(content);
                            Disposer.register(content, finalErrorTreeView);
                        } else {
                            content = finalFoundContent;
                        }
                        messageView.getContentManager().setSelectedContent(content);

                        ToolWindowManager.getInstance(project)
                                .getToolWindow(ToolWindowId.MESSAGES_WINDOW)
                                .activate(null);
                    }
                });
            }
        }, VcsBundle.message("command.name.open.error.message.view"), null);

        return errorTreeView;
    }


    private String getTabName() {
        return VcsBundle.message("message.title.annotate");
    }

    /**
     * Puts all the warnings into a by-hotfixdata object.  Those that don't have a
     * hotfix should go into the "null" hotfix data (key).
     *
     * @param warnings warnings to sort
     * @return sorted warnings.
     */
    @NotNull
    private Map<HotfixData, List<WarningMessage>> sortWarningsByHotfix(@NotNull Collection<WarningMessage> warnings) {
        Map<HotfixData, List<WarningMessage>> ret = new HashMap<HotfixData, List<WarningMessage>>();
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
            List<WarningMessage> byData = ret.get(data);
            if (byData == null) {
                byData = new ArrayList<WarningMessage>();
                ret.put(data, byData);
            }
            byData.add(warning);
        }
        return ret;
    }


    @NotNull
    private List<P4MessageElement> createMessageElementsFor(@NotNull Collection<WarningMessage> warnings) {
        VirtualFile root = project.getBaseDir();
        Map<String, GroupingElement> groups = new HashMap<String, GroupingElement>();
        List<P4MessageElement> ret = new ArrayList<P4MessageElement>();
        for (WarningMessage warning : warnings) {
            String groupName = warning.getSummary();
            GroupingElement group = groups.get(groupName);
            if (group == null) {
                group = new GroupingElement(groupName, root, root);
                groups.put(groupName, group);
            }
            Collection<VirtualFile> files = warning.getAffectedFiles();
            if (files.isEmpty()) {
                files = Collections.singletonList(root);
            }
            for (VirtualFile file : files) {
                String message = warning.getMessage();
                String exportText = "";
                if (files.size() > 1) {
                    exportText = " (" + file.getPresentableName() + ")";
                }
                ret.add(new P4MessageElement(warning.getErrorKind(), group,
                        new String[] { message },
                        new OpenFileDescriptor(project, file, -1, -1),
                        exportText, "", FilePathUtil.getFilePath(file)));
            }
        }
        return ret;
    }


    @NotNull
    private List<SimpleErrorData> createSimpleErrorData(@NotNull final List<WarningMessage> value) {
        List<SimpleErrorData> ret = new ArrayList<SimpleErrorData>(value.size());

        for (WarningMessage warningMessage : value) {
            String[] messages = new String[] { warningMessage.getMessage() };
            for (VirtualFile virtualFile : warningMessage.getAffectedFiles()) {
                ret.add(new SimpleErrorData(warningMessage.getErrorKind(), messages, virtualFile));
            }
        }

        return ret;
    }

}
