package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.P4JavaExceptions.rethrowFunction;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwRequestExceptionIfPerforceServerVersionOldThanExpected;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNonNullObjectListFromCommandResultMaps;
import static com.perforce.p4java.server.CmdSpec.MOVE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.perforce.p4java.Log;
import com.perforce.p4java.common.function.FunctionWithException;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.MoveFileOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IMoveDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation to handle the Move command.
 */
public class MoveDelegator extends BaseDelegator implements IMoveDelegator {
    /**
     * Instantiate a new MoveDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public MoveDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IFileSpec> moveFile(
            final int changelistId,
            final boolean listOnly,
            final boolean noClientMove,
            final String fileType,
            @Nonnull final IFileSpec fromFile,
            @Nonnull final IFileSpec toFile)
            throws ConnectionException, RequestException, AccessException {

        Validate.notNull(fromFile);
        Validate.notNull(fromFile.getPreferredPath());
        Validate.notNull(toFile);
        Validate.notNull(toFile.getPreferredPath());

        final int MIN_SUPPORTED_SERVER_VERSION = 20091;
        final int MIN_SUPPORTED_SERVER__VERSION_OPTION_K = 20092;
        throwRequestExceptionIfPerforceServerVersionOldThanExpected(
                server.getServerVersion() >= MIN_SUPPORTED_SERVER_VERSION,
                "command requires a Perforce server version 2009.1 or later");

        throwRequestExceptionIfPerforceServerVersionOldThanExpected(
                server.getServerVersion() >= MIN_SUPPORTED_SERVER__VERSION_OPTION_K || !noClientMove,
                "command option noClientMove requires a Perforce server version 2009.2 or later");

        try {
            MoveFileOptions moveFileOptions = new MoveFileOptions()
                    .setChangelistId(changelistId)
                    .setFileType(fileType)
                    .setForce(false)
                    .setListOnly(listOnly)
                    .setNoClientMove(noClientMove);

            return moveFile(fromFile, toFile, moveFileOptions);
        } catch (final ConnectionException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            Log.warn("Unexpected exception in IServer.moveFile: " + exc);
            return Collections.emptyList();
        }
    }

    @Override
    public List<IFileSpec> moveFile(
            @Nonnull final IFileSpec fromFile,
            @Nonnull final IFileSpec toFile,
            @Nullable final MoveFileOptions opts) throws P4JavaException {

        Validate.notNull(fromFile);
        Validate.notNull(fromFile.getPreferredPath());
        Validate.notNull(toFile);
        Validate.notNull(toFile.getPreferredPath());

        List<Map<String, Object>> resultMaps = execMapCmdList(MOVE,
                processParameters(
                        opts,
                        null,
                        new String[]{
                                fromFile.getPreferredPath().toString(),
                                toFile.getPreferredPath().toString()
                        },
                        server),
                null);

        return buildNonNullObjectListFromCommandResultMaps(resultMaps,
                // p4ic4idea: explicit generics
                rethrowFunction(new FunctionWithException<Map<String, Object>, IFileSpec>() {
                    // p4ic4idea: explicit generics
                    public IFileSpec apply(Map<String, Object> map) throws P4JavaException {
                        return ResultListBuilder.handleFileReturn(map, server);
                    }
                })
        );
    }
}
