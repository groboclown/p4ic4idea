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
package net.groboclown.idea.p4ic.v2.history;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.annotate.*;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.DateFormatUtil;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerId;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class P4FileAnnotation extends FileAnnotation {
    private final Project project;
    private final VirtualFile file;
    private final P4RevisionNumber fileRev;
    private final List<P4AnnotatedLine> annotations;
    private final String content;
    private final ClientServerId clientServerId;

    private final LineAnnotationAspect[] aspects = new LineAnnotationAspect[]{
            new P4LineAnnotationAspect(LineAnnotationAspect.AUTHOR, true) {
                @Override
                String getValue(@NotNull P4AnnotatedLine ann) {
                    return ann.getAuthor();
                }
            },
            new P4LineAnnotationAspect(LineAnnotationAspect.DATE, true) {
                @Override
                String getValue(@NotNull P4AnnotatedLine ann) {
                    Date date = ann.getDate();
                    return date == null ? "" : DateFormatUtil.formatPrettyDate(date);
                }
            },
            new P4LineAnnotationAspect(LineAnnotationAspect.REVISION, true) {
                @Override
                String getValue(@NotNull P4AnnotatedLine ann) {
                    return '#' + Integer.toString(ann.getRev().getRev());
                }
            }
    };


    public P4FileAnnotation(@NotNull Project project, @NotNull ClientServerId clientServerId, @NotNull VirtualFile file,
            @NotNull P4RevisionNumber fileRev, @NotNull List<P4AnnotatedLine> annotations, @NotNull String content) {
        super(project);
        this.project = project;
        this.file = file;
        this.fileRev = fileRev;
        this.annotations = annotations;
        this.content = content;
        this.clientServerId = clientServerId;
    }

    /**
     * This method is invoked when the annotation provider is no
     * more used by UI.
     */
    @Override
    public void dispose() {
        annotations.clear();
    }

    /**
     * Get annotation aspects. The typical aspects are revision
     * number, date, author. The aspects are displayed each
     * in own column in the returned order.
     *
     * @return annotation aspects
     */
    @Override
    public LineAnnotationAspect[] getAspects() {
        return aspects;
    }

    /**
     * <p>The tooltip that is shown over annotation.
     * Typically this is a comment associated with commit that has added or modified the line.</p>
     * <p/>
     * <p>If the method returns null, the tooltip is not shown for this line.</p>
     *
     * @param lineNumber the line number
     * @return the tooltip text
     */
    @Nullable
    @Override
    public String getToolTip(int lineNumber) {
        P4AnnotatedLine line = getOrNull(lineNumber);
        if (line == null) {
            return null;
        }
        return line.getComment();
    }

    /**
     * @return the text of the annotated file
     */
    @Override
    public String getAnnotatedContent() {
        return content;
    }

    /**
     * Get revision number for the line.
     * when "show merge sources" is turned on, returns merge source revision
     *
     * @param lineNumber the line number
     * @return the revision number or null for lines that contain uncommitted changes.
     */
    @Nullable
    @Override
    public VcsRevisionNumber getLineRevisionNumber(int lineNumber) {
        P4AnnotatedLine line = getOrNull(lineNumber);
        if (line == null) {
            return null;
        }
        return line.getRev();
    }

    @Nullable
    @Override
    public Date getLineDate(int lineNumber) {
        P4AnnotatedLine line = getOrNull(lineNumber);
        if (line == null) {
            return null;
        }
        return line.getDate();
    }

    /**
     * Get revision number for the line. (blame list)
     *
     * @param lineNumber line number.
     */
    @Nullable
    @Override
    public VcsRevisionNumber originalRevision(int lineNumber) {
        P4AnnotatedLine line = getOrNull(lineNumber);
        if (line == null) {
            return null;
        }
        return line.getRev();
    }

    /**
     * @return current revision of file for the moment when annotation was computed;
     * null if provider does not provide this information
     * <p/>
     * Needed for automatic annotation close when file current revision changes.
     */
    @Nullable
    @Override
    public VcsRevisionNumber getCurrentRevision() {
        return fileRev;
    }

    /**
     * Get all revisions that are mentioned in the annotations
     *
     * @return the list of revisions that are mentioned in annotations. Or null
     * if before/after popups cannot be supported by the VCS system.
     */
    @Nullable
    @Override
    public List<VcsFileRevision> getRevisions() {
        Set<VcsFileRevision> revs = new HashSet<VcsFileRevision>();
        for (P4AnnotatedLine line : annotations) {
            if (line != null) {
                P4FileRevision fileRev = new P4FileRevision(project, clientServerId,
                        FilePathUtil.getFilePath(file),
                        this.fileRev.getDepotPath(), line);
                revs.add(fileRev);
            }
        }
        return new ArrayList<VcsFileRevision>(revs);
    }

    @Override
    public boolean revisionsNotEmpty() {
        // TODO this is a slow implementation.  There should be some easy speed-up opportunities here.
        List<VcsFileRevision> revs = getRevisions();
        return revs != null && !revs.isEmpty();
    }

    @Nullable
    @Override
    public AnnotationSourceSwitcher getAnnotationSourceSwitcher() {

        // TODO this needs an implementation

        return null;
    }

    @Override
    public int getLineCount() {
        return annotations.size();
    }

    @Override
    public VirtualFile getFile() {
        return file;
    }


    @Nullable
    private P4AnnotatedLine getOrNull(int lineNum) {
        if (lineNum >= 0 && lineNum < annotations.size()) {
            return annotations.get(lineNum);
        }
        return null;
    }


    private abstract class P4LineAnnotationAspect extends LineAnnotationAspectAdapter {
        P4LineAnnotationAspect(@NotNull String id, boolean showByDefault) {
            super(id, showByDefault);
        }

        @Override
        protected void showAffectedPaths(int lineNum) {
            P4AnnotatedLine ann = getOrNull(lineNum);
            if (ann != null) {
                ShowAllAffectedGenericAction.showSubmittedFiles(project,
                        ann.getRev(), getFile(), P4Vcs.getKey());
            }
        }

        /**
         * Get annotation text for the specific line number
         *
         * @param line the line number to query
         * @return the annotation text
         */
        @Override
        public final String getValue(int line) {
            P4AnnotatedLine ann = getOrNull(line);
            if (ann != null) {
                return getValue(ann);
            }
            return "";
        }


        abstract String getValue(@NotNull P4AnnotatedLine ann);
    }
}
