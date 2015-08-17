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

package net.groboclown.idea.p4ic.v2.server.cache;

import org.jetbrains.annotations.NotNull;

public final class ObjectId {

    /** The type of object the ID represents */
    public enum Type {
        CHANGELIST,
        FILE,
        JOB,
        WORKSPACE
    }


    private final String id;
    private final Type type;

    public ObjectId(@NotNull final String id, @NotNull final Type type) {
        this.id = id;
        this.type = type;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + ":" + id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass().equals(getClass())) {
            ObjectId that = (ObjectId) obj;
            return that.id.equals(id) && that.type == type;
        }
        return false;
    }
}
