/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.connection;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.impl.mapbased.rpc.ServerStats;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.helper.MD5Digester;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher;
import com.perforce.p4java.impl.mapbased.rpc.packet.helper.RpcPacketFieldRule;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceDigestType;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.server.P4Charset;
import com.perforce.p4java.server.callback.IFilterCallback;
import com.perforce.p4java.util.PropertiesHelper;

// p4ic4idea: rather than pull in yet another jar, reuse the existing digest stuff.
// import org.apache.commons.codec.digest.DigestUtils;

import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

/**
 * Main abstract class for sending and receiving packets (etc.) to and from the
 * Perforce server. There is currently only one known subclass,
 * RpcStreamConnection, which implements the connection using java.io streams on
 * top of sockets.
 * <p>
 *
 * Note that charset conversion should never be necessary on connections to
 * non-Unicode servers, as any bytes in the incoming stream that are marked as
 * "text" should be interpreted as ASCII (seven or eight bits). Unfortunately,
 * in Java land, there's no such thing as non-interpreted bytes -- bytes are
 * converted to strings according to *some* charset or other (and Java uses
 * UTF-16 as the native encoding for strings under the covers). This explains
 * why we always do a conversion below -- if we didn't use an explicit "from"
 * charset, the current JVM's charset would be used, which would quite probably
 * be wrong. Similar observations apply for conversions in the other direction
 * -- all conversions must be with an explicit charset in case the JVM's charset
 * isn't the one we actually need.
 * <p>
 *
 * Note that, in general, we "know" that the Perforce server on the other end of
 * this connection is -- or should be -- a Unicode server by the fact that the
 * incoming clientCharset is not null. This probably isn't infallible, but it's
 * supposed to be true, so we use it as a proxy for IServer.supportsUnicode().
 * <p>
 *
 * See
 * http://computer.perforce.com/newwiki/index.php?title=P4Java_and_Charset_Support
 * for a detailed discussion of P4Java and server charset issues...
 */

public abstract class RpcConnection {

    public static final String TRACE_PREFIX = "RpcConnection";

    /**
     * The charset used internally by a Perforce Unicode-enabled server. If a
     * server is Unicode-enabled, then <i>every</i> non-binary RPC packet field
     * is sent to and received from the Perforce server in this charset.
     * <p>
     *
     * Do not change this unless you want all hell to break loose.
     */
    public static final Charset UNICODE_SERVER_CHARSET = CharsetDefs.UTF8;

    /**
     * The name of the assumed Unicode server internal charset. Do not change
     * this either.
     */
    public static final String UNICODE_SERVER_CHARSET_NAME = CharsetDefs.UTF8_NAME;

    /**
     * Charset assumed when the server is non in Unicode mode. Do not change
     * this field, either, unless you want problems...
     */
    public static final Charset NON_UNICODE_SERVER_CHARSET = CharsetDefs.DEFAULT;

    /**
     * The name of the assumed non-Unicode server charset. Do not change this
     * either.
     */
    public static final String NON_UNICODE_SERVER_CHARSET_NAME = CharsetDefs.DEFAULT_NAME;

    protected static final String UNKNOWN_SERVER_HOST = null;
    protected static final int UNKNOWN_SERVER_PORT = -1;

    protected Properties props = null;
    protected RpcConnectionFlowControl flowController = new RpcConnectionFlowControl();

    protected ServerStats stats = null;

    protected P4Charset p4Charset = null;

    protected String hostIp = UNKNOWN_SERVER_HOST;
    protected String ourIp = UNKNOWN_SERVER_HOST;
    protected String hostName = UNKNOWN_SERVER_HOST;
    protected int hostPort = UNKNOWN_SERVER_PORT;
    protected int ourPort = UNKNOWN_SERVER_PORT;
    protected boolean usingCompression = false;

    protected boolean unicodeServer = false;

    protected boolean secure = false;
    protected String fingerprint = null;
    protected boolean trusted = false;

    /**
     * Create a Perforce RPC connection to a given host and port number pair.
     * <p>
     *
     * This method will also implicitly connect to the server. Note that new
     * connections are never using connection compression -- this has to come as
     * an explicit command from the server after the connection has been
     * established.
     *
     * @param serverHost
     *            non-null Perforce server host name or IP address.
     * @param serverPort
     *            Perforce server host port number.
     * @param props
     *            if not null, use the Properties for any connection- or
     *            implementation-specific values (such as buffer sizes, etc.).
     * @param stats
     *            if not null, attempt to fill in these connection stats
     *            appropriately.
     * @param p4Charset
     *            if non-null, sets the connection's idea of what the current
     *            client charset is. If null, CharsetDefs.DEFAULT is used.
     * @throws ConnectionException
     *             if any user-reportable error occurred under the covers.
     */
    public RpcConnection(String serverHost, int serverPort, Properties props, ServerStats stats,
            P4Charset p4Charset) throws ConnectionException {

        this(serverHost, serverPort, props, stats, p4Charset, false);
    }

