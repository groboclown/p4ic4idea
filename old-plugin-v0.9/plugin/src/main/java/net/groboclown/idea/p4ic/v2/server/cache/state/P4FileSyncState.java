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

import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encompasses all the information about a "have" version of a file.
 */
public final class P4FileSyncState extends CachedState {
    @NotNull
    private final P4ClientFileMapping file;
    private int rev;
    private String md5;

    public P4FileSyncState(@NotNull final P4ClientFileMapping file) {
        this.file = file;
    }

    void setRev(int rev) {
        this.rev = rev;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
        setUpdated();
    }

    public String getMd5() {
        return this.md5;
    }

    public int getRev() {
        return this.rev;
    }

    @Nullable
    public VirtualFile getVirtualFile() {
        if (file.getLocalFilePath() == null) {
            return null;
        }
        return file.getLocalFilePath().getVirtualFile();
    }

    @NotNull
    public IFileSpec getFileSpec() throws P4Exception {
        return file.getFileSpec();
    }

    public void update(@NotNull final IFileSpec fileSpec, @NotNull final FileMappingRepo fileMappingRepo) {
        if (P4StatusMessage.isValid(fileSpec)) {
            if (fileSpec.getDepotPathString() != null) {
                fileMappingRepo.updateDepotPath(file, fileSpec.getDepotPathString());
            }
            if (fileSpec.getLocalPathString() != null) {
                fileMappingRepo.updateLocation(file,
                        FilePathUtil.getFilePath(fileSpec.getLocalPathString()));
            }
            if (fileSpec.getEndRevision() >= 0) {
                // the md5 is a delicate thing
                if (rev != fileSpec.getEndRevision()) {
                    md5 = null;
                }
                this.rev = fileSpec.getEndRevision();
            } else {
                // Unknown problem, probably not on client
                rev = IFileSpec.NONE_REVISION;
                md5 = null;
            }
        } else {
            // not on client
            rev = IFileSpec.NONE_REVISION;
            md5 = null;
        }
        setUpdated();
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
        wrapper.setAttribute("m5", md5 == null ? "" : md5);
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
        String md5 = getAttribute(wrapper, "m5");
        ret.md5 = md5 == null || md5.length() <= 0 ? null : md5;
        return ret;
    }
}
