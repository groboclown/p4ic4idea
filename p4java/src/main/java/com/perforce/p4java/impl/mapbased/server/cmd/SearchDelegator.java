package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.getInfoStr;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.throwRequestExceptionIfErrorMessageFound;
import static com.perforce.p4java.server.CmdSpec.SEARCH;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.server.SearchJobsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.ISearchDelegator;
import org.apache.commons.lang3.Validate;
/**
 * Implementation to handle the Search command.
 */
public class SearchDelegator extends BaseDelegator implements ISearchDelegator {
    /**
     * Instantiate a new SearchDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public SearchDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<String> searchJobs(
            final String words,
            final SearchJobsOptions opts) throws P4JavaException {

        Validate.notBlank(words, "Null or empty words passed to searchJobs method");

        List<Map<String, Object>> resultMaps = execMapCmdList(
                SEARCH,
                Parameters.processParameters(opts, null, new String[]{words}, server),
                null);

        List<String> jobIdList = new ArrayList<>();
        if (resultMaps != null) {
            for (Map<String, Object> map : resultMaps) {
                if (map != null) {
                    throwRequestExceptionIfErrorMessageFound(map);
                    jobIdList.add(getInfoStr(map));
                }
            }
        }

        return jobIdList;
    }
}
