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

package net.groboclown.p4plugin.ui.connection;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.cache.ActionChoice;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class ConnectionTreeCellRenderer extends ColoredTreeCellRenderer {

    private static final SimpleTextAttributes ROOT_NAME_STYLE =
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor.GREEN);
    private static final SimpleTextAttributes ROOT_PATH_STYLE =
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, null);
    private static final SimpleTextAttributes ROOT_ONLINE_STYLE =
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GREEN);
    private static final SimpleTextAttributes ROOT_OFFLINE_STYLE =
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor.ORANGE);
    private static final SimpleTextAttributes CONNECTION_PROPERTY_KEY_STYLE =
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, null);
    private static final SimpleTextAttributes CONNECTION_PROPERTY_VALUE_STYLE =
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor.BLUE);
    private static final SimpleTextAttributes PENDING_PARENT_STYLE =
            SimpleTextAttributes.REGULAR_ATTRIBUTES;
    private static final SimpleTextAttributes PENDING_ACTION_STYLE =
            SimpleTextAttributes.REGULAR_ATTRIBUTES;
    private static final SimpleTextAttributes PENDING_ACTION_SEPARATOR_STYLE =
            SimpleTextAttributes.GRAYED_ATTRIBUTES;
    private static final SimpleTextAttributes PENDING_ACTION_PARAMETER_STYLE =
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
    private static final SimpleTextAttributes FILENAME_STYLE =
            SimpleTextAttributes.SYNTHETIC_ATTRIBUTES;
    private static final SimpleTextAttributes FILEPATH_STYLE =
            SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES;
    private static final SimpleTextAttributes ERROR_STYLE =
            SimpleTextAttributes.ERROR_ATTRIBUTES;

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value,
            boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof DefaultMutableTreeNode) {
            Object other = ((DefaultMutableTreeNode) value).getUserObject();
            if (other != null) {
                value = other;
            }
        }
        if (value instanceof ClientConfigRoot) {
            renderClientConfigRoot((ClientConfigRoot) value);
        } else if (value instanceof P4ServerName) {
            renderP4ServerName((P4ServerName) value);
        } else if (value instanceof ClientServerRef) {
            renderClientServerRef((ClientServerRef) value);
        } else if (value instanceof PendingParentNode) {
            renderPendingParentNode((PendingParentNode) value);
        } else if (value instanceof ActionChoice) {
            renderActionChoice((ActionChoice) value);
        } else if (value instanceof FilePath) {
            renderFilePath((FilePath) value);
        } else if (value instanceof P4CommandRunner.ResultError) {
            renderResultError((P4CommandRunner.ResultError) value);
        } else {
            renderError(value);
        }

        SpeedSearchUtil.applySpeedSearchHighlighting(tree, this, true, selected);
    }


    private void renderClientConfigRoot(ClientConfigRoot value) {
        append(P4Bundle.message("connection.tree.root.name", value.getProjectVcsRootDir().getPresentableName()),
                ROOT_NAME_STYLE);

        String rootPath = value.getProjectVcsRootDir().getPath();
        if (rootPath.startsWith("file://")) {
            rootPath = rootPath.substring(7);
            // TODO normalize the path for Windows?
        }
        append(P4Bundle.message("connection.tree.root.path", rootPath), ROOT_PATH_STYLE);

        if (value.isOnline()) {
            append(P4Bundle.message("connection.tree.root.online"), ROOT_ONLINE_STYLE);
        }
        if (value.isOffline()) {
            append(P4Bundle.message("connection.tree.root.offline"), ROOT_OFFLINE_STYLE);
        }
    }

    private void renderP4ServerName(P4ServerName value) {
        append(P4Bundle.getString("connection.tree.server-name-key"), CONNECTION_PROPERTY_KEY_STYLE);
        append(value.getDisplayName(), CONNECTION_PROPERTY_VALUE_STYLE);
    }

    private void renderClientServerRef(ClientServerRef value) {
        append(P4Bundle.getString("connection.tree.client-name-key"), CONNECTION_PROPERTY_KEY_STYLE);
        append(value.getClientName() == null
                ? P4Bundle.getString("connection.tree.client-name-null")
                : value.getClientName(),
                CONNECTION_PROPERTY_VALUE_STYLE);
    }

    private void renderPendingParentNode(PendingParentNode value) {
        append(P4Bundle.message("connection.tree.pending-parent", value.getPendingCount()),
                PENDING_PARENT_STYLE);

    }

    private void renderActionChoice(ActionChoice value) {
        final String text = value.when(
                (c) -> c.getCmd().name().toLowerCase().replace('_', ' '),
                (s) -> s.getCmd().name().toLowerCase().replace('_', ' ')
        );
        append(text, PENDING_ACTION_STYLE);
        String[] params = value.getDisplayParameters();
        if (params.length > 0) {
            boolean first = true;
            for (String param : params) {
                if (first) {
                    append(" (", PENDING_ACTION_SEPARATOR_STYLE);
                    first = false;
                } else {
                    append(", ", PENDING_ACTION_SEPARATOR_STYLE);
                }
                append(param, PENDING_ACTION_PARAMETER_STYLE);
            }
            append(")", PENDING_ACTION_SEPARATOR_STYLE);
        }
    }

    private void renderFilePath(FilePath value) {
        // TODO make this look and act like the display for the changelist files.
        // You should be able to navigate to the file from here.
        append(value.getName(), FILENAME_STYLE);
        FilePath parent = value.getParentPath();
        if (parent != null) {
            append("  ", PENDING_ACTION_SEPARATOR_STYLE);
            append(parent.getPath(), FILEPATH_STYLE);
        }
    }

    private void renderResultError(P4CommandRunner.ResultError value) {
        append(value.getCategory() + ": " + value.getMessage(), ERROR_STYLE);
    }

    private void renderError(Object value) {
        append("<ERROR " + value.getClass() + ">", ERROR_STYLE);
    }
}
