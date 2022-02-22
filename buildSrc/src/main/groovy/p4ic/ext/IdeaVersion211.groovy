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

class IdeaVersion211 implements IdeaVersionLibMatcher {
    private static final LibGroup libs = new LibGroup().add(
            new NamedLib("openapi",
                    //"intellij.platform.util.rt.jar"
            ),
            new NamedLib("core-api",
                    //"intellij.platform.core.jar"
            ),
            new NamedLib("core-impl",
                    // Should only be used by tests, but no longer needed.
            ),
            new NamedLib("vcs-api-core",
                    //"intellij.platform.vcs.core.jar"
            ),
            new NamedLib("vcs-api",
                    //"intellij.platform.vcs.jar",

                    // Strange that
                    //   com.intellij.openapi.vfs.LocalFileSystem
                    // is now in the analysis jar.
                    //"intellij.platform.analysis.jar"
            ),
            new NamedLib("vcs-impl",
                    //"intellij.platform.vcs.impl.jar",
            ),
            new NamedLib("platform-api",
                    //"intellij.platform.ide.jar"
            ),
            new NamedLib("platform-impl",
                    //"intellij.platform.ide.impl.jar",
            ),
            new NamedLib("projectModel-api",
                    // No longer needed
            ),
            new NamedLib("extensions",
                    //"intellij.platform.extensions.jar"
            ),
            new NamedLib("util",
                    //"intellij.platform.util.jar",
                    //"intellij.platform.util.classLoader.jar",
            ),
            new NamedLib("util-ui",
                    //"intellij.platform.util.ui.jar"
            ),
            new NamedLib("util-rt",
                    //"intellij.platform.util.rt.jar",

                    // com.intellij.util.text.CharArrayCharSequence, used in password access, moved here:
                    //"intellij.platform.util.strings.jar"
            ),
            new NamedLib("editor-ui-api",
                    // this includes com.intellij.openapi.vcs.FileStatus
                    //"intellij.platform.editor.jar"
            ),
            new NamedLib("platform-resources-en",
                    // not used anymore
            ),
            new NamedLib('testFramework',
                    // not used anymore
            ),
            new NamedLib("forms_rt",
                    // "intellij.java.guiForms.rt.jar"
            ),
            new NamedLib("java-psi-impl",
                    // not used anymore
            ),
            new NamedLib("java-runtime",
                    // not used anymore
            ),
            new NamedLib("lang-api",
                    // not used anymore
            ),
            new NamedLib("lang-impl",
                    // not used anymore
            ),
            new NamedLib("instrumentation-util",
                    // not used anymore
            ),
            new NamedLib("javac2",
                    // not used anymore
            ),
            new NamedLib("forms-compiler",
                    // not used anymore
            ),

            // deps
            new NamedLib("jdom",
                    //"jdom-2.0.6.jar"
            ),
            new NamedLib("picocontainer",
                    // Note: This is ONLY used by tests.
                    //"picocontainer-1.2.jar"
            ),
            new NamedLib("trove4j",
                    // not needed anymore
            ),
            new NamedLib("kotlin",
                    // not needed anymore
            ),
            new NamedLib("jgoodies-forms",
                    //"forms-1.1-preview.jar"
            ),
            new NamedLib("annotations",
                    //"annotations-java5-20.0.0.jar"
            ),
    )

    private static final Pattern IDE_VERSION = Pattern.compile("^211\$")

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
