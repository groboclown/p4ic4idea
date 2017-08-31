package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNonNullObjectListFromCommandResultMaps;
import static com.perforce.p4java.server.CmdSpec.SIZES;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.file.IFileSize;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSize;
import com.perforce.p4java.option.server.GetFileSizesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.ISizesDelegator;

/**
 * Implementation to handle the Sizes command.
 */
public class SizesDelegator extends BaseDelegator implements ISizesDelegator {
    /**
     * Instantiate a new SizesDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public SizesDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IFileSize> getFileSizes(
            final List<IFileSpec> fileSpecs,
            final GetFileSizesOptions opts) throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(
                SIZES,
                processParameters(opts, fileSpecs, server),
                null);

        return buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                new Function<Map, IFileSize>() {
                    @Override
                    public IFileSize apply(Map map) {
                        return new FileSize(map);
                    }
                }
        );
    }
}
