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

package net.groboclown.idea.p4ic.v2.file;

import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// TODO eliminate this lock; it could make for a significant performance impact on some systems.
// (the new design means that this lock isn't necessary)
public class FileExtensions {
    private final P4Vcs vcs;
    private final AlertManager alerts;
    private final Lock vfsLock = new ReentrantLock();


    public FileExtensions(@NotNull P4Vcs vcs, @NotNull AlertManager alerts) {
        this.vcs = vcs;
        this.alerts = alerts;
    }

    @NotNull
    public P4EditFileProvider createEditFileProvider() {
        return new P4EditFileProvider(vcs, vfsLock);
    }


    @NotNull
    public P4RollbackEnvironment createRollbackEnvironment() {
        return new P4RollbackEnvironment(vcs, vfsLock);
    }


    @NotNull
    public P4VFSListener createVcsVFSListener() {
        return new P4VFSListener(vcs, alerts, vfsLock);
    }
}
