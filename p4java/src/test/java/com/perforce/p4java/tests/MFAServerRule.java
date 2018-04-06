package com.perforce.p4java.tests;

import com.perforce.test.TestServer;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

public class MFAServerRule extends SimpleServerRule {

	public MFAServerRule(String p4dVersion, String dataExtractLoc) {
		super(p4dVersion, dataExtractLoc);
	}

	protected void initializeServer(@Nonnull TestServer server)
			throws IOException {
		server.initialize(this.getClass().getClassLoader(),
				"data/2fa/depot.tar.gz",
				"data/2fa/checkpoint.gz");
	}
}