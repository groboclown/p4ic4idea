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

package net.groboclown.idea.p4ic.ui.pending;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ui.ChangeListRemoteState;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserChangeListNode;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNodeRenderer;
import com.intellij.ui.SimpleTextAttributes;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.ui.P4ChangeListDecorator;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListIdImpl;
import net.groboclown.idea.p4ic.v2.server.cache.P4ChangeListValue;

class PendingChangelistNode
        extends ChangesBrowserChangeListNode {
    private final PendingChangelist p4Change;

    public PendingChangelistNode(Project project, PendingChangelist userObject,
            ChangeListRemoteState changeListRemoteState) {
        super(project,
            userObject.getLocal() == null ? userObject : userObject.getLocal(),
            changeListRemoteState);
        this.p4Change = userObject;
    }


    @Override
    public void render(final ChangesBrowserNodeRenderer renderer, final boolean selected, final boolean expanded, final boolean hasFocus) {
        if (p4Change == userObject) {
            super.render(renderer, selected, expanded, hasFocus);
        } else {
            // Perform the custom rendering here, since we don't have a good local mapping.
            renderer.appendTextWithIssueLinks(p4Change.getName(),
                    p4Change.getP4ChangeListValue().isDefaultChangelist()
                            ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                            : SimpleTextAttributes.REGULAR_ATTRIBUTES);
            appendCount(renderer);

            P4ChangeListDecorator.ChangelistConnectionInfo info = new P4ChangeListDecorator.ChangelistConnectionInfo(1);
            P4ChangeListValue value = p4Change.getP4ChangeListValue();
            info.addOnline(value.getClientServerRef(), new P4ChangeListIdImpl(value.getClientServerRef(), value.getChangeListId()));
            P4ChangeListDecorator.decorateInfo(info, renderer);
        }
    }
}
