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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.commands.file.GetFileContentsQuery;
import net.groboclown.p4.server.api.commands.file.GetFileContentsResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HistoryContentLoaderImpl implements HistoryContentLoader {
    private final Project project;

    public HistoryContentLoaderImpl(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public byte[] loadContentForRev(@NotNull ClientConfig config, @NotNull String depotPath, int rev)
            throws VcsException {
        return loadContent(config,
                    new GetFileContentsQuery(getDepotPathForRev(depotPath, rev)))
                .getData();
    }

    @Nullable
    @Override
    public byte[] loadContentForLocal(@NotNull ClientConfig config, @NotNull FilePath localFile, int rev)
            throws VcsException {
        return loadContent(config, new GetFileContentsQuery(localFile, rev))
                .getData();
    }

    @Nullable
    @Override
    public String loadStringContentForLocal(@NotNull ClientConfig config, @NotNull FilePath localFile, int rev)
            throws IOException, VcsException {
        return loadContent(config, new GetFileContentsQuery(localFile, rev))
                .getStringData();
    }


    private GetFileContentsResult loadContent(@NotNull ClientConfig config, @NotNull GetFileContentsQuery query)
            throws VcsException {
        try {
            return P4ServerComponent
                    .query(project, config, query)
                    .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
    }

    private String getDepotPathForRev(String depotPath, int rev) {
        if (rev > 0) {
            return depotPath + '#' + rev;
        }
        return depotPath;
    }
}
