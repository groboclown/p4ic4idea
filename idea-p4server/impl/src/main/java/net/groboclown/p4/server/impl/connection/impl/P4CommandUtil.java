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

package net.groboclown.p4.server.impl.connection.impl;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.IJobSpec;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.ChangelistOptions;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;

import java.util.List;
import java.util.Map;

/**
 * Keeps all the different server commands that need to be run in one place.  Allows for
 * easier unit test code reuse without needing to call into every nuance of the runners.
 * <p>
 * These run all the p4java API commands, without translating the results or inputs to/from
 * the plugin types.
 * <p>
 * As a side effect, this maintains a catalogue of commands used by the plugin.  This
 * allows for an easier effort to focus on API compatibility and correctness.
 */
public class P4CommandUtil {
    public static List<IExtendedFileSpec> getFilesOpenInDefaultChangelist(IServer server,
            String clientName, int maxFileResults)
            throws P4JavaException {
        GetExtendedFilesOptions options = new GetExtendedFilesOptions("-Olhp -Rco -e default");
        options.setMaxResults(maxFileResults);
        return server.getExtendedFiles(
                FileSpecBuilder.makeFileSpecList("//" + clientName + "/..."),
                options
        );
    }

    public static List<IExtendedFileSpec> getFileDetailsForOpenedSpecs(IServer server, List<IFileSpec> sources,
            int maxFileResults)
            throws P4JavaException {
        GetExtendedFilesOptions options = new GetExtendedFilesOptions("-Olhp");
        options.setMaxResults(maxFileResults);
        return server.getExtendedFiles(sources, options);
    }

    public static IChangelist getChangelistDetails(IServer server, int changelistId)
            throws P4JavaException {
        ChangelistOptions co = new ChangelistOptions();
        co.setIncludeFixStatus(true);
        return server.getChangelist(changelistId, co);
    }

    public static List<IFileSpec> getShelvedFiles(IServer server, int changelistId, int maxFileResults)
            throws P4JavaException {
        return server.getShelvedFiles(changelistId, maxFileResults);
    }

    public static List<IChangelistSummary> getPendingChangelists(IClient client, int maxChangelistResults)
            throws P4JavaException {
        GetChangelistsOptions clOptions = new GetChangelistsOptions(
                maxChangelistResults, client.getName(), client.getServer().getUserName(),
                true, IChangelist.Type.PENDING, false
        );
        return client.getServer().getChangelists(null, clOptions);
    }

    public static IJob createJob(IOptionsServer server, Map<String, Object> fields)
            throws ConnectionException, AccessException, RequestException {
        return server.createJob(fields);
    }

    public static IJobSpec getJobSpec(IOptionsServer server)
            throws ConnectionException, AccessException, RequestException {
        return server.getJobSpec();
    }
}
