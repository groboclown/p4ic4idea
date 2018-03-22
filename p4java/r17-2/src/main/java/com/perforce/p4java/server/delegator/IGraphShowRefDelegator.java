package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.graph.IGraphRef;
import com.perforce.p4java.option.server.GraphShowRefOptions;

import java.util.List;

public interface IGraphShowRefDelegator {

	List<IGraphRef> getGraphShowRefs(GraphShowRefOptions opts) throws P4JavaException;
}
