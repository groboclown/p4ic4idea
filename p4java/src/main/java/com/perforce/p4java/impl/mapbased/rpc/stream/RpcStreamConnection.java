/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.stream;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwConnectionException;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwConnectionExceptionIfConditionFails;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwP4JavaError;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwP4JavaErrorIfConditionFails;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwProtocolErrorIfConditionFails;
import static com.perforce.p4java.common.base.StringHelper.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaError;

// p4ic4idea: use more precise exceptions
import com.perforce.p4java.exception.SslException;
import com.perforce.p4java.exception.SslHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import com.perforce.p4java.impl.mapbased.rpc.ExternalEnv;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Socket stream I/O based implementation of the RpcConnection class.
 * <p>
 *
 * The implementation here uses a small stack of input and output streams based
 * on socket streams at the lowest level, with (at least) an optional connection
 * compression stream on top of that layer, and with charset conversion where
 * necessary.
 * <p>
 */
public class RpcStreamConnection extends RpcConnection {
    public static final String TRACE_PREFIX = "RpcStreamConnection";

    /**
     * Number of bytes we allocate for initial byte arrays for sending RPC
     * packets. In general we don't know how big the final buffer is, so this
     * figure is a bit of a guessed compromise between over-allocation and
     * frequent resizing.
     */
    protected static final int INITIAL_SENDBUF_SIZE = 2048;

    /**
     * When we run out of send buffer space in putPacket, we allocate another,
     * larger, buffer; this constant determines how much larger than the
     * existing buffer the new one should be, or, alternatively, how much bigger
     * than the incoming field length the new buffer should be. Should probably
     * be more tunable...
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
     */
    public RpcStreamConnection(String serverHost, int serverPort, Properties props,
            ServerStats stats, Charset charset) throws ConnectionException {
        this(serverHost, serverPort, props, stats, charset, (Socket) null);
    }

    /**
     * Construct a new Perforce RPC connection to the named Perforce server
     * using java.io socket streams at the lowest level. This constructor sets
     * up the default non-compressed stack; in general this means just a couple
     * of simple socket streams.
     */
    public RpcStreamConnection(String serverHost, int serverPort, Properties props,
            ServerStats stats, Charset charset, boolean secure) throws ConnectionException {
        this(serverHost, serverPort, props, stats, charset, (Socket) null, secure);
    }

    /**
     * Construct a new Perforce RPC connection to the named Perforce server
     * using java.io socket streams at the lowest level. This constructor sets
     * up the default non-compressed stack; in general this means just a couple
     * of simple socket streams.
     */
    public RpcStreamConnection(String serverHost, int serverPort, Properties props,
            ServerStats stats, Charset charset, Socket socket) throws ConnectionException {
        this(serverHost, serverPort, props, stats, charset, socket, false);
    }

    /**
     * Construct a new Perforce RPC connection to the named Perforce server
     * using java.io socket streams at the lowest level. This constructor sets
     * up the default non-compressed stack; in general this means just a couple
     * of simple socket streams.
     */
    public RpcStreamConnection(String serverHost, int serverPort, Properties props,
            ServerStats stats, Charset charset, RpcSocketPool pool) throws ConnectionException {
        this(serverHost, serverPort, props, stats, charset, null, pool, false);
    }

    /**
     * Construct a new Perforce RPC connection to the named Perforce server
     * using java.io socket streams at the lowest level. This constructor sets
     * up the default non-compressed stack; in general this means just a couple
     * of simple socket streams.
     */
    public RpcStreamConnection(String serverHost, int serverPort, Properties props,
            ServerStats stats, Charset charset, Socket socket, boolean secure)
            throws ConnectionException {
        this(serverHost, serverPort, props, stats, charset, socket, null, secure);
    }

    /**
     * Construct a new Perforce RPC connection to the named Perforce server
     * using java.io socket streams at the lowest level. This constructor sets
     * up the default non-compressed stack; in general this means just a couple
     * of simple socket streams.
     */
    public RpcStreamConnection(String serverHost, int serverPort, Properties props,
            ServerStats stats, Charset charset, RpcSocketPool pool, boolean secure)
            throws ConnectionException {
        this(serverHost, serverPort, props, stats, charset, null, pool, secure);
    }

