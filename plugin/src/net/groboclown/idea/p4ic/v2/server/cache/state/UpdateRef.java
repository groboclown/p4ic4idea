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

package net.groboclown.idea.p4ic.v2.server.cache.state;

import com.intellij.openapi.diagnostic.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A state that is associated with a {@link PendingUpdateState}.
 * When the corresponding {@link PendingUpdateState} is removed,
 * this object should be removed too.
 *
 */
public abstract class UpdateRef extends CachedState {
    private static final Logger LOG = Logger.getInstance(UpdateRef.class);

    private int pendingUpdateRefId;


    protected UpdateRef(@Nullable PendingUpdateState state) {
        if (state != null) {
            this.pendingUpdateRefId = state.getRefId();
        }
    }


    public int getPendingUpdateRefId() {
        return pendingUpdateRefId;
    }



    // Take over the parent's meaning, so that this is
    // automatically serialized / deserialized for all subclasses.
    @Override
    protected void serializeDate(@NotNull Element wrapper) {
        super.serializeDate(wrapper);
        wrapper.setAttribute("ur", encodeLong(pendingUpdateRefId));
    }

    @Override
    protected void deserializeDate(@NotNull Element wrapper) {
        super.deserializeDate(wrapper);
        Long refId = decodeLong(getAttribute(wrapper, "ur"));
        if (refId == null) {
            LOG.warn("Orphan " + getClass().getSimpleName() + " - no update ref id");
            pendingUpdateRefId = -1;
        } else {
            pendingUpdateRefId = refId.intValue();
        }
    }
}
