package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseInt;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.handleErrorStr;
import static com.perforce.p4java.server.CmdSpec.REVIEW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IReviewChangelist;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.ReviewChangelist;
import com.perforce.p4java.option.server.GetReviewChangelistsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IReviewDelegator;

/**
 * Implementation to handle the Review command.
 */
public class ReviewDelegator extends BaseDelegator implements IReviewDelegator {
    /**
     * Instantiate a new ReviewDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public ReviewDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IReviewChangelist> getReviewChangelists(final GetReviewChangelistsOptions opts)
            throws P4JavaException {

        List<IReviewChangelist> reviewList = new ArrayList<>();
        List<Map<String, Object>> resultMaps = execMapCmdList(
                REVIEW,
                processParameters(opts, server),
                null);

        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                handleErrorStr(map);
                try {
                    ReviewChangelist reviewChangelist = new ReviewChangelist(
                            parseInt(map, "change"),
                            parseString(map, "user"),
                            parseString(map, "email"),
                            parseString(map, "name"));

                    reviewList.add(reviewChangelist);
                } catch (Throwable thr) {
                    Log.error("Unexpected exception in getReviews: %s", thr.getLocalizedMessage());
                    Log.exception(thr);
                }
            }
        }
        return reviewList;
    }
}
