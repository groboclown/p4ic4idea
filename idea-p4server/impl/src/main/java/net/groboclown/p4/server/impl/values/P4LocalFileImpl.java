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

package net.groboclown.p4.server.impl.values;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.api.values.P4ResolveType;
import net.groboclown.p4.server.api.values.P4Revision;
import net.groboclown.p4.server.impl.cache.store.ClientServerRefStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class P4LocalFileImpl implements P4LocalFile {
    private final P4RemoteFile depot;
    private final FilePath local;
    private final P4Revision haveRev;
    private final P4FileRevision headRev;
    private final P4ChangelistId changelistId;
    private final P4FileAction action;
    private final P4ResolveType resolveType;
    private final P4FileType fileType;
    private final P4RemoteFile integrateFrom;


    public static class Builder {
        private P4RemoteFile depot;
        private FilePath local;
        private P4Revision haveRev;
        private P4FileRevision headRev;
        private P4ChangelistId changelistId;
        private P4FileAction action;
        private P4ResolveType resolveType;
        private P4FileType fileType;
        private P4RemoteFile integrateFrom;

        public Builder withLocalFile(P4LocalFile file) {
            this.depot = file.getDepotPath();
            this.local = file.getFilePath();
            this.haveRev = file.getHaveRevision();
            this.headRev = file.getHeadFileRevision();
            this.changelistId = file.getChangelistId();
            this.action = file.getFileAction();
            this.resolveType = file.getResolveType();
            this.fileType = file.getFileType();
            this.integrateFrom = file.getIntegrateFrom();
            return this;
        }

        public P4RemoteFile getDepot() {
            return depot;
        }

        public Builder withDepot(P4RemoteFile file) {
            this.depot = file;
            return this;
        }

        public Builder withLocal(FilePath f) {
            this.local = f;
            return this;
        }

        public Builder withAction(P4FileAction a) {
            this.action = a;
            return this;
        }

        public Builder withHave(P4Revision have) {
            this.haveRev = have;
            return this;
        }

        public Builder withResolveType(P4ResolveType type) {
            this.resolveType = type;
            return this;
        }

        public Builder withFileType(P4FileType type) {
            this.fileType = type;
            return this;
        }

        public Builder withIntegrateFrom(P4RemoteFile r) {
            this.integrateFrom = r;
            return this;
        }

        public Builder withHead(P4FileRevision rev) {
            headRev = rev;
            return this;
        }

        public Builder withChangelist(P4ChangelistId id) {
            this.changelistId = id;
            return this;
        }

        public P4LocalFileImpl build() {
            return new P4LocalFileImpl(depot, local, haveRev, headRev, changelistId, action, resolveType,
                    fileType, integrateFrom);
        }

    }


    /**
     *
     * @param ref source client server reference
     * @param spec the description of the opened server file.
     */
    public P4LocalFileImpl(@NotNull ClientServerRef ref, @NotNull IExtendedFileSpec spec) {
        if (!spec.isMapped() || spec.getOpenAction() == null) {
            throw new IllegalArgumentException("not an opened file spec");
        }

        depot = new P4RemoteFileImpl(spec);
        local = VcsUtil.getFilePath(spec.getLocalPath().getPathString(), false);
        haveRev = new P4Revision(spec.getHaveRev());
        headRev = new P4FileRevisionImpl(ref, depot, spec);
        changelistId = new P4ChangelistIdImpl(spec.getOpenChangelistId(), ref);
        action = P4FileAction.convert(spec.getOpenAction());
        resolveType = P4ResolveType.convert(spec.getResolveType(), spec.getContentResolveType());
        fileType = P4FileType.convert(spec.getFileType());
        integrateFrom =
                // TODO double check this logic - like with a unit test!
                spec.getMovedFile() != null
                    ? new P4RemoteFileImpl(spec.getMovedFile())
                    : (spec.getOriginalPath() != null
                        ? new P4RemoteFileImpl(spec.getOriginalPath().getPathString())
                        : null);
    }


    private P4LocalFileImpl(@Nullable P4RemoteFile depot, @NotNull FilePath local, @NotNull P4Revision haveRev,
            @Nullable P4FileRevision headRev, @Nullable P4ChangelistId changelistId, @NotNull P4FileAction action,
            @NotNull P4ResolveType resolveType, @Nullable P4FileType fileType, @Nullable P4RemoteFile integrateFrom) {
        this.depot = depot;
        this.local = local;
        this.haveRev = haveRev;
        this.headRev = headRev;
        this.changelistId = changelistId;
        this.action = action;
        this.resolveType = resolveType;
        this.fileType = fileType;
        this.integrateFrom = integrateFrom;
    }


    @Nullable
    @Override
    public P4RemoteFile getDepotPath() {
        return depot;
    }

    @NotNull
    @Override
    public FilePath getFilePath() {
        return local;
    }

    @NotNull
    @Override
    public P4Revision getHaveRevision() {
        return haveRev;
    }

    @Nullable
    @Override
    public P4FileRevision getHeadFileRevision() {
        return headRev;
    }

    @Nullable
    @Override
    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    @NotNull
    @Override
    public P4FileAction getFileAction() {
        return action;
    }

    @NotNull
    @Override
    public P4ResolveType getResolveType() {
        return resolveType;
    }

    @NotNull
    @Override
    public P4FileType getFileType() {
        return fileType;
    }

    @Nullable
    @Override
    public P4RemoteFile getIntegrateFrom() {
        return integrateFrom;
    }
}
