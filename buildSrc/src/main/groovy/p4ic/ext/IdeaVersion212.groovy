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

package p4ic.ext

import java.util.regex.Pattern

class IdeaVersion212 implements IdeaVersionLibMatcher {
    private static final LibGroup libs = new LibGroup().add(
            new NamedLib("core",
                    //"intellij.platform.core.jar"
            ),
            new NamedLib("core-ui",
                    // com.intellij.openapi.actionSystem.DataContext
                    //"intellij.platform.core.ui.jar",
            ),
            new NamedLib("core-impl",
                    // com.intellij.openapi.application.impl.ModalityStateEx
                    //"intellij.platform.core.impl.jar",
            ),
            new NamedLib("vcs-core",
                    //"intellij.platform.vcs.core.jar"
            ),
            new NamedLib("vcs",
                    //"intellij.platform.vcs.jar",
            ),
            new NamedLib("analysis",
                    // Contains com.intellij.openapi.vfs.LocalFileSystem
                    //"intellij.platform.analysis.jar"
            ),
            new NamedLib("vcs-impl",
                    // com.intellij.openapi.vcs.history.VcsHistoryUtil
                    //"intellij.platform.vcs.impl.jar",
            ),
            new NamedLib("ide",
                    //"intellij.platform.ide.jar",
            ),
            new NamedLib("ide-impl",
                    // com.intellij.credentialStore.CredentialPromptDialog
                    //"intellij.platform.ide.impl.jar",
            ),
            new NamedLib("project-model",
                    //"intellij.platform.projectModel.jar",
            ),
            new NamedLib("extensions",
                    //"intellij.platform.extensions.jar"
            ),
            new NamedLib("editor",
                    // for com.intellij.openapi.vcs.FileStatus
                    //"intellij.platform.editor.jar",
            ),
            new NamedLib("util",
                    //"intellij.platform.util.jar",
            ),
            new NamedLib("util-rt",
                    //"intellij.platform.util.rt.jar"
            ),
            new NamedLib("util-ui",
                    //"intellij.platform.util.ui.jar",
            ),
            new NamedLib("util-strings",
                    // com.intellij.util.text.CharArrayCharSequence, used in password access, moved here:
                    //"intellij.platform.util.strings.jar",
            ),
            new NamedLib("util-io",
                    //"intellij.platform.ide.util.io.jar",
            ),
            new NamedLib("util-classloader",
                    //"intellij.platform.util.classLoader.jar",
            ),
            new NamedLib("util-collections",
                    //"intellij.platform.util.collections.jar",
            ),
            new NamedLib("editor-ui-api",
                    // this includes com.intellij.openapi.vcs.FileStatus
                    //"intellij.platform.editor.jar",
            ),
            new NamedLib("guiforms-rt",
                    //"intellij.java.guiForms.rt.jar",
            ),
            new NamedLib("concurrency",
                    //"intellij.platform.concurrency.jar",
            ),

            // deps
            new NamedLib("jdom",
                    //"jdom-2.0.6.jar"
            ),
            new NamedLib("jgoodies-forms",
                    //"forms-1.1-preview.jar"
            ),
            new NamedLib("annotations",
                    //"annotations-java5-20.0.0.jar"
            ),
            // test deps
            new NamedLib("fastutil",
                    //"intellij-deps-fastutil-8.4.1-4.jar",
            ),
            new NamedLib("trove4j",
                    //"trove4j-1.0.20200330.jar",
            ),
            new NamedLib("kotlin-stdlib",
                    //"kotlin-stdlib-1.4.0.jar",
            ),
            new NamedLib("intellij-util-collections",
                    //"util-collections-203.3157.jar",
            ),

            // Used by IntelliJInstrumentCodeTask
            new NamedLib("javac2",
                    //"javac2.jar",
            ),
            new NamedLib("compiler-instrumentationUtil",
                    // com.intellij.compiler.instrumentation.FailSafeClassReader
                    //"intellij.java.compiler.instrumentationUtil.jar"
            ),
            new NamedLib("compiler-antTasks",
                    // com.intellij.ant.InstrumentIdeaExtensions
                    //"intellij.java.compiler.antTasks.jar"
            ),
            new NamedLib("asm",
                    // org.jetbrains.org.objectweb.asm.ClassReader
                    //"asm-all-9.0.jar"
            ),
            new NamedLib("guiForms-compiler",
                    // com.intellij.uiDesigner.compiler.AlienFormFileException
                    //"intellij.java.guiForms.compiler.jar",
            ),
    )

    private static final Pattern IDE_VERSION = Pattern.compile("^212\$")

    /**
     * Checks if the filename is in the remaining list.  If it is, the item is
     * removed from the remaining list.  The filename ends with ".jar"
     *
     * @param filename
     * @param remaining
     * @return
     */
    @Override
    Pattern getIdeaVersionMatch() {
        return IDE_VERSION
    }

    @Override
    NamedLib getNamedLib(String name) {
        return libs.get(name)
    }
}
