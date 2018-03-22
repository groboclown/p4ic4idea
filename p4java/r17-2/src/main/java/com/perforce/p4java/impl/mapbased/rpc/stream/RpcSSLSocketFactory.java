/**
 * Copyright (c) 2011 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.stream;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;

/**
 * A factory for creating SSL socket objects.
 */
public class RpcSSLSocketFactory extends SSLSocketFactory {

    /** The ssl socket factory. */
    static private RpcSSLSocketFactory sslSocketFactory = null;

    /** The ssl context. */
    private SSLContext sslContext = null;

    /** The properties. */
    private Properties properties = null;

    /**
	 * Private constructor.
	 */
	private RpcSSLSocketFactory(Properties properties) {
		super();
		this.properties = properties;
	}
	
	/**
	 * Gets the single instance of RpcSSLSocketFactory.
	 * 
	 * @return single instance of RpcSSLSocketFactory
	 */
	public static synchronized RpcSSLSocketFactory getInstance(Properties properties) {
		if (sslSocketFactory == null) {
			sslSocketFactory = new RpcSSLSocketFactory(properties);
		}
		return sslSocketFactory;
	}
	
	/**
	 * Creates a new RpcSSLSocket object.
	 * 
	 * @return the SSL context
	 */
	private SSLContext createSSLContext() {
		try {
			if (this.properties == null) {
				this.properties = new Properties();
			}
			String protocol = RpcPropertyDefs.getProperty(properties,
					RpcPropertyDefs.RPC_SECURE_SOCKET_PROTOCOL_NICK,
					RpcPropertyDefs.RPC_DEFAULT_SECURE_SOCKET_PROTOCOL);
			boolean trustAll = RpcPropertyDefs.getPropertyAsBoolean(properties,
					RpcPropertyDefs.RPC_SECURE_SOCKET_TRUST_ALL_NICK,
					RpcPropertyDefs.RPC_DEFAULT_SECURE_SOCKET_TRUST_ALL);
			TrustManager[] trustManager = trustAll ?
					new TrustManager[] { new TrustAllTrustManager() } : null;
			SSLContext context = SSLContext.getInstance(protocol);
			context.init(null, trustManager, null);
			return context;
		} catch (NoSuchAlgorithmException e) {
			Log.error("Error occurred in RpcSSLSocketFactory constructor: "
					+ e.getLocalizedMessage());
			Log.exception(e);
			throw new P4JavaError(
					"Error occurred in RpcSSLSocketFactory constructor: "
							+ e.getLocalizedMessage());
		} catch (KeyManagementException e) {
			Log.error("Error occurred in RpcSSLSocketFactory constructor: "
					+ e.getLocalizedMessage());
			Log.exception(e);
			throw new P4JavaError(
					"Error occurred in RpcSSLSocketFactory constructor: "
							+ e.getLocalizedMessage());
		}
	}

	/**
	 * Gets the sSL context.
	 * 
	 * @return the sSL context
	 */
	private SSLContext getSSLContext() {
		if (this.sslContext == null) {
			this.sslContext = createSSLContext();
		}
		return this.sslContext;
	}

	/**
	 * Configure ssl socket.
	 * 
	 * @param socket
	 *            the socket
	 * @return the socket
	 */
	private Socket configureSSLSocket(Socket socket) {
		if (socket != null) {
			if (this.properties == null) {
				this.properties = new Properties();
			}
			boolean setEnabledProtocols = RpcPropertyDefs.getPropertyAsBoolean(properties,
					RpcPropertyDefs.RPC_SECURE_SOCKET_SET_ENABLED_PROTOCOLS_NICK,
					RpcPropertyDefs.RPC_DEFAULT_SECURE_SOCKET_SET_ENABLED_PROTOCOLS);
			if (setEnabledProtocols) {
				String[] enabledProtocols = RpcPropertyDefs.getProperty(properties,
						RpcPropertyDefs.RPC_SECURE_SOCKET_ENABLED_PROTOCOLS_NICK,
						RpcPropertyDefs.RPC_DEFAULT_SECURE_SOCKET_ENABLED_PROTOCOLS).split("\\s*,\\s*");
				((SSLSocket)socket).setEnabledProtocols(enabledProtocols);
			}
		}
		return socket;
	}
	
