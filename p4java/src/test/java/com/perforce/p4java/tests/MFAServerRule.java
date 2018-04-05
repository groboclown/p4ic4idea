package com.perforce.p4java.tests;

import java.io.File;

public class MFAServerRule extends SimpleServerRule {

	public MFAServerRule(String p4dVersion, String dataExtractLoc) {
		super(p4dVersion, dataExtractLoc);
	}

	@Override
	public void prepareServer() throws Exception {
		extract(new File(RESOURCES + "data/2fa/depot.tar.gz"));
		restore(new File(RESOURCES + "data/2fa/checkpoint.gz"));
		upgrade();
	}
}