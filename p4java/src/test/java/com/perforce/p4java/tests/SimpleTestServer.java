package com.perforce.p4java.tests;

import com.perforce.p4java.server.IServerAddress;
import com.perforce.test.P4ExtFileUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// p4ic4idea: TODO rewrite to use TestServer

public class SimpleTestServer {
	private String p4d;
	private final File p4root;

	public SimpleTestServer(String p4dVersion, String testId) {
		final File baseDir = new File("tmp/" + testId);
		this.p4root = new File(baseDir, "root").getAbsoluteFile();
		try {
			this.p4d = P4ExtFileUtils.extractP4d(baseDir, p4dVersion).getAbsolutePath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (!new File(p4d).isFile()) {
			throw new RuntimeException("Does not exist: " + p4d);
		}
	}

	public void prepareServer() throws Exception {
		extract("data/nonunicode/depot.tar.gz");
		restore("data/nonunicode/checkpoint.gz");
		upgrade();
	}

	public String getRSHURL() {
		return IServerAddress.Protocol.P4JRSH + "://" + getP4d() + " -r " + getPathToRoot() + " -L log -i --java -vrpc=3";
	}

	public String getNonThreadSafeRSHURL() {
		return IServerAddress.Protocol.P4JRSHNTS + "://" + getP4d() + " -r " + getPathToRoot() + " -L log -i --java -vrpc=3";
	}

	public String getPathToRoot() {
		return p4root.getAbsolutePath();
	}

	public String getP4d() {
		return p4d;
	}

	public String getRshPort() {
		String rsh = "rsh:" + p4d;
		rsh += " -r " + p4root;
		rsh += " -L log";
		rsh += " -i ";

		return rsh;
	}

	protected void upgrade() throws Exception {
		exec(new String[]{"-xu"}, true);
	}

	public void rotateJournal() throws Exception {
		exec(new String[]{"-jj"}, true);
	}

	protected void restore(String checkpointResource) throws Exception {
		File outfile = new File(p4root, "checkpoint.gz");
		P4ExtFileUtils.extractResource(getClass().getClassLoader(), checkpointResource, outfile, false);
		exec(new String[]{"-z", "-jr", formatPath(outfile.getAbsolutePath())}, true);
	}

	public void extract(String archiveResource) throws Exception {
		// p4ic4idea: reuse other extraction
		P4ExtFileUtils.extractResource(getClass().getClassLoader(), archiveResource, p4root, true);
	}

	protected void clean() {
		if (p4root.exists()) {
			try {
				FileUtils.cleanDirectory(p4root);
			} catch (IOException e) {
				// logger.warn("Unable to clean p4root: ", e);
			}
		} else {
			if (!p4root.mkdir()) {
				throw new RuntimeException("unable to create " + p4root);
			}
		}
	}

	protected void destroy() {
		if (p4root.exists()) {
			try {
				FileUtils.deleteDirectory(p4root);
			} catch (IOException e) {
				if (!retryDestroy()) {
					// logger.warn("Unable to delete p4root.");
				}
			}
		}
		final File p4dFile = new File(p4d);
		if (p4dFile.isFile()) {
			if (! p4dFile.delete()) {
				// logger.warn("Unable to delete " + p4dFile);
			}
		}
	}

	private boolean retryDestroy() {
		int count = 10;
		while (count > 0) {
			try {
				Thread.sleep(10);
				FileUtils.deleteDirectory(p4root);
				return true;
			} catch (IOException e) {
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			count--;
		}
		return false;
	}

	public int getVersion() throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		CommandLine cmdLine = new CommandLine(p4d);
		cmdLine.addArgument("-V");
		DefaultExecutor executor = new DefaultExecutor();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		executor.setStreamHandler(streamHandler);
		executor.execute(cmdLine);

		int version = 0;
		for (String line : outputStream.toString().split("\\n")) {
			if (line.startsWith("Rev. P4D")) {
				Pattern p = Pattern.compile("\\d{4}\\.\\d{1}");
				Matcher m = p.matcher(line);
				while (m.find()) {
					String found = m.group();
					found = found.replace(".", ""); // strip "."
					version = Integer.parseInt(found);
				}
			}
		}
		// logger.info("P4D Version: " + version);
		return version;
	}

	protected void exec(String[] args, boolean block) throws Exception {
		exec(args, block, null);
	}

	protected void exec(String[] args, boolean block, HashMap<String, String> environment) throws Exception {
		CommandLine cmdLine = new CommandLine(p4d);
		cmdLine.addArgument("-C0");
		cmdLine.addArgument("-r");
		cmdLine.addArgument(formatPath(p4root.getAbsolutePath()));
		for (String arg : args) {
			cmdLine.addArgument(arg);
		}

		// logger.debug("EXEC: " + cmdLine.toString());

		DefaultExecutor executor = new DefaultExecutor();
		if (block) {
			executor.execute(cmdLine, environment);
		} else {
			DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
			executor.execute(cmdLine, environment, resultHandler);
		}
	}

	private String formatPath(String path) {
		final String Q = "\"";
		path = Q + path + Q;
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			path = path.replace('\\', '/');
		}
		return path;
	}
}