    /**
     * Construct a new Perforce RPC connection to the named Perforce server
     * using java.io socket streams at the lowest level. This constructor sets
     * up the default non-compressed stack; in general this means just a couple
     * of simple socket streams.
     */
    public RpcStreamConnection(String serverHost, int serverPort, Properties props,
            ServerStats stats, Charset charset, Socket socket, RpcSocketPool pool, boolean secure)
            throws ConnectionException {
        this(serverHost, serverPort, props, stats, charset, socket, pool, secure, null);
    }

    /**
     * Construct a new Perforce RPC connection to the named Perforce server
     * using java.io socket streams at the lowest level. This constructor sets
     * up the default non-compressed stack; in general this means just a couple
     * of simple socket streams.
     */
    public RpcStreamConnection(String serverHost, int serverPort, Properties props,
            ServerStats stats, Charset charset, Socket socket, RpcSocketPool pool, boolean secure,
            String rsh) throws ConnectionException {
        super(serverHost, serverPort, props, stats, charset, secure);
        this.socket = socket;
        this.pool = pool;
        this.rsh = rsh;
        init();
    }

    /**
     * Initialize actual connection to the server.
     */
    private void init() throws ConnectionException {
        if (isNotBlank(rsh)) {
            initRshModeServer();
        } else {
            initSocketBasedServer();
        }

        topInputStream = inputStream;
        topOutputStream = outputStream;
    }

    private void initRshModeServer() throws ConnectionException {
        try {
            String[] command = new String[] { Server.isRunningOnWindows() ? "cmd.exe" : "/bin/sh",
                    Server.isRunningOnWindows() ? "/c" : "-c", rsh };

            ProcessBuilder builder = new ProcessBuilder(command);
            // builder.redirectErrorStream(true); // redirect error stream to
            // output stream
            Process process = builder.start();
            InputStream in = process.getInputStream();
            OutputStream out = process.getOutputStream();
            // InputStream err = process.getErrorStream();

            inputStream = new RpcRshInputStream(in, stats);
            outputStream = new RpcRshOutputStream(out, stats);
	// p4ic4idea: never, never, never catch a Throwable,
	// unless you're really careful, which this is not.
	// } catch (Throwable thr) {
	} catch (Exception thr) {
            Log.error("Unexpected exception: %s", thr.getLocalizedMessage());
            Log.exception(thr);
            throwConnectionException(thr);
        }
    }

    private void initSocketBasedServer() throws ConnectionException {
        try {
            if (isNull(socket)) {
                if (nonNull(pool)) {
                    socket = pool.acquire();
                } else {
                    socket = RpcSocketHelper.createSocket(hostName, hostPort, props, secure);
                }
            }
        } catch (UnknownHostException exc) {
            throwConnectionException(exc,
                    "Unable to resolve Perforce server host name '%s' for RPC connection",
                    hostName);
        } catch (IOException exc) {
            throwConnectionException(exc, "Unable to connect to Perforce server at %s:%s", hostName,
                    hostPort);
	// p4ic4idea: never, never, never catch a Throwable,
	// unless you're really careful, which this is not.
	// } catch (Throwable thr) {
	} catch (Exception thr) {
            Log.error("Unexpected exception: %s", thr.getLocalizedMessage());
            Log.exception(thr);
            throwConnectionException(thr);
        }

        getIpAddressFromSocketConnection();

        // Initialize SSL connection
        if (secure) {
            initSSL();
        }

        initRpcSocketInputAndOutputStreamIfSocketBasedServer();
    }

    private void getIpAddressFromSocketConnection() {
        if (nonNull(socket)) {
            InetAddress inetAddress = socket.getInetAddress();
            if (nonNull(inetAddress)) {
                // Check if it is an IPv6 address
                String hostAddress = inetAddress.getHostAddress();
                if (Inet6Address.class.isAssignableFrom(inetAddress.getClass())) {
                    // Add the square brackets for IPv6 address
                    hostIp = "[" + hostAddress + "]";
                } else {
                    hostIp = hostAddress;
                }
            }
            if (socket.isBound()) {
                InetAddress address = socket.getLocalAddress();
                // Check if it is an IPv6 address
                if (Inet6Address.class.isAssignableFrom(address.getClass())) {
                    // Add the square brackets for IPv6 address
                    this.ourIp = "[" + address.getHostAddress() + "]";
                } else {
                    this.ourIp = address.getHostAddress();
                }
                this.ourPort = socket.getLocalPort();
            }

        }
    }

