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

package net.groboclown.p4.server.api.values;

import com.intellij.openapi.vcs.versionBrowser.VcsRevisionNumberAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.Immutable;
import java.util.Date;

/**
 * Represents a revision of a file on the server.
 */
@Immutable
public interface P4FileRevision
        extends VcsRevisionNumberAware {
    @NotNull
    P4RemoteFile getFile();

    @NotNull
    P4ChangelistId getChangelistId();

    @NotNull
    P4Revision getRevision();

    @NotNull
    P4FileAction getFileAction();

    @NotNull
    P4FileType getFileType();

    /**
     *
     * @return null if the change was not an integration, or if the integration source is not known
     *      (due to the operation that requested the information).
     */
    @Nullable
    P4RemoteFile getIntegratedFrom();

    @Nullable
    Date getDate();

    @Nullable
    String getCharset();
}
