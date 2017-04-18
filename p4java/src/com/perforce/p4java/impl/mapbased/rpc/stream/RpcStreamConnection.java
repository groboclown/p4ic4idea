/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.stream;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.ProtocolError;
import com.perforce.p4java.exception.SslException;
import com.perforce.p4java.exception.SslHandshakeException;
import com.perforce.p4java.impl.mapbased.rpc.ServerStats;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientTrust;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketPreamble;
import com.perforce.p4java.impl.mapbased.rpc.packet.helper.RpcPacketFieldRule;
import com.perforce.p4java.impl.mapbased.rpc.stream.RpcSocketPool.ShutdownHandler;
import com.perforce.p4java.impl.mapbased.rpc.stream.helper.RpcSocketHelper;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.callback.IFilterCallback;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

/**
 * Socket stream I/O based implementation of the RpcConnection class.<p>
 * 
 * The implementation here uses a small stack of input and output
 * streams based on socket streams at the lowest level, with (at least) an
 * optional connection compression stream on top of that layer, and with
 * charset conversion where necessary.<p>
 */

public class RpcStreamConnection extends RpcConnection {

	public static final String TRACE_PREFIX = "RpcStreamConnection";

	/**
	 * Number of bytes we allocate for initial byte arrays for sending RPC packets.
	 * In general we don't know how big the final buffer is, so this figure is a bit
	 * of a guessed compromise between over-allocation and frequent resizing.
	 */
	protected static final int INITIAL_SENDBUF_SIZE = 2048;
	
	/**
	 * When we run out of send buffer space in putPacket, we allocate another,
	 * larger, buffer; this constant determines how much larger than the existing
	 * buffer the new one should be, or, alternatively, how much bigger than the
	 * incoming field length the new buffer should be. Should probably be more
	 * tunable...
	 */
	protected static final int SENDBUF_REALLOC_INCR = 1024;
	
	private RpcSocketPool pool = null;
	private Socket socket = null;
	private InputStream inputStream = null;
	private OutputStream outputStream = null;
	private InputStream topInputStream = null;
	private OutputStream topOutputStream = null;

	// 'rsh' mode server launch command
	private String rsh = null;

	/**
	 * Construct a new Perforce RPC connection to the named Perforce server
	 * using java.io socket streams at the lowest level. This constructor sets
	 * up the default non-compressed stack; in general this means just a couple
	 * of simple socket streams.
	 * 
	 * @param serverHost 
	 * @param serverPort 
	 * @param props 
	 * @param stats 
	 * @param charset 
	 * @throws ConnectionException 
	 */
	public RpcStreamConnection(String serverHost, int serverPort,
			Properties props, ServerStats stats, Charset charset)
			throws ConnectionException {
		this(serverHost, serverPort, props, stats, charset, (Socket) null);
	}

	/**
	 * Construct a new Perforce RPC connection to the named Perforce server
	 * using java.io socket streams at the lowest level. This constructor sets
	 * up the default non-compressed stack; in general this means just a couple
	 * of simple socket streams.
	 * 
	 * @param serverHost 
	 * @param serverPort 
	 * @param props 
	 * @param stats 
	 * @param charset 
	 * @throws ConnectionException 
	 */
	public RpcStreamConnection(String serverHost, int serverPort,
			Properties props, ServerStats stats, Charset charset, boolean secure)
			throws ConnectionException {
		this(serverHost, serverPort, props, stats, charset, (Socket) null, secure);
	}
	
	/**
	 * Construct a new Perforce RPC connection to the named Perforce server using
	 * java.io socket streams at the lowest level. This constructor sets up the default
	 * non-compressed stack; in general this means just a couple of simple socket streams.
	 * 
	 * @param serverHost 
	 * @param serverPort 
	 * @param props 
	 * @param stats 
	 * @param charset 
	 * @param socket 
	 * @throws ConnectionException 
	 */
	public RpcStreamConnection(String serverHost, int serverPort,
			Properties props, ServerStats stats, Charset charset,
			Socket socket) throws ConnectionException {
		this(serverHost, serverPort, props, stats, charset, socket, false);
	}
	
