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

package net.groboclown.p4.server.api.values;

import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;

/**
 * Simple typed depot path.  It must not include extra information, such as revision (<tt>#12</tt>)
 * or other data (<tt>@123</tt>, <tt>@abc</tt>, <tt>@12/21/1980</tt>).  Additionally, it must be
 * in an unescaped form that's usable by the end user.
 */
@Immutable
public interface P4RemoteFile {
    /**
     *
     * @return the full depot path, escaped if necessary, and possibly with annotations.
     */
    @NotNull
    String getDepotPath();

    /**
     *
     * @return the display name as the end-user should see it.  It should be unescaped
     *      and stripped of annotations.
     */
    @NotNull
    String getDisplayName();
}
