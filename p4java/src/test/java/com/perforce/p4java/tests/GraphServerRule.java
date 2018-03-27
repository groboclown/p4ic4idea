package com.perforce.p4java.tests;

import com.perforce.test.TestServer;

import javax.annotation.Nonnull;
import java.io.IOException;

public class GraphServerRule extends SimpleServerRule {

	public GraphServerRule(String p4dVersion, String dataExtractLoc) {
		super(p4dVersion, dataExtractLoc);
	}

	protected void initializeServer(@Nonnull TestServer server)
			throws IOException {
		server.initialize(this.getClass().getClassLoader(),
				"data/graph/depot.tar.gz",
				"data/graph/checkpoint.gz");
	}
}
