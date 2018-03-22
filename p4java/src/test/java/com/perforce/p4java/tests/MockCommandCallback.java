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

package com.perforce.p4java.tests;

import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.callback.ICommandCallback;

public class MockCommandCallback implements ICommandCallback {
    private int key;
    private String commandString;
    private long millisecsTaken;
    private IServerMessage message;

    @Override
    public void issuingServerCommand(int key, String commandString) {
        this.key = key;
        this.commandString = commandString;
    }

    @Override
    public void completedServerCommand(int key, long millisecsTaken) {
        this.key = key;
        this.millisecsTaken = millisecsTaken;
    }

    @Override
    public void receivedServerInfoLine(int key, IServerMessage infoLine) {
        this.key = key;
        this.message = infoLine;
    }

    @Override
    public void receivedServerErrorLine(int key, IServerMessage errorLine) {
        this.key = key;
        this.message = errorLine;
    }

    @Override
    public void receivedServerMessage(int key, IServerMessage message) {
        this.key = key;
        this.message = message;
    }

    public IServerMessage getMessage() {
        return message;
    }

    public long getMillisecsTaken() {
        return millisecsTaken;
    }
}
