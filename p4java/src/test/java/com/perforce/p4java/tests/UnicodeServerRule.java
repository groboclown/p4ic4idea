package com.perforce.p4java.tests;

import com.perforce.test.TestServer;

import javax.annotation.Nonnull;
import java.io.IOException;

public class UnicodeServerRule extends SimpleServerRule {

	public UnicodeServerRule(String p4dVersion, String dataExtractLoc) {
		super(p4dVersion, dataExtractLoc);
	}

	protected void initializeServer(@Nonnull TestServer server)
			throws IOException {
		server.setUnicode(true);
		server.initialize(this.getClass().getClassLoader(),
				"data/unicode/depot.tar.gz",
				"data/unicode/checkpoint.gz");
	}
}
