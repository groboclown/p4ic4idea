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
package net.groboclown.idea.p4ic.extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vcs.checkin.BaseCheckinHandlerFactory;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.util.PairConsumer;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO this overlaps with {@link P4CheckinEnvironment} - need to discover
 * the where and how.  This one only seems to be used as a validation for the checkin.
 * All the other elements of it seem to be ignored.
 */
public class P4CheckinHandler extends CheckinHandler {
    private static final Logger LOG = Logger.getInstance(P4CheckinHandler.class);

    private final CheckinProjectPanel panel;
    private final CheckinRefreshableComponent component;
    private final P4Vcs vcs;

    public P4CheckinHandler(@NotNull CheckinProjectPanel panel) {
        this.panel = panel;
        component = new CheckinRefreshableComponent();
        P4Vcs vcsVal;
        try {
            vcsVal = P4Vcs.getInstance(panel.getProject());
        } catch (NullPointerException e) {
            vcsVal = null;
        }
        vcs = vcsVal;
    }

    @Nullable
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        if (vcs == null) {
            return null;
        }

        // TODO add the change list jobs to the component.
        final Collection<P4ChangeListId> changelists = getP4Changes();

        return component;
    }


    /**
     * Performs the before check-in processing when a custom commit executor is used. The method can use the
     * {@link com.intellij.openapi.vcs.CheckinProjectPanel} instance passed to
     * {@link BaseCheckinHandlerFactory#createHandler(com.intellij.openapi.vcs.CheckinProjectPanel, CommitContext)} to
     * get information about the files to be checked in.
     *
     * @param executor the commit executor, or null if the standard commit operation is executed.
     * @param additionalDataConsumer
     * @return the code indicating whether the check-in operation should be performed or aborted.
     */
    public ReturnResult beforeCheckin(@Nullable CommitExecutor executor, PairConsumer<Object, Object> additionalDataConsumer) {
        // TODO take advantage of the executor
        return beforeCheckin();
    }


    @Override
    public ReturnResult beforeCheckin() {
        LOG.debug("Checkin handler: beforeCheckin");

        if (emptyCommitMessage() || vcs == null) {
            return ReturnResult.CANCEL;
        }

        final Collection<P4ChangeListId> changelists = getP4Changes();

        // TODO Add jobs to the changelist, and remove non-selected jobs
        // These are currently setup to be submitted with the
        // submit request.

        // TODO how to tell the commit action the job status?

        return ReturnResult.COMMIT;
    }


    Collection<P4ChangeListId> getP4Changes() {
        // TODO look to moving this into P4ChangeListMapping
        final Collection<Change> changes = panel.getSelectedChanges();
        Set<P4ChangeListId> ret = new HashSet<P4ChangeListId>();
        for (Change change: changes) {
            final LocalChangeList idea = ChangeListManager.getInstance(vcs.getProject()).getChangeList(change);
            final Collection<P4ChangeListId> p4Changes = vcs.getChangeListMapping().getPerforceChangelists(idea);
            if (p4Changes != null) {
                ret.addAll(p4Changes);
            }
        }
        return ret;
    }



    private boolean emptyCommitMessage() {
        if (panel.getCommitMessage().trim().isEmpty()) {
            Messages.showMessageDialog(panel.getComponent(),
                    P4Bundle.message("commit.message.empty"),
                    P4Bundle.message("commit.message.empty.title"),
                    Messages.getErrorIcon());
            return true;
        }
        return false;
    }


    /**
     * Performs the processing on successful check-in. The method can use the
     * {@link com.intellij.openapi.vcs.CheckinProjectPanel} instance passed to
     * {@link com.intellij.openapi.vcs.checkin.BaseCheckinHandlerFactory#createHandler(com.intellij.openapi.vcs.CheckinProjectPanel, CommitContext)} to
     * get information about the checked in files.
     */
    public void checkinSuccessful() {
        LOG.info("CheckinHandler.checkinSuccessful");
    }

    /**
     * Performs the processing on failed check-in. The method can use the
     * {@link com.intellij.openapi.vcs.CheckinProjectPanel} instance passed to
     * {@link com.intellij.openapi.vcs.checkin.BaseCheckinHandlerFactory#createHandler(CheckinProjectPanel, CommitContext)} to
     * get information about the checked in files.
     *
     * @param exception the list of VCS exceptions identifying the problems that occurred during the
     *                  commit operation.
     */
    public void checkinFailed(List<VcsException> exception) {
        LOG.info("CheckinHandler.checkinFailed " + exception);
    }

    /**
     * Called to notify handler that user has included/excluded some changes to/from commit
     */
    public void includedChangesChanged() {
        System.out.println("CheckinHandler.includedChangesChanged");
    }

    /**
     * allows to skip before checkin steps when is not applicable. E.g. there should be no check for todos before shelf/create patch
     *
     * @param executor current operation (null for commit)
     * @return true if handler should be skipped
     */
    public boolean acceptExecutor(CommitExecutor executor) {
        //System.out.println("CheckinHandler.acceptExecutor " + executor);
        return executor == null || !(executor instanceof LocalCommitExecutor);
    }


    static class CheckinRefreshableComponent implements RefreshableOnComponent {

        @Override
        public JComponent getComponent() {
            return null;
        }

        @Override
        public void refresh() {

        }

        @Override
        public void saveState() {

        }

        @Override
        public void restoreState() {

        }
    }
}
