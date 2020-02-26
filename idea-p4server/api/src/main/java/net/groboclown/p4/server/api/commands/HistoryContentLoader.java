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

package net.groboclown.p4.server.api.commands;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import net.groboclown.p4.server.api.config.ClientConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public interface HistoryContentLoader {
    @Nullable
    byte[] loadContentForRev(@NotNull ClientConfig config, @NotNull String depotPath, int rev)
            throws IOException, VcsException;

    @Nullable
    byte[] loadContentForLocal(@NotNull ClientConfig config, @NotNull FilePath localFile, int rev)
            throws IOException, VcsException;

    @Nullable
    String loadStringContentForLocal(@NotNull ClientConfig config, @NotNull FilePath localFile, int rev)
            throws IOException, VcsException;
}
