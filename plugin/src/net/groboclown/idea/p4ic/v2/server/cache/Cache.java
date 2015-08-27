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

package net.groboclown.idea.p4ic.v2.server.cache;

import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ClientFileMapping;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4FileUpdateState;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Delegates to all the {@link CacheFrontEnd} objects.  It's a top-level
 * constructed class that allows those objects to talk to each other.
 * <p/>
 * This is package protected, because it should only be used by the
 * {@link CacheFrontEnd} objects.
 */
interface Cache {
    @NotNull
    Client getClient();

    @NotNull
    String getClientName();

    @NotNull
    P4ClientFileMapping getClientMappingFor(@NotNull FilePath file);

    @NotNull
    Collection<P4FileUpdateState> fromOpenedToAction(
            @NotNull List<IFileSpec> validSpecs, @NotNull AlertManager alerts);

    /**
     * Called when the server state is found to be horribly out-of-sync
     * with the locally cached version of it.
     */
    void refreshServerState();

    /**
     * Is the file ignored?
     *
     * @param file
     * @return
     */
    boolean isFileIgnored(@Nullable FilePath file);
}
