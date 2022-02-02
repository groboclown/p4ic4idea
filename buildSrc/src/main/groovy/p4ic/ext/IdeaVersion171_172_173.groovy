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

class IdeaVersion171_172_173 implements IdeaVersionLibMatcher {
    private static final LibGroup libs = new LibGroup().add(
            new NamedLib("openapi",
                    "openapi.jar"
            ),
            new NamedLib("core-api",
                    "core-api.jar"
            ),
            new NamedLib("core-impl",
                    "core-impl.jar"
            ),
            new NamedLib("annotations",
                    "annotations.jar",
                    "annotations-common.jar"
            ),
            new NamedLib("vcs-api-core",
                    "vcs-api-core.jar"
            ),
            new NamedLib("vcs-api",
                    "vcs-api.jar"
            ),
            new NamedLib("vcs-impl",
                    "vcs-impl.jar"
            ),
            new NamedLib("platform-api",
                    "platform-api.jar"
            ),
            new NamedLib("platform-impl",
                    "platform-impl.jar"
            ),
            new NamedLib("projectModel-api",
                    "projectModel-api.jar",
            ),
            new NamedLib("extensions",
                    "extensions.jar"
            ),
            new NamedLib("util",
                    "util.jar"
            ),
            new NamedLib("util-ui",
            ),
            new NamedLib("util-rt",
                    "util-rt.jar"
            ),
            new NamedLib("editor-ui-api",
                    "editor-ui-api.jar"
            ),
            new NamedLib("platform-resources-en",
                    "platform-resources-en.jar"
            ),
            new NamedLib('testFramework',
                    'testFramework.jar'
            ),
            new NamedLib("forms_rt",
                    "forms_rt.jar"
            ),
            new NamedLib("java-psi-impl",
                    "java-psi-impl.jar"
            ),
            new NamedLib("java-runtime",
                    "java-runtime.jar"
            ),
            new NamedLib("lang-api",
                    "lang-api.jar"
            ),
            new NamedLib("lang-impl",
                    "lang-impl.jar"
            ),
            new NamedLib("instrumentation-util",
                    "instrumentation-util.jar",
            ),
            new NamedLib("javac2",
                    "javac2.jar"
            ),
            new NamedLib("forms-compiler",
                    "forms-compiler.jar"
            ),

            // deps
            new NamedLib("jdom",
                    "jdom.jar"
            ),
            new NamedLib("picocontainer",
                    "picocontainer.jar"
            ),
            new NamedLib("trove4j",
                    "trove4j.jar"
            ),
            new NamedLib("kotlin",
                    "kotlin-runtime.jar"
            ),
            new NamedLib("jgoodies-forms",
                    "jgoodies-forms.jar"
            ),
            new NamedLib("forms-rt",
                    "forms_rt.jar"
            ),
            new NamedLib("asm",
                    "asm-all.jar"
            ),
    )

    private static final Pattern IDE_VERSION = Pattern.compile("^17\\d\$")

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
