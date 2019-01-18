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

package net.groboclown.p4.server.impl.client;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.commands.client.ListOpenedFilesChangesResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4LocalChangelist;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.impl.values.P4LocalChangelistImpl;
import net.groboclown.p4.server.impl.values.P4LocalFileImpl;
import net.groboclown.p4.server.impl.values.P4RemoteFileImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OpenedFilesChangesFactory {
    public static ListOpenedFilesChangesResult createListOpenedFilesChangesResult(
            @NotNull ClientConfig config,
            @NotNull List<IChangelist> changes,
            @NotNull List<IExtendedFileSpec> pendingChangelistFiles,
            @NotNull Map<Integer, List<IFileSpec>> shelvedFiles,
            @NotNull List<IExtendedFileSpec> openedDefaultChangelistFiles) {

        return new ListOpenedFilesChangesResult(config,
                createOpenedFiles(config.getClientServerRef(), pendingChangelistFiles, openedDefaultChangelistFiles),
                createOpenedChanges(config, changes, pendingChangelistFiles, shelvedFiles,
                        openedDefaultChangelistFiles));

    }

    private static Collection<P4LocalFile> createOpenedFiles(
            @NotNull ClientServerRef ref,
            @NotNull List<IExtendedFileSpec> pendingChangelistFiles,
            @NotNull List<IExtendedFileSpec> openedDefaultChangelistFiles) {
        List<P4LocalFile> ret = new ArrayList<>(pendingChangelistFiles.size() + openedDefaultChangelistFiles.size());
        for (IExtendedFileSpec spec : pendingChangelistFiles) {
            ret.add(new P4LocalFileImpl(ref, spec));
        }
        for (IExtendedFileSpec spec : openedDefaultChangelistFiles) {
            ret.add(new P4LocalFileImpl(ref, spec));
        }
        return ret;
    }

    private static Collection<P4LocalChangelist> createOpenedChanges(
            @NotNull ClientConfig clientConfig,
            @NotNull List<IChangelist> changes, @NotNull List<IExtendedFileSpec> pendingChangelistFiles,
            @NotNull Map<Integer, List<IFileSpec>> shelvedFiles,
            @NotNull List<IExtendedFileSpec> openedDefaultChangelistFiles) {

        List<P4LocalChangelist> ret = new ArrayList<>(changes.size() + 1);

        // Default changelist
        ret.add(new P4LocalChangelistImpl.Builder()
                .withDefaultChangelist(clientConfig.getClientServerRef())
                .withClientname(clientConfig.getClientname())
                .withUsername(clientConfig.getServerConfig().getUsername())
                .withComment("")
                .withContainedFiles(getLocalFiles(openedDefaultChangelistFiles))
                .build());

        for (IChangelist change : changes) {
            ret.add(
                    new P4LocalChangelistImpl.Builder()
                            .withChangelistId(clientConfig.getClientServerRef(), change.getId())
                            .withClientname(clientConfig.getClientname())
                            .withUsername(clientConfig.getServerConfig().getUsername())
                            .withComment(change.getDescription())
                            .withShelvedFiles(toShelvedFiles(shelvedFiles.get(change.getId())))
                            .withDeleted(false)
                            .withContainedFiles(toFiles(change, pendingChangelistFiles))
                            .build()
            );
        }

        return ret;
    }

    @NotNull
    private static List<P4RemoteFile> toShelvedFiles(@Nullable List<IFileSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return Collections.emptyList();
        }
        List<P4RemoteFile> ret = new ArrayList<>(specs.size());
        for (IFileSpec spec : specs) {
            ret.add(new P4RemoteFileImpl(spec));
        }
        return ret;
    }

    @NotNull
    static List<FilePath> getLocalFiles(@NotNull List<IExtendedFileSpec> files) {
        List<FilePath> ret = new ArrayList<>(files.size());

        // For this particular call, the path to the file is stored in the localPath.
        for (IExtendedFileSpec file : files) {
            // Local paths do not need to be stripped of annotations or unescaped.
            ret.add(VcsUtil.getFilePath(file.getLocalPath().getPathString(), false));
        }

        return ret;
    }

    @NotNull
    private static List<FilePath> toFiles(IChangelist change, List<IExtendedFileSpec> pendingChangelistFiles) {
        List<FilePath> ret = new ArrayList<>();
        for (IExtendedFileSpec spec : pendingChangelistFiles) {
            if (spec.getChangelistId() == change.getId()) {
                // Local paths do not need to be stripped of annotations or unescaped.
                ret.add(VcsUtil.getFilePath(spec.getLocalPath().getPathString(), false));
            }
        }
        return ret;
    }
}
