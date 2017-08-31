package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.FSTAT;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.file.FileStatAncilliaryOptions;
import com.perforce.p4java.core.file.FileStatOutputOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IFstatDelegator;

/**
 * Implementation for 'p4 fstat'.
 */
public class FstatDelegator extends BaseDelegator implements IFstatDelegator {
    
    /**
     * Instantiates a new fstat delegator.
     *
     * @param server the server
     */
    public FstatDelegator(final IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IExtendedFileSpec> getExtendedFiles(final List<IFileSpec> fileSpecs,
            final int maxFiles, final int sinceChangelist, final int affectedByChangelist,
            final FileStatOutputOptions outputOptions,
            final FileStatAncilliaryOptions ancilliaryOptions)
            throws ConnectionException, AccessException {

        try {
            GetExtendedFilesOptions extendedFilesOptions = new GetExtendedFilesOptions()
                    .setAncilliaryOptions(ancilliaryOptions).setMaxResults(maxFiles)
                    .setOutputOptions(outputOptions).setSinceChangelist(sinceChangelist)
                    .setAffectedByChangelist(affectedByChangelist);
            return getExtendedFiles(fileSpecs, extendedFilesOptions);
        } catch (final ConnectionException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            // TODO Not sure why this should result in an empty list.
            Log.warn("Unexpected exception in IServer.getExtendedFiles: %s", exc);
            return Collections.emptyList();
        }
    }

    @Override
    public List<IExtendedFileSpec> getExtendedFiles(final List<IFileSpec> fileSpecs,
            final GetExtendedFilesOptions opts) throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(FSTAT,
                processParameters(opts, fileSpecs, server), null);

        return ResultListBuilder.buildNonNullExtendedFileSpecListFromCommandResultMaps(resultMaps,
                server);
    }
}
