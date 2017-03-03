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
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.VcsConnectionProblem;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.status.StatusBarUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.util.Consumer;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.actions.P4WorkOfflineAction;
import net.groboclown.idea.p4ic.actions.P4WorkOnlineAction;
import net.groboclown.idea.p4ic.actions.ReloadP4ConfigAction;
import net.groboclown.idea.p4ic.config.P4ProjectConfig;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.actions.P4ServerWorkOfflineAction;
import net.groboclown.idea.p4ic.v2.actions.P4ServerWorkOnlineAction;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.ConfigInvalidListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.events.ServerConnectionStateListener;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * Widget to display Perforce server connection information.
 */
public class P4MultipleConnectionWidget implements StatusBarWidget.IconPresentation, StatusBarWidget.Multiframe {
    // private static final Logger LOG = Logger.getInstance(P4MultipleConnectionWidget.class);


    @Nullable
    private P4Vcs myVcs;

    @Nullable
    private Project project;

    private MessageBusConnection appMessageBus;

    @Nullable
    private StatusBar statusBar;

    private final ArrayList<ClientServerRef> serverRefs = new ArrayList<ClientServerRef>();

    @NotNull
    private volatile Icon icon = P4Icons.UNKNOWN;
    private volatile String toolTip;



    public P4MultipleConnectionWidget(@NotNull P4Vcs vcs, @NotNull Project project) {
        myVcs = vcs;
        this.project = project;

        appMessageBus = ApplicationManager.getApplication().getMessageBus().connect();

        ConnectionStateListener listener = new ConnectionStateListener();
        Events.registerAppBaseConfigUpdated(appMessageBus, listener);
        Events.registerAppConfigInvalid(appMessageBus, listener);
        Events.appServerConnectionState(appMessageBus, listener);

        // At the end, we want to reload our connection display.
        reloadServerRefs();
    }

    @Override
    public StatusBarWidget copy() {
        if (project == null || myVcs == null) {
            throw new IllegalStateException(P4Bundle.message("error.connect-widget.disposed"));
        }
        return new P4MultipleConnectionWidget(myVcs, project);
    }

    @NotNull
    @Override
    public String ID() {
        return P4MultipleConnectionWidget.class.getName();
    }

    @Override
    public WidgetPresentation getPresentation(@NotNull PlatformType type) {
        return this;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.statusBar = statusBar;
    }

