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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.config.P4ConfigProject;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.server.cache.sync.ClientCacheManager;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maps the specific Perforce configuration object to the corresponding Project directories it covers.
 * <p/>
 * These are created via {@link P4ConfigProject#loadProjectConfigSources(Project)}.
 */
public class ProjectConfigSource {
    private final Project project;
    private final List<FilePath> projectSourceDirs;
    private final String clientName;
    private final ServerConfig configuration;

    public static class Builder {
        private final Project project;
        private final String clientName;
        private final ServerConfig serverConfig;
        private final P4Config baseConfig;
        private final Set<FilePath> dirs = new HashSet<FilePath>();

        public Builder(@NotNull Project project, @NotNull P4Config config) {
            this.project = project;
            this.baseConfig = config;
            this.clientName = config.getClientname();
            this.serverConfig = ServerConfig.createNewServerConfig(config);
        }

        public boolean isSame(@NotNull P4Config other) {
            final ServerConfig newConfig = ServerConfig.createNewServerConfig(other);
            return newConfig != null && newConfig.equals(serverConfig) && Comparing.equal(clientName, other.getClientname());
        }

        public void add(@NotNull VirtualFile dir) {
            dirs.add(FilePathUtil.getFilePath(dir));
        }

        public ProjectConfigSource create() throws P4InvalidConfigException {
            final List<FilePath> roots = new ArrayList<FilePath>();

            // FIXME this should be pulling from the workspace view!!!!
            throw new IllegalStateException("not implemented");

            /*
            // Note: this is a chicken and the egg problem.  We'll get the


            final ServerConnection conn = ServerConnectionManager.getInstance().getConnectionFor(
                    ClientServerId.create(serverConfig, clientName), serverConfig);
            final List<FilePath> roots = conn.query(project, );
            return new ProjectConfigSource(project, roots, clientName, serverConfig);
            */

        }
    }


    public ProjectConfigSource(@NotNull Project project, @NotNull List<FilePath> projectSourceDirs,
            @NotNull String clientName, @NotNull ServerConfig configuration) {
        this.project = project;
        this.projectSourceDirs = projectSourceDirs;
        this.clientName = clientName;
        this.configuration = configuration;
    }

    public Project getProject() {
        return project;
    }



    static class GetClientRoots implements ServerQuery<List<FilePath>> {
        @Nullable
        @Override
        public List<FilePath> query(@NotNull final P4Exec2 exec, @NotNull final ClientCacheManager cacheManager,
                @NotNull final ServerConnection connection, @NotNull final AlertManager alerts)
                throws InterruptedException {
            return null;
        }
    }
}
