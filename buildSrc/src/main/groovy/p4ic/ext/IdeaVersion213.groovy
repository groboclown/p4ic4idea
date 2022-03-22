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

class IdeaVersion213 implements IdeaVersionLibMatcher {
    private static final LibGroup libs = new LibGroup().add(
            new NamedLib("core",
                    // com.intellij.openapi.editor.Document
                    // com.intellij.openapi.fileTypes.FileType
                    // com.intellij.openapi.fileTypes.FileTypeRegistry
                    // com.intellij.openapi.project.Project
                    // com.intellij.openapi.vfs.VirtualFile
                    // com.intellij.openapi.vfs.VirtualFileSystem
                    // com.intellij.openapi.application.Application
                    // com.intellij.openapi.application.ApplicationInfo
                    // com.intellij.openapi.application.ApplicationManager
                    // com.intellij.openapi.application.ModalityState
                    "intellij.platform.core.jar"
            ),
            new NamedLib("core-ui",
                    // com.intellij.openapi.actionSystem.DataContext
                    // com.intellij.ui.SimpleTextAttributes
                    // com.intellij.openapi.ui.popup.PopupStep
                    "intellij.platform.core.ui.jar",
            ),
            new NamedLib("core-impl",
                    // com.intellij.openapi.application.impl.ModalityStateEx
                    // com.intellij.mock.MockApplication
                    // com.intellij.mock.MockProject
                    "intellij.platform.core.impl.jar",
            ),
            new NamedLib("vcs-core",
                    // com.intellij.openapi.vcs.VcsKey
                    // com.intellij.openapi.vcs.changes.Change
                    "intellij.platform.vcs.core.jar"
            ),
            new NamedLib("vcs",
                    // com.intellij.openapi.vcs.actions.VcsContextFactory
                    "intellij.platform.vcs.jar",
            ),
            new NamedLib("analysis",
                    // com.intellij.openapi.vfs.LocalFileSystem
                    "intellij.platform.analysis.jar"
            ),
            new NamedLib("vcs-impl",
                    // com.intellij.openapi.vcs.history.VcsHistoryUtil
                    // com.intellij.openapi.vcs.VcsVFSListener
                    // com.intellij.openapi.vcs.RemoteFilePath
                    // com.intellij.openapi.vcs.actions.AbstractVcsAction
                    "intellij.platform.vcs.impl.jar",
            ),
            new NamedLib("ide",
                    // com.intellij.util.ui.AsyncProcessIcon
                    "intellij.platform.ide.jar",
            ),
            new NamedLib("ide-core",
                    // com.intellij.openapi.vcs.FilePath
                    // com.intellij.openapi.application.Application
                    // com.intellij.openapi.vcs.FilePath
                    "intellij.platform.ide.core.jar",
            ),
            new NamedLib("ide-impl",
                    // com.intellij.credentialStore.CredentialPromptDialog
                    "intellij.platform.ide.impl.jar",
            ),
            new NamedLib("project-model",
                    // com.intellij.openapi.components.PersistentStateComponent
                    // com.intellij.openapi.components.State
                    "intellij.platform.projectModel.jar",
            ),
            new NamedLib("extensions",
                    // com.intellij.util.messages.MessageBus
                    // com.intellij.util.pico.DefaultPicoContainer
                    // org.picocontainer.PicoContainer
                    // org.picocontainer.ComponentAdapter
                    "intellij.platform.extensions.jar"
            ),
            new NamedLib("editor",
                    // com.intellij.openapi.vcs.FileStatus
                    // com.intellij.openapi.actionSystem.AnActionEvent
                    "intellij.platform.editor.jar",
            ),
            new NamedLib("remote",
                    // com.intellij.credentialStore.CredentialAttributes
                    // com.intellij.ide.passwordSafe.PasswordSafe
                    "intellij.platform.remote.core.jar",
            ),
            new NamedLib("util",
                    // com.intellij.openapi.Disposable
                    // com.intellij.openapi.application.AccessToken
                    // com.intellij.openapi.util.ClassLoaderUtil
                    // com.intellij.util.ReflectionUtil
                    // com.intellij.openapi.util.UserDataHolderBase
                    // com.intellij.openapi.progress.ProcessCanceledException
                    // com.intellij.openapi.util.Computable
                    // com.intellij.openapi.util.Disposer
                    "intellij.platform.util.jar",
            ),
            new NamedLib("util-rt",
                    // com.intellij.openapi.util.Condition
                    // com.intellij.openapi.util.Pair
                    // com.intellij.openapi.util.ThrowableComputable
                    "intellij.platform.util.rt.jar"
            ),
            new NamedLib("util-ui",
                    // com.intellij.util.ui.JBUI
                    // com.intellij.openapi.util.IconLoader
                    // com.intellij.ui.BooleanTableCellEditor
                    "intellij.platform.util.ui.jar",
            ),
            new NamedLib("util-strings",
                    // com.intellij.util.text.CharArrayCharSequence
                    "intellij.platform.util.base.jar",
            ),
            new NamedLib("util-ex",
                    // com.intellij.credentialStore.OneTimeString
                    "intellij.platform.util.ex.jar",
            ),
            new NamedLib("util-io",
                    // com.intellij.execution.configurations.GeneralCommandLine
                    // com.intellij.execution.process.CapturingProcessHandler
                    // com.intellij.execution.process.ProcessOutput
                    // com.intellij.execution.util.ExecUtil
                    // com.intellij.execution.ExecutionException
                    "intellij.platform.ide.util.io.jar",
            ),
            new NamedLib("util-classloader",
                    // com.intellij.util.lang.UrlClassLoader
                    "intellij.platform.util.classLoader.jar",
            ),
            new NamedLib("util-collections",
                    // com.intellij.util.SmartList
                    //"intellij.platform.util.collections.jar",
            ),
            new NamedLib("editor-ui-api"),
            new NamedLib("guiforms-rt",
                    // com.intellij.uiDesigner.core.GridConstraints
                    "intellij.java.guiForms.rt.jar",
            ),
            new NamedLib("concurrency",
                    // org.jetbrains.concurrency.AsyncPromise
                    // org.jetbrains.concurrency.Promises
                    "intellij.platform.concurrency.jar",
            ),
            new NamedLib("diagnostic",
                    // com.intellij.diagnostic.ActivityCategory
                    //"intellij.platform.util.diagnostic.jar",
            ),

            // deps
            new NamedLib("jdom",
                    // org.jdom.Element
                    "jdom-2.0.6.jar"
            ),
            new NamedLib("jgoodies-forms",
                    // com.jgoodies.forms.layout.CellConstraints
                    // com.jgoodies.forms.layout.FormLayout
                    "forms-1.1-preview.jar"
            ),
            new NamedLib("annotations",
                    // org.jetbrains.annotations.NotNull
                    // org.jetbrains.annotations.Nullable
                    "annotations-java5-20.0.0.jar"
            ),
            // test deps
            new NamedLib("fastutil",
                    // it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet
                    "intellij-deps-fastutil-8.3.1-3.jar",
            ),
            new NamedLib("trove4j",
                    // gnu.trove.TObjectHashingStrategy
                    "trove4j-1.0.20200330.jar",
            ),
            new NamedLib("kotlin-stdlib",
                    // kotlin.jvm.internal.Intrinsics
                    "kotlin-stdlib-1.4.0.jar",
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
                    // com.intellij.compiler.instrumentation.InstrumenterClassWriter
                    "intellij.java.compiler.instrumentationUtil.jar"
            ),
            new NamedLib("compiler-antTasks",
                    // com.intellij.ant.InstrumentIdeaExtensions
                    "intellij.java.compiler.antTasks.jar"
            ),
            new NamedLib("asm",
                    // org.jetbrains.org.objectweb.asm.ClassReader
                    // org.jetbrains.org.objectweb.asm.ClassWriter
                    "asm-all-8.0.1.jar"
            ),
            new NamedLib("guiForms-compiler",
                    // com.intellij.uiDesigner.compiler.AlienFormFileException
                    "intellij.java.guiForms.compiler.jar",
            ),
    )

    private static final Pattern IDE_VERSION = Pattern.compile("^213\$")

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
