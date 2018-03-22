package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.PRINT;

import java.io.InputStream;
import java.util.List;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IPrintDelegator;

/**
 * Implementation to handle the Print command.
 */
public class PrintDelegator extends BaseDelegator implements IPrintDelegator {

    /**
     * Instantiate a new PrintDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public PrintDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public InputStream getFileContents(
            final List<IFileSpec> fileSpecs,
            final boolean allRevs,
            final boolean noHeaderLine)
            throws ConnectionException, RequestException, AccessException {

        try {
            return getFileContents(fileSpecs, new GetFileContentsOptions(allRevs, noHeaderLine));
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public InputStream getFileContents(
            final List<IFileSpec> fileSpecs,
            final GetFileContentsOptions opts) throws P4JavaException {

        boolean annotateFiles = isNull(opts) || !opts.isDontAnnotateFiles();
        return execStreamCmd(PRINT,
                processParameters(
                        opts,
                        fileSpecs,
                        null,
                        annotateFiles,
                        server));
    }
}
