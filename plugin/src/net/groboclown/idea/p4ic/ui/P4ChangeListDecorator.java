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
package net.groboclown.idea.p4ic.ui;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListDecorator;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class P4ChangeListDecorator implements ChangeListDecorator, ProjectComponent {
    private final Project project;

    public P4ChangeListDecorator(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void decorateChangeList(LocalChangeList changeList, ColoredTreeCellRenderer cellRenderer, boolean selected, boolean expanded, boolean hasFocus) {
        if (project.isDisposed()) {
            return;
        }
        P4Vcs vcs = P4Vcs.getInstance(project);
        Collection<P4ChangeListId> p4changes = vcs.getChangeListMapping().getPerforceChangelists(changeList);
        if (p4changes != null) {
            // sort by clients
            List<Client> clients = vcs.getClients();
            List<P4ChangeListId> validIds = new ArrayList<P4ChangeListId>();
            Set<Client> offline = new HashSet<Client>();
            Set<Client> defaults = new HashSet<Client>();
            Set<Client> unknowns = new HashSet<Client>();
            for (Client client : clients) {
                for (P4ChangeListId p4cl : p4changes) {
                    if (p4cl.isIn(client)) {
                        if (client.isWorkingOnline()) {
                            if (p4cl.isNumberedChangelist()) {
                                validIds.add(p4cl);
                            } else if (p4cl.isDefaultChangelist()) {
                                defaults.add(client);
                            } else {
                                unknowns.add(client);
                            }
                        } else {
                            offline.add(client);
                        }
                    }
                }
            }

            boolean hasOne = false;
            if (clients.size() == 1 && validIds.size() == 1) {
                hasOne = true;
                cellRenderer.append(P4Bundle.message("changelist.render", validIds.get(0).getChangeListId()),
                        SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
            } else if (! validIds.isEmpty()) {
                hasOne = true;
                Iterator<P4ChangeListId> iter = validIds.iterator();
                P4ChangeListId next = iter.next();
                StringBuilder sb = new StringBuilder(P4Bundle.message("changelist.render-many.first",
                        next.getClientName(), next.getChangeListId()));
                while (iter.hasNext()) {
                    next = iter.next();
                    sb.append(P4Bundle.message("changelist.render-many.after",
                            next.getClientName(), next.getChangeListId()));
                }
                cellRenderer.append(sb.toString(), SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
            }

            if (clients.size() == 1 && defaults.size() == 1) {
                String msg = P4Bundle.message("changelist.decorator.default");
                cellRenderer.append(msg, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                hasOne = true;
            } else if (! defaults.isEmpty()) {
                Iterator<Client> iter = defaults.iterator();
                Client next = iter.next();
                StringBuilder sb = new StringBuilder();
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
                cellRenderer.append(sb.toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                hasOne = true;
            }

            if (clients.size() == 1 && unknowns.size() == 1) {
                String msg = P4Bundle.message("changelist.decorator.unknowns");
                cellRenderer.append(msg, SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
                hasOne = true;
            } else if (! unknowns.isEmpty()) {
                Iterator<Client> iter = unknowns.iterator();
                Client next = iter.next();
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

            if (clients.size() == 1 && offline.size() == 1) {
                String msg = P4Bundle.message("changelist.decorator.offline");
                cellRenderer.append(msg, SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
            } else if (!offline.isEmpty()) {
                Iterator<Client> iter = offline.iterator();
                Client next = iter.next();
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
                sb.append(P4Bundle.message("changelist.decorator.offline.end"));
                cellRenderer.append(sb.toString(), SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
            }
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
}
