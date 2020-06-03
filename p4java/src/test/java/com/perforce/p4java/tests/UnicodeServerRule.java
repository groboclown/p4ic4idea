package com.perforce.p4java.tests;

import com.perforce.p4java.tests.ignoreRule.ConditionallyIgnoreClassRule;
import com.perforce.test.TestServer;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.annotation.Nonnull;
import java.io.IOException;

public class UnicodeServerRule extends SimpleServerRule {
	public UnicodeServerRule(String p4dVersion, String dataExtractLoc) {
		super(p4dVersion, dataExtractLoc);
	}

	@Override
	public Statement apply(Statement statement, Description description) {
		// This server encounters errors about creating files every time it's used when run on Windows,
		// so just ignore these kinds of tests on Windows platforms.
		return ConditionallyIgnoreClassRule
			.ifWindows("Test uses characters for files that are invalid on Windows")
			.apply(super.apply(statement, description), description);
	}

	protected void initializeServer(@Nonnull TestServer server)
			throws IOException {
		server.setUnicode(true);
		server.initialize(this.getClass().getClassLoader(),
				"data/unicode/depot.tar.gz",
				"data/unicode/checkpoint.gz");
	}
}
