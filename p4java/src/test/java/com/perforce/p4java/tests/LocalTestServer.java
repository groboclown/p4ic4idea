package com.perforce.p4java.tests;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.Properties;

public class LocalTestServer extends SimpleTestServer {

	private static Logger logger = LoggerFactory.getLogger(LocalTestServer.class);

	private final String p4port;

	private static final String P4USER = "p4jtestsuper";
	private static final String P4PASS = "p4jtestsuper";

	public LocalTestServer(String p4dVersion, String testId, String p4port) {
		super(p4dVersion, testId);
		this.p4port = p4port;
	}

	public String getP4Port() {
		return p4port;
	}

	public String getP4JavaUri() throws Exception {
		Address address = new Address(p4port);
		if ("ssl".equals(address.getProto())) {
			return "p4javassl://" + address.getHost() + ":" + address.getPort();
		}
		return "p4java://" + address.getHost() + ":" + address.getPort();
	}

	protected void start() throws Exception {
		logger.info("Starting Perforce on: " + p4port);
		exec(new String[]{"-p", p4port, "-L", "log"}, false);

		while(!serverUp()) {
			Thread.sleep(100);
		}
		logger.info("Started. ");
	}

	protected void stop() throws Exception {
		logger.info("Stopping Perforce on: " + p4port);

		// Allow p4 admin commands.
		Properties props = System.getProperties();
		props.put(RpcPropertyDefs.RPC_RELAX_CMD_NAME_CHECKS_NICK, "true");

		IOptionsServer server = ServerFactory.getOptionsServer(getP4JavaUri(), props);
		if (p4port.contains("ssl")) {
			server.addTrust(new TrustOptions().setForce(true).setAutoAccept(true));
		}
		server.connect();
		server.setUserName(P4USER);

		try {
			server.login(P4PASS);
		} catch (AccessException e) {
			// Catch warning:  'login' not necessary, no password set for this user.
			logger.info(e.getMessage());
		}

		server.execMapCmd("admin", new String[]{"stop"}, null);

		while(serverUp()) {
			Thread.sleep(100);
		}
		logger.warn("Stopped. ");
	}

	protected boolean serverUp() {
		Address address = new Address(p4port);

		Socket s = null;
		try {
			s = new Socket(address.getHost(), address.getPort());
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (Exception e) {
				}
			}
		}
	}

	private static class Address {
		private final String proto;
		private final String host;
		private final int port;

		public Address(String p4port) {
			if (p4port == null || p4port.isEmpty()) {
				proto = "tcp";
				host = "localhost";
				port = 1666;
			} else if (!p4port.contains(":")) {
				proto = "tcp";
				host = "localhost";
				port = Integer.parseInt(p4port);
			} else {
				String[] parts = p4port.split(":");
				if (parts.length == 3) {
					proto = parts[0];
					host = parts[1];
					port = Integer.parseInt(parts[2]);
				} else {
					proto = "tcp";
					host = parts[0];
					port = Integer.parseInt(parts[1]);
				}
			}
		}

		public String getProto() {
			return proto;
		}

		public String getHost() {
			return host;
		}

		public int getPort() {
			return port;
		}
	}
}