	/**
	 * Construct a new Perforce RPC connection to the named Perforce server using
	 * java.io socket streams at the lowest level. This constructor sets up the default
	 * non-compressed stack; in general this means just a couple of simple socket streams.
	 * 
	 * @param serverHost 
	 * @param serverPort 
	 * @param props 
	 * @param stats 
	 * @param charset 
	 * @param socket 
	 * @param secure 
	 * @throws ConnectionException 
	 */
	public RpcStreamConnection(String serverHost, int serverPort,
			Properties props, ServerStats stats, Charset charset,
			Socket socket, boolean secure) throws ConnectionException {
		this(serverHost, serverPort, props, stats, charset, socket, null, secure);
	}
	
	/**
	 * Construct a new Perforce RPC connection to the named Perforce server using
	 * java.io socket streams at the lowest level. This constructor sets up the default
	 * non-compressed stack; in general this means just a couple of simple socket streams.
	 * 
	 * @param serverHost 
	 * @param serverPort 
	 * @param props 
	 * @param stats 
	 * @param charset 
	 * @param pool 
	 * @throws ConnectionException 
	 */
	public RpcStreamConnection(String serverHost, int serverPort,
			Properties props, ServerStats stats, Charset charset,
			RpcSocketPool pool) throws ConnectionException {
		this(serverHost, serverPort, props, stats, charset, null, pool, false);
	}

	/**
	 * Construct a new Perforce RPC connection to the named Perforce server using
	 * java.io socket streams at the lowest level. This constructor sets up the default
	 * non-compressed stack; in general this means just a couple of simple socket streams.
	 *
	 * @param serverHost
	 * @param serverPort
	 * @param props
	 * @param stats
	 * @param charset
	 * @param pool 
	 * @param secure
	 * @throws ConnectionException 
	 */
	public RpcStreamConnection(String serverHost, int serverPort,
			Properties props, ServerStats stats, Charset charset,
			RpcSocketPool pool, boolean secure) throws ConnectionException {
		this(serverHost, serverPort, props, stats, charset, null, pool, secure);
	}

	/**
	 * Construct a new Perforce RPC connection to the named Perforce server using
	 * java.io socket streams at the lowest level. This constructor sets up the default
	 * non-compressed stack; in general this means just a couple of simple socket streams.
	 * 
	 * @param serverHost 
	 * @param serverPort 
	 * @param props 
	 * @param stats 
	 * @param charset 
	 * @param socket
	 * @param pool
	 * @param secure
	 * @throws ConnectionException
	 */
	public RpcStreamConnection(String serverHost, int serverPort,
			Properties props, ServerStats stats, Charset charset,
			Socket socket, RpcSocketPool pool, boolean secure) throws ConnectionException {
		this(serverHost, serverPort, props, stats, charset, socket, pool, secure, null);
	}

	/**
	 * Construct a new Perforce RPC connection to the named Perforce server using
	 * java.io socket streams at the lowest level. This constructor sets up the default
	 * non-compressed stack; in general this means just a couple of simple socket streams.
	 *
	 * @param serverHost
	 * @param serverPort
	 * @param props
	 * @param stats
	 * @param charset
	 * @param socket
	 * @param pool
	 * @param secure
	 * @param rsh
	 * @throws ConnectionException
	 */
	public RpcStreamConnection(String serverHost, int serverPort,
			Properties props, ServerStats stats, Charset charset,
			Socket socket, RpcSocketPool pool, boolean secure, String rsh) throws ConnectionException {
		super(serverHost, serverPort, props, stats, charset, secure);
		this.socket = socket;
		this.pool = pool;
		this.rsh = rsh;
		init();
	}

