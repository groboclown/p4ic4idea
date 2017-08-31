package com.perforce.p4java.server.delegator;

import com.perforce.p4java.admin.ILogTail;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.LogTailOptions;

/**
 * Inteface for log tail.
 */
public interface ILogTailDelegator {

    /**
     * Gets the log tail.
     *
     * @param opts
     *            the opts
     * @return the log tail
     * @throws P4JavaException
     *             the p4 java exception
     */
    ILogTail getLogTail(LogTailOptions opts) throws P4JavaException;
}
