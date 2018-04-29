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

package net.groboclown.p4.server.api.values;

import org.jetbrains.annotations.NotNull;

public class MockP4ChangelistSummary implements P4ChangelistSummary {
    private P4ChangelistId changelistId;
    private String comment;
    private boolean deleted;
    private boolean submitted;
    private boolean onServer;
    private boolean hasShelved;
    private String clientname;
    private String username;

    public MockP4ChangelistSummary withRemoteChangelist(P4RemoteChangelist cl) {
        changelistId = cl.getChangelistId();
        comment = cl.getComment();
        deleted = cl.isDeleted();
        submitted = cl.isSubmitted();
        onServer = cl.isOnServer();
        hasShelved = cl.hasShelvedFiles();
        clientname = cl.getClientname();
        username = cl.getUsername();
        return this;
    }

    @NotNull
    @Override
    public P4ChangelistId getChangelistId() {
        return changelistId;
    }

    public MockP4ChangelistSummary withChangelistId(P4ChangelistId id) {
        this.changelistId = id;
        return this;
    }

    @NotNull
    @Override
    public String getComment() {
        return comment;
    }

    public MockP4ChangelistSummary withComment(String s) {
        comment = s;
        return this;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public MockP4ChangelistSummary withDeleted(boolean b) {
        deleted = b;
        return this;
    }

    @Override
    public boolean isSubmitted() {
        return submitted;
    }

    public MockP4ChangelistSummary withSubmitted(boolean b) {
        submitted = b;
        return this;
    }

    @Override
    public boolean isOnServer() {
        return onServer;
    }

    public MockP4ChangelistSummary withOnServer(boolean b) {
        onServer = b;
        return this;
    }

    @Override
    public boolean hasShelvedFiles() {
        return hasShelved;
    }

    public MockP4ChangelistSummary withHasShelved(boolean b) {
        hasShelved = b;
        return this;
    }

    @NotNull
    @Override
    public String getClientname() {
        return clientname;
    }

    public MockP4ChangelistSummary withClientname(String s) {
        clientname = s;
        return this;
    }

    @NotNull
    @Override
    public String getUsername() {
        return username;
    }

    public MockP4ChangelistSummary withUsername(String s) {
        username = s;
        return this;
    }
}
