package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNonNullObjectListFromCommandResultMaps;
import static com.perforce.p4java.server.CmdSpec.PROTECTS;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.admin.ProtectionEntry;
import com.perforce.p4java.option.server.GetProtectionEntriesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IProtectsDelegator;

/**
 * Implementation to handle the Protects command.
 */
public class ProtectsDelegator extends BaseDelegator implements IProtectsDelegator {

    /**
     * Instantiate a new ProtectsDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public ProtectsDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IProtectionEntry> getProtectionEntries(
            final boolean allUsers,
            final String hostName,
            final String userName,
            final String groupName,
            final List<IFileSpec> fileList)
            throws ConnectionException, RequestException, AccessException {

        try {
            GetProtectionEntriesOptions opts = new GetProtectionEntriesOptions()
                    .setAllUsers(allUsers)
                    .setHostName(hostName)
                    .setUserName(userName)
                    .setGroupName(groupName);
            return getProtectionEntries(fileList, opts);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public List<IProtectionEntry> getProtectionEntries(
            final List<IFileSpec> fileList,
            final GetProtectionEntriesOptions opts) throws P4JavaException {

        // Get preferred path array without annotations. The reason is the
        // Perforce server 'protects' command requires a file list devoid of
        // annotated revision specificity.
        List<Map<String, Object>> resultMaps = execMapCmdList(
                PROTECTS,
                processParameters(
                        opts,
                        fileList,
                        null,
                        false,
                        server),
                null);


        final AtomicInteger order = new AtomicInteger(-1);
        return buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                new Function<Map, IProtectionEntry>() {
                    @Override
                    public IProtectionEntry apply(Map map) {
                        return new ProtectionEntry(map, order.incrementAndGet());
                    }
                }
        );
    }
}
