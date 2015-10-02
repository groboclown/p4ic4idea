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

import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IChangelistSummary.Visibility;
import org.jdom.Element;
import org.jdom.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class P4ChangeListState extends CachedState {
    private int id;
    private String comment;
    private final Set<P4JobState> jobs = new HashSet<P4JobState>();
    private String fixState;
    private boolean isShelved = false;
    private boolean isRestricted = false;
    private boolean isDeleted = false;

    P4ChangeListState() {
        // intentionally empty
    }

    public P4ChangeListState(@NotNull IChangelistSummary summary) {
        id = summary.getId();
        comment = summary.getDescription();
        isRestricted = summary.getVisibility() == Visibility.RESTRICTED;
    }

    public P4ChangeListState(int dummyChangeListId) {
        this.id = dummyChangeListId;
    }


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

    public String getComment() {
        return comment;
    }

    public void setDeleted(final boolean deleted) {
        this.isDeleted = deleted;
    }

    public boolean isDeleted() {
        return isDeleted;
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
        String shelved = getAttribute(wrapper, "s");
        if (shelved != null && shelved.equals("y")) {
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
