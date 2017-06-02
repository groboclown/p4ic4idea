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

package net.groboclown.idea.p4ic.v2.server.cache.state;

import com.intellij.openapi.vcs.FileStatus;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IChangelistSummary.Visibility;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import org.jdom.Element;
import org.jdom.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This changelist only keeps information regarding the non-file aspects of the changelist.  The
 * files are handled as their own independent objects.  It should be the responsibility of the
 * wrapping object to make the connection between the file changelist and these objects.
 * <p/>
 * If used in a hash or set, the determining factor is the changelist number; it's up
 * the user of the instances to ensure data isn't lost.
 */
public class P4ChangeListState extends UpdateRef {

    private static final FileStatus[] SHELVED_FILE_STATUSES = {
            P4Vcs.SHELVED_ADDED, P4Vcs.SHELVED_DELETED, P4Vcs.SHELVED_MODIFIED, P4Vcs.SHELVED_UNKNOWN
    };

    private int id;
    private String comment;
    private final Set<P4JobState> jobs = new HashSet<P4JobState>();
    private String fixState;
    private boolean isShelved = false;
    private boolean isRestricted = false;
    private boolean isDeleted = false;
    private final Set<P4ShelvedFile> shelved = new HashSet<P4ShelvedFile>();

    private P4ChangeListState() {
        // intentionally empty
        super(null);
    }


    // This is a server-side state, which means no pending update is
    // associated with it.
    public P4ChangeListState(@NotNull IChangelistSummary summary) {
        super(null);
        id = summary.getId();
        comment = summary.getDescription();
        isRestricted = summary.getVisibility() == Visibility.RESTRICTED;
    }

    public P4ChangeListState(@NotNull PendingUpdateState state, int dummyChangeListId) {
        super(state);
        this.id = dummyChangeListId;
    }

    public P4ChangeListState(@NotNull PendingUpdateState update, @NotNull P4ChangeListState remote) {
        super(update);
        this.id = remote.getChangelistId();
        this.comment = remote.getComment();
        this.jobs.addAll(remote.getJobs());
        this.fixState = remote.fixState;
        this.isShelved = remote.isShelved();
        this.isRestricted = remote.isRestricted();
        this.isDeleted = remote.isDeleted();
        this.shelved.addAll(remote.getShelved());
    }

    /*
    public P4ChangeListState(@NotNull P4ChangeListState remote) {
        this.id = remote.getChangelistId();
        this.comment = remote.getComment();
        this.jobs.addAll(remote.getJobs());
        this.isShelved = remote.isShelved();
        this.isRestricted = remote.isRestricted();
        this.isDeleted = remote.isDeleted();
    }
    */


    public boolean isDefault() {
        return id == 0;
    }

    public boolean isOnServer() {
        return id >= 0;
    }

    public int getChangelistId() {
        return id;
    }

    public void addJob(@NotNull P4JobState job) {
        this.jobs.add(job);
    }

    public void addShelved(@NotNull IExtendedFileSpec spec) {
        String depotPath = spec.getDepotPathString();
        String clientPath = spec.getClientPathString();
        FileStatus status = getShelvedFileStatusFor(spec.getAction());
        this.shelved.add(new P4ShelvedFile(depotPath, clientPath, status));
    }

    private static FileStatus getShelvedFileStatusFor(FileAction action) {
        switch (action) {
            case ADD:
            case ADD_EDIT:
            case ADDED:
            case BRANCH:
            case MOVE:
            case MOVE_ADD:
            case COPY_FROM:
            case MERGE_FROM:
                return P4Vcs.SHELVED_ADDED;

            case EDIT:
            case INTEGRATE:
            case REPLACED:
            case UPDATED:
            case EDIT_FROM:
                return P4Vcs.SHELVED_MODIFIED;

            case DELETE:
            case DELETED:
            case MOVE_DELETE:
                return P4Vcs.SHELVED_DELETED;

            case SYNC:
            case REFRESHED:
            case IGNORED:
            case ABANDONED:
            case EDIT_IGNORED:
            case RESOLVED:
            case UNRESOLVED:
            case PURGE:
            case IMPORT:
            case ARCHIVE:
            case UNKNOWN:
            default:
                return P4Vcs.SHELVED_UNKNOWN;
        }
    }

