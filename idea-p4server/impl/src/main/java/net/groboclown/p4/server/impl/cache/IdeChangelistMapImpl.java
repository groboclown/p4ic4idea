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

package net.groboclown.p4.server.impl.cache;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.cache.CacheQueryHandler;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.impl.cache.store.IdeChangelistCacheStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class IdeChangelistMapImpl implements IdeChangelistMap {
    private final Project project;
    private final CacheQueryHandler queryHandler;
    private final IdeChangelistCacheStore cache;

    public IdeChangelistMapImpl(@NotNull Project project, @NotNull CacheQueryHandler queryHandler,
            @NotNull IdeChangelistCacheStore cache) {
        this.project = project;
        this.queryHandler = queryHandler;
        this.cache = cache;
    }

    @Nullable
    @Override
    public LocalChangeList getIdeChangeFor(@NotNull P4ChangelistId changelistId)
            throws InterruptedException {
        String ideId = cache.getMappedIdeChangeListId(changelistId);
        if (ideId != null) {
            return ChangeListManager.getInstance(project).getChangeList(ideId);
        }
        return null;
    }

    @NotNull
    @Override
    public Collection<P4ChangelistId> getP4ChangesFor(@NotNull LocalChangeList changeList)
            throws InterruptedException {
        return new HashSet<>(cache.getLinkedChangelists(changeList.getId()));
    }

    @Nullable
    @Override
    public P4ChangelistId getP4ChangeFor(@NotNull ClientServerRef ref, @NotNull LocalChangeList changeList)
            throws InterruptedException {
        for (P4ChangelistId p4ChangelistId : getP4ChangesFor(changeList)) {
            if (ref.equals(p4ChangelistId.getClientServerRef())) {
                return p4ChangelistId;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Map<P4ChangelistId, LocalChangeList> getLinkedIdeChanges()
            throws InterruptedException {
        Map<P4ChangelistId, LocalChangeList> ret = new HashMap<>();
        for (Map.Entry<P4ChangelistId, String> entry : cache.getLinkedChangelistIds().entrySet()) {
            ret.put(entry.getKey(),
                    ChangeListManager.getInstance(project).getChangeList(entry.getValue()));
        }
        return ret;
    }

    @Nullable
    @Override
    public P4LocalChangelist getMappedChangelist(@NotNull CreateChangelistAction action)
            throws InterruptedException {
        return cache.getPendingChangelist(action, false);
    }

    @Override
    public void setMapping(@NotNull P4ChangelistId p4ChangelistId, @NotNull LocalChangeList changeList)
            throws InterruptedException {
        cache.setLink(p4ChangelistId, changeList.getId());
    }

    @Override
    public void setMapping(@NotNull CreateChangelistAction action, @NotNull LocalChangeList changeList)
            throws InterruptedException {
        cache.mapPendingChangelistId(action, changeList.getId());
    }

    @Override
    public void setMapping(P4ChangelistId p4ChangelistId, CreateChangelistAction action)
            throws InterruptedException {
        cache.updateCreatedChangelist(p4ChangelistId, action);
    }

    @Override
    public void actionFailed(@NotNull CreateChangelistAction action)
            throws InterruptedException {
        cache.removeAction(action);
    }

    @Override
    public void changelistDeleted(@NotNull P4ChangelistId changelistId)
            throws InterruptedException {
        cache.deleteChangelist(changelistId);
    }
}
