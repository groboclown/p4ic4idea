package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwRequestExceptionIfConditionFails;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseInt;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.getErrorStr;
import static com.perforce.p4java.server.CmdSpec.ANNOTATE;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.Log;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary;
import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FileAnnotation;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IFileAnnotateDelegator;

// p4ic4idea: use IServerMessage
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.common.base.P4JavaExceptions;

/**
 * Implementation to handle the Annotate command.
 */
public class FileAnnotateDelegator extends BaseDelegator implements IFileAnnotateDelegator {
    /**
     * Instantiate a new FileAnnotateDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public FileAnnotateDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IFileAnnotation> getFileAnnotations(
            final List<IFileSpec> fileSpecs,
            @Nonnull final DiffType diffType,
            final boolean allResults,
            final boolean useChangeNumbers,
            final boolean followBranches)
            throws ConnectionException, RequestException, AccessException {

	    // p4ic4idea: TODO explicit RequestException thrown, but should be a more specific exception.
        if (! isNull(diffType) && ! diffType.isWsOption()) {
                throw new RequestException("Bad whitespace option in getFileAnnotations");
        }

        // p4ic4idea: better exception handling
        return P4JavaExceptions.asRequestException(() -> {
            GetFileAnnotationsOptions getFileAnnotationsOptions = new GetFileAnnotationsOptions()
                    .setAllResults(allResults)
                    .setUseChangeNumbers(useChangeNumbers)
                    .setFollowBranches(followBranches)
                    .setWsOpts(diffType);
            return getFileAnnotations(fileSpecs, getFileAnnotationsOptions);
        });
    }

    @Override
    public List<IFileAnnotation> getFileAnnotations(
            final List<IFileSpec> fileSpecs,
            final GetFileAnnotationsOptions opts) throws P4JavaException {

        List<IFileAnnotation> returnList = new ArrayList<>();

        List<Map<String, Object>> resultMaps = execMapCmdList(
                ANNOTATE,
                processParameters(opts, fileSpecs, server),
                null);

        if (nonNull(resultMaps)) {
            String depotFile = null;
            for (Map<String, Object> map : resultMaps) {
                if (nonNull(map)) {
                    // RPC version returns info, cmd version returns error... we
                    // throw
                    // an exception in either case.
                    // p4ic4idea: use IServerMessage
                    ResultMapParser.handleErrors(ResultMapParser.toServerMessage(map));

                    // Note that this processing depends a bit on the current
                    // ordering of tagged results back from the server; if this
                    // changes, we may need to change things here as well...
                    if (isNewDepotFile(map)) {
                        // marks the start of annotations for
                        depotFile = parseString(map, DEPOT_FILE);
                    } else {
                        returnList.addAll(pickupDataAnnotationAndBuildFileAnnotation(
                                depotFile,
                                server.getCurrentClient(),
                                map)
                        );
                    }
                }
            }
        }

        return returnList;
    }

    /**
     * Look for any associated contributing integrations
     */
    private static void bindAssociatedContributingIntegrationsToFileAnnotation(
            @Nonnull final Map<String, Object> map,
            @Nonnull final FileAnnotation dataAnnotation) {

        for (int order = 0; map.containsKey(DEPOT_FILE + order); order++) {
            try {
                dataAnnotation.addIntegrationAnnotation(
                        new FileAnnotation(
                                order,
                                parseString(map, DEPOT_FILE + order),
                                parseInt(map, "upper" + order),
                                parseInt(map, "lower" + order))
                );
            // p4ic4idea: only catch Throwable if you're really, really careful.
            //} catch (Throwable thr) {
            } catch (Exception thr) {
                Log.error("bad conversion in getFileAnnotations");
                Log.exception(thr);
            }
        }
    }

    private static boolean isNewDepotFile(Map<String, Object> map) {
        return map.containsKey(DEPOT_FILE);
    }

    /**
     * Pick up the "data" annotation
     */
    private static List<IFileAnnotation> pickupDataAnnotationAndBuildFileAnnotation(
            final String depotFile, final IClient currentClient,
            final Map<String, Object> map) {

        List<IFileAnnotation> returnList = new ArrayList<>();
        IClientSummary.ClientLineEnd lineEnd = null;
        if (nonNull(currentClient)) {
            lineEnd = currentClient.getLineEnd();
        }
        FileAnnotation dataAnnotation = new FileAnnotation(map, depotFile, lineEnd);

        returnList.add(dataAnnotation);
        bindAssociatedContributingIntegrationsToFileAnnotation(map, dataAnnotation);

        return returnList;
    }
}
