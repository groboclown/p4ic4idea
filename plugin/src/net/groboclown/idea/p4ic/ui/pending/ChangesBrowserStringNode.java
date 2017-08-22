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

import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNodeRenderer;
import net.groboclown.idea.p4ic.P4Bundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import javax.swing.*;

import static net.groboclown.idea.p4ic.P4Bundle.BUNDLE;

class ChangesBrowserStringNode extends ChangesBrowserNode<String> {
    private Icon icon;
    private String toolTip;
    private String itemCountLabel;

    ChangesBrowserStringNode(String userObject) {
        super(userObject);
    }

    public void setIcon(@Nullable Icon icon) {
        this.icon = icon;
    }

    public void setItemCountLabel(@Nullable @Nls @PropertyKey(resourceBundle = BUNDLE) String label) {
        this.itemCountLabel = label;
    }

    @Override
    public void render(@NotNull ChangesBrowserNodeRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
        super.render(renderer, selected, expanded, hasFocus);
        if (icon != null) {
            renderer.setIcon(icon);
        }
        if (toolTip != null) {
            renderer.setToolTipText(toolTip);
        }
    }


    @NotNull
    protected String getCountText() {
        if (itemCountLabel == null) {
            return "";
        }
        int count = getChildCount() == 0 ? 0 : getLeafCount();
        return "  " + P4Bundle.message(itemCountLabel, count);
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }
}
