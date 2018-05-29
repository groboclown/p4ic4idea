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

import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.api.values.P4Revision;
import net.groboclown.p4.server.impl.values.P4FileRevisionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class P4FileRevisionStore {

    @SuppressWarnings("WeakerAccess")
    public static class State {
        public P4RemoteFileStore.State remoteFile;
        public P4ChangelistIdStore.State changelistId;
        public int rev;
        public P4FileAction action;
        public String type;
        public P4RemoteFileStore.State integratedFrom;
        public int revisionNumber;
        public long date;
        public String charset;
    }

    @Nullable
    public static State getStateNullable(@Nullable P4FileRevision file) {
        if (file == null) {
            return null;
        }
        return getState(file);
    }

    @NotNull
    public static State getState(@NotNull P4FileRevision rev) {
        State ret = new State();
        ret.remoteFile = P4RemoteFileStore.getState(rev.getFile());
        ret.changelistId = P4ChangelistIdStore.getState(rev.getChangelistId());
        ret.rev = rev.getRevision().getValue();
        ret.action = rev.getFileAction();
        ret.type = rev.getFileType().toString();
        ret.integratedFrom = P4RemoteFileStore.getStateNullable(rev.getIntegratedFrom());
        ret.revisionNumber = rev.getRevisionNumber() == null
                ? -1
                : ((VcsRevisionNumber.Int) rev.getRevisionNumber()).getValue();
        ret.date = rev.getDate() == null
                ? -1
                : rev.getDate().getTime();
        ret.charset = rev.getCharset();
        return ret;
    }

    @Nullable
    public static P4FileRevision readNullable(@Nullable State state) {
        if (state == null) {
            return null;
        }
        return read(state);
    }

    @NotNull
    public static P4FileRevision read(@NotNull State state) {
        return new P4FileRevisionImpl(
                P4RemoteFileStore.read(state.remoteFile),
                P4ChangelistIdStore.read(state.changelistId),
                new P4Revision(state.rev),
                state.action,
                P4FileType.convert(state.type),
                P4RemoteFileStore.readNullable(state.integratedFrom),
                state.revisionNumber < 0
                    ? null
                    : new VcsRevisionNumber.Int(state.revisionNumber),
                state.date < 0
                    ? null
                    : new Date(state.date),
                state.charset
        );
    }
}
