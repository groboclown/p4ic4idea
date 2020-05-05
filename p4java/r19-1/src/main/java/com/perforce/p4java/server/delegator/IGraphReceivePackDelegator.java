package com.perforce.p4java.server.delegator;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GraphReceivePackOptions;

public interface IGraphReceivePackDelegator {
	void doGraphReceivePack(GraphReceivePackOptions options) throws P4JavaException;
}
