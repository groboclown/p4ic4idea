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

package net.groboclown.idea.p4ic.v2.ui.warning;

import com.intellij.ide.errorTreeView.ErrorTreeElementKind;
import com.intellij.ide.errorTreeView.HotfixGate;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class WarningMessage {
    private final Project project;
    private final String summary;
    private final String message;
    private final Throwable warning;
    private final Collection<VirtualFile> affectedFiles;
    private final Consumer<HotfixGate> hotfix;
    private final Date when = new Date();

    public WarningMessage(@NotNull Project project, @Nls @NotNull String summary,
            @Nls @NotNull final String message,
            @Nullable final Throwable warning, @Nullable Collection<VirtualFile> affectedFiles) {
        this(project, summary, message, warning, affectedFiles, null);
    }

    /**
     *
     * @param project project
     * @param summary display summary
     * @param message display message
     * @param warning error source
     * @param affectedFiles files that generated error
     * @param hotfix if the error is automatically solvable, add this in;
     *               it will display the error as "fixable", and give the
     *               user a chance to click it and resolve the issue.
     */
    public WarningMessage(@NotNull Project project, @Nls @NotNull String summary,
            @Nls @NotNull final String message,
            @Nullable final Throwable warning, @Nullable Collection<VirtualFile> affectedFiles,
            @Nullable Consumer<HotfixGate> hotfix) {
        this.project = project;
        this.summary = summary;
        this.message = message;
        this.warning = warning;
        this.affectedFiles = affectedFiles == null
                ? Collections.<VirtualFile>emptyList()
                : Collections.unmodifiableCollection(affectedFiles);
        this.hotfix = hotfix;
    }

    public WarningMessage(@NotNull Project project, @Nls @NotNull String summary,
            @Nls @NotNull final String message,
            @Nullable final Throwable warning, VirtualFile... affectedFiles) {
        this(project, summary, message, warning, Arrays.asList(affectedFiles));
    }

    @NotNull
    public String getSummary() {
        return summary;
    }

    @NotNull
    public String getMessage() {
        return message;
    }

    @Nullable
    public Throwable getWarning() {
        return warning;
    }

    @NotNull
    public Date getWhen() {
        return when;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public Collection<VirtualFile> getAffectedFiles() {
        return affectedFiles;
    }

    @Nullable
    public Consumer<HotfixGate> getHotfix() {
        return hotfix;
    }

    @NotNull
    public ErrorTreeElementKind getErrorKind() {
        // FIXME allow "notes" to return a warning
        return ErrorTreeElementKind.ERROR;
    }




    /*
    public void addToMessages() {
        // See AbstractVcsHelperImpl and AbstractVcsHelper
        // tab name VcsBundle.message("message.title.annotate")
        // this requires storing a project with the warning.

        AbstractVcsHelper.getInstance(project).showError(x, VcsBundle.message("message.title.annotate"));
    }
    */
}
