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
package net.groboclown.p4plugin.actions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsConnectionProblem;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.impl.commands.DoneActionAnswer;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class P4EditAction
        extends BasicAction {
    private static final Logger LOG = Logger.getInstance(P4EditAction.class);

    public P4EditAction() {
        super(P4Bundle.getString("files.edit.action-name"));
    }


    @Override
    protected boolean isEnabled(@NotNull Project project, @NotNull P4Vcs vcs, @NotNull VirtualFile... vFiles) {
        // If an input file does not map to a server, then we still report enabled.  If all the files only map to
        // non-P4 files, then disable.

        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking enabled state for files " + Arrays.asList(vFiles));
        }

        /*

        This seems to have trouble performing the correct calculations.
        Allow it to be shown, which will also skip the possible check
        and speed up the UI operation.  If the user tries to edit the
        file when it's not really possible, then the edit will handle the
        error conditions at that point.

        try {
            final Map<P4Server, List<VirtualFile>> servers = vcs.mapVirtualFilesToOnlineP4Server(Arrays.asList(vFiles));

            // all we care about for open for edit/add is whether
            // the files map to servers or not.  Online or offline
            // modes don't matter.
            return !(servers.isEmpty() || (servers.size() == 1 && servers.containsKey(null)));
        } catch (InterruptedException e) {
            LOG.info(e);
            return true;
        }

        */
        return true;
    }



    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    protected P4CommandRunner.ActionAnswer<?> perform(@NotNull final Project project, @NotNull final P4Vcs vcs,
            @NotNull final List<VcsException> exceptions, @NotNull final List<VirtualFile> affectedFiles) {
        if (affectedFiles.isEmpty()) {
            return new DoneActionAnswer(null);
        }

        // Note: the user is forcing the add action.  Do not inspect the IgnoreFileSet status.

        LocalChangeList defaultChangeList =
                ChangeListManager.getInstance(project).getDefaultChangeList();

        return getFilesByConfiguration(project, affectedFiles)
        .flatMap((Function<Pair<ClientConfig, List<VirtualFile>>, Stream<P4CommandRunner.ActionAnswer>>) (entity) -> {
            P4ChangelistId p4cl;
            try {
                p4cl = CacheComponent.getInstance(project).getServerOpenedCache().first.getP4ChangeFor(
                                entity.first.getClientServerRef(), defaultChangeList);
            } catch (InterruptedException e) {
                exceptions.add(new VcsInterruptedException(e));
                return Stream.empty();
            }
            return entity.second.stream()
                // Make sure we don't try to add a directory.
                .filter(f -> !f.isDirectory())
                .map((file) -> {
                    FilePath path = VcsUtil.getFilePath(file);
                    return P4ServerComponent
                            .perform(project, entity.first, new AddEditAction(path, null, p4cl, path.getCharset(project)))
                    .whenCompleted((r) -> VcsDirtyScopeManager.getInstance(project).filesDirty(Collections.singleton(file), null))
                    .whenServerError((err) -> exceptions.add(new VcsException(err)))
                            // TODO use message bundle
                    .whenOffline(() -> exceptions.add(new VcsConnectionProblem("went offline while editing " + file)))
                    ;
                });
        })
        .reduce((P4CommandRunner.ActionAnswer<?>) new DoneActionAnswer(null),
                (base, next) -> base.mapActionAsync((x) -> next))
        .whenCompleted((x) ->
                LOG.debug("added or edited files: " + affectedFiles))
        ;
    }
}