	/**
	 * Initialize actual connection to the server.
	 *
	 * @throws ConnectionException
	 */
	private void init() throws ConnectionException {
		if (this.rsh != null) { // 'rsh' mode server
			try {
				String[] command = new String[]{
						Server.isRunningOnWindows() ? "cmd.exe" : "/bin/sh",
						Server.isRunningOnWindows() ? "/c" : "-c",
						this.rsh};
				ProcessBuilder builder = new ProcessBuilder(command);
				//builder.redirectErrorStream(true); // redirect error stream to output stream
				Process process = builder.start();
				InputStream in = process.getInputStream();
				OutputStream out = process.getOutputStream();
				//InputStream err = process.getErrorStream();

				this.inputStream = new RpcRshInputStream(in, this.stats);
				this.outputStream = new RpcRshOutputStream(out, this.stats);
			// p4ic4idea: never, never, never catch a Throwable
			// unless you're really careful.  This is not being
			// careful.
			// } catch (Throwable thr) {
			} catch (Exception thr) {
				Log.error("Unexpected exception: " + thr.getLocalizedMessage());
				Log.exception(thr);
				// p4ic4idea: show the real source of the problem.
				//throw new ConnectionException(thr.getLocalizedMessage());
				throw new ConnectionException(thr);
			}

		} else { // socket based server
			try {
				if (this.socket == null) {
					if (this.pool != null) {
						this.socket = this.pool.acquire();
					} else {
						this.socket = RpcSocketHelper.createSocket(this.hostName, this.hostPort, this.props, this.secure);
					}
				}
			} catch (UnknownHostException exc) {
				throw new ConnectionException("Unable to resolve Perforce server host name '"
						+ hostName
						+ "' for RPC connection",
						// p4ic4idea: don't mask the source
						exc
						);
			} catch (IOException exc) {
				throw new ConnectionException("Unable to connect to Perforce server at "
						+ hostName + ":" + hostPort,
						// p4ic4idea: don't mask the source
						exc);
			// p4ic4idea: never, never, never catch a Throwable,
			// unless you're really careful, which this is not.
			// } catch (Throwable thr) {
			} catch (Exception thr) {
				Log.error("Unexpected exception: " + thr.getLocalizedMessage());
				Log.exception(thr);
				// p4ic4idea: Don't mask the source
				throw new ConnectionException(thr);
			}

			// Get IP address from socket connection
			if (this.socket != null) {
				if (socket.getInetAddress() != null) {
					InetAddress address = socket.getInetAddress();
					// Check if it is an IPv6 address
					if (Inet6Address.class.isAssignableFrom(address.getClass())) {
						// Add the square brackets for IPv6 address
						this.hostIp = "[" + socket.getInetAddress().getHostAddress() + "]";
					} else {
						this.hostIp = socket.getInetAddress().getHostAddress();
					}
				}
			}

			// Initialize SSL connection
			if (this.secure) {
				initSSL();
			}

			try {
				this.inputStream = new RpcSocketInputStream(this.socket, this.stats);
				this.outputStream = new RpcSocketOutputStream(this.socket, this.stats);
			// p4ic4idea: Never, never, never catch a Throwable
			// unless you're really careful, which this is not.
			// } catch (Throwable thr) {
			} catch (Exception thr) {
				Log.error("Unexpected exception: " + thr.getLocalizedMessage());
				Log.exception(thr);
				// p4ic4idea: Don't hide the source
				throw new ConnectionException(thr);
			}
		}

		this.topInputStream = this.inputStream;
		this.topOutputStream = this.outputStream;
	}