    /**
     * Create a Perforce RPC connection to a given host and port number pair.
     * <p>
     *
     * This method will also implicitly connect to the server. Note that new
     * connections are never using connection compression -- this has to come as
     * an explicit command from the server after the connection has been
     * established.
     *
     * @param serverHost
     *            non-null Perforce server host name or IP address.
     * @param serverPort
     *            Perforce server host port number.
     * @param props
     *            if not null, use the Properties for any connection- or
     *            implementation-specific values (such as buffer sizes, etc.).
     * @param stats
     *            if not null, attempt to fill in these connection stats
     *            appropriately.
     * @param p4Charset
     *            if non-null, sets the connection's idea of what the current
     *            client charset is. If null, CharsetDefs.DEFAULT is used.
     * @param secure
     *            indicate whether the connection is secure (SSL) or not.
     * @throws ConnectionException
     *             if any user-reportable error occurred under the covers.
     */
    public RpcConnection(@Nonnull String serverHost, int serverPort, Properties props,
                         ServerStats stats, P4Charset p4Charset, boolean secure) throws ConnectionException {

        this.hostName = Validate.notNull(serverHost);
        this.hostPort = serverPort;
        this.secure = secure;
        this.p4Charset = firstNonNull(p4Charset, P4Charset.getDefault());
        this.stats = firstNonNull(stats, new ServerStats());
        this.props = firstNonNull(props, new Properties());
        this.stats.serverConnections.incrementAndGet();
        this.unicodeServer = P4Charset.isUnicodeServer(p4Charset); // Note: NOT this.p4Charset.getCharset()....
    }

    /**
     * Get the server's IP and port used for the RPC connection.
     *
     * @return - possibly null server IP and port
     */
    public abstract String getServerIpPort();

    /**
     * Get the client's IP and port used for the RPC connection.
     *
     * @return - possibly null client IP and port
     */
    public abstract String getClientIpPort();

    /**
     * Disconnect this server. Assumes the connection is quiescent -- no further
     * sends or receives will be possible after this call, and only the output
     * stream is flushed (no attempt is made to look at anything still on the
     * wire coming in to the socket).
     */
    public abstract void disconnect(RpcPacketDispatcher dispatcher) throws ConnectionException;

    /**
     * Put a Perforce RPC packet onto the output stream. The implementing method
     * must make the appropriate charset translations and any other client- or
     * server- (or whatever-) specific processing on the passed-in packet.
     * <p>
     *
     * Returns the number of bytes actually sent to the Perforce server, which
     * may not bear any relationship at all to the size of the passed-in packet.
     */
    public abstract long putRpcPacket(RpcPacket rpcPacket) throws ConnectionException;

    /**
     * Put an array of RPC packets. A convenience method wrapping
     * putRpcPacket(packet) in the obvious way.
     */

    public abstract long putRpcPackets(RpcPacket[] rpcPackets) throws ConnectionException;

    /**
     * Get the next RPC packet from the receive queue. The implementing method
     * must make the appropriate charset translations and any other client- or
     * server- (or whatever-) specific processing on the packet returned from
     * this method by the time it's returned.
     * <p>
     *
     * Will wait until either a timeout occurs (if the stream's been set up
     * appropriately), the underlying stream returns EOF or error, or we get a
     * complete packet.
     * <p>
     */

    public abstract RpcPacket getRpcPacket() throws ConnectionException;

    /**
     * Get the next RPC packet from the receive queue with an optional rule to
     * handle the RPC packet fields.
     */
    public abstract RpcPacket getRpcPacket(RpcPacketFieldRule fieldRule,
            IFilterCallback filterCallback) throws ConnectionException;

    /**
     * Return the system (i.e. underlying implementation) send buffer size.
     */

    public abstract int getSystemSendBufferSize();

    /**
     * Return the system (i.e. underlying implementation) receive buffer size.
     */

    public abstract int getSystemRecvBufferSize();

