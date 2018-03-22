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

package com.perforce.test;

public class TestServer {
    private ExecutableSpecification serverExecutableSpecification;
    private String user;
    private String port;
    private String proxyPort;
    private int monitor;
    private String log;
    private boolean proxy;

    public ExecutableSpecification getServerExecutableSpecification() {
        return serverExecutableSpecification;
    }

    public void start() {

    }

    // Delete the server.
    public void delete() {

    }

    public String getUser() {
        return user;
    }

    public String getPort() {
        return port;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void setMonitor(int monitor) {
        this.monitor = monitor;
    }

    public void importRecord(String journalRecord) {

    }

    public String getLog() {
        return log;
    }

    public void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    public void stopServer() {

    }
}
