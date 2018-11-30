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
     * Checks if the filename is in the list under an alias.  The filename ends with ".jar", and does not
     * include its directory.
     *
     * @param filename
     * @param remaining
     * @return names of the matched entry in the choices list, or
     *  null if it didn't match.
     */
    String[] matches(@Nonnull final String filename, @Nonnull final Collection<String> choices)

    /**
     *
     * @param libChoiceName
     * @return true if the choice value is ignored for this version.
     */
    boolean ignoredChoice(@Nonnull final String libChoiceName)
}
