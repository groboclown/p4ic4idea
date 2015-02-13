/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
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
package net.groboclown.idea.p4ic.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.StatusBarUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.util.Consumer;
import com.intellij.util.ui.UIUtil;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.actions.P4WorkOfflineAction;
import net.groboclown.idea.p4ic.actions.P4WorkOnlineAction;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.config.P4ClientsReloadedListener;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4RemoteConnectionStateListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Widget to display Perforce server connection information.
 *
 * FIXME make this show for multiple server configs.
 * For now, this only shows "connected" if ALL the project
 * connections are online, otherwise disconnected.
 */
public class P4ConnectionWidget implements StatusBarWidget.IconPresentation,
        StatusBarWidget.Multiframe, P4RemoteConnectionStateListener, P4ClientsReloadedListener {

    @Nullable
    private P4Vcs myVcs;

    @Nullable
    private Project project;

    @Nullable
    private StatusBar statusBar;

    private volatile Icon icon;
    private volatile String toolTip;

    public P4ConnectionWidget(@NotNull P4Vcs vcs, @NotNull Project project) {
        myVcs = vcs;
        this.project = project;
        setValues();
        project.getMessageBus().connect().subscribe(P4RemoteConnectionStateListener.TOPIC, this);
        project.getMessageBus().connect().subscribe(P4ClientsReloadedListener.TOPIC, this);
    }

    @Override
    public StatusBarWidget copy() {
        if (project == null || myVcs == null) {
            throw new IllegalStateException("cannot copy a disposed widget");
        }
        return new P4ConnectionWidget(myVcs, project);
    }

    @NotNull
    @Override
    public String ID() {
        return P4ConnectionWidget.class.getName();
    }

    @Override
    public WidgetPresentation getPresentation(@NotNull PlatformType type) {
        return this;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.statusBar = statusBar;
    }

    private ListPopup createListPopup(DataContext dataContext) {
        DefaultActionGroup connectionGroup = new DefaultActionGroup();
        connectionGroup.add(new P4WorkOnlineAction());
        connectionGroup.add(new P4WorkOfflineAction());
        connectionGroup.add(new AnAction(P4Bundle.message("statusbar.connection.popup.cancel")) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // do nothing
            }
        });
        return PopupFactoryImpl.getInstance().createActionGroupPopup(
                P4Bundle.message("statusbar.connection.popup.title"),
                connectionGroup,
                dataContext,
                JBPopupFactory.ActionSelectionAid.NUMBERING,
                true);
    }


    @Override
    public String getTooltipText() {
        return toolTip;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return icon;
    }


    @Override
    // have no effect since the click opens a list popup, and the consumer is not called for the MultipleTextValuesPresentation
    public Consumer<MouseEvent> getClickConsumer() {
        return new Consumer<MouseEvent>() {
            public void consume(MouseEvent mouseEvent) {
                // update on click
                update();

                showPopup(mouseEvent);
            }
        };
    }

    private void showPopup(@NotNull MouseEvent event) {
        // it isn't getting bubbled up to the parent
        DataContext dataContext = getContext();
        final ListPopup popup = createListPopup(dataContext);
        if (popup == null) {
            return;
        }
        if (popup.isVisible()) {
            popup.cancel();
            return;
        }
        final Dimension dimension = popup.getContent().getPreferredSize();
        final Point at = new Point(0, -dimension.height);
        popup.show(new RelativePoint(event.getComponent(), at));
        Disposer.register(this, popup); // destroy popup on unexpected project close
    }


    @NotNull
    private DataContext getContext() {
        DataContext parent = DataManager.getInstance().getDataContext((Component) statusBar);
        return SimpleDataContext.getSimpleContext(CommonDataKeys.PROJECT.getName(), project,
            SimpleDataContext.getSimpleContext(PlatformDataKeys.CONTEXT_COMPONENT.getName(), getEditorComponent(),
                parent));
    }

    public void update() {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if ((project == null) || project.isDisposed()) {
                    emptyTextAndTooltip();
                    return;
                }
                setValues();
            }
        });
    }

    private void setValues() {
        if (isWorkingOnline()) {
            toolTip = P4Bundle.message("statusbar.connection.enabled");
            icon = P4Icons.CONNECTED;
        } else {
            toolTip = P4Bundle.message("statusbar.connection.disabled");
            icon = P4Icons.DISCONNECTED;
        }

        if (!isDisposed() && statusBar != null) {
            statusBar.updateWidget(ID());
        }
    }

    public void deactivate() {
        if (isDisposed()) return;
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            statusBar.removeWidget(ID());
        }
    }

    @Override
    public void dispose() {
        deactivate();
        myVcs = null;
        project = null;
        statusBar = null;
    }

    protected boolean isDisposed() {
        return project == null;
    }

    private void emptyTextAndTooltip() {
        icon = null;
        toolTip = "";
    }


    @Nullable
    private Editor getEditor() {
        if (project == null || project.isDisposed()) return null;

        FileEditor fileEditor = StatusBarUtil.getCurrentFileEditor(project, statusBar);
        Editor result = null;
        if (fileEditor instanceof TextEditor) {
            result = ((TextEditor) fileEditor).getEditor();
        }

        if (result == null) {
            final FileEditorManager manager = FileEditorManager.getInstance(project);
            Editor editor = manager.getSelectedTextEditor();
            if (editor != null && WindowManager.getInstance().getStatusBar(editor.getComponent(), project) == statusBar) {
                result = editor;
            }
        }

        return result;
    }


    private JComponent getEditorComponent() {
        Editor editor = getEditor();
        if (editor == null) {
            return null;
        } else {
            return editor.getComponent();
        }
    }

    @Override
    public void onPerforceServerDisconnected(@NotNull ServerConfig config) {
        update();
        if (statusBar != null) {
            statusBar.getComponent().repaint();
        }
    }

    @Override
    public void onPerforceServerConnected(@NotNull ServerConfig config) {
        update();
        if (statusBar != null) {
            statusBar.getComponent().repaint();
        }
    }

    @Override
    public void clientsLoaded(@NotNull Project project, @NotNull List<Client> clients) {
        update();
        if (statusBar != null) {
            statusBar.getComponent().repaint();
        }
    }


    private boolean isWorkingOnline() {
        if (myVcs == null) {
            return false;
        }
        // If any one client is offline, then report offline.
        List<Client> clients = myVcs.getClients();
        // Likewise, if there are no clients, or it's a config problem, then report offline
        if (clients.isEmpty()) {
            return false;
        }
        for (Client client: clients) {
            if (client.isWorkingOffline()) {
                return false;
            }
        }

        return true;
    }
}
