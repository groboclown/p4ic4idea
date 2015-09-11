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

package net.groboclown.idea.p4ic.v2.server.cache.state;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class CachedState {
    public static final Date NEVER_LOADED = new Date(Long.MIN_VALUE);

    @NotNull
    Date lastUpdated = NEVER_LOADED;

    @NotNull
    public final Date getLastUpdated() {
        return lastUpdated;
    }

    public void setUpdated() {
        lastUpdated = new Date();
    }

    protected abstract void serialize(@NotNull Element wrapper, @NotNull EncodeReferences ref);

    // This is always a static method.
    //protected abstract void deserialize(@NotNull Element wrapper, @NotNull DecodeReferences ref);

    protected void serializeDate(@NotNull Element wrapper) {
        wrapper.setAttribute("t", encodeLong(lastUpdated.getTime()));
    }

    protected void deserializeDate(@NotNull Element wrapper) {
        lastUpdated = NEVER_LOADED;
        final String timeStr = getAttribute(wrapper, "t");
        if (timeStr != null) {
            Long time = decodeLong(timeStr);
            if (time != null) {
                lastUpdated = new Date(time);
            }
        }
    }


    // XML safe character set which doesn't require escaping; "-" is still reserved as the negative sign.
    protected static String encodeLong(long val) {
        return Long.toString(val, Character.MAX_RADIX);
    }

    protected static Long decodeLong(String str) {
        if (str == null || str.length() <= 0) {
            return null;
        }
        try {
            return Long.parseLong(str, Character.MAX_RADIX);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    @Nullable
    public static String getAttribute(@NotNull Element el, @NotNull String name) {
        final Attribute attr = el.getAttribute(name);
        if (attr == null) {
            return null;
        }
        return attr.getValue();
    }

}