    /**
     * Marshal a packet field into a key value byte array pair. This must
     * respect the relevant charset settings, which can be a little
     * counter-intuitive or confusing.
     */
    public byte[] marshalPacketField(String key, Object value) {

        // Note: either key or value can be (and often are) null, but it's rare
        // for
        // both to be null. But it happens...

        // Note: this could easily be improved on for performance by tuning the
        // copies; the
        // implementation here is to get something that works first...

        byte[] retBytes = null;
        byte[] keyBytes = null;
        byte[] valBytes = null;
        byte[] valLengthBytes = null;

        if (key != null) {
            keyBytes = getNormalizedBytes(key);
        }

        valBytes = marshalPacketValue(value);

        valLengthBytes = RpcPacket.encodeInt4(valBytes == null ? 0 : valBytes.length);

        // Calculate the resulting field length, in bytes. Note that there's
        // a null byte after each sub field, hence the "2 +" here.

        int fieldLength = 2 + (keyBytes == null ? 0 : keyBytes.length) + valLengthBytes.length
                + (valBytes == null ? 0 : valBytes.length);

        retBytes = new byte[fieldLength];
        int retBytesPos = 0;

        if (keyBytes != null) {
            System.arraycopy(keyBytes, 0, retBytes, retBytesPos, keyBytes.length);
            retBytesPos += keyBytes.length;
        }
        retBytes[retBytesPos++] = 0;

        System.arraycopy(valLengthBytes, 0, retBytes, retBytesPos, valLengthBytes.length);
        retBytesPos += valLengthBytes.length;

        if (valBytes != null) {
            System.arraycopy(valBytes, 0, retBytes, retBytesPos, valBytes.length);
            retBytesPos += valBytes.length;
        }
        retBytes[retBytesPos++] = 0;

        return retBytes;
    }

    /**
     * Marshal a packet field value onto a byte array and return that array.
     * <p>
     *
     * For strings and similar types (e.g. StringBuilder, StringBuffer), we may
     * need to do a translation to the server charset (normally UTF-8) before
     * enbyteifying the underlying value. Other field types are sent as-is, and
     * are assumed to have been encoded properly upstream (and are almost always
     * just file contents).
     * <p>
     *
     * Note: if the value object passed in is a ByteBuffer, it must have been
     * flipped ready for use; this method will (of course) have predictable side
     * effects on that ByteBuffer.
     */
    protected byte[] marshalPacketValue(Object value) {

        byte[] valBytes = null;

        try {
            if (value != null) {
                if (value.getClass() == String.class) {
                    valBytes = getNormalizedBytes((String) value);
                } else if (value instanceof byte[]) {
                    valBytes = (byte[]) value;
                } else if (value instanceof StringBuffer) {
                    valBytes = getNormalizedBytes(value.toString());
                } else if (value instanceof ByteBuffer) {
                    int valLength = ((ByteBuffer) value).limit();
                    valBytes = new byte[valLength];
                    ((ByteBuffer) value).get(valBytes);
                } else {
                    throw new P4JavaError(
                            "Unmarshalable value in RpcStreamConnection.marshal; type: "
                                    + value.getClass().getCanonicalName());
                }
            }
        } catch (P4JavaError p4jerr) {
            throw p4jerr;
        // p4ic4idea: never catch Throwable
        } catch (Exception thr) {
            Log.error("Unexpected exception in RpcStreamConnection.marshalValue: "
                    + thr.getLocalizedMessage());
            Log.exception(thr);
            throw new P4JavaError("Unexpected exception in RpcStreamConnection.marshalValue: "
                    + thr.getLocalizedMessage());
        }

        return valBytes;
    }

    /**
     * If called, will set this connection to use (GZIP) compression for all
     * traffic on this connection from this point on. Can not be turned back off
     * again. See the main P4 help documentation (and C++ API code) for "client
     * compression" (etc.) for details.
     */
    public void useConnectionCompression() throws ConnectionException {
        if (!usingCompression) {
            this.usingCompression = true;
        }
    }

    /**
     * Encode the passed-in string properly for the server. If the server is
     * Unicode-enabled, this usually means converting to UTF-8 encoding for the
     * stream. This can be a very CPU-intensive process...
     * <p>
     *
     * FIXME: use proper encoding / decoding with error handling -- HR.
     *
     * @return - normalized string
     */
    protected byte[] getNormalizedBytes(String str) {
        if (str == null) {
            throw new NullPointerError("null string passed to RpcConnection.getNormalizedBytes");
        }
        try {
            if (this.unicodeServer) {
                return str.getBytes(UNICODE_SERVER_CHARSET_NAME);
            } else {
                return str.getBytes(this.p4Charset.getCharset().name());
            }
        } catch (UnsupportedEncodingException e) {
            // This should never be reached since we already have the Charset
            // object needed.
            Log.exception(e);
        }
        return null;
    }

