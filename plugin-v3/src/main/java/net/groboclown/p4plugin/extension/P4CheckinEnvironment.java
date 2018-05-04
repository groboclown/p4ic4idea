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
package net.groboclown.p4plugin.extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.checkin.CheckinChangeListSpecificComponent;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.NullableFunction;
import com.intellij.util.PairConsumer;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

// TODO look at switching to a CommitExecutor and CommitSession, CommitSessionContextAware
// to fix the long standing issue where changelists must have comments.
public class P4CheckinEnvironment implements CheckinEnvironment {
    private static final Logger LOG = Logger.getInstance(P4CheckinEnvironment.class);

    private final P4Vcs vcs;

    public P4CheckinEnvironment(@NotNull P4Vcs vcs) {
        this.vcs = vcs;
    }

    @Nullable
    @Override
    public RefreshableOnComponent createAdditionalOptionsPanel(CheckinProjectPanel panel, PairConsumer<Object, Object> additionalDataConsumer) {
        // #52 - we could be able to monitor panel.getCommitMessage(); to ensure
        // that there's a message, and when there isn't, disable the submit
        // button.
        // We can monitor the message (with a thread, disposing of it is
        // a bit of a bother, but it's doable).  However, disabling the button
        // currently requires:
        //  1. Grabbing the owning dialog object of the panel.
        //  2. Using reflection to make the protected "disableOKButton" method
        //     accessible and callable.
        // All of this means that this isn't a very good idea.
        // The panel has a "setWarning" method, but that doesn't do anything.

        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Nullable
    @Override
    public String getDefaultMessageFor(FilePath[] filesToCheckin) {
        return null;
    }

    @Nullable
    @Override
    public String getHelpId() {
        return null;
    }

    @Override
    public String getCheckinOperationName() {
        return P4Bundle.message("commit.operation.name");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public List<VcsException> commit(List<Change> changes, String preparedComment) {
        return commit(changes, preparedComment, NullableFunction.NULL, new HashSet<String>());
    }

    @Nullable
    @Override
    public List<VcsException> commit(List<Change> changes, final String preparedComment,
            @NotNull NullableFunction<Object, Object> parametersHolder, Set<String> feedback) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Nullable
    @Override
    public List<VcsException> scheduleMissingFileForDeletion(List<FilePath> files) {
        LOG.info("scheduleMissingFileForDeletion: " + files);
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Nullable
    @Override
    public List<VcsException> scheduleUnversionedFilesForAddition(List<VirtualFile> files) {
        LOG.info("scheduleUnversionedFilesForAddition: " + files);
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Override
    public boolean keepChangeListAfterCommit(ChangeList changeList) {
        return false;
    }

    @Override
    public boolean isRefreshAfterCommitNeeded() {
        // File status (read-only state) may have changed, or CVS substitution
        // may have happened.
        return true;
    }

}
