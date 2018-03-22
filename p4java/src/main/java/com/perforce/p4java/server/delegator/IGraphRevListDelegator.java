package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.graph.IRevListCommit;
import com.perforce.p4java.option.server.GraphRevListOptions;

import java.util.List;

public interface IGraphRevListDelegator {

	List<IRevListCommit> getGraphRevList(GraphRevListOptions options) throws P4JavaException;
}
