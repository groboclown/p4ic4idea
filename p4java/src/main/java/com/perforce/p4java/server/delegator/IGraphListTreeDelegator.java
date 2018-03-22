package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.graph.IGraphListTree;

import java.util.List;

public interface IGraphListTreeDelegator {

	List<IGraphListTree> getGraphListTree(String sha) throws P4JavaException;
}
