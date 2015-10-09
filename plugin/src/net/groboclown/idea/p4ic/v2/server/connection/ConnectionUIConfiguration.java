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

package net.groboclown.idea.p4ic.v2.server.connection;

import com.perforce.p4java.exception.P4JavaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the server connection code when setting up an initial connection.
 */
public class ConnectionUIConfiguration {
    public static void checkConnection(@NotNull ProjectConfigSource source)
            throws P4JavaException, IOException, URISyntaxException {
        ClientExec.getServerInfo(source.getServerConfig());
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @NotNull
    public static Map<ProjectConfigSource, Exception> findConnectionProblems(@NotNull
            Collection<ProjectConfigSource> sources) {
        final Map<ProjectConfigSource, Exception> ret = new HashMap<ProjectConfigSource, Exception>();
        for (ProjectConfigSource source : sources) {
            try {
                checkConnection(source);
            } catch (P4JavaException e) {
                ret.put(source, e);
            } catch (IOException e) {
                ret.put(source, e);
            } catch (URISyntaxException e) {
                ret.put(source, e);
            }
        }
        return ret;
    }

    @Nullable
    public static Map<ProjectConfigSource, ClientResult> getClients(@Nullable
            Collection<ProjectConfigSource> sources) {
        final Map<ProjectConfigSource, ClientResult> ret = new HashMap<ProjectConfigSource, ClientResult>();
        if (sources == null) {
            return null;
        }
        for (ProjectConfigSource source : sources) {
            try {
                final List<String> clients = ClientExec.getClientNames(source.getServerConfig());
                ret.put(source, new ClientResult(clients));
            } catch (IOException e) {
                ret.put(source, new ClientResult(e));
            } catch (P4JavaException e) {
                ret.put(source, new ClientResult(e));
            } catch (URISyntaxException e) {
                ret.put(source, new ClientResult(e));
            }
        }
        return ret;
    }



    public static class ClientResult {
        private final List<String> clientNames;
        private final Exception connectionProblem;

        private ClientResult(@NotNull List<String> clientNames) {
            this.clientNames = clientNames;
            this.connectionProblem = null;
        }

        private ClientResult(@NotNull Exception ex) {
            this.clientNames = null;
            this.connectionProblem = ex;
        }

        public boolean isInalid() {
            return clientNames == null;
        }

        public List<String> getClientNames() {
            return clientNames;
        }

        public Exception getConnectionProblem() {
            return connectionProblem;
        }
    }
}
