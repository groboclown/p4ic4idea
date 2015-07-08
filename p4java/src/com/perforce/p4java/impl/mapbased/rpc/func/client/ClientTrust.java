/*
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.TrustException;
import com.perforce.p4java.impl.mapbased.rpc.RpcServer;
import com.perforce.p4java.messages.PerforceMessages;
import com.perforce.p4java.server.Fingerprint;

/**
 * Handle the client trust and fingerprint for Perforce SSL connections.
 */
public class ClientTrust {

	public static final String DIGEST_TYPE = "SHA";

	public static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	public static final String USER_NAME_PLACE_HOLDER = "**++**";

	public static final String CLIENT_TRUST_MESSAGES = "com.perforce.p4java.messages.ClientTrustMessages";

	public static final String CLIENT_TRUST_WARNING_NEW_CONNECTION = "client.trust.warning.newconnection";
	public static final String CLIENT_TRUST_WARNING_NEW_KEY = "client.trust.warning.newkey";

	public static final String CLIENT_TRUST_EXCEPTION_NEW_CONNECTION = "client.trust.exception.newconnection";
	public static final String CLIENT_TRUST_EXCEPTION_NEW_KEY = "client.trust.exception.newkey";

	public static final String CLIENT_TRUST_ADD_EXCEPTION_NEW_CONNECTION = "client.trust.add.exception.newconnection";
	public static final String CLIENT_TRUST_ADD_EXCEPTION_NEW_KEY = "client.trust.add.exception.newkey";

	public static final String CLIENT_TRUST_ADDED = "client.trust.added";
	public static final String CLIENT_TRUST_REMOVED = "client.trust.removed";
	public static final String CLIENT_TRUST_ALREADY_ESTABLISHED = "client.trust.alreadyestablished";

	public static final String CLIENT_TRUST_INSTALL_EXCEPTION = "client.trust.install.exception";
	public static final String CLIENT_TRUST_UNINSTALL_EXCEPTION = "client.trust.uninstall.exception";

	private RpcServer rpcServer = null;

	private PerforceMessages messages = new PerforceMessages(
			ClientTrust.CLIENT_TRUST_MESSAGES);

	/**
	 * Instantiates a new client trust.
	 * 
	 * @param rpcServer
	 *            the rpc server
	 */
	public ClientTrust(RpcServer rpcServer) {
		if (rpcServer == null) {
			throw new NullPointerError(
					"null rpcServer passed to ClientTrust constructor");
		}
		this.rpcServer = rpcServer;
	}

	/**
	 * Install the fingerprint for the specified server IP and port
	 * 
	 * @param serverIpPort
	 *            the serverIpPort
	 * @param fingerprint
	 *            the fingerprint
	 * @throws TrustException
	 *             the trust exception
	 */
	public void installFingerprint(String serverIpPort, String fingerprint)
			throws TrustException {
		if (serverIpPort == null) {
			throw new NullPointerError(
					"null serverIpPort passed to the ClientTrust installFingerprint method");
		}
		if (fingerprint == null) {
			throw new NullPointerError(
					"null fingerprint passed to the ClientTrust installFingerprint method");
		}
		try {
			rpcServer.saveFingerprint(serverIpPort, fingerprint);
		} catch (ConfigException e) {
			throw new TrustException(TrustException.Type.INSTALL,
					rpcServer.getServerHostPort(), serverIpPort, fingerprint,
					messages.getMessage(ClientTrust.CLIENT_TRUST_INSTALL_EXCEPTION,
    						new Object[] { fingerprint, rpcServer.getServerHostPort(), serverIpPort }), e);
		}
	}

	/**
	 * Removes the fingerprint for the specified server IP and port
	 * 
	 * @throws TrustException
	 *             the trust exception
	 */
	public void removeFingerprint(String serverIpPort) throws TrustException {
		if (serverIpPort == null) {
			throw new NullPointerError(
					"null serverIpPort passed to the ClientTrust removeFingerprint method");
		}
		try {
			rpcServer.saveFingerprint(serverIpPort, null);
		} catch (ConfigException e) {
			throw new TrustException(TrustException.Type.UNINSTALL,
					rpcServer.getServerHostPort(), serverIpPort, null,
					messages.getMessage(ClientTrust.CLIENT_TRUST_UNINSTALL_EXCEPTION,
    						new Object[] { rpcServer.getServerHostPort(), serverIpPort }), e);
		}
	}

	/**
	 * Check if the fingerprint exists for the specified server IP and port
	 * 
	 * @param serverIpPort
	 *            the serverIpPort
	 * @return true, if successful
	 */
	public boolean fingerprintExists(String serverIpPort) {
		if (serverIpPort == null) {
			throw new NullPointerError(
					"null serverIpPort passed to the ClientTrust fingerprintExists method");
		}
		return (rpcServer.loadFingerprint(serverIpPort) != null);
	}

	/**
	 * Check if the fingerprint for the specified server IP and port matches the
	 * one in trust file.
	 * 
	 * @param serverIpPort
	 *            the serverIpPort
	 * @param fingerprint
	 *            the fingerprint
	 * @return true, if successful
	 */
	public boolean fingerprintMatches(String serverIpPort, String fingerprint) {
		if (serverIpPort == null) {
			throw new NullPointerError(
					"null serverIpPort passed to the ClientTrust fingerprintMatches method");
		}
		if (fingerprint == null) {
			throw new NullPointerError(
					"null fingerprint passed to the ClientTrust fingerprintMatches method");
		}
		if (fingerprintExists(serverIpPort)) {
			Fingerprint existingFingerprint = rpcServer
					.loadFingerprint(serverIpPort);
			if (existingFingerprint != null
					&& existingFingerprint.getFingerprintValue() != null) {
				if (fingerprint.equalsIgnoreCase(existingFingerprint
						.getFingerprintValue())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Generate fingerprint from public key using MessageDigest.
	 * 
	 * @param publicKey
	 *            the public key
	 * @return the string
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 */
	public static String generateFingerprint(PublicKey publicKey)
			throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(DIGEST_TYPE);
		md.update(publicKey.getEncoded());
		byte[] fp = md.digest();
		return convert2Hex(fp);
	}

	/**
	 * Generate fingerprint from a certificate using MessageDigest.
	 * 
	 * @param certificate
	 *            the certificate
	 * @return the string
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws CertificateEncodingException
	 *             the certificate encoding exception
	 */
	public static String generateFingerprint(X509Certificate certificate)
			throws NoSuchAlgorithmException, CertificateEncodingException {
		MessageDigest md = MessageDigest.getInstance(DIGEST_TYPE);
		md.update(certificate.getEncoded());
		byte[] fp = md.digest();
		return convert2Hex(fp);
	}

	/**
	 * Convert a byte array to a hexadecimal string
	 * 
	 * @param data
	 *            the data
	 * @return the string
	 */
	public static String convert2Hex(byte[] data) {
		int n = data.length;
		StringBuffer sb = new StringBuffer(n * 3 - 1);
		for (int i = 0; i < n; i++) {
			if (i > 0) {
				sb.append(':');
			}
			sb.append(HEX_CHARS[(data[i] >> 4) & 0x0F]);
			sb.append(HEX_CHARS[data[i] & 0x0F]);
		}
		return sb.toString();
	}

	/**
	 * Gets the messages.
	 * 
	 * @return the messages
	 */
	public PerforceMessages getMessages() {
		return messages;
	}
}