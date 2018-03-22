package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseInt;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.core.file.FileSpecOpStatus.ERROR;
import static com.perforce.p4java.core.file.FileSpecOpStatus.INFO;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.CLIENT_REC_DELETED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.INTEGRATION_REC_ADDED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.INTEGRATION_REC_DELETED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.LABEL_REC_DELETED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.PURGE_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.PURGE_REV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REPORT_ONLY;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REVISION_REC_DELETED;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.WORKING_REC_DELETED;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.handleFileErrorStr;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.isInfoMessage;
import static com.perforce.p4java.server.CmdSpec.OBLITERATE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.Log;
import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.core.file.IObliterateResult;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.generic.core.file.ObliterateResult;
import com.perforce.p4java.option.server.ObliterateFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.delegator.IObliterateDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation to handle the Obliterate command.
 */
public class ObliterateDelegator extends BaseDelegator implements IObliterateDelegator {
    /**
     * Instantiate a new ObliterateDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public ObliterateDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IObliterateResult> obliterateFiles(
            @Nonnull final List<IFileSpec> fileSpecs,
            final ObliterateFilesOptions opts) throws P4JavaException {

        Validate.notNull(fileSpecs);

        List<IObliterateResult> obliterateResults = new ArrayList<>();

        List<Map<String, Object>> resultMaps = execMapCmdList(
                OBLITERATE,
                processParameters(opts, fileSpecs, server),
                null);

        // The "obliterate" command can take multiple filespecs.
        // Each filespec has its own result in the results map.
        // Each result has summary keys: "revisionRecDeleted", etc.
        // The summary keys indicate boundary between results.
        // Additionally, there might be a "reportOnly" key at the end.
        // Note: some results might not have "purgeFile" and "purgeRev" values.
        if (nonNull(resultMaps)) {
            // Check for the "reportOnly" key in the last map entry.
            // We only check if there are two or more elements.
            boolean reportOnly = false;
            if (resultMaps.size() > 1) {
                Map<String, Object> lastResultMap = resultMaps.get(resultMaps.size() - 1);
                reportOnly = nonNull(lastResultMap) && lastResultMap.containsKey(REPORT_ONLY);
            }

            try {
                List<IFileSpec> fsList = new ArrayList<>();
                for (Map<String, Object> map : resultMaps) {
                    // p4ic4idea: use IServerMessage
                    final IServerMessage errStr = handleFileErrorStr(map);
                    if (isNull(errStr)) {
                        if (map.containsKey(PURGE_FILE)) {
                            FileSpec fs = createPurgeFileSpecFromMap(map);
                            fsList.add(fs);
                        } else if (map.containsKey(REVISION_REC_DELETED)) {
                            IObliterateResult result = createObliterateResultFromMapIfRevisionRecordsDeleted(
                                    map,
                                    fsList,
                                    reportOnly);

                            obliterateResults.add(result);
                            // Create a new list for the next result
                            fsList = new ArrayList<>();
                        }
                    } else {
                        FileSpec fs = createInfoOrErrorFileSpecFromMap(
                                map,
                                errStr,
                                new Function<Map<String, Object>, Boolean>() {
                                    @Override
                                    public Boolean apply(Map<String, Object> map) {
                                        return errStr.isInfo();
                                    }
                                });

                        fsList.add(fs);
                        IObliterateResult result = new ObliterateResult(fsList, 0, 0, 0, 0, 0, 0,
                                reportOnly);
                        obliterateResults.add(result);
                    }
                }
            } catch (Exception exc) {
                Log.error("Unexpected exception in ObliterateFileSpec constructor %s",
                        exc.getLocalizedMessage());
                Log.exception(exc);
            }
        }

        return obliterateResults;
    }

    private FileSpec createPurgeFileSpecFromMap(final Map<String, Object> map) {
        FileSpec fs = new FileSpec();
        fs.setDepotPath(parseString(map, PURGE_FILE));
        fs.setEndRevision(parseInt(map, PURGE_REV));

        return fs;
    }

    private FileSpec createInfoOrErrorFileSpecFromMap(
            final Map<String, Object> map,
            // p4ic4idea: Use an IServerMessage instead
            final IServerMessage errStr,
            final Function<Map<String, Object>, Boolean> infoMessageDetector) {

        FileSpec fs = new FileSpec(ERROR, errStr);
        if (infoMessageDetector.apply(map)) {
            fs = new FileSpec(INFO, errStr);
        }

        return fs;
    }

    private IObliterateResult createObliterateResultFromMapIfRevisionRecordsDeleted(
            @Nonnull final Map<String, Object> map,
            @Nonnull final List<IFileSpec> fsList,
            final boolean reportOnly) {

        return new ObliterateResult(
                fsList,
                parseInt(map, INTEGRATION_REC_ADDED),
                parseInt(map, LABEL_REC_DELETED),
                parseInt(map, CLIENT_REC_DELETED),
                parseInt(map, INTEGRATION_REC_DELETED),
                parseInt(map, WORKING_REC_DELETED),
                parseInt(map, REVISION_REC_DELETED),
                reportOnly);
    }
}
