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

class IdeaVersion182_183 implements IdeaVersionLibMatcher {
    private static final LibGroup libs = new LibGroup().add(
            new NamedLib("openapi",
                    "intellij.java.jar"
            ),
            new NamedLib("core-api",
                    "intellij.platform.core.jar"
            ),
            new NamedLib("core-impl",
                    "intellij.platform.core.impl.jar"
            ),
            new NamedLib("vcs-api-core",
                    "intellij.platform.vcs.core.jar"
            ),
            new NamedLib("vcs-api",
                    "intellij.platform.vcs.jar"
            ),
            new NamedLib("vcs-impl",
                    "intellij.platform.vcs.impl.jar"
            ),
            new NamedLib("platform-api",
                    "intellij.platform.ide.jar"
            ),
            new NamedLib("platform-impl",
                    "intellij.platform.ide.impl.jar"
            ),
            new NamedLib("projectModel-api",
                    "intellij.platform.projectModel.jar",
            ),
            new NamedLib("extensions",
                    "intellij.platform.extensions.jar"
            ),
            new NamedLib("util",
                    "intellij.platform.util.jar"
            ),
            new NamedLib("util-ui",
            ),
            new NamedLib("util-rt",
                    "intellij.platform.util.rt.jar"
            ),
            new NamedLib("editor-ui-api",
                    "intellij.platform.editor.jar"
            ),
            new NamedLib("platform-resources-en",
                    "intellij.platform.resources.en.jar"
            ),
            new NamedLib('testFramework',
                    'intellij.platform.testFramework.jar'
            ),
            new NamedLib("forms_rt",
                    "intellij.java.guiForms.rt.jar"
            ),
            new NamedLib("java-psi-impl",
                    "intellij.java.psi.impl.jar"
            ),
            new NamedLib("java-runtime",
                    "intellij.java.rt.jar"
            ),
            new NamedLib("lang-api",
                    "intellij.platform.lang.jar"
            ),
            new NamedLib("lang-impl",
                    "intellij.platform.lang.impl.jar"
            ),
            new NamedLib("instrumentation-util",
                    "intellij.java.compiler.instrumentationUtil.jar",
            ),
            new NamedLib("javac2",
                    "intellij.java.compiler.antTasks.jar"
            ),
            new NamedLib("forms-compiler",
                    "intellij.java.guiForms.compiler.jar"
            ),

            // deps
            new NamedLib("jdom",
                    "jdom.jar"
            ),
            new NamedLib("picocontainer",
                    "picocontainer-1.2.jar"
            ),
            new NamedLib("trove4j",
                    "trove4j.jar"
            ),
            new NamedLib("kotlin",
                    "kotlin-stdlib.jar"
            ),
            new NamedLib("jgoodies-forms",
                    "forms-1.1-preview.jar",
            ),
            new NamedLib("forms-rt",
                    "forms_rt.jar",
                    "intellij.java.guiForms.rt.jar",
            ),
            new NamedLib("annotations",
                    "annotations.jar"
            ),
            new NamedLib("asm",
                    "asm-all.jar"
            ),
    )

    private static final Pattern IDE_VERSION = Pattern.compile("^18[23]\$")

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
