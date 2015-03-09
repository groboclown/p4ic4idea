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

package net.groboclown.idea.p4ic.changes;

import com.intellij.openapi.util.Comparing;
import com.perforce.p4java.core.IChangelistSummary;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

class P4ChangeListIdImpl implements P4ChangeListId {
    private final int clid;

    @NotNull
    private final String scid;

    @NotNull
    private final String clientName;

    P4ChangeListIdImpl(@NotNull String serverConfigId, @NotNull String clientName, int clid) {
        this.clid = clid;
        this.scid = serverConfigId;
        this.clientName = clientName;
        assert clid >= P4ChangeListCache.P4_DEFAULT;
    }


    P4ChangeListIdImpl(@NotNull Client client, @NotNull IChangelistSummary summary) {
        this.clid = summary.getId();
        this.scid = client.getConfig().getServiceName();
        this.clientName = client.getClientName();
        assert clid >= P4ChangeListCache.P4_DEFAULT;
    }

    P4ChangeListIdImpl(@NotNull ServerConfig config, @NotNull IChangelistSummary summary) {
        this.clid = summary.getId();
        this.scid = config.getServiceName();
        this.clientName = summary.getClientId();
        assert clid >= P4ChangeListCache.P4_DEFAULT;
    }

    P4ChangeListIdImpl(@NotNull Client client, final int p4id) {
        this.clid = p4id;
        this.scid = client.getConfig().getServiceName();
        this.clientName = client.getClientName();
        assert clid >= P4ChangeListCache.P4_DEFAULT;
    }

    @Override
    public int getChangeListId() {
        return clid;
    }

    @NotNull
    @Override
    public String getServerConfigId() {
        return scid;
    }

    @NotNull
    @Override
    public String getClientName() {
        return clientName;
    }

    @Override
    public boolean isNumberedChangelist() {
        return clid > 0;
    }

    @Override
    public boolean isDefaultChangelist() {
        return clid == P4ChangeListCache.P4_DEFAULT;
    }

    @Override
    public boolean isIn(@NotNull Client client) {
        return scid.equals(client.getConfig().getServiceName()) &&
                clientName.equals(client.getClientName());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o.getClass().equals(P4ChangeListIdImpl.class))) {
            return false;
        }
        if (o == this) {
            return true;
        }
        P4ChangeListIdImpl that = (P4ChangeListIdImpl) o;
        // due to the dynamic loading nature of this class, there are some
        // weird circumstances where the scid and client name can be null.
        return that.clid == this.clid &&
                Comparing.equal(that.scid, this.scid) &&
                Comparing.equal(that.clientName, this.clientName);
    }

    @Override
    public int hashCode() {
        return clid + scid.hashCode();
    }

    @Override
    public String toString() {
        return getServerConfigId() + "+" + getClientName() + "@" + getChangeListId();
    }
}
