package com.perforce.p4java.tests;

import java.io.File;

public class UnicodeServerRule extends SimpleServerRule {

	public UnicodeServerRule(String p4dVersion, String dataExtractLoc) {
		super(p4dVersion, dataExtractLoc);
	}

	private void unicode() throws Exception {
		exec(new String[]{"-xi"});
	}

	@Override
	public void prepareServer() throws Exception {
		extract(new File(RESOURCES + "data/unicode/depot.tar.gz"));
		restore(new File(RESOURCES + "data/unicode/checkpoint.gz"));
		upgrade();
		unicode();
	}
}
