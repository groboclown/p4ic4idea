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
 */
public class P4ChangeListState extends CachedState {
    private int id;
    private String comment;
    private final Set<P4JobState> jobs = new HashSet<P4JobState>();
    private String fixState;

    public boolean isDefault() {
        return id == 0;
    }

    public boolean isOnServer() {
        return id >= 0;
    }

    @Override
    protected void serialize(@NotNull final Element wrapper, @NotNull final EncodeReferences refs) {
        serializeDate(wrapper);
        wrapper.setAttribute("c", encodeLong(id));
        if (comment != null) {
            wrapper.addContent(new Text(comment));
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

        return ret;
    }
}
