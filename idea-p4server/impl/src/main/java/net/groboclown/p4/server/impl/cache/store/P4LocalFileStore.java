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

package net.groboclown.p4.server.impl.cache.store;

import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4ResolveType;
import net.groboclown.p4.server.api.values.P4Revision;
import net.groboclown.p4.server.impl.values.P4LocalFileImpl;
import org.jetbrains.annotations.NotNull;

public class P4LocalFileStore {

    @SuppressWarnings("WeakerAccess")
    public static class State {
        public P4RemoteFileStore.State depot;
        public String local;
        public int haveRev;
        public P4FileRevisionStore.State headRev;
        public P4ChangelistIdStore.State changelistId;
        public P4FileAction action;
        public String resolveType;
        public String contentResolveType;
        public String fileType;
        public P4RemoteFileStore.State integrateFrom;
    }

    @NotNull
    public static State getState(@NotNull P4LocalFile file) {
        State ret = new State();

        ret.depot = P4RemoteFileStore.getStateNullable(file.getDepotPath());
        ret.local = file.getFilePath().getPath();
        ret.haveRev = file.getHaveRevision().getValue();
        ret.headRev = P4FileRevisionStore.getStateNullable(file.getHeadFileRevision());
        ret.changelistId = P4ChangelistIdStore.getStateNullable(file.getChangelistId());
        ret.action = file.getFileAction();
        ret.resolveType = file.getResolveType().getResolveType();
        ret.contentResolveType = file.getResolveType().getContentResolveType();
        ret.fileType = file.getFileType().toString();
        ret.integrateFrom = P4RemoteFileStore.getStateNullable(file.getIntegrateFrom());

        return ret;
    }


    @NotNull
    public static P4LocalFile read(@NotNull State state) {
        return new P4LocalFileImpl.Builder()
                .withDepot(P4RemoteFileStore.readNullable(state.depot))
                .withLocal(VcsUtil.getFilePath(state.local))
                .withHave(new P4Revision(state.haveRev))
                .withHead(P4FileRevisionStore.readNullable(state.headRev))
                .withChangelist(P4ChangelistIdStore.readNullable(state.changelistId))
                .withAction(state.action)
                .withResolveType(P4ResolveType.convert(state.resolveType, state.contentResolveType))
                .withFileType(P4FileType.convert(state.fileType))
                .withIntegrateFrom(P4RemoteFileStore.readNullable(state.integrateFrom))
                .build();
    }
}