	private void initSSL() throws ConnectionException {
		// Start SSL handshake
		if (this.socket != null) {
			try {
                // The SSLSocket.getSession() method will initiate the initial
                // handshake if necessary. Thus, the SSLSocket.startHandshake()
                // call is not necessary.
                SSLSession sslSession = ((SSLSocket) this.socket).getSession();

                // p4ic4idea: isValid isn't what this thinks it is.  It's checking whether the
                // session is resumeable or not, which isn't what we care about.
                // if (!sslSession.isValid()) {
                // If an error occurs during the initial handshake,
                // this method returns an invalid session object which
                // reports an invalid cipher suite of "SSL_NULL_WITH_NULL_NULL".
                // p4ic4idea: use a more precise exception
                // 	throw new SslException("Error occurred during the SSL handshake: invalid SSL session");
                // }

                // Get the certificates
                Certificate[] serverCerts = sslSession.getPeerCertificates();
                if (serverCerts == null || serverCerts.length == 0 || serverCerts[0] == null) {
                    // p4ic4idea: use a more precise exception
                    throw new SslException(
                            "Error occurred during the SSL handshake: no certificate retrieved from SSL session");
                }
                // Check that the certificate is currently valid. Check the
                // current date and time are within the validity period given
                // in the certificate.
                try {
                    ((X509Certificate) serverCerts[0]).checkValidity();
                } catch (CertificateExpiredException e) {
                    // p4ic4idea: use a more precise exception
                    throw new SslException(
                            "Error occurred during the SSL handshake: certificate expired: " + e.toString(), e);
                } catch (CertificateNotYetValidException e) {
                    // p4ic4idea: use a more precise exception
                    throw new SslException(
                            "Error occurred during the SSL handshake: certificate not yet valid: " + e.toString(), e);
                }
                // Get the public key from the first certificate
                PublicKey serverPubKey = serverCerts[0].getPublicKey();
                if (serverPubKey == null) {
                    // p4ic4idea: use a more precise exception
                    throw new SslException(
                            "Error occurred during the SSL handshake: no public key retrieved from server certificate");
                }
                // Generate the fingerprint
                try {
                    this.fingerprint = ClientTrust.generateFingerprint(serverPubKey);
                    //this.fingerprint = ClientTrust.generateFingerprint((X509Certificate)this.serverCerts[0]);
                } catch (NoSuchAlgorithmException e) {
                    // p4ic4idea: use a more precise exception
                    throw new SslException(
                            "Error occurred while generating the fingerprint for the Perforce SSL connection", e);
                    //} catch (CertificateEncodingException e) {
                    //	throw new ConnectionException("Error occurred while generating the fingerprint for the Perforce SSL connection", e);
                }
            } catch (SSLPeerUnverifiedException e) {
				// p4ic4idea: fix typo
				String message = "Error occurred during SSL handshake. "
						+ "Please check the release notes for known SSL issues: "
						+ e.getLocalizedMessage();
				Log.error(message);
				Log.exception(e);
				// p4ic4idea: use a more precise exception
                // Because this occurred from within one of the API calls, and not in
                // the processing of the API results, we use the special handshake exception.
				throw new SslHandshakeException(message, e);
			}
		}
	}

