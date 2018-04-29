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

package net.groboclown.p4plugin.ui.pending;

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4JobState;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ShelvedFile;

import java.util.List;

public class PendingChangeItemSet {
    private final List<FilePath> files;
    private final List<P4ShelvedFile> shelved;
    private final List<P4JobState> jobs;

    public PendingChangeItemSet(List<FilePath> files,
            List<P4ShelvedFile> shelved, List<P4JobState> jobs) {
        this.files = files;
        this.shelved = shelved;
        this.jobs = jobs;
    }
}
