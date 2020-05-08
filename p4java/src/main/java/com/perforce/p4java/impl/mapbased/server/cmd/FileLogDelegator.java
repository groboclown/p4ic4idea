package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.core.file.FileSpecOpStatus.ERROR;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REV;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.handleFileErrorStr;
import static com.perforce.p4java.server.CmdSpec.FILELOG;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileRevisionData;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IFileLogDelegator;

// p4ic4idea: use IServerMessage
import com.perforce.p4java.server.IServerMessage;

/**
 * @author Sean Shou
 * @since 23/09/2016
 */
public class FileLogDelegator extends BaseDelegator implements IFileLogDelegator {
    /**
     * Instantiate a new FileDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public FileLogDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(
            final List<IFileSpec> fileSpecs,
            final int maxRevs,
            final boolean contentHistory,
            final boolean includeInherited,
            final boolean longOutput,
            final boolean truncatedLongOutput) throws ConnectionException, AccessException {

        try {
            GetRevisionHistoryOptions revisionHistoryOptions = new GetRevisionHistoryOptions()
                    .setContentHistory(contentHistory)
                    .setIncludeInherited(includeInherited)
                    .setLongOutput(longOutput)
                    .setTruncatedLongOutput(truncatedLongOutput)
                    .setMaxRevs(maxRevs);
            return getRevisionHistory(fileSpecs, revisionHistoryOptions);
        } catch (final ConnectionException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            Log.warn("Unexpected exception in IServer.getRevisionHistory: %s",
                    exc.getLocalizedMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<IFileSpec, List<IFileRevisionData>> getRevisionHistory(
            final List<IFileSpec> fileSpecs,
            final GetRevisionHistoryOptions opts) throws P4JavaException {

        Map<IFileSpec, List<IFileRevisionData>> fileRevisionDataMap = new HashMap<>();
        List<Map<String, Object>> resultMaps = execMapCmdList(
                FILELOG,
                processParameters(opts, fileSpecs, server),
                null);

        if (nonNull(resultMaps)) {
            for (Map<String, Object> resultMap : resultMaps) {
                String depotPath = parseString(resultMap, DEPOT_FILE);
                // p4ic4idea: use IServerMessage
                IServerMessage errStr = handleFileErrorStr(resultMap);
                if (nonNull(errStr)) {
                    FileSpec fileSpec = new FileSpec(
                            ERROR,
                            errStr,
                            resultMap);
                    fileSpec.setDepotPath(depotPath);
                    fileRevisionDataMap.put(fileSpec, null);
                } else {
                    List<IFileRevisionData> fileRevisionDataList = new ArrayList<>();
                    fileRevisionDataList.addAll(parseFileRevisionDataList(resultMap));

                    FileSpec fileSpec = new FileSpec();
                    fileSpec.setDepotPath(depotPath);
                    fileRevisionDataMap.put(fileSpec, fileRevisionDataList);
                }
            }
        }
        return fileRevisionDataMap;
    }

    private static List<FileRevisionData> parseFileRevisionDataList(
            @Nonnull final Map<String, Object> resultMap) {

        List<FileRevisionData> fileRevisionDataList = new ArrayList<>();
        int revisionNumber = 0;
        while (nonNull(resultMap.get(REV + revisionNumber))) {
            fileRevisionDataList.add(new FileRevisionData(resultMap, revisionNumber));
            revisionNumber++;
        }

        return fileRevisionDataList;
    }
}
