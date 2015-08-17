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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encompasses all the information about a "have" version of a file.
 */
public class P4FileSyncState extends CachedState {
    @NotNull
    private final P4ClientFileMapping file;
    private int rev;

    public P4FileSyncState(@NotNull final P4ClientFileMapping file) {
        this.file = file;
    }


    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o.getClass().equals(P4FileSyncState.class)) {
            P4FileSyncState that = (P4FileSyncState) o;
            return that.file.equals(file) && that.rev == rev;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (file.hashCode() << 3) + rev;
    }

    @Override
    protected void serialize(@NotNull final Element wrapper, @NotNull final EncodeReferences refs) {
        wrapper.setAttribute("f", refs.getFileMappingId(file));
        wrapper.setAttribute("r", encodeLong(rev));
        serializeDate(wrapper);
    }

    @Nullable
    protected static P4FileSyncState deserialize(@NotNull final Element wrapper,
            @NotNull final DecodeReferences refs) {
        P4ClientFileMapping file = refs.getFileMapping(getAttribute(wrapper, "f"));
        if (file == null) {
            return null;
        }
        P4FileSyncState ret = new P4FileSyncState(file);
        ret.deserializeDate(wrapper);
        Long r = decodeLong(getAttribute(wrapper, "r"));
        ret.rev = (r == null) ? -1 : r.intValue();
        return ret;
    }
}
