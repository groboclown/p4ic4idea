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

package net.groboclown.p4plugin.ui.sync;

import com.intellij.openapi.util.Comparing;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SyncOptions {
    private static final int INVALID_REV = Integer.MIN_VALUE;

    private @NotNull SyncType type;
    private int rev;
    private @Nullable String other;
    private boolean force;

    enum SyncType {
        HEAD, REV, OTHER
    }

    public static SyncOptions createDefaultSyncOptions() {
        return new SyncOptions(SyncOptions.SyncType.HEAD, INVALID_REV, null, false);
    }

    SyncOptions(final @NotNull SyncType type, final int rev, @Nullable final String other,
            final boolean force) {
        this.type = type;
        this.rev = rev;
        this.other = other;
        this.force = force;
    }

    SyncOptions(SyncOptions copy) {
        this.type = copy.type;
        this.rev = copy.rev;
        this.other = copy.other;
        this.force = copy.force;
    }

    void copyFrom(@NotNull SyncOptions copy) {
        this.type = copy.type;
        this.rev = copy.rev;
        this.other = copy.other;
        this.force = copy.force;
    }

    void setForce(boolean f) {
        this.force = f;
    }

    void setRevision(int rev) {
        this.type = SyncType.REV;
        this.rev = rev;
        this.other = null;
    }

    void setHead() {
        this.type = SyncType.HEAD;
        this.rev = -1;
        this.other = null;
    }

    void setOther(@NotNull String other) {
        this.type = SyncType.OTHER;
        this.rev = -1;
        this.other = other;
    }

    SyncType getSyncType() {
        return type;
    }

    public int getRev() {
        return rev;
    }

    public String getOther() {
        return other;
    }

    public boolean hasError() {
        return type == SyncType.REV && rev == INVALID_REV;
    }

    @NotNull
    public String getSpecAnnotation() {
        switch (type) {
            case HEAD:
                return "";
            case REV:
                return "#" + Integer.toString(rev);
            case OTHER:
                return "@" + other;
        }
        throw new IllegalStateException("invalid type " + type);
    }

    public boolean isForce() {
        return force;
    }

    @Nullable
    public String getError() {
        if (hasError()) {
            return P4Bundle.message("sync.options.rev.error");
        }
        return null;
    }

    @Override
    public int hashCode() {
        return type.hashCode() + rev +
                (other == null ? 0 : other.hashCode()) +
                (force ? 200 : 100);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SyncOptions)) {
            return false;
        }
        SyncOptions that = (SyncOptions) obj;
        return this.type == that.type &&
                Comparing.equal(this.rev, that.rev) &&
                Comparing.equal(this.other, that.other) &&
                this.force == that.force;
    }

    @Override
    public String toString() {
        return "(" + type + ": " + rev + ", " + other + " f? " + force + ")";
    }
}
