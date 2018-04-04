package com.perforce.p4java.tests;

import java.io.File;

public class GraphServerRule extends SimpleServerRule {

	public GraphServerRule(String p4dVersion, String dataExtractLoc) {
		super(p4dVersion, dataExtractLoc);
	}

	@Override
	public void prepareServer() throws Exception {
		extract(new File(RESOURCES + "data/graph/depot.tar.gz"));
		restore(new File(RESOURCES + "data/graph/checkpoint.gz"));
		upgrade();
	}
}
