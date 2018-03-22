package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.FILES;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetDepotFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IFilesDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Delegator for the 'p4 files' command.
 */
public class FilesDelegator extends BaseDelegator implements IFilesDelegator {

    /**
     * Instantiates a new files delegator.
     *
     * @param server
     *            the server
     */
    public FilesDelegator(final IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IFileSpec> getDepotFiles(@Nonnull final List<IFileSpec> fileSpecs,
            final boolean allRevs) throws ConnectionException, AccessException {

        List<IFileSpec> depotFileSpecs = new ArrayList<>();
        try {
            depotFileSpecs = getDepotFiles(fileSpecs,
                    new GetDepotFilesOptions().setAllRevs(allRevs));
        } catch (final ConnectionException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            // TODO Why does RequestException get handled differently depending
            // on the call?
            Log.warn("Unexpected exception in IServer.getDepotFiles: %s", exc);
        }
        return depotFileSpecs;
    }

    @Override
    public List<IFileSpec> getDepotFiles(@Nonnull final List<IFileSpec> fileSpecs,
            final GetDepotFilesOptions opts) throws P4JavaException {

        Validate.notNull(fileSpecs);
        List<IFileSpec> fileList = new ArrayList<>();
        List<Map<String, Object>> resultMaps = execMapCmdList(FILES,
                processParameters(opts, fileSpecs, server), null);

        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                fileList.add(ResultListBuilder.handleFileReturn(map, server));
            }
        }
        return fileList;
    }
}
