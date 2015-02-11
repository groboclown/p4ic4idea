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

import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.changes.LocalCommitExecutor;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory;
import com.intellij.util.PairConsumer;
import net.groboclown.idea.p4ic.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class P4CheckinHandler extends CheckinHandler {
    private final CheckinProjectPanel panel;

    public P4CheckinHandler(@NotNull CheckinProjectPanel panel) {
        this.panel = panel;
    }

    @Override
    public ReturnResult beforeCheckin(@Nullable CommitExecutor executor, PairConsumer<Object, Object> additionalDataConsumer) {
        //System.out.println("Checkin handler: beforeCheckin with executor " + executor + "; " + additionalDataConsumer);

        if (emptyCommitMessage()) {
            return ReturnResult.CANCEL;
        }
        return ReturnResult.COMMIT;
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
        System.out.println("CheckinHandler.checkinSuccessful");
    }

    /**
     * Performs the processing on failed check-in. The method can use the
     * {@link com.intellij.openapi.vcs.CheckinProjectPanel} instance passed to
     * {@link com.intellij.openapi.vcs.checkin.BaseCheckinHandlerFactory#createHandler(com.intellij.openapi.vcs.CheckinProjectPanel, CommitContext)} to
     * get information about the checked in files.
     *
     * @param exception the list of VCS exceptions identifying the problems that occurred during the
     *                  commit operation.
     */
    public void checkinFailed(List<VcsException> exception) {
        System.out.println("CheckinHandler.checkinFailed " + exception);
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

}
