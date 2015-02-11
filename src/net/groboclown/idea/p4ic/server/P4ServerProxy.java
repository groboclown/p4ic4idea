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

import com.intellij.openapi.vcs.CalledInBackground;
import com.perforce.p4java.admin.IDbSchema;
import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.*;
import com.perforce.p4java.core.file.*;
import com.perforce.p4java.exception.*;
import com.perforce.p4java.option.server.DescribeOptions;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerStatus;
import com.perforce.p4java.server.callback.ICommandCallback;
import com.perforce.p4java.server.callback.IProgressCallback;
import com.perforce.p4java.server.callback.ISSOCallback;
import com.perforce.p4java.server.callback.IStreamingCallback;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class P4ServerProxy implements IServer {
    private final IServer proxy;

    public P4ServerProxy(IServer proxy) {
        this.proxy = proxy;
    }

    public IServer getRealServer() {
        return proxy;
    }

    @CalledInBackground
    @Override
    public Properties getProperties() {
        return proxy.getProperties();
    }

    @CalledInBackground
    @Override
    public ICommandCallback registerCallback(ICommandCallback iCommandCallback) {
        return proxy.registerCallback(iCommandCallback);
    }

    @CalledInBackground
    @Override
    public IProgressCallback registerProgressCallback(IProgressCallback iProgressCallback) {
        return proxy.registerProgressCallback(iProgressCallback);
    }

    @CalledInBackground
    @Override
    public ISSOCallback registerSSOCallback(ISSOCallback issoCallback, String s) {
        return proxy.registerSSOCallback(issoCallback, s);
    }

    @CalledInBackground
    @Override
    public ServerStatus getStatus() {
        return proxy.getStatus();
    }

    @CalledInBackground
    @Override
    public boolean setCharsetName(String s) throws UnsupportedCharsetException {
        return proxy.setCharsetName(s);
    }

    @CalledInBackground
    @Override
    public String getCharsetName() {
        return proxy.getCharsetName();
    }

    @CalledInBackground
    @Override
    public int getServerVersionNumber() {
        return proxy.getServerVersionNumber();
    }

    @CalledInBackground
    @Override
    public boolean isCaseSensitive() {
        return proxy.isCaseSensitive();
    }

    @CalledInBackground
    @Override
    public boolean supportsUnicode() throws ConnectionException, RequestException, AccessException {
        return proxy.supportsUnicode();
    }

    @CalledInBackground
    @Override
    public boolean supportsSmartMove() throws ConnectionException, RequestException, AccessException {
        return proxy.supportsSmartMove();
    }

    @CalledInBackground
    @Override
    public String[] getKnownCharsets() {
        return proxy.getKnownCharsets();
    }

    @CalledInBackground
    @Override
    public void setUserName(String s) {
        proxy.setUserName(s);
    }

    @CalledInBackground
    @Override
    public void setAuthTicket(String s) {
        proxy.setAuthTicket(s);
    }

    @CalledInBackground
    @Override
    public String getAuthTicket() {
        return proxy.getAuthTicket();
    }

    @CalledInBackground
    @Override
    public void setWorkingDirectory(String s) {
        proxy.setWorkingDirectory(s);
    }

    @CalledInBackground
    @Override
    public String getWorkingDirectory() {
        return proxy.getWorkingDirectory();
    }

    @CalledInBackground
    @Override
    public String getUserName() {
        return proxy.getUserName();
    }

    @CalledInBackground
    @Override
    public void connect() throws ConnectionException, AccessException, RequestException, ConfigException {
        logStart();
        proxy.connect();
        logExit();
    }

    @CalledInBackground
    @Override
    public boolean isConnected() {
        return proxy.isConnected();
    }

    @CalledInBackground
    @Override
    public void disconnect() throws ConnectionException, AccessException {
        logStart();
        proxy.disconnect();
        logExit();
    }

    @CalledInBackground
    @Override
    public void login(String s, boolean b) throws ConnectionException, RequestException, AccessException, ConfigException {
        proxy.login(s, b);
    }

    @CalledInBackground
    @Override
    public void login(String s) throws ConnectionException, RequestException, AccessException, ConfigException {
        logStart();
        proxy.login(s);
        logExit();
    }

    @CalledInBackground
    @Override
    public String getLoginStatus() throws P4JavaException {
        return proxy.getLoginStatus();
    }

    @CalledInBackground
    @Override
    public void logout() throws ConnectionException, RequestException, AccessException, ConfigException {
        proxy.logout();
    }

    @CalledInBackground
    @Override
    public IServerInfo getServerInfo() throws ConnectionException, RequestException, AccessException {
        return proxy.getServerInfo();
    }

    @CalledInBackground
    @Override
    public List<IDepot> getDepots() throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getDepots());
    }

    @CalledInBackground
    @Override
    public IUser getUser(String s) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getUser(s));
    }

    @CalledInBackground
    @Override
    public String createUser(IUser iUser, boolean b) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.createUser(iUser, b));
    }

    @CalledInBackground
    @Override
    public String updateUser(IUser iUser, boolean b) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.updateUser(iUser, b));
    }

    @CalledInBackground
    @Override
    public String deleteUser(String s, boolean b) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.deleteUser(s, b));
    }

    @CalledInBackground
    @Override
    public List<IUserSummary> getUsers(List<String> list, int i) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getUsers(list, i));
    }

    @CalledInBackground
    @Override
    public List<IUserGroup> getUserGroups(String s, boolean b, boolean b1, int i) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getUserGroups(s, b, b1, i));
    }

    @CalledInBackground
    @Override
    public IUserGroup getUserGroup(String s) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getUserGroup(s));
    }

    @CalledInBackground
    @Override
    public String createUserGroup(IUserGroup iUserGroup) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.createUserGroup(iUserGroup));
    }

    @CalledInBackground
    @Override
    public String updateUserGroup(IUserGroup iUserGroup, boolean b) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.updateUserGroup(iUserGroup, b));
    }

    @CalledInBackground
    @Override
    public String deleteUserGroup(IUserGroup iUserGroup) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.deleteUserGroup(iUserGroup));
    }

    @CalledInBackground
    @Override
    public List<IProtectionEntry> getProtectionEntries(boolean b, String s, String s1, String s2, List<IFileSpec> list) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.getProtectionEntries(b, s, s1, s2, list));
    }

    @CalledInBackground
    @Override
    public List<IClientSummary> getClients(String s, String s1, int i) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getClients(s, s1, i));
    }

    @CalledInBackground
    @Override
    public List<ILabelSummary> getLabels(String s, int i, String s1, List<IFileSpec> list) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getLabels(s, i, s1, list));
    }

    @CalledInBackground
    @Override
    public ILabel getLabel(String s) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getLabel(s));
    }

    @CalledInBackground
    @Override
    public String createLabel(ILabel iLabel) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.createLabel(iLabel));
    }

    @CalledInBackground
    @Override
    public String updateLabel(ILabel iLabel) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.updateLabel(iLabel));
    }

    @CalledInBackground
    @Override
    public String deleteLabel(String s, boolean b) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.deleteLabel(s, b));
    }

    @CalledInBackground
    @Override
    public List<IFileSpec> tagFiles(List<IFileSpec> list, String s, boolean b, boolean b1) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.tagFiles(list, s, b, b1));
    }

    @CalledInBackground
    @Override
    public List<IBranchSpecSummary> getBranchSpecs(String s, String s1, int i) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getBranchSpecs(s, s1, i));
    }

    @CalledInBackground
    @Override
    public IBranchSpec getBranchSpec(String s) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getBranchSpec(s));
    }

    @CalledInBackground
    @Override
    public String createBranchSpec(IBranchSpec iBranchSpec) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.createBranchSpec(iBranchSpec));
    }

    @CalledInBackground
    @Override
    public String updateBranchSpec(IBranchSpec iBranchSpec) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.updateBranchSpec(iBranchSpec));
    }

    @CalledInBackground
    @Override
    public String deleteBranchSpec(String s, boolean b) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.deleteBranchSpec(s, b));
    }

    @CalledInBackground
    @Override
    public IClient getCurrentClient() {
        logStart();
        return logExit(createClientProxy(proxy.getCurrentClient()));
    }

    @CalledInBackground
    @Override
    public void setCurrentClient(IClient iClient) throws ConnectionException, RequestException, AccessException {
        logStart();
        proxy.setCurrentClient(iClient);
        logExit();
    }

    @CalledInBackground
    @Override
    public IClient getClient(String s) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(createClientProxy(proxy.getClient(s)));
    }

    @CalledInBackground
    @Override
    public IClient getClient(IClientSummary iClientSummary) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(createClientProxy(proxy.getClient(iClientSummary)));
    }

    @CalledInBackground
    @Override
    public IClient getClientTemplate(String s) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.getClientTemplate(s));
    }

    @CalledInBackground
    @Override
    public IClient getClientTemplate(String s, boolean b) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.getClientTemplate(s, b));
    }

    @CalledInBackground
    @Override
    public String createClient(IClient iClient) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.createClient(iClient));
    }

    @CalledInBackground
    @Override
    public String updateClient(IClient iClient) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.updateClient(iClient));
    }

    @CalledInBackground
    @Override
    public String deleteClient(String s, boolean b) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.deleteClient(s, b));
    }

    @CalledInBackground
    @Override
    public List<IFileSpec> getDepotFiles(List<IFileSpec> list, boolean b) throws ConnectionException, AccessException {
        logStart();
        return logExit(proxy.getDepotFiles(list, b));
    }

    @CalledInBackground
    @Override
    public List<IFileAnnotation> getFileAnnotations(List<IFileSpec> list, DiffType diffType, boolean b, boolean b1, boolean b2) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getFileAnnotations(list, diffType, b, b1, b2));
    }

    @CalledInBackground
    @Override
    public List<IFileSpec> moveFile(int i, boolean b, boolean b1, String s, IFileSpec iFileSpec, IFileSpec iFileSpec1) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.moveFile(i, b, b1, s, iFileSpec, iFileSpec1));
    }

    @CalledInBackground
    @Override
    public List<IFileSpec> getDirectories(List<IFileSpec> list, boolean b, boolean b1, boolean b2) throws ConnectionException, AccessException {
        logStart();
        return logExit(proxy.getDirectories(list, b, b1, b2));
    }

    @CalledInBackground
    @Override
    public List<IChangelistSummary> getChangelists(int i, List<IFileSpec> list, String s, String s1, boolean b, IChangelist.Type type, boolean b1) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getChangelists(i, list, s, s1, b, type, b1));
    }

    @CalledInBackground
    @Override
    public List<IChangelistSummary> getChangelists(int i, List<IFileSpec> list, String s, String s1, boolean b, boolean b1, boolean b2, boolean b3) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getChangelists(i, list, s, s1, b, b1, b2, b3));
    }

    @CalledInBackground
    @Override
    public IChangelist getChangelist(int i) throws ConnectionException, RequestException, AccessException {
        //logStart();
        // This doesn't do the right thing.
        // Use GetChangelistServerTask.getChangelist instead.
        invalid();
        return logExit(proxy.getChangelist(i));
    }

    @CalledInBackground
    @Override
    public String deletePendingChangelist(int i) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.deletePendingChangelist(i));
    }

    @CalledInBackground
    @Override
    public List<IFileSpec> getChangelistFiles(int i) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getChangelistFiles(i));
    }

    @CalledInBackground
    @Override
    public InputStream getChangelistDiffs(int i, DiffType diffType) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getChangelistDiffs(i, diffType));
    }

    @CalledInBackground
    @Override
    public InputStream getChangelistDiffsStream(int i, DescribeOptions describeOptions) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getChangelistDiffsStream(i, describeOptions));
    }

    @CalledInBackground
    @Override
    public InputStream getFileContents(List<IFileSpec> list, boolean b, boolean b1) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getFileContents(list, b, b1));
    }

    @CalledInBackground
    @Override
    public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(List<IFileSpec> list, int i, boolean b, boolean b1, boolean b2, boolean b3) throws ConnectionException, AccessException {
        logStart();
        return logExit(proxy.getRevisionHistory(list, i, b, b1, b2, b3));
    }

    @CalledInBackground
    @Override
    public List<IUserSummary> getReviews(int i, List<IFileSpec> list) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.getReviews(i, list));
    }

    @CalledInBackground
    @Override
    public List<IFileSpec> getOpenedFiles(List<IFileSpec> list, boolean b, String s, int i, int i1) throws ConnectionException, AccessException {
        logStart();
        return logExit(proxy.getOpenedFiles(list, b, s, i, i1));
    }

    @CalledInBackground
    @Override
    public List<IExtendedFileSpec> getExtendedFiles(List<IFileSpec> list, int i, int i1, int i2, FileStatOutputOptions fileStatOutputOptions, FileStatAncilliaryOptions fileStatAncilliaryOptions) throws ConnectionException, AccessException {
        logStart();
        return logExit(proxy.getExtendedFiles(list, i, i1, i2, fileStatOutputOptions, fileStatAncilliaryOptions));
    }

    @CalledInBackground
    @Override
    public List<IFileSpec> getSubmittedIntegrations(List<IFileSpec> list, String s, boolean b) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getSubmittedIntegrations(list, s, b));
    }

    @CalledInBackground
    @Override
    public List<IChangelist> getInterchanges(IFileSpec iFileSpec, IFileSpec iFileSpec1, boolean b, boolean b1, int i) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getInterchanges(iFileSpec, iFileSpec1, b, b1, i));
    }

    @CalledInBackground
    @Override
    public List<IChangelist> getInterchanges(String s, List<IFileSpec> list, List<IFileSpec> list1, boolean b, boolean b1, int i, boolean b2, boolean b3) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getInterchanges(s, list, list1, b, b1, i, b2, b3));
    }

    @CalledInBackground
    @Override
    public List<IJob> getJobs(List<IFileSpec> list, int i, boolean b, boolean b1, boolean b2, String s) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getJobs(list, i, b, b1, b2, s));
    }

    @CalledInBackground
    @Override
    public IJob getJob(String s) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getJob(s));
    }

    @CalledInBackground
    @Override
    public IJob createJob(Map<String, Object> map) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.createJob(map));
    }

    @CalledInBackground
    @Override
    public String updateJob(IJob iJob) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.updateJob(iJob));
    }

    @CalledInBackground
    @Override
    public String deleteJob(String s) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.deleteJob(s));
    }

    @CalledInBackground
    @Override
    public IJobSpec getJobSpec() throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getJobSpec());
    }

    @CalledInBackground
    @Override
    public List<IFix> getFixList(List<IFileSpec> list, int i, String s, boolean b, int i1) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getFixList(list, i, s, b, i1));
    }

    @CalledInBackground
    @Override
    public List<IFix> fixJobs(List<String> list, int i, String s, boolean b) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.fixJobs(list, i, s, b));
    }

    @CalledInBackground
    @Override
    public List<IServerProcess> getServerProcesses() throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getServerProcesses());
    }

    @CalledInBackground
    @Override
    public InputStream getServerFileDiffs(IFileSpec iFileSpec, IFileSpec iFileSpec1, String s, DiffType diffType, boolean b, boolean b1, boolean b2) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getServerFileDiffs(iFileSpec, iFileSpec1, s, diffType, b, b1, b2));
    }

    @CalledInBackground
    @Override
    public List<IFileDiff> getFileDiffs(IFileSpec iFileSpec, IFileSpec iFileSpec1, String s, DiffType diffType, boolean b, boolean b1, boolean b2) throws ConnectionException, RequestException, AccessException {
        logStart();
        return logExit(proxy.getFileDiffs(iFileSpec, iFileSpec1, s, diffType, b, b1, b2));
    }

    @CalledInBackground
    @Override
    public Map<String, Object>[] execMapCmd(String s, String[] strings, Map<String, Object> map) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.execMapCmd(s, strings, map));
    }

    @CalledInBackground
    @Override
    public Map<String, Object>[] execInputStringMapCmd(String s, String[] strings, String s1) throws P4JavaException {
        invalid();
        return logExit(proxy.execInputStringMapCmd(s, strings, s1));
    }

    @CalledInBackground
    @Override
    public Map<String, Object>[] execQuietMapCmd(String s, String[] strings, Map<String, Object> map) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.execQuietMapCmd(s, strings, map));
    }

    @CalledInBackground
    @Override
    public InputStream execStreamCmd(String s, String[] strings) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.execStreamCmd(s, strings));
    }

    @CalledInBackground
    @Override
    public InputStream execQuietStreamCmd(String s, String[] strings) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.execQuietStreamCmd(s, strings));
    }

    @CalledInBackground
    @Override
    public void execStreamingMapCommand(String s, String[] strings, Map<String, Object> map, IStreamingCallback iStreamingCallback, int i) throws P4JavaException {
        invalid();
        proxy.execStreamingMapCommand(s, strings, map, iStreamingCallback, i);
        logExit();
    }

    @CalledInBackground
    @Override
    public void execInputStringStreamingMapComd(String s, String[] strings, String s1, IStreamingCallback iStreamingCallback, int i) throws P4JavaException {
        invalid();
        proxy.execInputStringStreamingMapComd(s, strings, s1, iStreamingCallback, i);
        logExit();
    }

    @CalledInBackground
    @Override
    public String getCounter(String s) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.getCounter(s));
    }

    @CalledInBackground
    @Override
    public void setCounter(String s, String s1, boolean b) throws ConnectionException, RequestException, AccessException {
        invalid();
        proxy.setCounter(s, s1, b);
        logExit();
    }

    @CalledInBackground
    @Override
    public void deleteCounter(String s, boolean b) throws ConnectionException, RequestException, AccessException {
        invalid();
        proxy.deleteCounter(s, b);
        logExit();
    }

    @CalledInBackground
    @Override
    public Map<String, String> getCounters() throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.getCounters());
    }

    @CalledInBackground
    @Override
    public List<IDbSchema> getDbSchema(List<String> list) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.getDbSchema(list));
    }

    @CalledInBackground
    @Override
    public List<Map<String, Object>> getExportRecords(boolean b, long l, int i, long l1, boolean b1, String s, String s1) throws ConnectionException, RequestException, AccessException {
        invalid();
        return logExit(proxy.getExportRecords(b, l, i, l1, b1, s, s1));
    }

    private void logStart() {
        Exception e = new Exception();
        e.fillInStackTrace();
        StackTraceElement caller = e.getStackTrace()[1];
        System.out.println("IServer." + caller.getMethodName() + ": enter");
    }

    private void logExit() {
        Exception e = new Exception();
        e.fillInStackTrace();
        StackTraceElement caller = e.getStackTrace()[1];
        System.out.println("IServer." + caller.getMethodName() + ": exit");
    }

    private <T> T logExit(T value) {
        Exception e = new Exception();
        e.fillInStackTrace();
        StackTraceElement caller = e.getStackTrace()[1];
        System.out.println("IServer." + caller.getMethodName() + ": exit");
        return value;
    }

    private void invalid() {
        throw new IllegalStateException("call not allowed");
    }

    private IClient createClientProxy(final IClient real) {
        return (IClient) Proxy.newProxyInstance(IClient.class.getClassLoader(),
                new Class[]{IClient.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        System.out.println("IClient." + method.getName() + ": enter");
                        Object ret = null;
                        try {
                            ret = method.invoke(real, args);
                        } finally {
                            System.out.println("IClient." + method.getName() + ": exit");
                        }
                        return ret;
                    }
                });
    }
}