	/**
	 * @see com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection#getServerIpPort()
	 */
	public String getServerIpPort() {
        String serverIpPort = null;
		// p4ic4idea: compare strings with "equals"
        if (! this.hostIp.equals(UNKNOWN_SERVER_HOST)) {
        	serverIpPort = this.hostIp;
            if (this.hostPort != UNKNOWN_SERVER_PORT) {
            	serverIpPort += ":" + Integer.toString(this.hostPort);
            }
        } else if (this.hostPort != UNKNOWN_SERVER_PORT) {
        	serverIpPort = Integer.toString(this.hostPort);
        }
        return serverIpPort;
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection#disconnect(RpcPacketDispatcher)
	 */
	public void disconnect(final RpcPacketDispatcher dispatcher) throws ConnectionException {
		try {
			// NOTE: don't do gratuitous (any) flushes here -- this has all been
			// handled already and will often cause errors in compressed client
			// connection setups -- HR.
			
			ShutdownHandler handler = new ShutdownHandler() {
				
				public void shutdown(Socket socket) {
					if (dispatcher != null) {
						try {
							dispatcher.shutdown(RpcStreamConnection.this);
						} catch (ConnectionException e) {
							Log.exception(e);
						}
					}					
				}
			};
			// Handle 'rsh' mode server shutdown
			if (this.rsh != null) {
				try {
					dispatcher.shutdown(RpcStreamConnection.this);
				} catch (ConnectionException e) {
					Log.exception(e);
				}
				this.topInputStream.close();
				this.topOutputStream.close();
			} else {
				if (this.pool != null) {
					this.pool.release(this.socket, handler);
				} else {
					handler.shutdown(this.socket);
					this.topInputStream.close();
					this.topOutputStream.close();
					if (socket != null) {
						this.socket.close();
					}
				}
			}
		} catch (IOException exc) {
			throw new ConnectionException(
					"RPC disconnection error: " + exc.getLocalizedMessage(), exc);
		}
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection#useConnectionCompression()
	 */
	@Override
	public void useConnectionCompression() throws ConnectionException {
		if (!this.usingCompression) {
			super.useConnectionCompression();
			
			try {
				this.topOutputStream.flush();
				// We do this here immediately to avoid having the compress2 itself
				// compressed...
				this.putRpcPacket(RpcPacket.constructRpcPacket(
									RpcFunctionSpec.PROTOCOL_COMPRESS2,
									"compress2",
									(String[]) null, null));
				this.topOutputStream.flush();
				this.topOutputStream = new RpcGZIPOutputStream(this.outputStream);
				this.topInputStream = new RpcGZIPInputStream(this.inputStream);
			} catch (IOException exc) {
				Log.error("I/O exception encountered while setting up GZIP streaming: "
						+ exc.getLocalizedMessage());
				Log.exception(exc);
				throw new ConnectionException(
						"unable to set up client compression streaming to Perforce server: "
						+ exc.getLocalizedMessage(), exc);
			}
		}
	}

	/**
	 * Get a Perforce RPC packet from the underlying stream. If we're talking
	 * to a Unicode-enabled Perforce server, we attempt to translate the incoming
	 * bytes to the relevant client-side charsets where appropriate based on packet
	 * field type, etc.
	 */
	public RpcPacket getRpcPacket() throws ConnectionException {
		return getRpcPacket(null, null);
	}

	/**
	 * Get a Perforce RPC packet from the underlying stream with an optional
	 * rule to handle the RPC packet fields.
	 */
	public RpcPacket getRpcPacket(RpcPacketFieldRule fieldRule, IFilterCallback filterCallback) throws ConnectionException {
		
		byte[] preambleBytes = new byte[RpcPacketPreamble.RPC_PREAMBLE_SIZE];
		RpcPacket packet = null;
		
		try {
			int bytesRead = this.topInputStream.read(preambleBytes);
			this.stats.streamRecvs.incrementAndGet();
			
			if (bytesRead < 0) {
				throw new ConnectionException("server connection unexpectedly closed");
			}
			
			// If we get a partial read, try again until something goes wrong...
			
			while ((bytesRead >= 0) && (bytesRead < preambleBytes.length)) {
				
				int moreBytesRead = this.topInputStream.read(preambleBytes, bytesRead,
												preambleBytes.length - bytesRead);
				this.stats.streamRecvs.incrementAndGet();
				if (moreBytesRead < 0) {
					throw new ConnectionException("server connection unexpectedly closed");
				} else {
					bytesRead += moreBytesRead;
				}
			}
			
			this.stats.totalBytesRecv.getAndAdd(bytesRead);
			
			if (bytesRead != preambleBytes.length) {
				throw new ConnectionException(
						"Incomplete RPC packet preamble read from Perforce server; connection probably broken."
						+ " bytes read: " + bytesRead);
			}
			
			RpcPacketPreamble preamble = RpcPacketPreamble.retrievePreamble(preambleBytes);
			
			if (preamble == null) {
				throw new ProtocolError("Null RPC packet preamble in byte buffer");
			} else if (!preamble.isValidChecksum()) {
				throw new ProtocolError("Bad checksum in RPC preamble");
			}
			
			int payloadLength = preamble.getPayloadSize(); // Note: size is for the *rest of the packet*...
			// FIXME: really should sanity check the size better here -- HR.
			
			if (payloadLength <= 0) {
				throw new ProtocolError("Bad payload size in RPC preamble: " + payloadLength);
			}
						
			// We know how many bytes to expect for the rest of this packet, so
			// try to read this in. This can be a ginormous packet in some pathological
			// cases, so we need to be flexible...
			
			byte[] packetBytes = new byte[payloadLength];
			
			int packetBytesRead = this.topInputStream.read(packetBytes, 0, payloadLength);
			this.stats.streamRecvs.incrementAndGet();
			this.stats.totalBytesRecv.getAndAdd(packetBytesRead);
			
			if (packetBytesRead <=0) {
				throw new ConnectionException(
						"Perforce server network connection closed unexpectedly");
			} else {
				while (packetBytesRead < payloadLength) {
					// Incomplete read; just try until we get a complete or something goes wrong...
					this.stats.incompleteReads.incrementAndGet();
					int moreBytesRead = this.topInputStream.read(packetBytes,
												packetBytesRead, payloadLength - packetBytesRead);
					this.stats.streamRecvs.incrementAndGet();
					this.stats.totalBytesRecv.getAndAdd(moreBytesRead);
					
					if (moreBytesRead < 0) {
						throw new ConnectionException(
						"Perforce server network connection closed unexpectedly");
					}
					packetBytesRead += moreBytesRead;
				}
			}
			
			if (packetBytesRead != payloadLength) {
				throw new P4JavaError("RPC packet payload read size mismatch; expected: "
						+ payloadLength + "; got: " + packetBytesRead);
			}
			
			packet = RpcPacket.constructRpcPacket(preamble, packetBytes, this.unicodeServer,
													this.clientCharset, fieldRule, filterCallback);
			
			this.stats.packetsRecv.incrementAndGet();
			this.stats.largestRpcPacketRecv.set(Math.max(this.stats.largestRpcPacketRecv.get(),	packet.getPacketLength()));

		} catch (IOException exc) {
			throw new ConnectionException(exc);
		} catch (ConnectionException p4jexc) {
			// Just passing through...
			throw p4jexc;
		} catch (P4JavaError p4je) {
			// Just passing through...
			throw p4je;
		} catch (Throwable thr) {
			// Never a good sign; typically a buffer overflow or positioning
			// problem, and almost always unrecoverable.
			
			Log.error("Unexpected exception: " + thr.getLocalizedMessage());
			Log.exception(thr);
			throw new P4JavaError(thr.getLocalizedMessage(), thr);
		}
		
		return packet;
	}
	
	/**
	 * Put a Perforce RPC packet onto the output stream. In some cases this
	 * may require considerable processing and things like charset translation
	 * here and downstream, but it's normally fairly straightforward.
	 */
	public long putRpcPacket(RpcPacket packet) throws ConnectionException {
		
		// Note that in general, we don't know how large the packet's output byte
		// buffer is going to have to be until we've finished the packet contents
		// marshaling, so we implement buffer resizing when needed. Our initial
		// guess is INITIAL_SENDBUF_SIZE bytes; we grow the buffer by increasing
		// it SENDBUF_REALLOC_INCR times each buffer increase.
		
		byte[] sendBytes = new byte[INITIAL_SENDBUF_SIZE];
		int sendPos = 0;
		
		if (packet == null) {
			throw new NullPointerError(
					"null RPC packet passed to RpcStreamConnection.putPacket");
		}

		if (packet.getFuncNameString() == null) {
			throw new P4JavaError("Unmapped / unmappable function in RpcPacket.put()");
		}
		
		// Skip over the first few bytes for the preamble, which we'll
		// come back to fill in later when we know the marshaled length.
		
		sendPos += RpcPacketPreamble.RPC_PREAMBLE_SIZE;
		
		Map<String, Object> mapArgs = packet.getMapArgs();
		String[] strArgs = packet.getStrArgs();
		
		if (mapArgs != null) {
			for (Map.Entry<String, Object> entry : mapArgs.entrySet()) {
				byte[] fieldBytes = marshalPacketField(entry.getKey(), entry.getValue());
				
				if ((sendBytes.length - sendPos)  <= fieldBytes.length) {
					this.stats.bufferCompacts.getAndIncrement(); // We're overloading the meaning here...
					int newBytesLength = sendBytes.length + fieldBytes.length + SENDBUF_REALLOC_INCR;
					byte[] newBytes = new byte[newBytesLength];
					System.arraycopy(sendBytes, 0, newBytes, 0, sendPos);
					sendBytes = newBytes;
				}
				
				System.arraycopy(fieldBytes, 0, sendBytes, sendPos, fieldBytes.length);
				sendPos += fieldBytes.length;
			}
		}
		
		if (strArgs != null) {
			for (String arg : strArgs) {	
				if (arg != null) {
					byte[] fieldBytes = marshalPacketField(null, arg);
					
					if ((sendBytes.length - sendPos)  <= fieldBytes.length) {
						this.stats.bufferCompacts.getAndIncrement(); // We're overloading the meaning here...
						int newBytesLength = sendBytes.length + fieldBytes.length + SENDBUF_REALLOC_INCR;
						byte[] newBytes = new byte[newBytesLength];
						System.arraycopy(sendBytes, 0, newBytes, 0, sendPos);
						sendBytes = newBytes;
					}
					
					System.arraycopy(fieldBytes, 0, sendBytes, sendPos, fieldBytes.length);
					sendPos += fieldBytes.length;
				}
			}
		}
		
		if (packet.getEnv() != null) {
			byte[] envBytes = packet.getEnv().marshal();
			
			if ((sendBytes.length - sendPos)  <= envBytes.length) {
				this.stats.bufferCompacts.getAndIncrement(); // We're overloading the meaning here...
				int newBytesLength = sendBytes.length + envBytes.length + SENDBUF_REALLOC_INCR;
				byte[] newBytes = new byte[newBytesLength];
				System.arraycopy(sendBytes, 0, newBytes, 0, sendPos);
				sendBytes = newBytes;
			}
			
			System.arraycopy(envBytes, 0, sendBytes, sendPos, envBytes.length);
			sendPos += envBytes.length;
		}
		
		byte[] nameBytes = marshalPacketField(RpcFunctionMapKey.FUNCTION,
												packet.getFuncNameString());
		
		if ((sendBytes.length - sendPos)  <= nameBytes.length) {
			this.stats.bufferCompacts.getAndIncrement(); // We're overloading the meaning here...
			int newBytesLength = sendBytes.length + nameBytes.length;
			byte[] newBytes = new byte[newBytesLength];
			System.arraycopy(sendBytes, 0, newBytes, 0, sendPos);
			sendBytes = newBytes;
		}
		
		System.arraycopy(nameBytes, 0, sendBytes, sendPos, nameBytes.length);
		sendPos += nameBytes.length;
		
		// Now go back and calculate the preamble bytes...
		
		byte[] preambleBytes = RpcPacketPreamble.constructPreamble(
								sendPos - RpcPacketPreamble.RPC_PREAMBLE_SIZE).marshalAsBytes();
		
		System.arraycopy(preambleBytes, 0, sendBytes, 0, preambleBytes.length);
		
		// Now let's try sending it downstream and see what happens...
		
		try {
			this.topOutputStream.write(sendBytes, 0, sendPos);
			this.topOutputStream.flush();
			this.stats.streamSends.incrementAndGet();
			this.stats.totalBytesSent.getAndAdd(sendPos);
			this.stats.packetsSent.incrementAndGet();
			if (this.stats.largestRpcPacketSent.get() < sendPos) {
				this.stats.largestRpcPacketSent.set(sendPos);
			}
		} catch (IOException exc) {
			Log.exception(exc);
			StringBuilder message = new StringBuilder();
			if (exc instanceof SocketTimeoutException && this.secure) {
				message.append(
						MessageFormat
								.format("SSL connect to ssl:{0}:{1,number,#} failed.\nRemove SSL protocol prefix.\n",
										this.hostName, this.hostPort));
			} else {
				message.append("Unable to send command to Perforce server: ");
			}
			message.append(exc.getMessage());
			throw new ConnectionException(message.toString(), exc);
		}
		
		return 0;
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection#putRpcPackets(com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket[])
	 */
	public long putRpcPackets(RpcPacket[] packets) throws ConnectionException {
		int retVal = 0;
		
		if (packets == null) {
			throw new NullPointerError(
					"Null RPC packets passed to RpcStreamConnection.putPacket");
		}
		
		for (RpcPacket packet : packets) {
			if (packet != null) {
				retVal += putRpcPacket(packet);
			}
		}
		
		return retVal;
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection#getSystemSendBufferSize()
	 */
	public int getSystemSendBufferSize() {
		if (this.socket != null) {
			try {
				return this.socket.getSendBufferSize();
			} catch (SocketException exc) {
				Log.error("unexpected exception: " + exc.getLocalizedMessage());
				Log.exception(exc);
			}
		}
		
		return 0;
	}
	
	/**
	 * @see com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection#getSystemRecvBufferSize()
	 */
	public int getSystemRecvBufferSize() {
		if (this.socket != null) {
			try {
				return this.socket.getReceiveBufferSize();
			} catch (SocketException exc) {
				Log.error("unexpected exception: " + exc.getLocalizedMessage());
				Log.exception(exc);
			}
		}
		
		return 0;
	}
}
