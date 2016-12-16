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

package net.groboclown.idea.p4ic.compat;

import org.jetbrains.annotations.NotNull;

public interface CompatFactory {
    @NotNull
    String getMinCompatibleApiVersion();

    /**
     * The API is compatible with versions up to, but not including,
     * this returned version.
     *
     * @return API version beyond what this factory supports.
     */
    @NotNull
    String getMaxCompatibleApiVersion();

    @NotNull
    CompatManager createCompatManager()
            throws IllegalStateException;
}