    private void initSSL() throws ConnectionException {
        // Start SSL handshake
        if (nonNull(socket)) {
            try {
                // The SSLSocket.getSession() method will initiate the initial
                // handshake if necessary. Thus, the SSLSocket.startHandshake()
                // call is not necessary.
                SSLSession sslSession = ((SSLSocket) socket).getSession();

                // p4ic4idea: isValid isn't what this thinks it is.  It's checking whether the
                // session is resumeable or not, which isn't what we care about.
                // Even if it is valid, it should be throwing the more precise
                // exception SslException
                /*
                 * If an error occurs during the initial handshake, this method
                 * returns an invalid session object which reports an invalid
                 * cipher suite of "SSL_NULL_WITH_NULL_NULL".
                 */
                //throwConnectionExceptionIfConditionFails(sslSession.isValid(),
                //        "Error occurred during the SSL handshake: invalid SSL session");

                // Get the certificates
                Certificate[] serverCerts = sslSession.getPeerCertificates();
                // p4ic4idea: use a more precise exception
                //throwConnectionExceptionIfConditionFails(
                //        nonNull(serverCerts) && (serverCerts.length != 0)
                //                && nonNull(serverCerts[0]),
                //        "Error occurred during the SSL handshake: no certificate retrieved from SSL session");
                if (isNull(serverCerts) || (serverCerts.length <= 0) || isNull(serverCerts[0])) {
                    throw new SslException(
                            "Error occurred during the SSL handshake: no certificate retrieved from SSL session");
                }
                // Check that the certificate is currently valid. Check the
                // current date and time are within the validity period given
                // in the certificate.
                ((X509Certificate) serverCerts[0]).checkValidity();

                // Get the public key from the first certificate
                PublicKey serverPubKey = serverCerts[0].getPublicKey();
                // p4ic4idea: use a more precise exception
                //throwConnectionExceptionIfConditionFails(nonNull(serverPubKey),
                //        "Error occurred during the SSL handshake: no public key retrieved from server certificate");
                if (isNull(serverPubKey)) {
                    throw new SslException(
                            "Error occurred during the SSL handshake: no public key retrieved from server certificate");
                }
                // Generate the fingerprint
                fingerprint = ClientTrust.generateFingerprint(serverPubKey);
            } catch (CertificateExpiredException e) {
                // p4ic4idea: use a more precise exception
                throw new SslException(
                        "Error occurred during the SSL handshake: certificate expired",
			e);
            } catch (CertificateNotYetValidException e) {
                // p4ic4idea: use a more precise exception
                throw new SslException(
                        "Error occurred during the SSL handshake: certificate not yet valid",
			e);
            } catch (NoSuchAlgorithmException e) {
                // p4ic4idea: use a more precise exception
                throw new SslHandshakeException(
                        "Error occurred while generating the fingerprint for the Perforce SSL connection",
                        e);
            } catch (SSLPeerUnverifiedException e) {
                // p4ic4idea: use a more precise exception
                throw new SslHandshakeException("Error occurred while verifying the SSL peer.", e);
            }
        }
    }

    private void initRpcSocketInputAndOutputStreamIfSocketBasedServer() throws ConnectionException {
        try {
            inputStream = new RpcSocketInputStream(socket, stats);
            outputStream = new RpcSocketOutputStream(socket, stats);
        // p4ic4idea: never catch Throwable unless you're really careful
	// } catch (Throwable thr) {
	} catch (Exception thr) {
            Log.error("Unexpected exception: %s", thr.getLocalizedMessage());
            Log.exception(thr);
            throwConnectionException(thr);
        }
    }

