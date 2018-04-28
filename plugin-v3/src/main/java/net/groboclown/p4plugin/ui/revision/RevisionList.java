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
import com.intellij.ui.components.JBList;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.history.P4FileRevision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RevisionList extends JPanel {
    private static final Logger LOG = Logger.getInstance(RevisionList.class);
    private final RevisionListModel model;


    private String error;
    private JBList list;
    private RevisionSelectedListener listener;

    public RevisionList(final @NotNull P4Vcs vcs, final @NotNull VirtualFile file) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        PathRevsSet revsSet = PathRevsSet.create(vcs, file);

        error = revsSet.getError();

        if (error != null) {
            LOG.info("Errors in retrieving revisions: " + error);
            JLabel label = new JLabel(P4Bundle.message("revision.list.error", error));
            add(label, BorderLayout.NORTH);
            model = null;
        } else {
            // FIXME this isn't being rendered correctly.  Why?
            LOG.info("Found " + revsSet.getPathRevs().size() + " revision branches");
            String text = "<html>Revisions:<br>";
            for (PathRevs revs : revsSet.getPathRevs()) {
                text = text + revs.getDepotPath() + "<br>";
                for (P4FileRevision rev : revs.getRevisions()) {
                    text = text + '#' + rev.getRev() + "<br>";
                }
            }
            add(new JLabel(text), BorderLayout.NORTH);
            model = null;

            /*
            model = new RevisionListModel(revsSet.getPathRevs());
            list = new JBList(model);
            add(new JBScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
            add(list, BorderLayout.CENTER);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
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
            */
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
        int row = list.getSelectedIndex();
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



    private static class RevisionListModel implements ListModel {
        private final List<CellData> revisions;

        private RevisionListModel(final List<PathRevs> revisions) {
            List<CellData> revs = new ArrayList<CellData>();
            for (PathRevs rev : revisions) {
                for (P4FileRevision frev : rev.getRevisions()) {
                    revs.add(new CellData(frev));
                }
            }
            this.revisions = Collections.unmodifiableList(revs);
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
        public int getSize() {
            return revisions.size();
        }

        @Override
        public Object getElementAt(final int index) {
            if (index < 0 || index >= revisions.size()) {
                return null;
            }
            CellData data = revisions.get(index);
            return '#' + Integer.toString(data.rev.getRev());
        }

        @Override
        public void addListDataListener(final ListDataListener l) {

        }

        @Override
        public void removeListDataListener(final ListDataListener l) {

        }
    }


    private static final class CellData {
        final String depotPath;
        final P4FileRevision rev;

        private CellData(@NotNull final P4FileRevision rev) {
            this.depotPath = null;
            this.rev = rev;
        }

        boolean isPath() {
            return depotPath != null;
        }
    }
}