    @NotNull
    public Set<P4JobState> getJobs() {
        return Collections.unmodifiableSet(jobs);
    }

    @NotNull
    public Set<P4ShelvedFile> getShelved() {
        return Collections.unmodifiableSet(shelved);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(@NotNull String comment) {
        this.comment = comment;
    }

    public void setDeleted(final boolean deleted) {
        this.isDeleted = deleted;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public boolean isShelved() {
        return isShelved;
    }

    public boolean isRestricted() {
        return isRestricted;
    }

    @Override
    public String toString() {
        return "Change " + id + " (" + comment + "); " + jobs +
                ";" + (isShelved ? "" : " not") + " shelved;" +
                (isRestricted ? "" : " not") + " restricted;" +
                (isDeleted ? "" : " not") + " deleted";
    }

    @Override
    protected void serialize(@NotNull final Element wrapper, @NotNull final EncodeReferences refs) {
        serializeDate(wrapper);
        wrapper.setAttribute("c", encodeLong(id));
        if (comment != null) {
            wrapper.addContent(new Text(comment));
        }
        if (isShelved) {
            wrapper.setAttribute("s", "y");
        }
        if (isRestricted) {
            wrapper.setAttribute("v", "r");
        }
        if (isDeleted) {
            wrapper.setAttribute("d", "y");
        }
        if (fixState != null) {
            wrapper.setAttribute("f", fixState);
        }
        for (P4ShelvedFile shelvedFile : shelved) {
            Element el = new Element("h");
            wrapper.addContent(el);
            el.setAttribute("d", shelvedFile.getDepotPath());
            el.setAttribute("l", shelvedFile.getLocalPath());
            el.setAttribute("s", shelvedFile.getStatus().getId());
        }
        for (P4JobState job : jobs) {
            Element el = new Element("j");
            wrapper.addContent(el);
            el.setAttribute("r", refs.getJobId(job));
        }
    }

    @Nullable
    protected static P4ChangeListState deserialize(@NotNull final Element wrapper,
            @NotNull final DecodeReferences refs) {
        P4ChangeListState ret = new P4ChangeListState();
        ret.deserializeDate(wrapper);
        Long cid = decodeLong(getAttribute(wrapper, "c"));
        if (cid != null) {
            ret.id = cid.intValue();
        } else {
            return null;
        }
        ret.comment = wrapper.getText();
        for (Element el: wrapper.getChildren("j")) {
            P4JobState job = refs.getJob(getAttribute(el, "r"));
            if (job != null) {
                ret.jobs.add(job);
            }
        }
        for (Element el: wrapper.getChildren("h")) {
            P4ShelvedFile file = createShelvedFile(
                    getAttribute(el, "d"),
                    getAttribute(el, "l"),
                    getAttribute(el, "s")
            );
            if (file != null) {
                ret.shelved.add(file);
            }
        }
        String shelvedState = getAttribute(wrapper, "s");
        if (shelvedState != null && shelvedState.equals("y")) {
            ret.isShelved = true;
        }
        String visibility = getAttribute(wrapper, "v");
        if (visibility != null && visibility.equals("r")) {
            ret.isRestricted = true;
        }
        String deleted = getAttribute(wrapper, "d");
        if (deleted != null && deleted.equals("y")) {
            ret.isDeleted = true;
        }
        ret.fixState = getAttribute(wrapper, "f");

        return ret;
    }

    @Nullable
    private static P4ShelvedFile createShelvedFile(@Nullable String depotPath, @Nullable String localPath,
            @Nullable String status) {
        if (status == null || depotPath == null || localPath == null) {
            return null;
        }
        for (FileStatus shelvedFileStatus : SHELVED_FILE_STATUSES) {
            if (shelvedFileStatus.getId().equals(status)) {
                return new P4ShelvedFile(depotPath, localPath, shelvedFileStatus);
            }
        }
        return null;
    }


    @Override
    public int hashCode() {
        return getChangelistId();
    }


    @Override
    public boolean equals(Object val) {
        if (val == null || ! val.getClass().equals(getClass())) {
            return false;
        }
        if (val == this) {
            return true;
        }
        P4ChangeListState that = (P4ChangeListState) val;
        // In terms of Set usage, this is all that matters.
        // It's up to the user to correctly organize these so
        // that no data is lost.
        return that.getChangelistId() == this.getChangelistId();
    }
}
