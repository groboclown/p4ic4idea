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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.CoreFactory;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.IJobSpec;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.changelist.SubmitOptions;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.server.ChangelistOptions;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.option.server.MoveFileOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import net.groboclown.p4.server.api.values.JobStatus;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.api.values.P4Job;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private static final Logger LOG = Logger.getInstance(P4CommandUtil.class);

    private static final int BUFFER_SIZE = 4096;


    public List<IExtendedFileSpec> getFilesOpenInDefaultChangelist(IServer server,
            String clientName, int maxFileResults)
            throws P4JavaException {
        GetExtendedFilesOptions options = new GetExtendedFilesOptions("-Olhp -Rco -e default");
        options.setMaxResults(maxFileResults);
        return server.getExtendedFiles(
                FileSpecBuilder.makeFileSpecList("//" + clientName + "/..."),
                options
        );
    }

    public List<IExtendedFileSpec> getFileDetailsForOpenedSpecs(IServer server, List<IFileSpec> sources,
            int maxFileResults)
            throws P4JavaException {
        GetExtendedFilesOptions options = new GetExtendedFilesOptions("-Olhp");
        options.setMaxResults(maxFileResults);
        return server.getExtendedFiles(sources, options);
    }

    public IChangelist getChangelistDetails(IServer server, int changelistId)
            throws P4JavaException {
        ChangelistOptions co = new ChangelistOptions();
        co.setIncludeFixStatus(true);
        return server.getChangelist(changelistId, co);
    }

    public List<IFileSpec> getShelvedFiles(IServer server, int changelistId, int maxFileResults)
            throws P4JavaException {
        return server.getShelvedFiles(changelistId, maxFileResults);
    }

    public List<IChangelistSummary> getPendingChangelists(IClient client, int maxChangelistResults)
            throws P4JavaException {
        GetChangelistsOptions clOptions = new GetChangelistsOptions(
                maxChangelistResults, client.getName(), client.getServer().getUserName(),
                true, IChangelist.Type.PENDING, false
        );
        return client.getServer().getChangelists(null, clOptions);
    }

    public IJob createJob(IOptionsServer server, Map<String, Object> fields)
            throws ConnectionException, AccessException, RequestException {
        return server.createJob(fields);
    }

    public IJob getJob(IServer server, String jobId)
            throws ConnectionException, AccessException, RequestException {
        return server.getJob(jobId);
    }

    public IJobSpec getJobSpec(IOptionsServer server)
            throws ConnectionException, AccessException, RequestException {
        return server.getJobSpec();
    }

    /**
     *
     * @param server server
     * @param files escaped name of the files
     * @return annotated file information
     */
    public List<IFileAnnotation> getAnnotations(IServer server, List<IFileSpec> files)
            throws P4JavaException {
        GetFileAnnotationsOptions options = new GetFileAnnotationsOptions(
                false, // allResults
                false, // useChangeNumbers
                false, // followBranches
                false, // ignoreWhitespaceChanges
                false, // ignoreWhitespace
                true, // ignoreLineEndings
                false // followAllIntegrations
        );
        return server.getFileAnnotations(files, options);
    }

    /**
     *
     * @param client client
     * @param files non-escaped name of the files.
     * @param fileType file type, or null if not specified
     * @param changelistId changelist ID, or null if default changelist.
     * @param charset charset, or null if not specified
     * @return result of adding the files.
     */
    public List<IFileSpec> addFiles(IClient client, List<IFileSpec> files, P4FileType fileType,
            P4ChangelistId changelistId,
            String charset)
            throws P4JavaException {
        AddFilesOptions addOptions = new AddFilesOptions();
        if (fileType != null) {
            addOptions.setFileType(fileType.toString());
        }
        if (changelistId != null && !changelistId.isDefaultChangelist()) {
            addOptions.setChangelistId(changelistId.getChangelistId());
        }
        addOptions.setUseWildcards(true);
        if (charset != null) {
            addOptions.setCharset(charset);
        }
        return client.addFiles(files, addOptions);
    }


    /**
     *
     * @param client client
     * @param files escaped file specs
     * @param onlyUnchanged true if revert only unchanged files.
     * @return result
     * @throws P4JavaException underlying error
     */
    public List<IFileSpec> revertFiles(IClient client, List<IFileSpec> files, boolean onlyUnchanged)
            throws P4JavaException {
        RevertFilesOptions options = new RevertFilesOptions();
        options.setRevertOnlyUnchanged(onlyUnchanged);
        return client.revertFiles(files, options);
    }

    /**
     *
     * @param client client
     * @param files escaped file specs
     * @param fileType file type
     * @param changelistId changelist to edit the file in
     * @param charset charset
     * @return edit file results
     * @throws P4JavaException underlying error
     */
    public List<IFileSpec> editFiles(IClient client, List<IFileSpec> files, P4FileType fileType,
            P4ChangelistId changelistId, String charset)
            throws P4JavaException {
        EditFilesOptions editOptions = new EditFilesOptions();
        if (fileType != null) {
            editOptions.setFileType(fileType.toString());
        }
        if (changelistId != null && !changelistId.isDefaultChangelist()) {
            editOptions.setChangelistId(changelistId.getChangelistId());
        }
        if (charset != null) {
            editOptions.setCharset(charset);
        }
        return client.editFiles(files, editOptions);
    }

    public List<IFileSpec> deleteFiles(IClient client, List<IFileSpec> files, P4ChangelistId changelistId)
            throws P4JavaException {
        DeleteFilesOptions options = new DeleteFilesOptions();
        if (changelistId != null && changelistId.getState() == P4ChangelistId.State.NUMBERED) {
            options.setChangelistId(changelistId.getChangelistId());
        }

        // Let the IDE perform the delete.
        // FIXME double check if this is right.
        options.setBypassClientDelete(true);

        return client.deleteFiles(files, options);
    }

    public List<IFileSpec> submitChangelist(
            JobStatus jobStatus,
            Collection<P4Job> updatedJobs,
            IChangelist changelist)
            throws P4JavaException {
        SubmitOptions options = new SubmitOptions();
        if (jobStatus != null) {
            options.setJobStatus(jobStatus.getName());
        }
        if (updatedJobs != null) {
            List<String> jobIds = new ArrayList<>(updatedJobs.size());
            for (P4Job p4Job : updatedJobs) {
                jobIds.add(p4Job.getJobId());
            }
            options.setJobIds(jobIds);
        }
        return changelist.submit(options);
    }

    public IChangelist createChangelist(IClient client, String description)
            throws P4JavaException {
        return CoreFactory.createChangelist(client, description, true);
    }

    public IExtendedFileSpec getFileDetails(IServer server, String clientname, List<IFileSpec> sources)
            throws P4JavaException {
        if (clientname != null) {
            // For fetching the local File information, we need a client to perform the mapping.
            IClient client = server.getClient(clientname);
            server.setCurrentClient(client);
        }
        assert sources.size() == 1;
        GetExtendedFilesOptions options = new GetExtendedFilesOptions();
        List<IExtendedFileSpec> ret = server.getExtendedFiles(sources, options);
        MessageStatusUtil.throwIfMessageOrEmpty("get file details", ret);
        if (LOG.isDebugEnabled()) {
            LOG.debug("File details fetch for " + sources.get(0) + ": count " + ret.size() + ": " + ret);
        }
        assert ret.size() == 1;
        return ret.get(0);
    }

    public Map<IFileSpec, IExtendedFileSpec> getFilesDetails(IServer server, String clientname, List<IFileSpec> sources)
            throws P4JavaException {
        if (clientname != null) {
            // For fetching the local File information, we need a client to perform the mapping.
            IClient client = server.getClient(clientname);
            server.setCurrentClient(client);
        }
        GetExtendedFilesOptions options = new GetExtendedFilesOptions();
        List<IExtendedFileSpec> res = server.getExtendedFiles(sources, options);
        if (LOG.isDebugEnabled()) {
            LOG.debug("File details for " + sources + ": " + res);
        }
        MessageStatusUtil.throwIfMessageOrEmpty("get file details", res);
        Map<IFileSpec, IExtendedFileSpec> ret = new HashMap<>();
        Iterator<IFileSpec> sourceIter = sources.iterator();
        for (IExtendedFileSpec extendedFileSpec : res) {
            if (extendedFileSpec.getStatusMessage() != null) {
                if (!sourceIter.hasNext()) {
                    throw new P4JavaException("Incorrect Perforce server result: too many responses for fstat; "
                            + "invoked p4 fstat " +
                            String.join(" ",
                                    sources.stream().map(IFileSpec::toString).collect(Collectors.toList())));
                }
            }
            ret.put(sourceIter.next(), extendedFileSpec);
        }
        return ret;
    }

    public byte[] loadContents(IServer server, IFileSpec spec, String clientname)
            throws P4JavaException, IOException {
        if (clientname != null) {
            server.setCurrentClient(server.getClient(clientname));
        }
        int maxFileSize = VcsUtil.getMaxVcsLoadedFileSize();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GetFileContentsOptions fileContentsOptions = new GetFileContentsOptions(false, true);
        // setting "don't annotate files" to true means we ignore the revision
        fileContentsOptions.setDontAnnotateFiles(false);
        InputStream inp = server.getFileContents(Collections.singletonList(spec),
                fileContentsOptions);
        if (inp == null) {
            return null;
        }

        try {
            byte[] buff = new byte[BUFFER_SIZE];
            int len;
            while ((len = inp.read(buff, 0, BUFFER_SIZE)) > 0 && baos.size() < maxFileSize) {
                baos.write(buff, 0, len);
            }
        } finally {
            // Note: be absolutely sure to close the InputStream that is returned.
            inp.close();
        }
        return baos.toByteArray();
    }

    public List<Pair<IFileSpec, IFileRevisionData>> getExactHistory(IOptionsServer server, List<IFileSpec> specs)
            throws P4JavaException {
        GetRevisionHistoryOptions opts = new GetRevisionHistoryOptions()
                .setMaxRevs(1)
                .setContentHistory(false)
                .setIncludeInherited(false)
                .setLongOutput(true)
                .setTruncatedLongOutput(false);
        Map<IFileSpec, List<IFileRevisionData>> res = server.getRevisionHistory(specs, opts);
        List<Pair<IFileSpec, IFileRevisionData>> ret = new ArrayList<>(res.size());
        for (Map.Entry<IFileSpec, List<IFileRevisionData>> entry: res.entrySet()) {
            // it can return empty values for a server message
            List<IFileRevisionData> value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Mapped " + entry.getKey().getDepotPath() + " to " + entry.getValue());
                }
                if (entry.getValue().size() != 1) {
                    throw new IllegalStateException("Unexpected revision count for " + entry.getKey().getDepotPath());
                }
                ret.add(Pair.create(entry.getKey(), entry.getValue().get(0)));
            }
        }
        return ret;
    }

    @NotNull
    public List<IFileRevisionData> getHistory(IOptionsServer server, String clientname,
            List<IFileSpec> singleSpec, int maxRevCount)
            throws P4JavaException {
        // Even though we expect only 1 spec, the List is used by the server.getRevisionHistory, and the builders
        // all return lists.
        if (singleSpec.size() != 1) {
            throw new IllegalArgumentException("Expected exactly 1 file spec argument.");
        }
        // Because the spec can be a reference to the local filesystem, it must have a workspace
        // associated with it in order to map from the local filesystem to the depot path.
        server.setCurrentClient(server.getClient(clientname));

        GetRevisionHistoryOptions opts = new GetRevisionHistoryOptions()
                .setMaxRevs(maxRevCount)
                .setContentHistory(false)
                .setIncludeInherited(true)
                .setLongOutput(true)
                .setTruncatedLongOutput(false);
        Map<IFileSpec, List<IFileRevisionData>> res = server.getRevisionHistory(singleSpec, opts);
        List<IFileRevisionData> ret = new ArrayList<>();
        for (Map.Entry<IFileSpec, List<IFileRevisionData>> entry: res.entrySet()) {
            // it can return empty values for a server message
            List<IFileRevisionData> value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Mapped " + entry.getKey().getDepotPath() + " to " + entry.getValue());
                }
                // There can be multiple entries returned for the single file.  This is due to renaming and
                // following the include inherited path.
                ret.addAll(value);
            } else {
                // TODO should be an error.
                LOG.warn(singleSpec.get(0) + ": Encountered server remark for fetching file history: " + entry.getKey());
            }
        }
        return ret;
    }

    public List<IFileSpec> moveFile(IClient client, IFileSpec source, IFileSpec target, P4ChangelistId changelistId)
            throws P4JavaException {
        MoveFileOptions options = new MoveFileOptions()
                .setChangelistId(changelistId.getChangelistId());
        return client.getServer().moveFile(source, target, options);
    }
}
