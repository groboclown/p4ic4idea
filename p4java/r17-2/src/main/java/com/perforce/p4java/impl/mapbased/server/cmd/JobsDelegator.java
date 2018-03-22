package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.server.CmdSpec.JOBS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.core.IJob;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Job;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.server.GetJobsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IJobsDelegator;

/**
 * Implementation for 'p4 jobs'..
 */
public class JobsDelegator extends BaseDelegator implements IJobsDelegator {

    /**
     * Instantiates a new jobs delegator.
     *
     * @param server
     *            the server
     */
    public JobsDelegator(final IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IJob> getJobs(final List<IFileSpec> fileSpecs, final int maxJobs,
            final boolean longDescriptions, final boolean reverseOrder,
            final boolean includeIntegrated, final String jobView)
            throws ConnectionException, RequestException, AccessException {

        try {
            GetJobsOptions getJobsOptions = new GetJobsOptions()
                    .setIncludeIntegrated(includeIntegrated).setLongDescriptions(longDescriptions)
                    .setMaxJobs(maxJobs).setReverseOrder(reverseOrder).setJobView(jobView);

            return getJobs(fileSpecs, getJobsOptions);
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public List<IJob> getJobs(final List<IFileSpec> fileSpecs, final GetJobsOptions opts)
            throws P4JavaException {
        List<IJob> jobList = new ArrayList<>();

        List<Map<String, Object>> resultMaps = execMapCmdList(JOBS,
                Parameters.processParameters(opts, fileSpecs, server), null);

        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                if (nonNull(map)) {
                    ResultMapParser.throwRequestExceptionIfErrorMessageFound(map);
                    jobList.add(new Job(server, map, nonNull(opts) && opts.isLongDescriptions()));
                }
            }
        }

        return jobList;
    }
}