    public String getServerIpPort() {
        String serverIpPort = null;
        if (!StringUtils.equals(hostIp, UNKNOWN_SERVER_HOST)) {
            serverIpPort = hostIp;
            if (hostPort != UNKNOWN_SERVER_PORT) {
		// p4ic4idea: use the more efficient Integer.valueOf
                serverIpPort += ":" + Integer.valueOf(hostPort);
            }
        } else if (hostPort != UNKNOWN_SERVER_PORT) {
            serverIpPort = Integer.toString(hostPort);
        }
        return serverIpPort;
    }

    /**
     * @see com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection#getClientIpPort()
     */
    public String getClientIpPort() {
        String clientIpPort = null;
        if (this.ourIp != UNKNOWN_SERVER_HOST) {
            clientIpPort = this.ourIp;
            if (this.ourPort != UNKNOWN_SERVER_PORT) {
                clientIpPort += ":" + Integer.toString(this.ourPort);
            }
        } else if (this.ourPort != UNKNOWN_SERVER_PORT) {
            clientIpPort = Integer.toString(this.ourPort);
        }
        return clientIpPort;
    }

    public void disconnect(final RpcPacketDispatcher dispatcher) throws ConnectionException {
        try {
            // NOTE: don't do gratuitous (any) flushes here -- this has all been
            // handled already and will often cause errors in compressed client
            // connection setups -- HR.
            ShutdownHandler handler = new ShutdownHandler() {
                @Override
                public void shutdown(Socket theSocket) {
                    if (nonNull(dispatcher)) {
                        try {
                            dispatcher.shutdown(RpcStreamConnection.this);
                        } catch (ConnectionException e) {
                            Log.exception(e);
                        }
                    }
                }
            };
            // Handle 'rsh' mode server shutdown
            if (isNotBlank(rsh)) {
                try {
                    dispatcher.shutdown(RpcStreamConnection.this);
                } catch (ConnectionException e) {
                    Log.exception(e);
                }
                topInputStream.close();
                topOutputStream.close();
            } else {
                if (nonNull(pool)) {
                    pool.release(socket, handler);
                } else {
                    handler.shutdown(socket);
                    topInputStream.close();
                    topOutputStream.close();
                    if (nonNull(socket)) {
                        socket.close();
                    }
                }
            }
        } catch (IOException exc) {
            throwConnectionException(exc, "RPC disconnection error: %s", exc.getLocalizedMessage());
        }
    }

    /**
     * Get a Perforce RPC packet from the underlying stream. If we're talking to
     * a Unicode-enabled Perforce server, we attempt to translate the incoming
     * bytes to the relevant client-side charsets where appropriate based on
     * packet field type, etc.
     */
    public RpcPacket getRpcPacket() throws ConnectionException {
        return getRpcPacket(null, null);
    }

