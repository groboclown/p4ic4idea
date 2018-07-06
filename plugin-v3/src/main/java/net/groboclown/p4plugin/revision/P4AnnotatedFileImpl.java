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

package net.groboclown.p4plugin.revision;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.annotate.AnnotationSourceSwitcher;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.annotate.LineAnnotationAspect;
import com.intellij.openapi.vcs.annotate.LineAnnotationAspectAdapter;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.DateFormatUtil;
import net.groboclown.p4.server.api.commands.file.AnnotateFileResult;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4AnnotatedLine;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileAnnotation;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4.server.api.values.P4Revision;
import net.groboclown.p4.server.impl.repository.HistoryContentLoader;
import net.groboclown.p4.server.impl.repository.HistoryMessageFormatter;
import net.groboclown.p4.server.impl.repository.P4HistoryVcsFileRevision;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.extension.P4Vcs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class P4AnnotatedFileImpl extends FileAnnotation {
    private static final Logger LOG = Logger.getInstance(P4AnnotatedFileImpl.class);

    private final FilePath file;
    private final ServerConfig config;
    private final P4FileAnnotation annotatedFile;
    private final P4FileRevision head;
    private final String content;
    private final HistoryMessageFormatter formatter;
    private final HistoryContentLoader loader;

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
                    P4ChangelistId changelist = ann.getChangelist();
                    if (changelist == null || UserProjectPreferences.getPreferRevisionsForFiles(null)) {
                        return '#' + Integer.toString(ann.getRevNumber());
                    }
                    return '@' + Integer.toString(changelist.getChangelistId());
                }
            }
    };

    public P4AnnotatedFileImpl(@NotNull Project project,
            @NotNull FilePath file,
            @Nullable HistoryMessageFormatter formatter,
            @Nullable HistoryContentLoader loader,
            @NotNull AnnotateFileResult annotatedFileResult) {
        super(project);
        this.file = file;
        this.config = annotatedFileResult.getServerConfig();
        this.annotatedFile = annotatedFileResult.getAnnotatedFile();
        this.head = annotatedFileResult.getHeadRevision();
        this.content = annotatedFileResult.getContent();
        this.formatter = formatter;
        this.loader = loader;
    }

    @Nullable
    public VcsKey getVcsKey() {
        return P4Vcs.getKey();
    }

    /**
     * This method is invoked when the annotation provider is no
     * more used by UI.
     */
    @Override
    public void dispose() {
        // The object will be GC'd, so use this opportunity to remove
        // any long-term data that's stored outside this object.
    }

    /**
     * Get annotation aspects. The typical aspects are revision
     * number, date, author. The aspects are displayed each
     * in own column in the returned order.
     *
     * @return annotation aspects
     */
    @Nonnull
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
        return new P4Revision(line.getRevNumber());
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
        return getLineRevisionNumber(lineNumber);
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
        return head.getRevision();
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

        // Possibly look into using P4HistoryProvider#getHistory instead,
        // however that wouldn't conform to the API.

        Set<VcsFileRevision> revs = new HashSet<>();
        for (P4AnnotatedLine line : annotatedFile.getAnnotatedLines()) {
            if (line != null) {
                P4HistoryVcsFileRevision fileRev = new P4HistoryVcsFileRevision(
                        file, config, line.getRevisionData(), formatter, loader);
                revs.add(fileRev);
            }
        }
        LOG.debug("getRevisions(): " + revs.size() + " " + revs);
        return new ArrayList<>(revs);
    }

    @Nullable
    @Override
    public AnnotationSourceSwitcher getAnnotationSourceSwitcher() {
        // This is not needed.  It is used to swap between
        // the merged files and the local files; Perforce doesn't
        // work that way.

        // TODO if the merge conflict code is implemented, this might
        // have a reason for being implemented as well.

        return null;
    }

    @Override
    public int getLineCount() {
        return annotatedFile.getAnnotatedLines().size();
    }

    @Override
    public VirtualFile getFile() {
        return file.getVirtualFile();
    }


    @Nullable
    private P4AnnotatedLine getOrNull(int lineNum) {
        if (lineNum >= 0 && lineNum < annotatedFile.getAnnotatedLines().size()) {
            P4AnnotatedLine line = annotatedFile.getAnnotatedLines().get(lineNum);
            if (line.getLineNumber() != lineNum) {
                LOG.warn("Line number " + lineNum + " incorrectly mapped to " + line.getLineNumber());
            }
            return line;
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
                LOG.warn("FIXME implement showAffectedPaths");
                // FIXME implement
                //ShowAllAffectedGenericAction.showSubmittedFiles(getProject(),
                //        ann.getRevNumber(), getFile(), P4Vcs.getKey());
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
