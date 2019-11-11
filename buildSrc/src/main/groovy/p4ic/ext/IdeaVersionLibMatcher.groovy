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

import javax.annotation.Nonnull
import java.util.regex.Pattern

interface IdeaVersionLibMatcher {
    Pattern getIdeaVersionMatch()

    /**
     * @return list of named libraries.
     */
    NamedLib getNamedLib(String name)
}


/*
idea-p4server.api.api:
        'openapi',
        'core-api',
        'annotations-common',
        'annotations',
        'vcs-api-core',
        'vcs-api',

        'platform-api',
        'projectModel-api'

idea-p4server.api.impl:
        'extensions',
        'util',
        'util-rt',

        'jdom',
        'picocontainer',
        'trove4j',

idea-p4server.api,test:
        'kotlin-runtime'

idea-p4server.impl.impl:
        'openapi',
        'core-api',
        'annotations-common',
        'annotations',
        'vcs-api-core',
        'vcs-api',
        'platform-api',
        'util',
        'util-rt',
        'extensions',

        // TODO look at removing this
        // this os only used by P4FileAction
        'editor-ui-api',

        'jdom',
        'kotlin-runtime'

idea-p4server.impl.test:
        'platform-resources-en',

idea-test-core.api:
        'openapi',
        'core-api',
        'annotations-common',
        'annotations',
        'vcs-api-core',
        'vcs-api',
        'platform-api',

idea-test-core.impl:
        'util',
        'util-rt',
        'testFramework',
        'extensions',
        'projectModel-api',

        // for the VcsContextFactory
        'editor-ui-api',

        'jdom',
        'picocontainer',
        'trove4j',

plugin-v3.impl:
        'openapi',
        'core-api',
        'annotations-common',
        'annotations',
        'vcs-api-core',
        'vcs-api',
        'platform-api',
        'util',
        'util-rt',
        'editor-ui-api',
        'extensions',
        'jdom',
        'forms_rt',
        'jgoodies-forms',
        'projectModel-api',

        // Look at getting rid of the "impl" dependencies
        'platform-impl',
        'vcs-impl',
        'core-impl'

plugin-v3.test:
        'testFramework',
        'java-psi-impl',
        'java-runtime',
        'lang-api',
        'lang-impl',
        'trove4j'

 */