    /**
     * Get a Perforce RPC packet from the underlying stream with an optional
     * rule to handle the RPC packet fields.
     */
    public RpcPacket getRpcPacket(final RpcPacketFieldRule fieldRule,
            final IFilterCallback filterCallback) throws ConnectionException {
        byte[] preambleBytes = new byte[RpcPacketPreamble.RPC_PREAMBLE_SIZE];
        RpcPacket packet = null;

        try {
            int bytesRead = topInputStream.read(preambleBytes);
            throwConnectionExceptionIfConditionFails(bytesRead >= 0,
                    "server connection unexpectedly closed");
            AtomicLong streamRecvs = stats.streamRecvs;
            streamRecvs.incrementAndGet();

            bytesRead = continueReadIfGetPartialRead(preambleBytes, bytesRead, streamRecvs);
            throwConnectionExceptionIfConditionFails(bytesRead == preambleBytes.length,
                    "Incomplete RPC packet preamble read from Perforce server; connection probably broken. bytes read: %s",
                    bytesRead);
            stats.totalBytesRecv.getAndAdd(bytesRead);

            RpcPacketPreamble preamble = RpcPacketPreamble.retrievePreamble(preambleBytes);
            throwProtocolErrorIfConditionFails(preamble.isValidChecksum(),
                    "Bad checksum in RPC preamble");

            int payloadLength = preamble.getPayloadSize(); // Note: size is for
                                                           // the *rest of the
                                                           // packet*...
            // FIXME: really should sanity check the size better here -- HR.
            throwProtocolErrorIfConditionFails(payloadLength > 0,
                    "Bad payload size in RPC preamble: %s", payloadLength);

            // We know how many bytes to expect for the rest of this packet, so
            // try to read this in. This can be a ginormous packet in some
            // pathological
            // cases, so we need to be flexible...
            byte[] packetBytes = new byte[payloadLength];
            int packetBytesRead = topInputStream.read(packetBytes, 0, payloadLength);
            throwConnectionExceptionIfConditionFails(packetBytesRead > 0,
                    "Perforce server network connection closed unexpectedly");
            streamRecvs.incrementAndGet();
            stats.totalBytesRecv.getAndAdd(packetBytesRead);

            packetBytesRead = continueReadIfIncompleteRead(streamRecvs, payloadLength, packetBytes,
                    packetBytesRead);
            throwP4JavaErrorIfConditionFails(packetBytesRead == payloadLength,
                    "RPC packet payload read size mismatch; expected: %s; got: %s", payloadLength,
                    packetBytesRead);

            packet = RpcPacket.constructRpcPacket(preamble, packetBytes, unicodeServer,
                    clientCharset, fieldRule, filterCallback);
            stats.packetsRecv.incrementAndGet();
            stats.largestRpcPacketRecv
                    .set(Math.max(stats.largestRpcPacketRecv.get(), packet.getPacketLength()));
        } catch (IOException exc) {
            throwConnectionException(exc);
        } catch (ConnectionException | P4JavaError p4jexc) {
            throw p4jexc;
	// p4ic4idea: never catch Throwable unless you're really careful
        //} catch (Throwable thr) {
	} catch (Exception thr) {
            // Never a good sign; typically a buffer overflow or positioning
            // problem, and almost always unrecoverable.
            Log.error("Unexpected exception: %s", thr.getLocalizedMessage());
            Log.exception(thr);
            throwP4JavaError(thr, thr.getLocalizedMessage());
        }

        return packet;
    }

    /**
     * If we get a partial read, try again until something goes wrong...
     */
    private int continueReadIfGetPartialRead(@Nonnull final byte[] preambleBytes,
            final int bytesRead, @Nonnull final AtomicLong streamRecvs)
            throws IOException, ConnectionException {
        int totalBytesRead = bytesRead;
        while ((totalBytesRead >= 0) && (totalBytesRead < preambleBytes.length)) {
            int moreBytesRead = topInputStream.read(preambleBytes, totalBytesRead,
                    preambleBytes.length - totalBytesRead);
            throwConnectionExceptionIfConditionFails(moreBytesRead >= 0,
                    "server connection unexpectedly closed");

            streamRecvs.incrementAndGet();
            totalBytesRead += moreBytesRead;
        }
        return totalBytesRead;
    }

    /**
     * Incomplete read; just try until we get a complete or something goes
     * wrong...
     */
    private int continueReadIfIncompleteRead(@Nonnull final AtomicLong streamRecvs,
            final int payloadLength, @Nonnull final byte[] packetBytes, final int packetBytesRead)
            throws IOException, ConnectionException {
        int totalPacketBytesRead = packetBytesRead;
        while (totalPacketBytesRead < payloadLength) {
            stats.incompleteReads.incrementAndGet();
            int moreBytesRead = topInputStream.read(packetBytes, totalPacketBytesRead,
                    payloadLength - totalPacketBytesRead);
            throwConnectionExceptionIfConditionFails(moreBytesRead >= 0,
                    "Perforce server network connection closed unexpectedly");

            streamRecvs.incrementAndGet();
            stats.totalBytesRecv.getAndAdd(moreBytesRead);
            totalPacketBytesRead += moreBytesRead;
        }
        return totalPacketBytesRead;
    }

    public int getSystemRecvBufferSize() {
        if (nonNull(socket)) {
            try {
                return socket.getReceiveBufferSize();
            } catch (SocketException exc) {
                Log.error("unexpected exception: %s", exc.getLocalizedMessage());
                Log.exception(exc);
            }
        }

        return 0;
    }

