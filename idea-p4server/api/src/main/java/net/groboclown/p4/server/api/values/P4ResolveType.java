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

import org.jetbrains.annotations.Nullable;

public enum P4ResolveType {
    /** Not in a state where the file can be resolved.  Use this instead of "null" */
    NO_RESOLVE(null, null),

    /** No resolve necessary */
    NOT_NECESSARY("???", "???");


    private final String resolveType;
    private final String contentResolveType;

    P4ResolveType(String resolveType, String contentResolveType) {
        this.resolveType = resolveType;
        this.contentResolveType = contentResolveType;
    }

    @Nullable
    public String getResolveType() {
        return resolveType;
    }

    @Nullable
    public String getContentResolveType() {
        return contentResolveType;
    }

    public static P4ResolveType convert(String resolveType, String contentResolveType) {
        if (resolveType == null || contentResolveType == null) {
            return NOT_NECESSARY;
        }
        // FIXME
        return NO_RESOLVE;
    }
}
