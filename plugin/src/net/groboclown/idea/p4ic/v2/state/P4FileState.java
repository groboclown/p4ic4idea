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

package net.groboclown.idea.p4ic.v2.state;

import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.core.file.FileAction;

import java.util.Date;

public class P4FileState implements CachedState {
    private P4ClientFile file;
    private int rev;
    private int changelist;
    private FileAction action;
    private Date lastRead;


    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o.getClass().equals(P4FileState.class)) {
            P4FileState that = (P4FileState) o;
            return that.file.equals(file) && that.rev == rev;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (file.hashCode() << 3) + rev;
    }

    @Override
    public Date getLastUpdated() {
        return null;
    }

    void refreshInternalState(String depotPath, FilePath localPath, int rev) {
        file.refreshInternalState(depotPath, localPath);
        this.rev = rev;
    }
}
