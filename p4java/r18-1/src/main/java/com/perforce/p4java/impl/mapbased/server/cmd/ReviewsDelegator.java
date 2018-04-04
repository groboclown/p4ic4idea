package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.handleErrorStr;
import static com.perforce.p4java.server.CmdSpec.REVIEWS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.UserSummary;
import com.perforce.p4java.option.server.GetReviewsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IReviewsDelegator;

/**
 * Implementation to handle the Reviews command.
 */
public class ReviewsDelegator extends BaseDelegator implements IReviewsDelegator {
    /**
     * Instantiate a new ReviewsDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public ReviewsDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IUserSummary> getReviews(
            final List<IFileSpec> fileSpecs,
            final GetReviewsOptions opts) throws P4JavaException {

        List<IUserSummary> userList = new ArrayList<>();
        List<Map<String, Object>> resultMaps = execMapCmdList(REVIEWS,
                processParameters(opts, fileSpecs, server), null);

        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                handleErrorStr(map);
                try {
                    UserSummary userSummary = new UserSummary(
                            parseString(map, "user"),
                            parseString(map, "email"),
                            parseString(map, "name"),
                            null, // access
                            null // update
                    );

                    userList.add(userSummary);
                } catch (Throwable thr) {
                    Log.error("Unexpected exception in getReviews: %s", thr.getLocalizedMessage());
                    Log.exception(thr);
                }
            }
        }
        return userList;
    }

    @Override
    public List<IUserSummary> getReviews(
            final int changelistId,
            final List<IFileSpec> fileSpecs)
            throws ConnectionException, RequestException, AccessException {

        try {
            return getReviews(fileSpecs, new GetReviewsOptions(changelistId));
        } catch (final ConnectionException | RequestException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }
}
