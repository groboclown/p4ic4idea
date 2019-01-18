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

import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileRevisionData;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.api.values.P4Revision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;


public class P4FileRevisionImpl
        implements P4FileRevision {
    private final P4RemoteFile remoteFile;
    private final P4ChangelistId changelistId;
    private final P4Revision rev;
    private final P4FileAction action;
    private final P4FileType type;
    private final P4RemoteFile integratedFrom;
    private final VcsRevisionNumber revisionNumber;
    private final Date date;
    private final String charset;

    public static P4FileRevision getHead(ClientServerRef ref, IExtendedFileSpec spec) {
        return new P4FileRevisionImpl(new P4RemoteFileImpl(spec), new P4ChangelistIdImpl(spec.getHeadChange(), ref),
                new P4Revision(spec.getHeadRev()),
                P4FileAction.convert(spec.getHeadAction()),
                P4FileType.convert(spec.getHeadType()), null, new P4Revision(spec.getHeadRev()),
                spec.getHeadTime(), spec.getHeadCharset());
    }

    public static P4FileRevision getHave(ClientServerRef ref, IExtendedFileSpec spec) {
        final int changeId = (spec.getChangelistId() > 0)
            ? spec.getChangelistId()
            : spec.getHeadChange();
        final FileAction action = (spec.getAction() != null)
            ? spec.getAction()
            : spec.getHeadAction();
        final Date date = (spec.getDate() != null)
            ? spec.getDate()
            : spec.getHeadTime();
        final String charset = (spec.getCharset() != null)
            ? spec.getCharset()
            : spec.getHeadCharset();
        final String fileType = (spec.getFileType() != null)
            ? spec.getFileType()
            : spec.getHeadType();

        return new P4FileRevisionImpl(new P4RemoteFileImpl(spec), new P4ChangelistIdImpl(changeId, ref),
                new P4Revision(spec.getHaveRev()),
                P4FileAction.convert(action),
                P4FileType.convert(fileType), null, new P4Revision(spec.getHeadRev()),
                date, charset);
    }

    P4FileRevisionImpl(@NotNull ClientServerRef ref, @NotNull IFileRevisionData data) {
        this(new P4RemoteFileImpl(data.getDepotFileName()),
                new P4ChangelistIdImpl(data.getChangelistId(), ref),
                new P4Revision(data.getRevision()),
                P4FileAction.convert(data.getAction()),
                P4FileType.convert(data.getFileType()),
                null,
                new P4Revision(data.getRevision()),
                data.getDate(),
                null);
    }

    public P4FileRevisionImpl(@NotNull P4RemoteFile depotPath, @NotNull P4ChangelistId changelistId,
            @NotNull P4Revision rev, @NotNull P4FileAction action, @NotNull P4FileType type,
            @Nullable P4RemoteFile integratedFrom, @Nullable VcsRevisionNumber revisionNumber,
            Date date, String charset) {
        this.remoteFile = depotPath;
        this.changelistId = changelistId;
        this.rev = rev;
        this.action = action;
        this.type = type;
        this.integratedFrom = integratedFrom;
        this.revisionNumber = revisionNumber;
        this.date = date == null ? null : new Date(date.getTime());
        this.charset = charset;
    }

    @NotNull
    @Override
    public P4RemoteFile getFile() {
        return remoteFile;
    }

    @NotNull
    @Override
    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    @NotNull
    @Override
    public P4Revision getRevision() {
        return rev;
    }

    @NotNull
    @Override
    public P4FileAction getFileAction() {
        return action;
    }

    @NotNull
    @Override
    public P4FileType getFileType() {
        return type;
    }

    @Nullable
    @Override
    public P4RemoteFile getIntegratedFrom() {
        return integratedFrom;
    }

    @Nullable
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return revisionNumber;
    }

    @Nullable
    @Override
    public Date getDate() {
        return date;
    }

    @Nullable
    @Override
    public String getCharset() {
        return charset;
    }

    @Override
    public String toString() {
        return remoteFile.toString() + '@' + changelistId + ";#" + revisionNumber;
    }
}
