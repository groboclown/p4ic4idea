/* *************************************************************************
 * (c) Copyright 2015 Zilliant Inc. All rights reserved.                   *
 * *************************************************************************
 *                                                                         *
 * THIS MATERIAL IS PROVIDED "AS IS." ZILLIANT INC. DISCLAIMS ALL          *
 * WARRANTIES OF ANY KIND WITH REGARD TO THIS MATERIAL, INCLUDING,         *
 * BUT NOT LIMITED TO ANY IMPLIED WARRANTIES OF NONINFRINGEMENT,           *
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.                   *
 *                                                                         *
 * Zilliant Inc. shall not be liable for errors contained herein           *
 * or for incidental or consequential damages in connection with the       *
 * furnishing, performance, or use of this material.                       *
 *                                                                         *
 * Zilliant Inc. assumes no responsibility for the use or reliability      *
 * of interconnected equipment that is not furnished by Zilliant Inc,      *
 * or the use of Zilliant software with such equipment.                    *
 *                                                                         *
 * This document or software contains trade secrets of Zilliant Inc. as    *
 * well as proprietary information which is protected by copyright.        *
 * All rights are reserved.  No part of this document or software may be   *
 * photocopied, reproduced, modified or translated to another language     *
 * prior written consent of Zilliant Inc.                                  *
 *                                                                         *
 * ANY USE OF THIS SOFTWARE IS SUBJECT TO THE TERMS AND CONDITIONS         *
 * OF A SEPARATE LICENSE AGREEMENT.                                        *
 *                                                                         *
 * The information contained herein has been prepared by Zilliant Inc.     *
 * solely for use by Zilliant Inc., its employees, agents and customers.   *
 * Dissemination of the information and/or concepts contained herein to    *
 * other parties is prohibited without the prior written consent of        *
 * Zilliant Inc..                                                          *
 *                                                                         *
 * (c) Copyright 2015 Zilliant Inc. All rights reserved.                   *
 *                                                                         *
 * *************************************************************************/

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

public class SyncOptionConfigurable implements Configurable {
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
        LOG.info("SyncOptions set to " + currentOptions);
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
        LOG.debug("SyncOptions pending to " + options);
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
