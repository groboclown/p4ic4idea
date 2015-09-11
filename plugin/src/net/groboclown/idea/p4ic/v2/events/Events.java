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

package net.groboclown.idea.p4ic.v2.events;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import org.jetbrains.annotations.NotNull;

public final class Events {
    private Events() {
        // utility class
    }


    /* Should only be called by the {@link P4ConfigProject}
    public static void baseConfigUpdated(@NotNull Project project, @NotNull List<ProjectConfigSource> sources) {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(BaseConfigUpdatedListener.TOPIC).
                configUpdated(project, sources);
    }
    */


    public static void appBaseConfigUpdated(@NotNull MessageBusConnection appBus, @NotNull BaseConfigUpdatedListener listener) {
        appBus.subscribe(BaseConfigUpdatedListener.TOPIC, listener);
    }


    public static void configInvalid(@NotNull Project project, @NotNull P4Config config, @NotNull P4InvalidConfigException e)
            throws P4InvalidConfigException {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ConfigInvalidListener.TOPIC).
                configurationProblem(project, config, e);
        throw e;
    }


    public static void appConfigInvalid(@NotNull MessageBusConnection appBus, @NotNull ConfigInvalidListener listener) {
        appBus.subscribe(ConfigInvalidListener.TOPIC, listener);
    }


    public static void serverConnected(@NotNull ServerConfig config) {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ServerConnectionStateListener.TOPIC).
                connected(config);
    }


    public static void serverDisconnected(@NotNull ServerConfig config) {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(ServerConnectionStateListener.TOPIC).
                disconnected(config);
    }


    public static void appServerConnectionState(@NotNull MessageBusConnection appBus, @NotNull ServerConnectionStateListener listener) {
        appBus.subscribe(ServerConnectionStateListener.TOPIC, listener);
    }
}
