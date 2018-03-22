package com.perforce.p4java.tests;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleTestServer {

	private static Logger logger = LoggerFactory.getLogger(SimpleTestServer.class);

	public static final String RESOURCES = "src/test/resources/";
	private String p4d;
	private File p4root;

	public SimpleTestServer(String p4dVersion, String testId) {

		init(p4dVersion);
		this.p4root = new File("tmp/" + testId).getAbsoluteFile();
	}

	private void init(String p4dVersion) {
		String rootPath = RESOURCES + "bin/" + p4dVersion;
		String p4d = new File(rootPath).getAbsolutePath().toString();
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			p4d += "/bin.ntx64/p4d.exe";
		}
		if (os.contains("mac")) {
			p4d += "/bin.darwin90x86_64/p4d";
		}
		if (os.contains("nix") || os.contains("nux")) {
			p4d += "/bin.linux26x86_64/p4d";
		}
		this.p4d = p4d;
	}

	public void prepareServer() throws Exception {
		extract(new File(RESOURCES + "data/nonunicode/depot.tar.gz"));
		restore(new File(RESOURCES + "data/nonunicode/checkpoint.gz"));
		upgrade();
	}

	public String getRSHURL() {
		return "p4jrsh://" + getP4d() + " -r " + getPathToRoot() + " -L log -i --java";
	}

	public String getPathToRoot() {
		return p4root.getAbsolutePath();
	}

	public String getP4d() {
		return p4d;
	}

	public String getResources() {
		return RESOURCES;
	}

	public String getRshPort() {
		String rsh = "rsh:" + p4d;
		rsh += " -r " + p4root;
		rsh += " -L log";
		rsh += " -i ";

		return rsh;
	}

	protected void upgrade() throws Exception {
		exec(new String[]{"-xu"});
	}

	protected void restore(File ckp) throws Exception {
		exec(new String[]{"-z", "-jr", formatPath(ckp.getAbsolutePath())});
	}

	public void extract(File archive) throws Exception {
		TarArchiveInputStream tarIn = null;
		tarIn = new TarArchiveInputStream(
				new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(archive))));

		TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
		while (tarEntry != null) {
			File node = new File(p4root, tarEntry.getName());

			if (tarEntry.isDirectory()) {
				node.mkdirs();
			} else {
				try {
					node.createNewFile();
				} catch (IOException e) {
					logger.warn("Could not extract file: ", e);
					tarEntry = tarIn.getNextTarEntry();
					continue;
				}
				byte[] buf = new byte[1024];
				BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(node));

				int len = 0;
				while ((len = tarIn.read(buf)) != -1) {
					bout.write(buf, 0, len);
				}
				bout.close();
			}
			tarEntry = tarIn.getNextTarEntry();
		}
		tarIn.close();
	}

	protected void clean() {
		if (p4root.exists()) {
			try {
				FileUtils.cleanDirectory(p4root);
			} catch (IOException e) {
				logger.warn("Unable to clean p4root: ", e);
			}
		} else {
			p4root.mkdir();
		}
	}

	protected void destroy() {
		if (p4root.exists()) {
			try {
				FileUtils.deleteDirectory(p4root);
			} catch (IOException e) {
				logger.warn("Unable to delete p4root: ", e);
			}
		}
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
		logger.info("P4D Version: " + version);
		return version;
	}

	protected int exec(String[] args) throws Exception {
		CommandLine cmdLine = new CommandLine(p4d);
		cmdLine.addArgument("-C0");
		cmdLine.addArgument("-r");
		cmdLine.addArgument(formatPath(p4root.getAbsolutePath()));
		for (String arg : args) {
			cmdLine.addArgument(arg);
		}

		logger.debug("EXEC: " + cmdLine.toString());

		DefaultExecutor executor = new DefaultExecutor();
		return executor.execute(cmdLine);
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
