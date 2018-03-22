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

package net.groboclown.idea.p4ic.ui.sync;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Comparing;
import net.groboclown.idea.p4ic.P4Bundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class SyncOptionConfigurable implements Configurable {
    private static final Logger LOG = Logger.getInstance(SyncOptionConfigurable.class);

    @NotNull
    private SyncOptions currentOptions = createDefaultSyncOptions();

    @Nullable
    private SyncOptions pendingOptions = null;


    // TODO allow for changelist browsing.
    // For changelist browsing, we can limit the number of changes returned, and have a paging
    // mechanism - "p4 changes -m 10 ...@<(last changelist number)"


    @Nls
    @Override
    public String getDisplayName() {
        return P4Bundle.getString("sync.options.title");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        currentOptions = createDefaultSyncOptions();
        pendingOptions = null;
        return new SyncPanel(this).getPanel();
    }

    @Override
    public boolean isModified() {
        return pendingOptions != null && pendingOptions.equals(currentOptions);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (pendingOptions == null) {
            pendingOptions = currentOptions;
        }
        currentOptions = pendingOptions;
        if (LOG.isDebugEnabled()) {
            LOG.debug("SyncOptions set to " + currentOptions);
        }
    }

    @Override
    public void reset() {
        pendingOptions = null;
    }

    @Override
    public void disposeUIResources() {
        // should dispose of the panel
        // however, we don't keep references to it, so it is
        // automatically cleaned up.
    }


    void onOptionChange(SyncOptions options) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("SyncOptions pending to " + options);
        }
        pendingOptions = options;
    }


    public int getRevision() {
        if (currentOptions.type != SyncType.REV || currentOptions.rev == null) {
            return -1;
        }
        return currentOptions.rev;
    }


    @Nullable
    public String getChangelist() {
        if (currentOptions.type != SyncType.OTHER) {
            return null;
        }
        return currentOptions.other;
    }


    public boolean isForceSync() {
        return currentOptions.force;
    }


    @NotNull
    public SyncOptions getCurrentOptions() {
        return currentOptions;
    }


    private static SyncOptions createDefaultSyncOptions() {
        return new SyncOptions(SyncType.HEAD, null, null, false);
    }

    enum SyncType {
        HEAD, REV, OTHER
    }

    static class SyncOptions {
        @NotNull final SyncType type;
        @Nullable final Integer rev;
        @Nullable final String other;
        final boolean force;

        SyncOptions(final @NotNull SyncType type, @Nullable final Integer rev, @Nullable final String other,
                final boolean force) {
            this.type = type;
            this.rev = rev;
            this.other = other;
            this.force = force;
        }

        boolean hasError() {
            return type == SyncType.REV && rev == null;
        }

        @Nullable
        String getError() {
            if (hasError()) {
                return P4Bundle.message("sync.options.rev.error");
            }
            return null;
        }

        @Override
        public int hashCode() {
            return type.hashCode() + (rev == null ? 0 : rev.hashCode()) +
                    (other == null ? 0 : other.hashCode()) +
                    (force ? 200 : 100);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || ! obj.getClass().equals(SyncOptions.class)) {
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
}