	/**
	 * @see javax.net.ssl.SSLSocketFactory#createSocket(java.net.Socket, java.lang.String, int, boolean)
	 */
	@Override
	public Socket createSocket(Socket socket, String host, int port,
			boolean autoClose) throws IOException, UnknownHostException {
		return configureSSLSocket(getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose));
	}

	/**
	 * @see javax.net.SocketFactory#createSocket()
	 */
	@Override
	public Socket createSocket() throws IOException {
		return configureSSLSocket(getSSLContext().getSocketFactory().createSocket());
	}

	/**
	 * @see javax.net.SocketFactory#createSocket(java.lang.String, int)
	 */
	@Override
	public Socket createSocket(String host, int port) throws IOException,
			UnknownHostException {
		return configureSSLSocket(getSSLContext().getSocketFactory().createSocket(host, port));
	}

	/**
	 * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int)
	 */
	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		return configureSSLSocket(getSSLContext().getSocketFactory().createSocket(host, port));
	}

	/**
	 * @see javax.net.SocketFactory#createSocket(java.lang.String, int, java.net.InetAddress, int)
	 */
	@Override
	public Socket createSocket(String host, int port, InetAddress localHost,
			int localPort) throws IOException, UnknownHostException {
		return configureSSLSocket(getSSLContext().getSocketFactory().createSocket(host, port, localHost, localPort));
	}

	/**
	 * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int, java.net.InetAddress, int)
	 */
	@Override
	public Socket createSocket(InetAddress address, int port,
			InetAddress localAddress, int localPort) throws IOException {
		return configureSSLSocket(getSSLContext().getSocketFactory().createSocket(address, port, localAddress, localPort));
	}

	/**
	 * @see javax.net.ssl.SSLSocketFactory#getDefaultCipherSuites()
	 */
	@Override
	public String[] getDefaultCipherSuites() {
		return getSSLContext().getSocketFactory().getDefaultCipherSuites();
	}

	/**
	 * @see javax.net.ssl.SSLSocketFactory#getSupportedCipherSuites()
	 */
	@Override
	public String[] getSupportedCipherSuites() {
		return getSSLContext().getSocketFactory().getSupportedCipherSuites();
	}

	/**
	 * This class allow any X509 certificates to be used to authenticate the
	 * remote side of a secure socket, including self-signed certificates.
	 * <p>
	 * 
	 * Note that the tradeoff of this convenience usage is the vulnerability of
	 * man-in-the-middle attacks.
	 */
	public static class TrustAllTrustManager implements X509TrustManager {

		/**
		 * Empty array of certificate authority certificates.
		 */
		private static final X509Certificate[] acceptedIssuers = new X509Certificate[] {};

		/**
		 * Always trust for client SSL chain peer certificate chain with any
		 * authType authentication types.
		 * 
		 * @param chain
		 *            the peer certificate chain.
		 * @param authType
		 *            the authentication type based on the client certificate.
		 */
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
		}

		/**
		 * Always trust for server SSL chain peer certificate chain with any
		 * authType exchange algorithm types.
		 * 
		 * @param chain
		 *            the peer certificate chain.
		 * @param authType
		 *            the key exchange algorithm used.
		 */
		public void checkServerTrusted(X509Certificate[] chain, String authType) {
		}

		/**
		 * Return an empty array of certificate authority certificates which are
		 * trusted for authenticating peers.
		 * 
		 * @return a empty array of issuer certificates.
		 */
		public X509Certificate[] getAcceptedIssuers() {
			return acceptedIssuers;
		}
	}

}
