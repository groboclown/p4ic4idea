package com.perforce.p4java.tests.dev.unit.features151;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;

public class RshConnection {

	public static IOptionsServer getRshConnection(String p4root) {
		// Get a server connection
		String p4d = getP4D();
		String rsh = "rsh:" + p4d + " -r " + p4root + " -i";
		String p4javaUri = getP4JavaUri(rsh);

		return getIOptionsServer(p4javaUri);
	}

	private static IOptionsServer getIOptionsServer(String p4javaUri) {
		IOptionsServer newIServer = null;
		Properties props = System.getProperties();

		// Identify ourselves in server log files.
		props.put(PropertyDefs.PROG_NAME_KEY, "jrsh");
		props.put(PropertyDefs.PROG_VERSION_KEY, "1.0");

		// Currently, if enable RPC socket pool (size > 0) causes problems...
		// Child processes (FDs) being closed...
		// Need to look into RPC socket pool shutdown code...
		// Or simply bypass RPC socket pool code, if the protocol is "p4jrsh://"...
		//props.put(RpcPropertyDefs.RPC_SOCKET_POOL_SIZE_NICK, "1");
		props.put(RpcPropertyDefs.RPC_RELAX_CMD_NAME_CHECKS_NICK, "true");
		props.put(RpcPropertyDefs.RPC_SOCKET_SO_TIMEOUT_NICK, "0");

		try {
			newIServer = ServerFactory.getOptionsServer(p4javaUri, props, null);
		} catch (Exception e) {
			StringBuffer sb = new StringBuffer();
			sb.append("Unable to get servert: ");
			sb.append(p4javaUri);
			e.printStackTrace();
		}

		return newIServer;
	}

	private static String getP4D() {
	    Path p4dBasePath = Paths.get("src", "test", "resources", "r15.1");
		String os = System.getProperty("os.name").toLowerCase();
		String p4d = null;
		if (os.contains("win")) {
		    p4d = Paths.get(p4dBasePath.toString(), "bin.ntx64", "p4d.exe").toString();
		}
		if (os.contains("mac")) {
		    p4d = Paths.get(p4dBasePath.toString(), "bin.darwin90x86_64", "p4d").toString();
		}
		if (os.contains("nix") || os.contains("nux")) {
		    p4d = Paths.get(p4dBasePath.toString(), "bin.linux26x86_64", "p4d").toString();
		}
		return p4d;
	}

	private static String getP4JavaUri(String port) {
		if (port.startsWith("ssl:")) {
			String trim = port.substring(4, port.length());
			return "p4javassl://" + trim;
		}
		if (port.startsWith("rsh:")) {
			String trim = port.substring(4, port.length());
			return "p4jrsh://" + trim + " --java";
		}
		return "p4java://" + port;
	}
}
