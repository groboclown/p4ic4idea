package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwRequestExceptionIfConditionFails;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.JOURNALWAIT;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.JournalWaitOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IJournalWaitDelegator;

/**
 * Implementation for journal wait.
 */
public class JournalWaitDelegator extends BaseDelegator implements IJournalWaitDelegator {

    /**
     * Instantiates a new journal wait delegator.
     *
     * @param server
     *            the server
     */
    public JournalWaitDelegator(final IOptionsServer server) {
        super(server);
    }

    /**
     * Journal wait.
     *
     * @param opts
     *            the opts
     * @throws P4JavaException
     *             the p4 java exception
     */
    @Override
    public void journalWait(final JournalWaitOptions opts) throws P4JavaException {
        List<Map<String, Object>> resultMaps = execMapCmdList(JOURNALWAIT,
                processParameters(opts, server), null);

        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                String errStr = ResultMapParser.getErrorStr(map);
                throwRequestExceptionIfConditionFails(isBlank(errStr), parseCode0ErrorString(map),
                        errStr);
            }
        }
    }
}
