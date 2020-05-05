package com.perforce.p4java.tests;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class LocalServerRule extends LocalTestServer implements TestRule {

	public LocalServerRule(String p4dVersion, String testId, String p4port) {
		super(p4dVersion, testId, p4port);
	}

	@Override
	public Statement apply(Statement statement, Description description) {
		return new ServerStatement(statement);
	}

	public class ServerStatement extends Statement {

		private final Statement statement;

		public ServerStatement(Statement statement) {
			this.statement = statement;
		}

		@Override
		public void evaluate() throws Throwable {
			clean();
			prepareServer();
			start();
			statement.evaluate();
			stop();
			destroy();
			clean();
		}
	}
}
