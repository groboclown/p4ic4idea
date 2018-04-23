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

package net.groboclown.p4.server.impl.config.part;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ConfigProblem;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RelativeConfigCompositePart extends CompositePart {
    static final String TAG_NAME = "relative-config-composite-part";
    static final ConfigPartFactory<RelativeConfigCompositePart> FACTORY = new Factory();
    private static final String NAME_ATTRIBUTE = "name";

    private final Project project;
    private String name;

    @NotNull
    private List<ConfigProblem> problems = new ArrayList<ConfigProblem>();

    @NotNull
    private List<ConfigPart> parts = new ArrayList<ConfigPart>();

    public RelativeConfigCompositePart(@NotNull Project project) {
        this.project = project;
    }

    RelativeConfigCompositePart(@NotNull Project project, @NotNull String name) {
        this.project = project;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        RelativeConfigCompositePart that = (RelativeConfigCompositePart) o;
        return StringUtil.equals(that.name, name);
    }

    public void setName(@Nullable String name) {
        this.name = name;

        // Note: do not reload when the name is set.  This can be costly in terms
        // of performance.  See #139.
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Override
    public boolean reload() {
        problems.clear();
        List<FilePath> p4ConfigFiles = findChildP4ConfigFiles();

        if (p4ConfigFiles.isEmpty()) {
            p4ConfigFiles = findParentP4ConfigFile();
        }

        List<ConfigPart> discovered = new ArrayList<ConfigPart>();
        for (FilePath p4ConfigFile : p4ConfigFiles) {
            discovered.add(new FileDataPart(project, p4ConfigFile.getIOFile()));
        }
        parts = discovered;
        return problems.isEmpty();
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        return new ArrayList<ConfigProblem>(problems);
    }

    @NotNull
    @Override
    public List<ConfigPart> getConfigParts() {
        return new ArrayList<ConfigPart>(parts);
    }


    @NotNull
    @Override
    public Element marshal() {
        final Element ret = new Element(TAG_NAME);

        if (name != null) {
            ret.setAttribute(NAME_ATTRIBUTE, name);
        }

        return ret;
    }


    private static class Factory extends ConfigPartFactory<RelativeConfigCompositePart> {
        @Override
        RelativeConfigCompositePart create(@NotNull Project project, @NotNull Element element) {
            final RelativeConfigCompositePart ret = new RelativeConfigCompositePart(project);
            if (isTag(TAG_NAME, element)) {
                final Attribute nameAttr = element.getAttribute(NAME_ATTRIBUTE);
                if (nameAttr != null) {
                    ret.setName(nameAttr.getValue());
                }
            }
            return ret;
        }
    }


    /**
     *
     * @return list of all P4Config files that are children of the project directory.
     */
    @NotNull
    private List<FilePath> findChildP4ConfigFiles() {
        VirtualFile rootSearchPath = project.getBaseDir();
        if (! rootSearchPath.exists()) {
            problems.add(new ConfigProblem(this, true, "error.roots.not-directory", rootSearchPath));
            return Collections.emptyList();
        }
        if (! rootSearchPath.isDirectory()) {
            rootSearchPath = rootSearchPath.getParent();
        }

        List<FilePath> ret = new ArrayList<FilePath>();

        List<File> depthStack = new ArrayList<File>();
        // Make sure we use the actual I/O file in order to avoid some
        // IDEA refresh issues.
        // bug #32 - make sure to add in the root directory
        depthStack.add(new File(rootSearchPath.getPath()));

        while (!depthStack.isEmpty()) {
            final File file = depthStack.remove(depthStack.size() - 1);
            // Just-in-case check
            if (file == null) {
                continue;
            }
            if (! file.exists()) {
                continue;
            }
            if (file.isDirectory()) {
                final File[] children = file.listFiles();
                if (children != null) {
                    depthStack.addAll(Arrays.asList(children));
                }
            } else if (file.isFile() && file.getName().equals(name)) {
                ret.add(FilePathUtil.getFilePath(file));
            }
        }
        return ret;
    }

    /**
     * Find the first p4config file that's a parent of the project directory.
     *
     * @return either an empty list or a singleton list.
     */
    @NotNull
    private List<FilePath> findParentP4ConfigFile() {
        if (name == null || name.isEmpty()) {
            return Collections.emptyList();
        }
        VirtualFile rootSearchPath = project.getBaseDir();
        if (! rootSearchPath.exists()) {
            throw new IllegalArgumentException(P4Bundle.message("error.roots.not-directory", rootSearchPath));
        }
        if (! rootSearchPath.isDirectory()) {
            rootSearchPath = rootSearchPath.getParent();
        }
        File previous = new File(rootSearchPath.getPath());
        File current = previous.getParentFile();
        while (current != null && ! FileUtil.filesEqual(previous, current)) {
            File p4config = new File(current, name);
            if (p4config.exists() && p4config.isFile()) {
                return Collections.singletonList(FilePathUtil.getFilePath(p4config));
            }
            previous = current;
            current = previous.getParentFile();
        }
        return Collections.emptyList();
    }
}
