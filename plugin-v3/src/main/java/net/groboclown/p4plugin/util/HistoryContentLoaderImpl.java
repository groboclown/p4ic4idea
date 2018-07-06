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

package net.groboclown.p4plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import net.groboclown.p4.server.api.commands.file.GetFileContentsQuery;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.impl.repository.HistoryContentLoader;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

public class HistoryContentLoaderImpl implements HistoryContentLoader {
    private final Project project;

    public HistoryContentLoaderImpl(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public byte[] loadContentForRev(@NotNull ServerConfig config, @NotNull String depotPath, int rev)
            throws VcsException {
        try {
            return P4ServerComponent.getInstance(project)
                    .getCommandRunner()
                    .query(config, new GetFileContentsQuery(depotPath + '#' + rev))
                    .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS)
                    .getData();
        } catch (InterruptedException e) {
            // TODO better exception?
            throw new VcsException(e);
        }
    }
}
