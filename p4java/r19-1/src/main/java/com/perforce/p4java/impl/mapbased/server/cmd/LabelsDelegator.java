package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNonNullObjectListFromCommandResultMaps;
import static com.perforce.p4java.server.CmdSpec.LABELS;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.ILabelSummary;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.LabelSummary;
import com.perforce.p4java.option.server.GetLabelsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.ILabelsDelegator;

/**
 * Implementation to handle the Labels command.
 */
public class LabelsDelegator extends BaseDelegator implements ILabelsDelegator {
    /**
     * Instantiate a new LabelsDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public LabelsDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<ILabelSummary> getLabels(
            final String user,
            final int maxLabels,
            final String nameFilter,
            final List<IFileSpec> fileList)
            throws ConnectionException, RequestException, AccessException {

        checkMinSupportedPerforceVersion(user, maxLabels, nameFilter, "label");

        try {
            GetLabelsOptions getLabelsOptions = new GetLabelsOptions()
                    .setMaxResults(maxLabels)
                    .setUserName(user)
                    .setNameFilter(nameFilter);

            return getLabels(fileList, getLabelsOptions);
        } catch (final ConnectionException | RequestException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public List<ILabelSummary> getLabels(
            final List<IFileSpec> fileList,
            final GetLabelsOptions opts) throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(
                LABELS,
                processParameters(opts, fileList, server),
                null);

        return buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                new Function<Map, ILabelSummary>() {
                    @Override
                    public ILabelSummary apply(Map map) {
                        return new LabelSummary(map);
                    }
                }
        );
    }
}
