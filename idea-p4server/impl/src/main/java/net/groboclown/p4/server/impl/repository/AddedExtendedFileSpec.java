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

package net.groboclown.p4.server.impl.repository;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IResolveRecord;
import com.perforce.p4java.impl.generic.core.file.FileSpec;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * An extended file spec implementation to represent files open for add or move/add or integrate.
 */
public class AddedExtendedFileSpec
        extends FileSpec implements IExtendedFileSpec {
    public static IExtendedFileSpec create(IFileSpec src) {
        if (src instanceof IExtendedFileSpec) {
            return (IExtendedFileSpec) src;
        }
        if (!(src instanceof FileSpec) || src.getAction() == null) {
            throw new IllegalArgumentException("src (" + src + ") not a file FileSpec, but " + src.getClass().getName());
        }
        return new AddedExtendedFileSpec((FileSpec) src);
    }


    private AddedExtendedFileSpec(FileSpec spec) {
        super(spec);
    }

    // All these methods are related to discovering information about the file information
    // on the server.  Because it doesn't exist on the server, we can return empty values.

    @Override
    public boolean isMapped() {
        return getLocalPath() != null;
    }

    @Override
    public void setMapped(boolean mapped) {
        // ignore
    }

    @Override
    public FileAction getHeadAction() {
        return null;
    }

    @Override
    public void setHeadAction(FileAction action) {
        // ignore
    }

    @Override
    public int getHeadChange() {
        return IChangelist.UNKNOWN;
    }

    @Override
    public void setHeadChange(int change) {
        // ignore
    }

    @Override
    public int getHeadRev() {
        return NONE_REVISION;
    }

    @Override
    public void setHeadRev(int rev) {
        // ignore
    }

    @Override
    public String getHeadType() {
        return null;
    }

    @Override
    public void setHeadType(String type) {
        // ignore
    }

    @Override
    public Date getHeadTime() {
        return null;
    }

    @Override
    public void setHeadTime(Date date) {
        // ignore
    }

    @Override
    public Date getHeadModTime() {
        return null;
    }

    @Override
    public void setHeadModTime(Date date) {
        // ignore
    }

    @Override
    public String getHeadCharset() {
        return null;
    }

    @Override
    public void setHeadCharset(String charset) {
        // ignore
    }

    @Override
    public int getHaveRev() {
        return NONE_REVISION;
    }

    @Override
    public void setHaveRev(int rev) {
        // ignore
    }

    @Override
    public String getDesc() {
        return null;
    }

    @Override
    public void setDesc(String desc) {
        // ignore
    }

    @Override
    public String getDigest() {
        return null;
    }

    @Override
    public void setDigest(String digest) {
        // ignore
    }

    @Override
    public long getFileSize() {
        return 0;
    }

    @Override
    public void setFileSize(long size) {
        // ignore
    }

    @Override
    public FileAction getOpenAction() {
        return getAction();
    }

    @Override
    public void setOpenAction(FileAction action) {
        // ignore
    }

    @Override
    public String getOpenType() {
        return getFileType();
    }

    @Override
    public void setOpenType(String type) {
        // ignore
    }

    @Override
    public String getOpenActionOwner() {
        return null;
    }

    @Override
    public void setOpenActionOwner(String owner) {
        // ignore
    }

    @Override
    public String getCharset() {
        // TODO is there a valid value for this?
        return null;
    }

    @Override
    public void setCharset(String charset) {
        // ignore
    }

    @Override
    public int getOpenChangelistId() {
        return getChangelistId();
    }

    @Override
    public void setOpenChangelistId(int id) {
        // ignore
    }

    @Override
    public boolean isUnresolved() {
        // added files are always resolved.
        return false;
    }

    @Override
    public void setUnresolved(boolean unresolved) {
        // ignore
    }

    @Override
    public boolean isResolved() {
        // added files are always resolved.
        return true;
    }

    @Override
    public void setResolved(boolean resolved) {
        // ignore
    }

    @Override
    public boolean isReresolvable() {
        return false;
    }

    @Override
    public void setReresolvable(boolean reresolvable) {
        // ignore
    }

    @Override
    public boolean isOtherLocked() {
        return false;
    }

    @Override
    public void setOtherLocked(boolean otherLocked) {
        // ignore
    }

    @Override
    public List<String> getOtherOpenList() {
        return Collections.emptyList();
    }

    @Override
    public void setOtherOpenList(List<String> otherOpenList) {
        // ignore
    }

    @Override
    public List<String> getOtherChangelist() {
        return Collections.emptyList();
    }

    @Override
    public void setOtherChangelist(List<String> otherChangelist) {
        // ignore
    }

    @Override
    public List<String> getOtherActionList() {
        return Collections.emptyList();
    }

    @Override
    public void setOtherActionList(List<String> actionList) {
        // ignore
    }

    @Override
    public boolean isShelved() {
        return false;
    }

    @Override
    public String getActionOwner() {
        return null;
    }

    @Override
    public void setActionOwner(String actionOwner) {
        // ignore
    }

    @Override
    public List<IResolveRecord> getResolveRecords() {
        return Collections.emptyList();
    }

    @Override
    public void setResolveRecords(List<IResolveRecord> resolveRecords) {
        // ignore
    }

    @Override
    public String getMovedFile() {
        return null;
    }

    @Override
    public void setMovedFile(String movedFile) {
        // ignore
    }

    @Override
    public String getVerifyStatus() {
        return null;
    }

    @Override
    public void setVerifyStatus(String status) {
        // ignore
    }

    @Override
    public Map<String, byte[]> getAttributes() {
        return Collections.emptyMap();
    }
}
