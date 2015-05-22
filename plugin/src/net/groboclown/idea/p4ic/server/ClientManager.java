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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.config.*;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidClientException;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
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
                project.getMessageBus().connect().subscribe(P4ConfigListener.TOPIC, new ConfigListener());
                loadConfig();
                initialized = true;
            }
        } finally {
            clientAccess.writeLock().unlock();
        }
    }


    public void dispose() {
        // All writes are mandatory (not interruptable, not tryLock).
        clientAccess.writeLock().lock();
        try {
            writeLockedDispose();
            clients = Collections.emptyList();
        } finally {
            clientAccess.writeLock().unlock();
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

        // add notice to message bus about clients loaded.
        project.getMessageBus().syncPublisher(P4ClientsReloadedListener.TOPIC).clientsLoaded(project, clients);
    }


    private void writeLockedDispose() {
        for (Client client : clients) {
            client.dispose();
            ServerStoreService.getInstance().removeServerConfig(project, client.getConfig());
        }
    }


    class ConfigListener implements P4ConfigListener {
        @Override
        public void configChanges(@NotNull Project project, @NotNull P4Config original, @NotNull P4Config config) {
            loadConfig();
        }

        @Override
        public void configurationProblem(@NotNull Project project, @NotNull P4Config config, @NotNull P4InvalidConfigException ex) {
            dispose();
        }
    }
}
