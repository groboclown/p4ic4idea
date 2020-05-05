package com.perforce.p4java.tests;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class SimpleServerRule extends SimpleTestServer implements TestRule {

	public SimpleServerRule(String p4dVersion, String dataExtractLoc) {
		super(p4dVersion, dataExtractLoc);
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
			statement.evaluate();
			destroy();
			clean();
		}
	}
}