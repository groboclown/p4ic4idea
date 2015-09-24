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
package net.groboclown.idea.p4ic.server;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import net.groboclown.idea.p4ic.config.*;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidClientException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.v2.events.BaseConfigUpdatedListener;
import net.groboclown.idea.p4ic.v2.events.ConfigInvalidListener;
import net.groboclown.idea.p4ic.v2.events.Events;
import net.groboclown.idea.p4ic.v2.server.connection.ProjectConfigSource;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientManager {
    private static final Logger LOG = Logger.getInstance(ClientManager.class);

    private final Project project;
    private final P4ConfigProject configProject;
    private final ReadWriteLock clientAccess;
    private boolean initialized = false;
    private MessageBusConnection projectMessageBus;
    private MessageBusConnection appMessageBus;

    // The clients list is an unmodifiable list, that is wholly replaced
    // inside a write lock.  This allows reads to always just return
    // the list itself.
    @NotNull
    private List<Client> clients = Collections.emptyList();

    public ClientManager(@NotNull Project project, @NotNull P4ConfigProject configProject) {
        this.project = project;
        this.configProject = configProject;
        clientAccess = new ReentrantReadWriteLock();
    }


    @NotNull
    public List<Client> getClients() {
        // do not block if a write is happening; a write in progress
        // means that the current config is in flux, so it's okay to
        // have no clients.
        boolean didLock = clientAccess.readLock().tryLock();
        if (didLock) {
            try {
                return clients;
            } finally {
                clientAccess.readLock().unlock();
            }
        } else {
            return Collections.emptyList();
        }
    }


    public void initialize() {
        clientAccess.writeLock().lock();
        try {
            if (!initialized) {
                //ApplicationManager.getApplication().getMessageBus().connect().subscribe(
                //        P4ConfigListener.TOPIC, new ConfigListener());
                projectMessageBus = project.getMessageBus().connect();

                // FIXME old stuff
                ConfigListener listener = new ConfigListener();
                projectMessageBus.subscribe(P4ConfigListener.TOPIC, listener);

                appMessageBus = ApplicationManager.getApplication().getMessageBus().connect();

                Events.appBaseConfigUpdated(appMessageBus, listener);
                Events.appConfigInvalid(appMessageBus, listener);

                loadConfig();
                initialized = true;
            }
        } finally {
            clientAccess.writeLock().unlock();
        }
    }


    public void dispose() {
        // All writes are mandatory (not interruptable, not tryLock).
        // The connections can be redone, so don't formally mark
        // this as disposed (where it can't be reused).
        clientAccess.writeLock().lock();
        try {
            writeLockedDispose();
            clients = Collections.emptyList();
        } finally {
            clientAccess.writeLock().unlock();
        }
        if (projectMessageBus != null) {
            projectMessageBus.disconnect();
            projectMessageBus = null;
        }
        if (appMessageBus != null) {
            appMessageBus.disconnect();
            appMessageBus = null;
        }
    }


    public void loadConfig() {
        clientAccess.writeLock().lock();
        try {
            writeLockedDispose();
            clients = configProject.loadClients(project);
        } catch (P4InvalidClientException e) {
            // NOTE invalid configuration call will be done
            // outside of here
            LOG.info(e);
            clients = Collections.emptyList();
        } catch (P4InvalidConfigException e) {
            // NOTE invalid configuration call will be done
            // outside of here
            LOG.info(e);
            clients = Collections.emptyList();
        } finally {
            clientAccess.writeLock().unlock();
        }
    }


    private void writeLockedDispose() {
        for (Client client : clients) {
            client.dispose();
            ServerStoreService.getInstance().removeServerConfig(project, client.getConfig());
        }
    }


    class ConfigListener implements P4ConfigListener, BaseConfigUpdatedListener, ConfigInvalidListener {
        @Override
        public void configChanges(@NotNull Project project, @NotNull P4Config original, @NotNull P4Config config) {
            loadConfig();
        }

        @Override
        public void configurationProblem(@NotNull Project project, @NotNull P4Config config, @NotNull P4InvalidConfigException ex) {
            dispose();
        }

        @Override
        public void configUpdated(@NotNull final Project project, @NotNull final List<ProjectConfigSource> sources) {
            loadConfig();
        }
    }
}
