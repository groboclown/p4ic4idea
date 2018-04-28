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
import com.intellij.ui.table.JBTable;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.history.P4FileRevision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RevisionTable extends JPanel {
    private static final Logger LOG = Logger.getInstance(RevisionTable.class);
    private final RevisionListModel model;


    private final String error;
    private JBTable table;
    private RevisionSelectedListener listener;

    public RevisionTable(final @NotNull P4Vcs vcs, final @NotNull VirtualFile file) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        PathRevsSet revs = PathRevsSet.create(vcs, file);

        error = revs.getError();

        if (error != null) {
            LOG.info("Errors in retrieving revisions: " + error);
            JLabel label = new JLabel(P4Bundle.message("revision.list.error", error));
            add(label, BorderLayout.NORTH);
            model = null;
        } else {
            LOG.info("Found " + revs.getPathRevs().size() + " revision branches");
            model = new RevisionListModel(revs.getPathRevs());
            table = new JBTable(model);
            table.setDefaultRenderer(Object.class, model);
            add(new JBScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
            add(table, BorderLayout.CENTER);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setRowSelectionAllowed(true);
            table.setColumnSelectionAllowed(false);
            table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
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
        int row = table.getSelectedRow();
        return getRevAt(row);
    }


    @Nullable
    private P4FileRevision getRevAt(int index) {
        if (model != null) {
            return model.getRevAt(index);
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

    private static class RevisionListModel implements TableModel, TableCellRenderer {
        private final List<CellData> revisions;
        private final JLabel revCellLabel;
        private final DateFormat dateFormatter;

        private RevisionListModel(final List<PathRevs> revisions) {
            List<CellData> revs = new ArrayList<CellData>();
            for (PathRevs rev : revisions) {
                revs.add(new CellData(rev.getDepotPath()));
                for (P4FileRevision frev : rev.getRevisions()) {
                    revs.add(new CellData(frev));
                }
            }
            this.revisions = Collections.unmodifiableList(revs);
            this.revCellLabel = new JLabel();
            this.dateFormatter = DateFormat.getDateInstance();
        }

        P4FileRevision getRevAt(int row) {
            if (row >= 0 && row < revisions.size()) {
                CellData data = revisions.get(row);
                if (! data.isPath()) {
                    return data.rev;
                }
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return revisions.size();
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
            // To work with the cell renderer, we return Object.
            return Object.class;
        }

        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (rowIndex < 0 || rowIndex >= revisions.size() || columnIndex < 0 || columnIndex >= COL_COUNT) {
                return null;
            }
            CellData data = revisions.get(rowIndex);
            if (data.isPath()) {
                if (columnIndex == 0) {
                    return "<html><b>" + data.depotPath + "</b>";
                }
                return null;
            }

            switch (columnIndex) {
                case COL_REV:
                    return '#' + Integer.toString(data.rev.getRev());
                case COL_DATE:
                    return data.rev.getRevisionDate();
                case COL_AUTH:
                    return data.rev.getAuthor();
                case COL_COMMENT:
                    return data.rev.getCommitMessage();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
            // do nothing
        }

        @Override
        public void addTableModelListener(final TableModelListener l) {
            // ignored - table is currently not modifiable
        }

        @Override
        public void removeTableModelListener(final TableModelListener l) {
            // ignored - table is currently not modifiable
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                final boolean hasFocus,
                final int row, final int column) {
            if (value == null || row < 0 || row >= revisions.size() || column < 0) {
                // should only happen when there should be nothing shown
                return null;
            }
            Color fg = isSelected ? table.getSelectionForeground() : table.getForeground();
            Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();
            CellData data = revisions.get(row);
            if (data == null || (data.isPath() && column > 0)) {
                return null;
            }
            revCellLabel.setForeground(fg);
            revCellLabel.setBackground(bg);
            final Font font = revCellLabel.getFont();
            if (data.isPath()) {
                revCellLabel.setFont(font.deriveFont(Font.BOLD));
                revCellLabel.setText(data.depotPath);
                // TODO expand?
            } else {
                revCellLabel.setFont(font.deriveFont(Font.PLAIN));
                switch (column) {
                    case COL_REV:
                        revCellLabel.setText('#' + Integer.toString(data.rev.getRev()));
                        break;
                    case COL_DATE:
                        // TODO get the right presentation of dates
                        revCellLabel.setText(dateFormatter.format(data.rev.getRevisionDate()));
                        break;
                    case COL_AUTH:
                        revCellLabel.setText(data.rev.getAuthor());
                        break;
                    case COL_COMMENT:
                        revCellLabel.setText(data.rev.getCommitMessage());
                        break;
                    default:
                        return null;
                }
            }
            return revCellLabel;
        }
    }

    private static final class CellData {
        final String depotPath;
        final P4FileRevision rev;

        CellData(@NotNull final String depotPath) {
            this.depotPath = depotPath;
            this.rev = null;
        }

        private CellData(@NotNull final P4FileRevision rev) {
            this.depotPath = null;
            this.rev = rev;
        }

        boolean isPath() {
            return depotPath != null;
        }
    }
}
