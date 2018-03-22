package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FUNCTION;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNonNullObjectListFromCommandResultMaps;
import static com.perforce.p4java.server.CmdSpec.EXPORT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.ExportRecordsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.callback.IStreamingCallback;
import com.perforce.p4java.server.delegator.IExportDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation to handle the Export command.
 */
public class ExportDelegator extends BaseDelegator implements IExportDelegator {
    /**
     * Instantiate a new ExportDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public ExportDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<Map<String, Object>> getExportRecords(
            final boolean useJournal,
            final long maxRecs,
            final int sourceNum,
            final long offset,
            final boolean format,
            final String journalPrefix,
            final String filter) throws ConnectionException, RequestException, AccessException {

        try {
            ExportRecordsOptions exportRecordsOptions = new ExportRecordsOptions()
                    .setFormat(format)
                    .setFilter(filter)
                    .setJournalPrefix(journalPrefix)
                    .setMaxRecs(maxRecs)
                    .setOffset(offset)
                    .setSourceNum(sourceNum)
                    .setUseJournal(useJournal);

            return getExportRecords(exportRecordsOptions);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public List<Map<String, Object>> getExportRecords(final ExportRecordsOptions opts)
            throws P4JavaException {
        Map<String, Object> inMap = new HashMap<>();
        if (nonNull(opts)) {
            inMap = opts.processFieldRules();
        }

        List<Map<String, Object>> resultMaps = execMapCmdList(
                EXPORT,
                processParameters(opts, server),
                inMap);

        return buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                // p4ic4idea: explicit generics
                new Function<Map<String, Object>, Map<String, Object>>() {
                    @Override
                    // p4ic4idea: explicit generics
                    public Map<String, Object> apply(Map<String, Object> map) {
                        if (map.containsKey(FUNCTION)) {
                            map.remove(FUNCTION);
                        }
                        return map;
                    }
                });
    }

    @Override
    public void getStreamingExportRecords(
            final ExportRecordsOptions opts,
            @Nonnull final IStreamingCallback callback,
            final int key) throws P4JavaException {

        Validate.notNull(callback);

        Map<String, Object> inMap = new HashMap<>();
        if (nonNull(opts)) {
            inMap = opts.processFieldRules();
        }

        server.execStreamingMapCommand(
                EXPORT.toString(),
                processParameters(opts, server),
                inMap,
                callback,
                key);
    }
}
