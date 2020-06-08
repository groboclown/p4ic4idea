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
package net.groboclown.p4plugin.ui;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.changes.ChangeListDecorator;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class P4ChangeListDecorator implements ChangeListDecorator, ProjectComponent {
    private static final Logger LOG = Logger.getInstance(P4ChangeListDecorator.class);

    private final Project project;

    public static class ChangelistConnectionInfo {
        private final List<P4ChangelistId> validIds = new ArrayList<>();
        private final List<ClientServerRef> defaults = new ArrayList<>();
        private final List<ClientServerRef> unsynced = new ArrayList<>();
        private final List<ClientServerRef> unknowns = new ArrayList<>();
        private final List<ClientServerRef> offline = new ArrayList<>();
        private final boolean hasOneServer;
        private final int serverCount;

        ChangelistConnectionInfo(int serverCount) {
            this.hasOneServer = serverCount == 1;
            this.serverCount = serverCount;
        }

        void addOffline(@NotNull P4ChangelistId p4cl) {
            offline.add(p4cl.getClientServerRef());
        }

        void addOnline(@NotNull P4ChangelistId p4cl) {
            switch (p4cl.getState()) {
                case NUMBERED:
                    validIds.add(p4cl);
                    break;
                case PENDING_CREATION:
                    unsynced.add(p4cl.getClientServerRef());
                    break;
                case DEFAULT:
                    defaults.add(p4cl.getClientServerRef());
                    break;
                default:
                    unknowns.add(p4cl.getClientServerRef());
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public P4ChangeListDecorator(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void decorateChangeList(LocalChangeList changeList, ColoredTreeCellRenderer cellRenderer, boolean selected, boolean expanded, boolean hasFocus) {
        if (isProjectInvalid()) {
            return;
        }
        CacheComponent cache = CacheComponent.getInstance(project);
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        Pair<IdeChangelistMap, IdeFileMap>
                openedCache = cache.getServerOpenedCache();
        try {
            Collection<P4ChangelistId> p4Changes = openedCache.first.getP4ChangesFor(changeList);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Change " + changeList + " has p4 changes " + p4Changes);
            }
            if (p4Changes.isEmpty()) {
                // Early exit
                return;
            }
            ChangelistConnectionInfo info = new ChangelistConnectionInfo(p4Changes.size());
            for (P4ChangelistId p4Change: p4Changes) {
                if (registry != null && registry.isOnline(p4Change.getClientServerRef())) {
                    info.addOnline(p4Change);
                } else {
                    info.addOffline(p4Change);
                }
            }
            decorateInfo(info, cellRenderer);
        } catch (InterruptedException e) {
            InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(new VcsInterruptedException(e)));
        }
    }

    private static void decorateInfo(@NotNull ChangelistConnectionInfo info,
            @NotNull ColoredTreeCellRenderer cellRenderer) {
        if (info.serverCount <= 0) {
            return;
        }

        boolean hasOne = false;

        if (info.hasOneServer && info.validIds.size() == 1) {
            hasOne = true;
            // Cannot do all servers here, because each connection's corresponding changelist
            // is most probably different.
            cellRenderer.append(P4Bundle.message("changelist.render", info.validIds.get(0).getChangelistId()),
                    SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
        } else if (! info.validIds.isEmpty()) {
            hasOne = true;
            Iterator<P4ChangelistId> iter = info.validIds.iterator();
            P4ChangelistId next = iter.next();
            StringBuilder sb = new StringBuilder(P4Bundle.message("changelist.render-many.first",
                    next.getClientname(), next.getChangelistId()));
            while (iter.hasNext()) {
                next = iter.next();
                sb.append(P4Bundle.message("changelist.render-many.after",
                        next.getClientname(), next.getChangelistId()));
            }
            cellRenderer.append(sb.toString(), SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
        }

        if (info.defaults.size() == info.serverCount) {
            String msg = P4Bundle.message("changelist.decorator.default");
            cellRenderer.append(msg, SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
            hasOne = true;
        } else if (! info.defaults.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            Iterator<ClientServerRef> iter = info.defaults.iterator();
            ClientServerRef next = iter.next();
            if (hasOne) {
                sb.append(P4Bundle.message("changelist.decorator.default.second.first", next.getClientName()));
            } else {
                sb.append(P4Bundle.message("changelist.decorator.default.first.first", next.getClientName()));
            }
            while (iter.hasNext()) {
                next = iter.next();
                sb.append(P4Bundle.message("changelist.decorator.default.middle", next.getClientName()));
            }
            sb.append(P4Bundle.message("changelist.decorator.default.end"));
            cellRenderer.append(sb.toString(), SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
            hasOne = true;
        }

        if (info.unknowns.size() == info.serverCount) {
            String msg = P4Bundle.message("changelist.decorator.unknowns");
            cellRenderer.append(msg, SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
            hasOne = true;
        } else if (! info.unknowns.isEmpty()) {
            Iterator<ClientServerRef> iter = info.unknowns.iterator();
            ClientServerRef next = iter.next();
            StringBuilder sb = new StringBuilder();
            if (hasOne) {
                sb.append(P4Bundle.message("changelist.decorator.unknowns.second.first", next.getClientName()));
            } else {
                sb.append(P4Bundle.message("changelist.decorator.unknowns.first.first", next.getClientName()));
            }
            while (iter.hasNext()) {
                next = iter.next();
                sb.append(P4Bundle.message("changelist.decorator.unknowns.middle", next.getClientName()));
            }
            sb.append(P4Bundle.message("changelist.decorator.unknowns.end"));
            cellRenderer.append(sb.toString(), SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
            hasOne = true;
        }

        if (info.unsynced.size() == info.serverCount) {
            String msg = P4Bundle.message("changelist.decorator.unsynced");
            cellRenderer.append(msg, SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES);
        } else if (! info.unsynced.isEmpty()) {
            Iterator<ClientServerRef> iter = info.unsynced.iterator();
            ClientServerRef next = iter.next();
            StringBuilder sb = new StringBuilder();
            if (hasOne) {
                sb.append(P4Bundle.message("changelist.decorator.unsynced.second.first", next.getClientName()));
            } else {
                sb.append(P4Bundle.message("changelist.decorator.unsynced.first.first", next.getClientName()));
            }
            while (iter.hasNext()) {
                next = iter.next();
                sb.append(P4Bundle.message("changelist.decorator.unsynced.middle", next.getClientName()));
            }
            sb.append(P4Bundle.message("changelist.decorator.unsynced.end"));
            cellRenderer.append(sb.toString(), SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES);
            hasOne = true;
        }

        if (info.serverCount == info.offline.size()) {
            String msg = P4Bundle.message("changelist.decorator.offline");
            cellRenderer.append(msg, SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
        } else if (!info.offline.isEmpty()) {
            Iterator<ClientServerRef> iter = info.offline.iterator();
            ClientServerRef next = iter.next();
            StringBuilder sb = new StringBuilder();
            if (hasOne) {
                sb.append(P4Bundle.message("changelist.decorator.offline.second.first", next.getClientName()));
            } else {
                sb.append(P4Bundle.message("changelist.decorator.offline.first.first", next.getClientName()));
            }
            while (iter.hasNext()) {
                next = iter.next();
                sb.append(P4Bundle.message("changelist.decorator.offline.middle", next.getClientName()));
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(P4Bundle.message("changelist.decorator.offline.end"));
            cellRenderer.append(sb.toString(), SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
        }
    }

    @Override
    public void projectOpened() {
        // ignore
    }

    @Override
    public void projectClosed() {
        // ignore
    }

    @Override
    public void initComponent() {
        // ignore
    }

    @Override
    public void disposeComponent() {
        // ignore
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "PerforceChangeListDecorator";
    }


    private boolean isProjectInvalid() {
        return ! P4Vcs.isProjectValid(project);
    }
}
