package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNonNullObjectListFromCommandResultMaps;
import static com.perforce.p4java.server.CmdSpec.DISKSPACE;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.admin.IDiskSpace;
import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.admin.DiskSpace;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IDiskspaceDelegator;

/**
 * Implementation to handle the Diskspace command.
 */
public class DiskspaceDelegator extends BaseDelegator implements IDiskspaceDelegator {
    /**
     * Instantiate a new DiskspaceDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public DiskspaceDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IDiskSpace> getDiskSpace(final List<String> filesystems) throws P4JavaException {
        String[] cmdArgs = null;
        if (nonNull(filesystems)) {
            cmdArgs = filesystems.toArray(new String[filesystems.size()]);
        }

        List<Map<String, Object>> resultMaps = execMapCmdList(
                DISKSPACE,
                cmdArgs,
                null);

        return buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                // p4ic4idea: explicit generics
                new Function<Map<String, Object>, IDiskSpace>() {
                    @Override
                    // p4ic4idea: explicit generics
                    public IDiskSpace apply(Map<String, Object> map) {
                        return new DiskSpace(map);
                    }
                });
    }
}
