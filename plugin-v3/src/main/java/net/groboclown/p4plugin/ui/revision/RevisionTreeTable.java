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

package net.groboclown.idea.p4ic.ui.revision;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.history.P4FileRevision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Date;
import java.util.List;

public class RevisionTreeTable extends JPanel {
    private static final Logger LOG = Logger.getInstance(RevisionTreeTable.class);


    private String error;
    private TreeTable tree;
    private RevisionSelectedListener listener;

    public RevisionTreeTable(final @NotNull P4Vcs vcs, final @NotNull VirtualFile file) {
        PathRevsSet revsSet = PathRevsSet.create(vcs, file);

        List<PathRevs> revisions = revsSet.getPathRevs();
        error = revsSet.getError();

        if (error != null) {
            add(new JLabel(P4Bundle.message("revision.list.error", error)));
        } else {
            setLayout(new BorderLayout());
            RevisionListModel model = new RevisionListModel(revisions);
            tree = new TreeTable(model);
            add(new JBScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
            tree.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tree.setRowSelectionAllowed(true);
            tree.setColumnSelectionAllowed(false);
            tree.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(final ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) {
                        return;
                    }
                    if (listener != null) {
                        if (e.getFirstIndex() < 0 || e.getLastIndex() < e.getFirstIndex()) {
                            listener.revisionSelected(null);
                        } else {
                            listener.revisionSelected(getRevAt(e.getFirstIndex()));
                        }
                    }
                }
            });
        }
    }


    public String getError() {
        return error;
    }


    public boolean isValid() {
        return error == null;
    }


    @Nullable
    public P4FileRevision getSelectedRevision() {
        if (!isValid()) {
            return null;
        }
        int row = tree.getSelectedRow();
        return getRevAt(row);
    }


    @Nullable
    private P4FileRevision getRevAt(int index) {
        final TreePath path = tree.getTree().getPathForRow(index);
        if (path == null) {
            return null;
        }
        final Object obj = path.getLastPathComponent();
        if (obj instanceof P4FileRevision) {
            return (P4FileRevision) obj;
        }
        return null;
    }


    @Nullable
    public ValidationInfo getValidationError() {
        if (error == null) {
            return null;
        }
        return new ValidationInfo(error, this);
    }


    public void addRevisionSelectedListener(@NotNull RevisionSelectedListener listener) {
        this.listener = listener;
    }



    private static final int COL_REV = 0;
    private static final int COL_DATE = 1;
    private static final int COL_AUTH = 2;
    private static final int COL_COMMENT = 3;
    private static final int COL_COUNT = 4;

    private static class RevisionListModel implements TreeTableModel {
        private final List<PathRevs> revisions;
        private JTree tree;

        private RevisionListModel(final List<PathRevs> revisions) {
            this.revisions = revisions;
        }

        @Override
        public int getColumnCount() {
            return COL_COUNT;
        }

        @Override
        public String getColumnName(final int column) {
            switch (column) {
                case COL_REV:
                    return P4Bundle.getString("revision.list.rev");
                case COL_DATE:
                    return P4Bundle.getString("revision.list.datetime");
                case COL_AUTH:
                    return P4Bundle.getString("revision.list.author");
                case COL_COMMENT:
                    return P4Bundle.getString("revision.list.comment");
                default:
                    throw new IllegalArgumentException("invalid column " + column);
            }
        }

        @Override
        public Class getColumnClass(final int column) {
            switch (column) {
                case COL_REV:
                    return Integer.class;
                case COL_DATE:
                    return Date.class;
                case COL_AUTH:
                    return String.class;
                case COL_COMMENT:
                    return String.class;
                default:
                    throw new IllegalArgumentException("invalid column " + column);
            }
        }

        @Override
        public Object getValueAt(final Object node, final int columnIndex) {
            if (node == null) {
                return "";
            }
            if (node instanceof P4FileRevision) {
                final P4FileRevision rev = (P4FileRevision) node;
                switch (columnIndex) {
                    case COL_REV:
                        return rev.getRev();
                    case COL_DATE:
                        return rev.getRevisionDate();
                    case COL_AUTH:
                        return rev.getAuthor();
                    case COL_COMMENT:
                        return rev.getCommitMessage();
                    default:
                        return "";
                }
            }

            // TODO if not all revisions were loaded, add an extra "..." row.

            LOG.info("unknown getValueAt model call: " + node);
            return null;
        }

        @Override
        public Object getChild(final Object parent, final int index) {
            final PathRevs node = lookupNode(parent);
            if (node != null && index >= 0 && index < node.getRevisions().size()) {
                return node.getRevisions().get(index);
            }
            return null;
        }

        @Override
        public int getChildCount(final Object parent) {
            final PathRevs node = lookupNode(parent);
            if (node != null) {
                return node.getRevisions().size();
            }
            // unknown node, so no children
            return 0;
        }

        @Override
        public boolean isLeaf(final Object node) {
            return node != null && (node instanceof P4FileRevision);
        }

        @Override
        public int getIndexOfChild(final Object parent, final Object child) {
            if (parent == null || child == null) {
                return -1;
            }
            if (!(child instanceof P4FileRevision)) {
                // not a valid child object.
                return -1;
            }
            final PathRevs node = lookupNode(parent);
            if (node != null) {
                for (int i = 0; i < node.getRevisions().size(); ++i) {
                    if (child.equals(node.getRevisions().get(i))) {
                        return i;
                    }
                }
            }
            // parent not in list
            return -1;
        }


        @Nullable
        private PathRevs lookupNode(final Object parent) {
            if (parent == null || !(parent instanceof String)) {
                return null;
            }
            for (PathRevs pr : revisions) {
                if (parent.equals(pr.getDepotPath())) {
                    return pr;
                }
            }
            return null;
        }


        @Override
        public boolean isCellEditable(final Object node, final int column) {
            return false;
        }

        @Override
        public void valueForPathChanged(final TreePath path, final Object newValue) {
            // ignore
        }

        @Override
        public Object getRoot() {
            // No single root
            return null;
        }

        @Override
        public void setValueAt(final Object aValue, final Object node, final int column) {
            // do nothing
        }

        @Override
        public void setTree(final JTree tree) {
            this.tree = tree;
        }

        @Override
        public void addTreeModelListener(final TreeModelListener l) {
            // Unmodifiable right now (can't load more), so ignore
        }

        @Override
        public void removeTreeModelListener(final TreeModelListener l) {
            // Unmodifiable right now (can't load more), so ignore
        }
    }


}