    /**
     * Decode the passed-in bytes from the server into a suitable string. In
     * general, bytes from the server will be in ASCII (non-Unicode servers) or
     * UTF-8 (Unicode servers), but there are exceptions.
     * <p>
     *
     * FIXME: use proper encoding / decoding with error handling -- HR.
     *
     * @return - normalized string
     */
    protected String getNormalizedString(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerError("null bytes passed to RpcConnection.getNormalizedString");
        }
        try {
            if (this.unicodeServer) {
                return new String(bytes, UNICODE_SERVER_CHARSET_NAME);
            } else {
                return new String(bytes, this.p4Charset.getCharset().name());
            }
        } catch (UnsupportedEncodingException e) {
            // This should never be reached since we already have the Charset
            // object needed.
            Log.exception(e);
        }
        return null;
    }

    public boolean isUsingCompression() {
        return this.usingCompression;
    }

    public RpcConnectionFlowControl getFlowController() {
        return this.flowController;
    }

    public void setFlowController(RpcConnectionFlowControl flowController) {
        this.flowController = flowController;
    }

    public Properties getProps() {
        return props;
    }

    public void setProps(Properties props) {
        this.props = props;
    }

    public Charset getClientCharset() {
        return this.p4Charset.getCharset();
    }

    public P4Charset getP4Charset() {
        return p4Charset;
    }

    public void setClientCharset(P4Charset p4Charset) {
        this.p4Charset = p4Charset;
        this.unicodeServer = P4Charset.isUnicodeServer(p4Charset);
    }

    public ServerStats getStats() {
        return this.stats;
    }

    public void setStats(ServerStats stats) {
        this.stats = stats;
    }

    public String getHostIp() {
        return this.hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getHostName() {
        return this.hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getHostPort() {
        return this.hostPort;
    }

    public void setHostPort(int hostPort) {
        this.hostPort = hostPort;
    }

    public void setUsingCompression(boolean usingCompression) {
        this.usingCompression = usingCompression;
    }

    public boolean isUnicodeServer() {
        return this.unicodeServer;
    }

    public void setUnicodeServer(boolean unicodeServer) {
        this.unicodeServer = unicodeServer;
    }

    public int getFilesysUtf8bom() {
        String val = PropertiesHelper.getPropertyByKeys(props, PropertyDefs.FILESYS_UTF8BOM_SHORT_FORM, PropertyDefs.FILESYS_UTF8BOM, "1");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public int getFilesysRestrictedSymlinks() {
        String val = PropertiesHelper.getPropertyByKeys(props, PropertyDefs.FILESYS_RESTRICTSYMLINKS_SHORT_FORM, PropertyDefs.FILESYS_RESTRICTSYMLINKS, "0");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isSecure() {
        return this.secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

	public RpcPacketDispatcher.RpcPacketDispatcherResult clientConfirm(String confirm, Map<String, Object> resultsMap)
            throws ConnectionException {

		if(confirm == null) {
            return RpcPacketDispatcher.RpcPacketDispatcherResult.CONTINUE_LOOP;
		}

		// Copy all incoming vars to outgoing vars

		Map<String, Object> respMap = new HashMap<String, Object>();

		for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
			if ((entry.getKey() != null) && !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.FUNCTION)) {
				respMap.put(entry.getKey(), entry.getValue());
			}
		}

		RpcPacket respPacket = RpcPacket.constructRpcPacket(confirm, respMap, null);

		putRpcPacket(respPacket);

        return RpcPacketDispatcher.RpcPacketDispatcherResult.CONTINUE_LOOP;
    }


    public String getDigest(RpcPerforceFileType fileType, File file) {
        return getDigest(fileType, file, null);
    }

    public String getDigest(RpcPerforceFileType fileType, File file,
                            RpcPerforceDigestType digest) {

        MD5Digester digester = new MD5Digester();

        if(digest == null) {
            digest = RpcPerforceDigestType.MD5;
        }

        Charset digestCharset = null;
        boolean convertLineEndings = false;
        switch (fileType) {
            case FST_SYMLINK:
                return getSymlinkMD5Digest(file);

            case FST_UTF16:
                digestCharset = CharsetDefs.UTF16;
                break;
            case FST_UTF8:
                digestCharset = CharsetDefs.UTF8;
                convertLineEndings = true;
                break;
            case FST_UNICODE:
                digestCharset = getClientCharset();
                break;
            case FST_XTEXT:
            case FST_TEXT:
                // Convert line endings
                convertLineEndings = true;
                break;
            default:
                break;
        }

        // Digest the file using the configured local file content
        // charset. A null digestCharset specified will cause the
        // file to be read as raw byte stream directly off disk.
        //TODO: Digester might be SHA* variant
        String digestStr = digester.digestFileAs32ByteHex(file, digestCharset, convertLineEndings);

        return digestStr;
    }

    private String getSymlinkMD5Digest(File file) {
        String targetPath = SymbolicLinkHelper.readSymbolicLink(file.getAbsolutePath());

        // p4ic4idea: use existing md5 stuff.
        // String md5 = DigestUtils.md5Hex(targetPath + "\n").toUpperCase();
        String source = targetPath + "\n";
        MD5Digester digester = new MD5Digester();
        digester.update(source);
        String md5 = digester.digestAs32ByteHex().toUpperCase();

        return md5;
    }
}