    @NotNull
    private ListPopup createListPopup(DataContext dataContext) {
        if (hasNoServerRefs()) {
            reloadServerRefs();
        }
        DefaultActionGroup connectionGroup = new DefaultActionGroup();
        int groupCount = 0;
        if (myVcs != null) {
            // Don't hang the UI.
            for (ClientServerRef ref: getServerRefs()) {
                connectionGroup.add(new P4ServerWorkOnlineAction(ref));
                connectionGroup.add(new P4ServerWorkOfflineAction(ref));
                groupCount++;
            }
        }
        // If there are multiple servers, allow for turning them all online or offline at once.
        if (groupCount > 1) {
            connectionGroup.addSeparator();
            connectionGroup.add(new P4WorkOnlineAction());
            connectionGroup.add(new P4WorkOfflineAction());
        }
        if (groupCount <= 0) {
            connectionGroup.add(new ReloadP4ConfigAction());
        }
        connectionGroup.addSeparator();
        /* this is for testing.
        connectionGroup.add(new AnAction(P4Bundle.getString("statusbar.connection.popup.show-config")) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                Project project = anActionEvent.getProject();
                if (project != null) {
                    UICompat.getInstance().editVcsConfiguration(project, P4Vcs.getInstance(project).getConfigurable());
                }
            }
        });
        */
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

                // click will cause the UI to refresh the view automatically,
                // so we don't need an explicit repaint.
                update(false);

                showPopup(mouseEvent);
            }
        };
    }

    private void showPopup(@NotNull MouseEvent event) {
        // it isn't getting bubbled up to the parent
        DataContext dataContext = getContext();
        reloadServerRefs();
        final ListPopup popup = createListPopup(dataContext);
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

    private void update(final boolean refreshStatusBar) {
        if (hasNoServerRefs()) {
            reloadServerRefs();
        }
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if (project == null || project.isDisposed()) {
                    emptyTextAndTooltip();
                } else {
                    setValues();
                }
                if (statusBar != null && refreshStatusBar) {
                    // LOG.info("updating the connection widget display");
                    statusBar.getComponent().repaint();
                }
            }
        });
    }

    // MUST BE RUN IN THE EDT!!!
    public void setValues() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        int online = 0;
        int offline = 0;
        if (myVcs != null) {
            // Don't hang up the UI thread by waiting on servers to be validated and
            // authenticated.
            for (P4Server server : myVcs.getOnlineP4Servers()) {
                if (server.isWorkingOffline()) {
                    offline++;
                } else {
                    online++;
                }
            }
        }

        if (online > 0 && offline <= 0) {
            toolTip = P4Bundle.message("statusbar.connection.enabled");
            icon = P4Icons.CONNECTED;
        } else if (offline > 0 && online <= 0) {
            toolTip = P4Bundle.message("statusbar.connection.disabled");
            icon = P4Icons.DISCONNECTED;
        } else if (offline > 0 && online > 0) {
            toolTip = P4Bundle.message("statusbar.connection.mixed");
            icon = P4Icons.MIXED;
        } else {
            toolTip = P4Bundle.message("statusbar.connection.none");
            icon = P4Icons.UNKNOWN;
        }

        if (!isDisposed() && statusBar != null) {
            statusBar.updateWidget(ID());
        }
    }

    public void deactivate() {
        if (isDisposed()) {
            return;
        }
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            statusBar.removeWidget(ID());
        }
        if (appMessageBus != null) {
            appMessageBus.disconnect();
            appMessageBus = null;
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
        icon = P4Icons.UNKNOWN;
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


    private ArrayList<ClientServerRef> getServerRefs() {
        synchronized (serverRefs) {
            return new ArrayList<ClientServerRef>(serverRefs);
        }
    }


    private boolean hasNoServerRefs() {
        synchronized (serverRefs) {
            return ! serverRefs.isEmpty();
        }
    }


    private void setServerRefs(ArrayList<ClientServerRef> refs) {
        synchronized (serverRefs) {
            serverRefs.clear();
            serverRefs.addAll(refs);
        }
    }


    private void reloadServerRefs() {
        if (hasNoServerRefs()) {
            // Populate the server list with the ones we know we don't have
            // to wait on.
            ArrayList<ClientServerRef> tmp = new ArrayList<ClientServerRef>();
            for (P4Server p4Server : P4Vcs.getInstance(project).getOnlineP4Servers()) {
                tmp.add(p4Server.getClientServerId());
            }
            // Make doubly sure we need to load it.
            if (hasNoServerRefs()) {
                setServerRefs(tmp);
            }
        }
        Runnable refresher = new Runnable() {
            @Override
            public void run() {
                ArrayList<ClientServerRef> tmp = new ArrayList<ClientServerRef>();
                for (P4Server p4Server : P4Vcs.getInstance(project).getP4Servers()) {
                    tmp.add(p4Server.getClientServerId());
                }
                setServerRefs(tmp);
                update(true);
            }
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().executeOnPooledThread(refresher);
        } else {
            refresher.run();
        }
    }


    private class ConnectionStateListener implements
            BaseConfigUpdatedListener, ServerConnectionStateListener, ConfigInvalidListener {

        @Override
        public void configUpdated(@NotNull Project project, @NotNull P4ProjectConfig newConfig,
                @Nullable P4ProjectConfig previousConfiguration) {
            // Just update everything.
            reloadServerRefs();
        }

        @Override
        public void configurationProblem(@NotNull Project project, @NotNull P4ProjectConfig config, @NotNull VcsConnectionProblem ex) {
            // Just update everything.
            reloadServerRefs();
        }

        @Override
        public void connected(@NotNull final ServerConfig config) {
            // No need to reload the configuration.
            update(true);
        }

        @Override
        public void disconnected(@NotNull final ServerConfig config) {
            // No need to reload the configuration.
            update(true);
        }
    }
}
