package com.perforce.p4java.tests;

import com.perforce.test.TestServer;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

// p4ic4idea: redone to use TestServer instead of SimpleTestServer
public class SimpleServerRule implements TestRule {
	private final TestServer testServer;

	public SimpleServerRule(String p4dVersion, String dataExtractLoc) {
		testServer = new TestServer(new File("tmp/" + dataExtractLoc));
		testServer.setP4dVersion(p4dVersion);
	}

	public String getRSHURL()
			throws IOException {
		return testServer.getRSHURL();
	}

	@Override
	public Statement apply(Statement statement, Description description) {
		return new ServerStatement(statement);
	}

	protected void initializeServer(@Nonnull TestServer server)
			throws IOException {
		server.initialize(this.getClass().getClassLoader(),
				"data/nonunicode/depot.tar.gz",
				"data/nonunicode/checkpoint.gz");
	}

	public String getPathToRoot() {
		return testServer.getPathToRoot();
	}

	public String getNonThreadSafeRSHURL()
			throws IOException {
		return testServer.getRSHURL();
	}

	public void rotateJournal()
			throws Exception {
		testServer.rotateJournal();
	}

	public class ServerStatement extends Statement {

		private final Statement statement;

		ServerStatement(Statement statement) {
			this.statement = statement;
		}

		@Override
		public void evaluate() throws Throwable {
			testServer.delete();
			try {
				initializeServer(testServer);
				statement.evaluate();
				testServer.stopServer();
			} finally {
				testServer.delete();
			}
		}
	}
}