    public int getSystemSendBufferSize() {
        if (nonNull(socket)) {
            try {
                return socket.getSendBufferSize();
            } catch (SocketException exc) {
                Log.error("unexpected exception: %s", exc.getLocalizedMessage());
                Log.exception(exc);
            }
        }

        return 0;
    }

    public long putRpcPackets(@Nonnull RpcPacket[] packets) throws ConnectionException {
        Validate.notNull(packets);
        int retVal = 0;

        for (RpcPacket packet : packets) {
            if (nonNull(packet)) {
                retVal += putRpcPacket(packet);
            }
        }
        return retVal;
    }

    /**
     * Put a Perforce RPC packet onto the output stream. In some cases this may
     * require considerable processing and things like charset translation here
     * and downstream, but it's normally fairly straightforward.
     *
     * <pre>
     * <strong>Note</strong> that in general, we don't know how large the packet's output byte
     * buffer is going to have to be until we've finished the packet contents
     * marshaling, so we implement buffer resizing when needed. Our initial
     * guess is <code>INITIAL_SENDBUF_SIZE</code> bytes; we grow the buffer by increasing
     * it <code>SENDBUF_REALLOC_INCR</code> times each buffer increase.
     * </pre>
     */
    public long putRpcPacket(@Nonnull RpcPacket packet) throws ConnectionException {
        Validate.notNull(packet);
        throwP4JavaErrorIfConditionFails(nonNull(packet.getFuncNameString()),
                "Unmapped / unmappable function in RpcPacket.put()");

        // Skip over the first few bytes for the preamble,
        // which we'll come back to fill in later when we know the marshaled
        // length.
        int startPos = RpcPacketPreamble.RPC_PREAMBLE_SIZE;

        RpcPacketSupplier supplier = new RpcPacketSupplier();
        supplier.sendBytes(new byte[INITIAL_SENDBUF_SIZE]).sendPos(startPos);

        // FixedOrder
        processNameArgs(packet, supplier);
        processStringArgs(packet, supplier);
        processExternalEnv(packet, supplier);
        processFuncName(packet, supplier);

        calculatePreambleBytesAndSendtoDownstream(supplier);
        return 0;
    }

    private void processNameArgs(@Nonnull RpcPacket packet,
            @Nonnull final RpcPacketSupplier argsSupplier) {
        Map<String, Object> mapArgs = packet.getMapArgs();
        if (nonNull(mapArgs)) {
            for (Map.Entry<String, Object> entry : mapArgs.entrySet()) {
                reallocateSendBufferInPutPacketIfRunOut(argsSupplier,
                        marshalPacketField(entry.getKey(), entry.getValue()), SENDBUF_REALLOC_INCR);
            }
        }
    }

    private void reallocateSendBufferInPutPacketIfRunOut(@Nonnull final RpcPacketSupplier supplier,
            @Nonnull final byte[] fieldBytes, final int reallocateIncrement) {
        byte[] sendBytes = supplier.sendBytes();
        int sendPos = supplier.sendPos();

        byte[] newSendBytes = sendBytes;
        if ((sendBytes.length - sendPos) <= fieldBytes.length) {
            stats.bufferCompacts.getAndIncrement(); // We're overloading the
                                                    // meaning here...
            int newBytesLength = sendBytes.length + fieldBytes.length + reallocateIncrement;
            newSendBytes = new byte[newBytesLength];
            System.arraycopy(sendBytes, 0, newSendBytes, 0, sendPos);
        }

        System.arraycopy(fieldBytes, 0, newSendBytes, sendPos, fieldBytes.length);
        supplier.sendBytes(newSendBytes).sendPos(sendPos + fieldBytes.length);
    }

    private void processStringArgs(@Nonnull RpcPacket packet,
            @Nonnull final RpcPacketSupplier argsSupplier) {
        String[] strArgs = packet.getStrArgs();
        if (nonNull(strArgs)) {
            for (String arg : strArgs) {
                if (isNotBlank(arg)) {
                    reallocateSendBufferInPutPacketIfRunOut(argsSupplier,
                            marshalPacketField(null, arg), SENDBUF_REALLOC_INCR);
                }
            }
        }
    }

