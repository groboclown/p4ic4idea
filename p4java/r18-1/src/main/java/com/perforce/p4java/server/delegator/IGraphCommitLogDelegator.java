package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.graph.ICommit;
import com.perforce.p4java.option.server.GraphCommitLogOptions;

import java.util.List;

public interface IGraphCommitLogDelegator {

    List<ICommit> getGraphCommitLogList(GraphCommitLogOptions options) throws P4JavaException;
}
