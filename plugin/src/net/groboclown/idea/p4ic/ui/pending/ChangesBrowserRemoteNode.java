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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNodeRenderer;
import com.intellij.ui.SimpleTextAttributes;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.P4FileAction;
import net.groboclown.idea.p4ic.v2.server.cache.FileUpdateAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

class ChangesBrowserRemoteNode
        extends ChangesBrowserNode<P4FileAction> {

    ChangesBrowserRemoteNode(P4FileAction userObject) {
        super(userObject);
    }

    /*@Override*/
    protected boolean isFile() {
        return true;
    }

    @Override
    protected boolean isDirectory() {
        return false;
    }

    @Override
    public void render(@NotNull ChangesBrowserNodeRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
        P4FileAction file = getUserObject();
        String name = file.getName();
        if (name == null) {
            // Don't know what to do
            renderer.append(P4Bundle.getString("changes.browser.remote.node.unknown"));
            return;
        }
        SimpleTextAttributes color;
        if (file.getClientFileStatus() == null) {
            color = SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES;
        } else {
            color = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, file.getClientFileStatus().getColor());
        }
        renderer.append(name, color);

        String path = file.getParentPath();
        if (renderer.isShowFlatten() && path != null) {
            renderer.append(' ' + FileUtil.getLocationRelativeToUserHome(path),
                    SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        String toolTip = getTooltip();
        if (toolTip != null) {
            renderer.setToolTipText(toolTip);
        }

        renderer.setIcon(getIcon(file.getFileUpdateAction()));
    }

    @Nullable
    private Icon getIcon(@NotNull FileUpdateAction action) {
        return AllIcons.Ide.Link;
    }

    public String getTooltip() {
        return getUserObject().getDepotPath();
    }

    @Override
    public String getTextPresentation() {
        String name = getUserObject().getName();
        if (name == null) {
            return P4Bundle.getString("changes.browser.remote.node.unknown");
        }
        return name;
    }

    @Override
    public String toString() {
        P4FileAction file = getUserObject();
        if (file.getDepotPath() != null) {
            return file.getDepotPath();
        }
        if (file.getFile() != null) {
            return FileUtil.toSystemDependentName(file.getFile().getPath());
        }
        return null;
    }

    @Override
    public int getSortWeight() {
        return 6; // CHANGE_SORT_WEIGHT;
    }

    // FIXME 2017.1
    //public int compareUserObjects(final Object o2) {
    public int compareUserObjects(final P4FileAction o2) {
        if (o2 instanceof P4FileAction) {
            P4FileAction that = (P4FileAction) o2;
            return getUserObject().compareTo(that);
        }
        return 0;
    }
}
