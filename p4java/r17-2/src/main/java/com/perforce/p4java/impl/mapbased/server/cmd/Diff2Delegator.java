package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.FileDiffUtils.setFileDiffsOptionsByDiffType;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNonNullObjectListFromNonMessageCommandResultMaps;
import static com.perforce.p4java.server.CmdSpec.DIFF2;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.IFileDiff;
import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.FileDiff;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetFileDiffsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IDiff2Delegator;

/**
 * Implementation to handle the Diff2 command.
 */
public class Diff2Delegator extends BaseDelegator implements IDiff2Delegator {
    /**
     * Instantiate a new Diff2Delegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public Diff2Delegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IFileDiff> getFileDiffs(
            final IFileSpec file1,
            final IFileSpec file2,
            final String branchSpecName,
            final GetFileDiffsOptions opts) throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(
                DIFF2,
                processParameters(
                        opts,
                        file1,
                        file2,
                        branchSpecName,
                        server),
                null);
        return buildNonNullObjectListFromNonMessageCommandResultMaps(
                resultMaps,
                new Function<Map, IFileDiff>() {
                    @Override
                    public IFileDiff apply(Map map) {
                        return new FileDiff(map);
                    }
                });
    }

    @Override
    public List<IFileDiff> getFileDiffs(
            final IFileSpec file1,
            final IFileSpec file2,
            final String branchSpecName,
            final DiffType diffType,
            final boolean quiet,
            final boolean includeNonTextDiffs,
            final boolean gnuDiffs) throws ConnectionException, RequestException, AccessException {

        try {
            GetFileDiffsOptions opts = new GetFileDiffsOptions()
                    .setQuiet(quiet)
                    .setIncludeNonTextDiffs(includeNonTextDiffs)
                    .setGnuDiffs(gnuDiffs);
            setFileDiffsOptionsByDiffType(diffType, opts);
            return getFileDiffs(file1, file2, branchSpecName, opts);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public InputStream getFileDiffsStream(
            final IFileSpec file1,
            final IFileSpec file2,
            final String branchSpecName,
            final GetFileDiffsOptions opts) throws P4JavaException {

        return execStreamCmd(
                DIFF2,
                processParameters(
                        opts,
                        file1,
                        file2,
                        branchSpecName,
                        server)
        );
    }

    @Override
    public InputStream getServerFileDiffs(
            final IFileSpec file1,
            final IFileSpec file2,
            final String branchSpecName,
            final DiffType diffType,
            final boolean quiet,
            final boolean includeNonTextDiffs,
            final boolean gnuDiffs) throws ConnectionException, RequestException, AccessException {

        try {
            GetFileDiffsOptions opts = new GetFileDiffsOptions()
                    .setQuiet(quiet)
                    .setIncludeNonTextDiffs(includeNonTextDiffs)
                    .setGnuDiffs(gnuDiffs);

            setFileDiffsOptionsByDiffType(diffType, opts);
            return getFileDiffsStream(file1, file2, branchSpecName, opts);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }
}