    private void processExternalEnv(@Nonnull RpcPacket packet,
            @Nonnull final RpcPacketSupplier argsSupplier) {
        ExternalEnv externalEnv = packet.getEnv();
        if (nonNull(externalEnv)) {
            reallocateSendBufferInPutPacketIfRunOut(argsSupplier, externalEnv.marshal(),
                    SENDBUF_REALLOC_INCR);
        }
    }

    private void processFuncName(@Nonnull RpcPacket packet, @Nonnull RpcPacketSupplier supplier) {
        byte[] nameBytes = marshalPacketField(RpcFunctionMapKey.FUNCTION,
                packet.getFuncNameString());
        reallocateSendBufferInPutPacketIfRunOut(supplier, nameBytes, nameBytes.length);
    }

    /**
     * Now go back and calculate the preamble bytes and sending it to downstream
     */
    private void calculatePreambleBytesAndSendtoDownstream(
            @Nonnull final RpcPacketSupplier argsSupplier) throws ConnectionException {
        byte[] sendBytes = argsSupplier.sendBytes();
        int sendPos = argsSupplier.sendPos();

        byte[] preambleBytes = RpcPacketPreamble
                .constructPreamble(sendPos - RpcPacketPreamble.RPC_PREAMBLE_SIZE).marshalAsBytes();
        System.arraycopy(preambleBytes, 0, sendBytes, 0, preambleBytes.length);
        try {
            topOutputStream.write(sendBytes, 0, sendPos);
            topOutputStream.flush();
            stats.streamSends.incrementAndGet();
            stats.totalBytesSent.getAndAdd(sendPos);
            stats.packetsSent.incrementAndGet();
            if (stats.largestRpcPacketSent.get() < sendPos) {
                stats.largestRpcPacketSent.set(sendPos);
            }
        } catch (IOException exc) {
            Log.exception(exc);
            StringBuilder message = new StringBuilder();
            if (exc instanceof SocketTimeoutException && secure) {
                message.append(MessageFormat.format(
                        "SSL connect to ssl:{0}:{1,number,#} failed.\nRemove SSL protocol prefix.\n",
                        hostName, hostPort));
            } else {
                message.append("Unable to send command to Perforce server: ");
            }
            message.append(exc.getMessage());
            throwConnectionException(exc, message.toString());
        }
    }

    @Override
    public void useConnectionCompression() throws ConnectionException {
        if (!usingCompression) {
            super.useConnectionCompression();

            try {
                topOutputStream.flush();
                // We do this here immediately to avoid having the compress2
                // itself compressed...
                putRpcPacket(RpcPacket.constructRpcPacket(RpcFunctionSpec.PROTOCOL_COMPRESS2,
                        "compress2", null, null));
                topOutputStream.flush();
                topOutputStream = new RpcGZIPOutputStream(outputStream);
                topInputStream = new RpcGZIPInputStream(inputStream);
            } catch (IOException exc) {
                Log.error("I/O exception encountered while setting up GZIP streaming: %s",
                        exc.getLocalizedMessage());
                Log.exception(exc);
                throwConnectionException(exc,
                        "unable to set up client compression streaming to Perforce server: %s",
                        exc.getLocalizedMessage());
            }
        }
    }

    RpcStreamConnection rsh(String rsh) {
        this.rsh = rsh;
        return this;
    }

    RpcStreamConnection socket(Socket socket) {
        this.socket = socket;
        return this;
    }

    RpcStreamConnection topInputStream(InputStream topInputStream) {
        this.topInputStream = topInputStream;
        return this;
    }

    RpcStreamConnection topOutputStream(OutputStream topOutputStream) {
        this.topOutputStream = topOutputStream;
        return this;
    }

    static class RpcPacketSupplier {
        private byte[] sendBytes = new byte[INITIAL_SENDBUF_SIZE];
        private int sendPos = 0;

        RpcPacketSupplier sendBytes(final byte[] sendBytes) {
            this.sendBytes = sendBytes;
            return this;
        }

        RpcPacketSupplier sendPos(final int sendPos) {
            this.sendPos = sendPos;
            return this;
        }

        byte[] sendBytes() {
            return sendBytes;
        }

        int sendPos() {
            return sendPos;
        }
    }
}
