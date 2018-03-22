/**
 * Copyright (c) 2013 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.stream.helper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

import com.perforce.p4java.Log;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.impl.mapbased.rpc.stream.RpcSSLSocketFactory;

/**
 * Helper class for creating and configuring sockets.
 */
public class RpcSocketHelper {
	
	/**
	 * Configure a socket with specified properties.
	 */
	public static void configureSocket(Socket socket, Properties properties) {
		if (socket == null || properties == null) {
			return;
		}

		try {
			// Enable/disable TCP_NODELAY (disable/enable Nagle's algorithm).
			boolean tcpNoDelay = RpcPropertyDefs.getPropertyAsBoolean(properties,
					RpcPropertyDefs.RPC_SOCKET_TCP_NO_DELAY_NICK,
					RpcPropertyDefs.RPC_SOCKET_TCP_NO_DELAY_DEFAULT);
			socket.setTcpNoDelay(tcpNoDelay);
			
			String keepAlive = RpcPropertyDefs.getProperty(properties,
					RpcPropertyDefs.RPC_SOCKET_USE_KEEPALIVE_NICK);

			int timeouts = RpcPropertyDefs.getPropertyAsInt(properties,
					RpcPropertyDefs.RPC_SOCKET_SO_TIMEOUT_NICK,
					RpcPropertyDefs.RPC_SOCKET_SO_TIMEOUT_DEFAULT);

			int[] perfPrefs = RpcPropertyDefs.getPropertyAsIntArray(properties,
					RpcPropertyDefs.RPC_SOCKET_PERFORMANCE_PREFERENCES_NICK,
					RpcPropertyDefs.RPC_DEFAULT_PROPERTY_DELIMITER,
					RpcPropertyDefs.RPC_SOCKET_PERFORMANCE_PREFERENCES_DEFAULT);

			// Setting the socket performance preferences, described by three
			// integers whose values indicate the relative importance of short
			// connection time, low latency, and high bandwidth.
			// Socket.setPerformancePreferences(int connectionTime, int latency, int bandwidth)
			// The default values is (1, 2, 0), assume no one changes them.
			// This gives the highest importance to low latency, followed by
			// short connection time, and least importance to high bandwidth.
			if (perfPrefs != null && perfPrefs.length == 3) {
				socket.setPerformancePreferences(
						perfPrefs[0],
						perfPrefs[1],
						perfPrefs[2]);
			}
			
			socket.setSoTimeout(timeouts);

			if ((keepAlive != null)
					&& (keepAlive.startsWith("n") || keepAlive.startsWith("N"))) {
				socket.setKeepAlive(false);
			} else {
				socket.setKeepAlive(true);
			}

			int sockRecvBufSize = RpcPropertyDefs.getPropertyAsInt(properties,
					RpcPropertyDefs.RPC_SOCKET_RECV_BUF_SIZE_NICK, 0);
			int sockSendBufSize = RpcPropertyDefs.getPropertyAsInt(properties,
					RpcPropertyDefs.RPC_SOCKET_SEND_BUF_SIZE_NICK, 0);

			if (sockRecvBufSize != 0) {
				socket.setReceiveBufferSize(sockRecvBufSize);
			}

			if (sockSendBufSize != 0) {
				socket.setSendBufferSize(sockSendBufSize);
			}
		} catch (Throwable exc) {
			Log
					.warn("Unexpected exception while setting Perforce RPC socket options: "
							+ exc.getLocalizedMessage());
			Log.exception(exc);
		}
	}
	
	/**
	 * Create a socket with the specified properties and connect to the specified host and port.
	 */
	public static Socket createSocket(String host, int port, Properties properties, boolean secure) throws IOException {
		Socket socket = null;

		if (secure) {
			socket = RpcSSLSocketFactory.getInstance(properties).createSocket();
		} else {
			socket = new Socket();
		}
		
		configureSocket(socket, properties);

		socket.bind(new InetSocketAddress(0));
		socket.connect(new InetSocketAddress(host, port));
		
		return socket;
	}
}
