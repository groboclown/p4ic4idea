package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.INTERCHANGES;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.ChangelistSummary;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.server.GetInterchangesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IInterchangesDelegator;

// p4ic4idea: use IServerMessage
import com.perforce.p4java.server.IServerMessage;

/**
 * @author Sean Shou
 * @since 20/09/2016
 */
public class InterchangesDelegator extends BaseDelegator implements IInterchangesDelegator {
    /**
     * 
     * Build a new InterchangesDelegator object and keep the server object for
     * using in the command processing. Note that this also delegates legacy Iserver
     * commands until they can be effectively withdrawn
     * 
     * @param server
     *            - the currently effective server implementation
     * 
     */
    public InterchangesDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IChangelist> getInterchanges(final IFileSpec fromFile, final IFileSpec toFile,
            final GetInterchangesOptions opts) throws P4JavaException {
        List<IFileSpec> files = new ArrayList<>();
        files.add(fromFile);
        files.add(toFile);

        List<Map<String, Object>> resultMaps = execMapCmdList(INTERCHANGES,
                processParameters(opts, files, server), null);
        return processInterchangeMaps(resultMaps,
                InterchangesDelegatorHidden.isListIndividualFilesThatRequireIntegration(opts));
    }

    @Override
    public List<IChangelist> getInterchanges(final String branchSpecName,
            final List<IFileSpec> fromFileList, final List<IFileSpec> toFileList,
            final GetInterchangesOptions opts) throws P4JavaException {
        List<Map<String, Object>> resultMaps = execMapCmdList(INTERCHANGES,
                processParameters(opts, fromFileList, toFileList, branchSpecName, server), null);
        return processInterchangeMaps(resultMaps,
                InterchangesDelegatorHidden.isListIndividualFilesThatRequireIntegration(opts));
    }

    /* 
     * On behalf of IServer
     */
    public List<IChangelist> getInterchanges(final IFileSpec fromFile, final IFileSpec toFile,
            final boolean showFiles, final boolean longDesc, final int maxChangelistId)
            throws ConnectionException, RequestException, AccessException {
        try {
            GetInterchangesOptions getInterchangesOptions = new GetInterchangesOptions()
                    .setShowFiles(showFiles).setLongDesc(longDesc)
                    .setMaxChangelistId(maxChangelistId);

            return getInterchanges(fromFile, toFile, getInterchangesOptions);
        } catch (final ConnectionException | RequestException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    /* 
     * On behalf of IServer
     */
    public List<IChangelist> getInterchanges(final String branchSpecName,
            final List<IFileSpec> fromFileList, final List<IFileSpec> toFileList,
            final boolean showFiles, final boolean longDesc, final int maxChangelistId,
            final boolean reverseMapping, final boolean biDirectional)
            throws ConnectionException, RequestException, AccessException {
        try {
            GetInterchangesOptions getInterchangesOptions = new GetInterchangesOptions()
                    .setShowFiles(showFiles).setLongDesc(longDesc)
                    .setMaxChangelistId(maxChangelistId).setReverseMapping(reverseMapping)
                    .setBiDirectional(biDirectional);
            return getInterchanges(branchSpecName, fromFileList, toFileList,
                    getInterchangesOptions);
        } catch (final ConnectionException | RequestException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    public List<IChangelist> processInterchangeMaps(final List<Map<String, Object>> resultMaps,
            final boolean showFiles) throws ConnectionException, AccessException, RequestException {

        List<IChangelist> interchangeList = new ArrayList<>();
        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                if (nonNull(map)) {
                    // map is either an error (in which case we do what we'd
                    // normally do with an error), or it's an "error" -- i.e.
                    // it's
                    // telling us there's no interchanges -- in which case we
                    // return
                    // an empty list,
                    // or it's a change summary (in which case we return a full
                    // changelist
                    // constructed from the summary, even though we don't get
                    // the full
                    // changelist info back),
                    // or (if the showFiles option was set) it's a change
                    // summary with a
                    // nested set of depot file specs inside the same map, in
                    // which case we pick the files off as best we can and then
                    // associate
                    // them with the changelist constructed as above.
                    // p4ic4idea: use IServerMessage instead.
                    IServerMessage err = server.handleFileErrorStr(map);
                    if (nonNull(err)) {
                        // What we're doing here is weeding out the "all
                        // revision(s)
                        // already integrated" non-error error...
                        // Note that the code here may be fragile in the face of
                        // server-side changes to error messages and code
                        // changes.
                        if (err.getGeneric() != 17 && err.getSeverity() != 2
                                && !err.hasMessageFragment("all revision(s) already integrated")) {
                            throw new RequestException(err);
                        }
                    } else {
                        ChangelistSummary changelistSummary = new ChangelistSummary(map, true,
                                server);
                        Changelist changelist = new Changelist(changelistSummary, server, false);
                        interchangeList.add(changelist);
                        List<IFileSpec> fileSpecs = new ArrayList<IFileSpec>();
                        changelist.setFileSpecs(fileSpecs);
                        if (showFiles) {
                            int i = 0;
                            while (nonNull(map.get(DEPOT_FILE + i))) {
                                FileSpec fileSpec = new FileSpec(map, server, i);
                                fileSpec.setChangelistId(changelist.getId());
                                fileSpecs.add(fileSpec);
                                i++;
                            }
                        }
                    }
                }
            }
        }
        return interchangeList;
    }

    public static class InterchangesDelegatorHidden {
        // p4ic4idea: make package private for unit tests
        static boolean isListIndividualFilesThatRequireIntegration(
                GetInterchangesOptions opts) {
            return nonNull(opts) && opts.isShowFiles();
        }
    }
}
