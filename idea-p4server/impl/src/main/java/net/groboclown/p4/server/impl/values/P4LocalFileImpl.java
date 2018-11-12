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
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.api.values.P4ResolveType;
import net.groboclown.p4.server.api.values.P4Revision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.util.Optional;


public class P4LocalFileImpl implements P4LocalFile {
    private final P4RemoteFile depot;
    private final P4RemoteFile clientDepot;
    private final FilePath local;
    private final P4Revision haveRev;
    private final P4FileRevision headRev;
    private final P4ChangelistId changelistId;
    private final P4FileAction action;
    private final P4ResolveType resolveType;
    @NotNull private final P4FileType fileType;
    private final P4RemoteFile integrateFrom;
    private final Charset charset;


    public static class Builder {
        private P4RemoteFile depot;
        private P4RemoteFile clientDepot;
        private FilePath local;
        private P4Revision haveRev;
        private P4FileRevision headRev;
        private P4ChangelistId changelistId;
        private P4FileAction action;
        private P4ResolveType resolveType;
        private P4FileType fileType;
        private P4RemoteFile integrateFrom;
        private Charset charset;

        public Builder withLocalFile(P4LocalFile file) {
            this.depot = file.getDepotPath();
            this.clientDepot = file.getClientDepotPath().orElse(null);
            this.local = file.getFilePath();
            this.haveRev = file.getHaveRevision();
            this.headRev = file.getHeadFileRevision();
            this.changelistId = file.getChangelistId();
            this.action = file.getFileAction();
            this.resolveType = file.getResolveType();
            this.fileType = file.getFileType();
            this.integrateFrom = file.getIntegrateFrom();
            this.charset = file.getCharset();
            return this;
        }

        public P4RemoteFile getDepot() {
            return depot;
        }

        public Builder withDepot(P4RemoteFile file) {
            this.depot = file;
            return this;
        }

        public Builder withClientDepot(P4RemoteFile file) {
            this.clientDepot = file;
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

        public Builder withCharset(Charset c) {
            this.charset = c;
            return this;
        }

        public P4LocalFileImpl build() {
            return new P4LocalFileImpl(depot, clientDepot, local, haveRev, headRev, changelistId, action, resolveType,
                    fileType, integrateFrom, charset);
        }

    }


    /**
     *
     * @param ref source client server reference
     * @param spec the description of the opened server file.
     */
    public P4LocalFileImpl(@NotNull ClientServerRef ref, @NotNull IExtendedFileSpec spec) {
        if (!spec.isMapped() || spec.getAction() == null) {
            throw new IllegalArgumentException("not an opened file spec:" +
                    spec.getDepotPathString() +
                    " :: " + spec.getClientPathString() +
                    " :: " + spec.getLocalPathString() +
                    "; isMapped? " + spec.isMapped() +
                    "; open action: " + spec.getOpenAction() +
                    "; action: " + spec.getAction() +
                    "; head action: " + spec.getHeadAction() +
                    "; desc: " + spec.getDesc() +
                    "; open type: " + spec.getOpenType() +
                    "; open change id: " + spec.getOpenChangelistId() +
                    "; code: " + spec.getRawCode() +
                    "; message: " + spec.getStatusMessage());
        }

        depot = new P4RemoteFileImpl(spec);
        clientDepot = spec.getClientPathString() == null
                ? null
                : new P4RemoteFileImpl(spec.getClientPathString(), spec.getClientPathString(), spec.getLocalPathString());
        local = VcsUtil.getFilePath(spec.getLocalPath().getPathString(), false);
        haveRev = new P4Revision(spec.getHaveRev());
        headRev = P4FileRevisionImpl.getHead(ref, spec);
        changelistId = new P4ChangelistIdImpl(spec.getOpenChangelistId(), ref);

        // note: use Action, not OpenAction
        action = P4FileAction.convert(spec.getAction());

        resolveType = P4ResolveType.convert(spec.getResolveType(), spec.getContentResolveType());
        fileType = P4FileType.convert(spec.getFileType());
        integrateFrom =
                // TODO double check this logic - like with a unit test!
                spec.getMovedFile() != null
                    ? new P4RemoteFileImpl(spec.getMovedFile())
                    : (spec.getOriginalPath() != null
                        ? new P4RemoteFileImpl(spec.getOriginalPath().getPathString())
                        : null);
        charset = CharsetToolkit.forName(spec.getCharset());
    }

    public P4LocalFileImpl(@NotNull ClientServerRef ref, @NotNull IFileSpec spec) {
        this(new P4RemoteFileImpl(spec),
            spec.getClientPathString() == null
                    ? null
                    : new P4RemoteFileImpl(spec.getClientPathString(), spec.getClientPathString(),
                            spec.getLocalPathString()),
            getLocalFile(spec),
            new P4Revision(IFileSpec.HEAD_REVISION),
            null, null,
            P4FileAction.NONE,
            P4ResolveType.convert(null, null),
            P4FileType.convert(spec.getFileType()),
            null,
            null);
    }


    private P4LocalFileImpl(@Nullable P4RemoteFile depot, @Nullable P4RemoteFile clientDepot, @NotNull FilePath local,
            @NotNull P4Revision haveRev, @Nullable P4FileRevision headRev, @Nullable P4ChangelistId changelistId,
            @NotNull P4FileAction action, @NotNull P4ResolveType resolveType, @Nullable P4FileType fileType,
            @Nullable P4RemoteFile integrateFrom, @Nullable Charset charset) {
        this.depot = depot;
        this.clientDepot = clientDepot;
        this.local = local;
        this.haveRev = haveRev;
        this.headRev = headRev;
        this.changelistId = changelistId;
        this.action = action;
        this.resolveType = resolveType;
        this.fileType = fileType == null ? P4FileType.convert("unknown") : fileType;
        this.integrateFrom = integrateFrom;
        this.charset = charset;
    }


    @Nullable
    @Override
    public P4RemoteFile getDepotPath() {
        return depot;
    }

    @Nonnull
    @NotNull
    @Override
    public Optional<P4RemoteFile> getClientDepotPath() {
        return Optional.ofNullable(clientDepot);
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

    @Nullable
    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("P4LocalFile(");
        sb.append(action).append(' ')
                .append(local).append('#').append(haveRev);
        if (depot != null) {
            sb.append(" -> ").append(depot);
        }
        if (changelistId != null) {
            sb.append(" @").append(changelistId);
        }
        if (headRev != null) {
            sb.append("; head ").append(headRev);
        }
        sb.append("; ").append(fileType);
        if (integrateFrom != null) {
            sb.append("; integrated from ").append(integrateFrom);
        }
        sb.append(')');
        return sb.toString();
    }

    private static FilePath getLocalFile(IFileSpec spec) {
        String path;
        if (spec.getLocalPath() != null) {
            path = spec.getLocalPath().getPathString();
        } else if (spec.getClientPath() != null) {
            path = spec.getClientPath().getPathString();
        } else {
            throw new IllegalArgumentException("FileSpec does not have a local component: " + spec);
        }
        return VcsUtil.getFilePath(path, false);
    }
}
