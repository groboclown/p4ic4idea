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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.idea.p4ic.v2.server.cache.FileUpdateAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encompasses all the information about an update to a file (edit, add, delete, integrate, move).
 */
public final class P4FileUpdateState extends UpdateRef {
    private static final Logger LOG = Logger.getInstance(P4FileUpdateState.class);

    // NOTE: this does not implement hashCode or equals.  Due to the changing
    // nature of the class, it leads to too many problems when this is used
    // in a hash set or as a hash map key.  Instead, use an invariant
    // as the key.


    // TODO include information about a cached backup file (for reverts)


    @NotNull
    private final P4ClientFileMapping file;
    /** 0 for the default changelist, &lt; -1 for a locally stored changelist (no server side number),
     * -1 for not assigned to a changelist (like what Revert operations do), else a numbered changelist. */
    private int activeChangelist;
    @NotNull
    private FileUpdateAction action;
    /** If this is an integrate, this reference sthe source of the integrate. */
    @Nullable
    private P4ClientFileMapping integrateSource;


    private P4FileUpdateState(@NotNull final P4ClientFileMapping file, int changelist, @NotNull FileUpdateAction action) {
        super(null);
        this.file = file;
        this.activeChangelist = changelist;
        this.action = action;
    }

    public P4FileUpdateState(@NotNull PendingUpdateState state, @NotNull final P4ClientFileMapping file, int changelist,
            @NotNull FileUpdateAction action) {
        super(state);
        this.file = file;
        this.activeChangelist = changelist;
        this.action = action;
    }

    public P4FileUpdateState(@NotNull final P4ClientFileMapping file, int changelist,
            @NotNull FileUpdateAction action, boolean isServer) {
        super(null);
        assert isServer;
        this.file = file;
        this.activeChangelist = changelist;
        this.action = action;
    }

    @NotNull
    public FileUpdateAction getFileUpdateAction() {
        return action;
    }

    public int getActiveChangelist() {
        return activeChangelist;
    }


    @NotNull
    public P4ClientFileMapping getClientFileMapping() {
        return file;
    }


    @Nullable
    public String getDepotPath() {
        return file.getDepotPath();
    }

    @Nullable
    public FilePath getLocalFilePath() {
        return file.getLocalFilePath();
    }

    @Override
    protected void serialize(@NotNull final Element wrapper, @NotNull final EncodeReferences refs) {
        wrapper.setAttribute("f", refs.getFileMappingId(file));
        wrapper.setAttribute("c", encodeLong(activeChangelist));
        wrapper.setAttribute("a", action.name());
        if (integrateSource != null) {
            wrapper.setAttribute("s", refs.getFileMappingId(integrateSource));
        }
        serializeDate(wrapper);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Serialized P4FileUpdateState " + file);
        }
    }

    @Override
    public String toString() {
        return file + "@" + activeChangelist + " " + action.toString().toLowerCase();
    }

    @Nullable
    protected static P4FileUpdateState deserialize(@NotNull final Element wrapper,
            @NotNull final DecodeReferences refs) {
        P4ClientFileMapping file = refs.getFileMapping(getAttribute(wrapper, "f"));
        if (file == null) {
            LOG.warn("no file defined for file update state");
            return null;
        }
        Long c = decodeLong(getAttribute(wrapper, "c"));
        String a = getAttribute(wrapper, "a");
        if (a == null) {
            LOG.warn("no action defined for file update state of " + file);
            return null;
        }
        FileUpdateAction action;
        try {
            action = FileUpdateAction.valueOf(a);
        } catch (IllegalArgumentException e) {
            LOG.warn("unknown file update action " + a);
            return null;
        }
        P4FileUpdateState ret = new P4FileUpdateState(file, (c == null) ? -1 : c.intValue(), action);
        ret.integrateSource = refs.getFileMapping(getAttribute(wrapper, "s"));
        ret.deserializeDate(wrapper);
        return ret;
    }
}
