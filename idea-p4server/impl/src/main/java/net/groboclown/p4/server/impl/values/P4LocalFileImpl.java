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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class P4LocalFileImpl implements P4LocalFile {
    private final P4RemoteFile depot;
    private final FilePath local;
    private final P4Revision haveRev;
    private final P4FileRevision headRev;
    private final P4ChangelistId changelistId;
    private final P4FileAction action;
    private final P4ResolveType resolveType;
    private final P4FileType fileType;

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
}
