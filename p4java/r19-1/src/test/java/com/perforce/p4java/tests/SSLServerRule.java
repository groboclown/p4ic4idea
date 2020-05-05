package com.perforce.p4java.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;

public class SSLServerRule extends LocalServerRule {

	private static Logger logger = LoggerFactory.getLogger(SSLServerRule.class);

	private static final String P4SSLDIR = "p4ssldir";

	private HashMap<String, String> env = new HashMap<>();
	private final String p4ssldir;

	public SSLServerRule(String p4dVersion, String testId, String p4port) {
		super(p4dVersion, testId, p4port);
		p4ssldir = getPathToRoot() + "/" + P4SSLDIR;
	}

	@Override
	public void prepareServer() throws Exception {
		extract(new File(RESOURCES + "data/nonunicode/depot.tar.gz"));
		restore(new File(RESOURCES + "data/nonunicode/checkpoint.gz"));
		upgrade();
		createSSLCert();
	}

	private void createSSLCert() {
		try {
			File file = new File(p4ssldir);
			file.mkdirs();
			if (!System.getProperty("os.name").toLowerCase().contains("win")) {
				Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rwx------"));
			}
			env.put("P4SSLDIR", p4ssldir);
			exec(new String[]{"-Gc"}, true, env);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void start() throws Exception {
		logger.info("Starting Perforce on: " + getP4Port());
		exec(new String[]{"-p", getP4Port(), "-L", "log"}, false, env);

		while(!serverUp()) {
			Thread.sleep(100);
		}
		logger.info("Started. ");

	}
}
