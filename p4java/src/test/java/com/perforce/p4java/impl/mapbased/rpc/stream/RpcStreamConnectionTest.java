package com.perforce.p4java.impl.mapbased.rpc.stream;

import com.perforce.p4java.AbstractP4JavaUnitTest;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.impl.mapbased.rpc.ExternalEnv;
import com.perforce.p4java.impl.mapbased.rpc.ServerStats;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher;
import com.perforce.p4java.impl.mapbased.rpc.packet.helper.RpcPacketFieldRule;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.server.P4Charset;
import com.perforce.p4java.server.callback.IFilterCallback;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.SslException;
import com.perforce.p4java.exception.SslHandshakeException;

import com.google.common.collect.ImmutableMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Sean Shou
 * @since 1/09/2016
 */
public class RpcStreamConnectionTest extends AbstractP4JavaUnitTest {
	private RpcStreamConnection mockConnection;
	private int serverPort = 1666;
	private Socket socket;
	private ServerStats serverStats;
	private RpcSocketPool rpcSocketPool;
	private String rsh;
	private InputStream topInputStream = mock(InputStream.class);
	private OutputStream topOutputStream = mock(OutputStream.class);
	private int socketBufferSize = 10;
	private int recvBufferSize = 15;

	@Before
	public void beforeEach() throws ConnectionException, IOException {
		socket = mock(Socket.class);
		when(socket.getInetAddress()).thenReturn(null);
		when(socket.getSendBufferSize()).thenReturn(socketBufferSize);
		when(socket.getReceiveBufferSize()).thenReturn(recvBufferSize);
		serverStats = new ServerStats();
		rpcSocketPool = mock(RpcSocketPool.class);
		when(rpcSocketPool.acquire()).thenReturn(socket);

		mockConnection = new RpcStreamConnection("localhost", serverPort, null, serverStats, P4Charset.getUTF8(),
				socket, rpcSocketPool, false, rsh);
		mockConnection.topInputStream(topInputStream).topOutputStream(topOutputStream);
	}

	@Test
	public void given_unknownHostIp_and_validHostPort_when_getServerIpPort() throws Exception {
		String serverIpPort = mockConnection.getServerIpPort();
		assertThat(serverIpPort, is(String.valueOf(serverIpPort)));
	}

	@Test
	public void given_knownHostIp_and_validHostPort_when_getServerIpPort() throws Exception {
		String hostIp = "127.0.0.1";
		mockConnection.setHostIp(hostIp);
		String serverIpPort = mockConnection.getServerIpPort();
		assertThat(serverIpPort, is(hostIp + ":" + String.valueOf(serverPort)));
	}

	@Test
	public void given_knownHostIp_and_nonHostPort_when_getServerIpPort() throws Exception {
		mockConnection.setHostPort(-1);
		String hostIp = "127.0.0.1";
		mockConnection.setHostIp(hostIp);
		String serverIpPort = mockConnection.getServerIpPort();
		assertThat(serverIpPort, is(hostIp));
	}

	@Test
	public void disconnect_non_socketPool_and_null_rsh() throws Exception {
		RpcPacketDispatcher dispatcher = mock(RpcPacketDispatcher.class);
		mockConnection.disconnect(dispatcher);
		verify(rpcSocketPool, times(1)).release(eq(socket), any(RpcSocketPool.ShutdownHandler.class));
	}

	@Test
	public void disconnect_null_socketPool_and_null_rsh() throws Exception {
		mockConnection = new RpcStreamConnection("localhost", serverPort, null, serverStats, P4Charset.getUTF8(),
				socket, null, false, rsh);
		InputStream topInputStream = mock(InputStream.class);
		OutputStream topOutputStream = mock(OutputStream.class);
		mockConnection.topInputStream(topInputStream).topOutputStream(topOutputStream);

		RpcPacketDispatcher dispatcher = mock(RpcPacketDispatcher.class);
		mockConnection.disconnect(dispatcher);

		verify(topInputStream, times(1)).close();
		verify(topOutputStream, times(1)).close();
		verify(socket, times(1)).close();
	}

	@Test
	public void disconnect_null_socketPool_null_socket_null_rsh() throws Exception {
		mockConnection = new RpcStreamConnection("localhost", serverPort, null, serverStats, P4Charset.getUTF8(),
				socket, null, false, rsh);
		InputStream topInputStream = mock(InputStream.class);
		OutputStream topOutputStream = mock(OutputStream.class);
		mockConnection.topInputStream(topInputStream).topOutputStream(topOutputStream).socket(null);

		RpcPacketDispatcher dispatcher = mock(RpcPacketDispatcher.class);
		mockConnection.disconnect(dispatcher);

		verify(topInputStream, times(1)).close();
		verify(topOutputStream, times(1)).close();
	}

	@Test
	public void disconnect_nonNull_rsh_without_exception() throws Exception {
		RpcPacketDispatcher dispatcher = mock(RpcPacketDispatcher.class);
		mockConnection.rsh("rsh");

		mockConnection.disconnect(dispatcher);
		verify(dispatcher, times(1)).shutdown(any(RpcStreamConnection.class));
		verify(topInputStream, times(1)).close();
		verify(topOutputStream, times(1)).close();
	}

	@Test
	public void disconnect_nonNull_rsh_with_inner_exception() throws Exception {
		RpcPacketDispatcher dispatcher = mock(RpcPacketDispatcher.class);
		mockConnection.rsh("rsh");

		doThrow(new ConnectionException()).when(dispatcher).shutdown(any(RpcStreamConnection.class));

		mockConnection.disconnect(dispatcher);
		verify(topInputStream, times(1)).close();
		verify(topOutputStream, times(1)).close();
	}

	@Test(expected=ConnectionException.class)
	public void disconnect_nonNull_rsh_with_out_exception() throws Exception {
		RpcPacketDispatcher dispatcher = mock(RpcPacketDispatcher.class);
		mockConnection.rsh("rsh");
		InputStream topInputStream = mock(InputStream.class);
		doThrow(new IOException()).when(topInputStream).close();

		mockConnection.topInputStream(topInputStream);
		mockConnection.disconnect(dispatcher);
	}

	@Test
	public void given_usingCompression_isFalse_and_without_exception_when_useConnectionCompression() throws Exception {
		mockConnection.setUsingCompression(false);

		mockConnection.useConnectionCompression();

		verify(topOutputStream, times(3)).flush();
	}

	@Test(expected=ConnectionException.class)
	public void given_usingCompression_isFalse_and_with_exception_when_useConnectionCompression() throws Exception {
		mockConnection.setUsingCompression(false);

		doThrow(new IOException()).when(topOutputStream).flush();
		mockConnection.useConnectionCompression();
	}

	@Test(expected=ConnectionException.class)
	public void given_topInputStream_read_return_lessThanZero_when_getRpcPacket() throws Exception {
		RpcPacketFieldRule fieldRule = mock(RpcPacketFieldRule.class);
		IFilterCallback filterCallback = mock(IFilterCallback.class);

		when(topInputStream.read(any((byte[].class)))).thenReturn(-1);
		mockConnection.getRpcPacket(fieldRule, filterCallback);
	}

	@Test(expected=NullPointerException.class)
	public void putRpcPacket_null_rpcPacket() throws ConnectionException {
		mockConnection.putRpcPacket(null);
	}

	@Test(expected=P4JavaError.class)
	public void putRpcPacket_nonNull_rpcPacket_but_funcNameString_isNull() throws Exception {
		RpcPacket rpcPacket = mock(RpcPacket.class);
		when(rpcPacket.getFuncNameString()).thenReturn(null);
		mockConnection.putRpcPacket(rpcPacket);
	}

	@Test
	public void processNameArgs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
	    HashMap<String, Object> nameArgs = new HashMap<String, Object>();
		nameArgs.put("key1", "value1");
		RpcPacket packet = RpcPacket.constructRpcPacket(RpcFunctionSpec.CLIENT_ACK, nameArgs, null);
		RpcStreamConnection.RpcPacketSupplier supplier = createMockRpcPacketSupplier();
		mockConnection.processNameArgs(packet, supplier);

		verifyMockRpcPacketSupplier(supplier, nameArgs.size());
	}

	@Test
	public void processStringArgs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		String[] args = { "value1", "value2" };
		RpcPacket packet = RpcPacket.constructRpcPacket(RpcFunctionSpec.CLIENT_ACK, "test", args, null);
		RpcStreamConnection.RpcPacketSupplier supplier = createMockRpcPacketSupplier();
		mockConnection.processStringArgs(packet, supplier);

		verifyMockRpcPacketSupplier(supplier, args.length);
	}

	@Test
	public void processExternalEnv() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		ExternalEnv externalEnv = mock(ExternalEnv.class);
		byte[] sendBytes = new byte[] { 'd', 'e', 'f' };
		when(externalEnv.marshal()).thenReturn(sendBytes);
		RpcPacket packet = RpcPacket.constructRpcPacket("test", null, externalEnv);
		RpcStreamConnection.RpcPacketSupplier supplier = createMockRpcPacketSupplier();
		mockConnection.processExternalEnv(packet, supplier);

		verifyMockRpcPacketSupplier(supplier, 1);
	}

	@Test
	public void processFuncName() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		String[] args = { "value1" };
		RpcPacket packet = RpcPacket.constructRpcPacket(RpcFunctionSpec.CLIENT_ACK, "test", args, null);
		RpcStreamConnection.RpcPacketSupplier supplier = createMockRpcPacketSupplier();
		mockConnection.processFuncName(packet, supplier);

		verifyMockRpcPacketSupplier(supplier, 1);
	}

	private RpcStreamConnection.RpcPacketSupplier createMockRpcPacketSupplier() {
		RpcStreamConnection.RpcPacketSupplier supplier = mock(RpcStreamConnection.RpcPacketSupplier.class);

		byte[] sendBytes = new byte[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h' };
		when(supplier.sendBytes()).thenReturn(sendBytes);
		when(supplier.sendBytes(any(byte[].class))).thenReturn(supplier);
		when(supplier.sendPos()).thenReturn(7);
		when(supplier.sendPos(anyInt())).thenReturn(supplier);

		return supplier;
	}

	private void verifyMockRpcPacketSupplier(RpcStreamConnection.RpcPacketSupplier supplier, int argsLength) {
		verify(supplier, times(argsLength)).sendBytes();
		verify(supplier, times(argsLength)).sendBytes(any(byte[].class));
		verify(supplier, times(argsLength)).sendPos();
		verify(supplier, times(argsLength)).sendPos(anyInt());
	}

	@Test
	public void reallocateSendBufferInPutPacketIfRunOut()
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		RpcStreamConnection.RpcPacketSupplier supplier = createMockRpcPacketSupplier();
		byte[] fieldBytes = new byte[] { 'a', 'b', 'c' };

		mockConnection.reallocateSendBufferInPutPacketIfRunOut(supplier, fieldBytes, 1);
		verifyMockRpcPacketSupplier(supplier, 1);
	}

	@Test
	public void calculatePreambleBytesAndSendtoDownstream()
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException,
			ConnectionException {
		RpcStreamConnection.RpcPacketSupplier supplier = createMockRpcPacketSupplier();
		mockConnection.calculatePreambleBytesAndSendtoDownstream(supplier);
		verify(topOutputStream).flush();
		verify(topOutputStream).write(any(byte[].class), eq(0), anyInt());
	}

	// p4ic4idea: changed exception
	@Test(expected=ConnectionException.class)
	public void calculatePreambleBytesAndSendtoDownstream_with_SocketTimeoutException()
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException,
			ConnectionException {
		RpcStreamConnection.RpcPacketSupplier supplier = createMockRpcPacketSupplier();
		doThrow(SocketTimeoutException.class).when(topOutputStream).write(any(), eq(0), anyInt());
		mockConnection.calculatePreambleBytesAndSendtoDownstream(supplier);
	}

	@Test
	public void putRpcPackets_null_rpcPackets() throws Exception {
		assertThrows(NullPointerException.class, () -> mockConnection.putRpcPackets(null));
	}

	@Test
	public void putRpcPackets_empty_rpcPackets() throws Exception {
		long retVal = mockConnection.putRpcPackets(new RpcPacket[0]);
		assertThat(retVal, is(0L));
	}

	@Test
	public void putRpcPackets_size_equal_2_rpcPackets_() throws Exception {
		RpcPacket rpcPacket1 = mock(RpcPacket.class);
		when(rpcPacket1.getFuncNameString()).thenReturn("add");
		RpcPacket rpcPacket2 = mock(RpcPacket.class);
		when(rpcPacket2.getFuncNameString()).thenReturn("sync");
		RpcPacket[] rpcPackets = new RpcPacket[] { rpcPacket1, rpcPacket2 };
		long retVal = mockConnection.putRpcPackets(rpcPackets);
		assertThat(retVal, is(0L));
		verify(rpcPacket1, times(2)).getFuncNameString();
		verify(rpcPacket2, times(2)).getFuncNameString();
		verify(rpcPacket1).getMapArgs();
		verify(rpcPacket1).getStrArgs();
		verify(rpcPacket1).getEnv();
	}

	@Test
	public void getSystemSendBufferSize() throws Exception {
		int systemSendBufferSize = mockConnection.getSystemSendBufferSize();
		assertThat(systemSendBufferSize, is(socketBufferSize));
	}

	@Test
	public void getSystemSendBufferSize_with_exception() throws Exception {
		reset(socket);
		doThrow(SocketException.class).when(socket).getSendBufferSize();
		int systemSendBufferSize = mockConnection.getSystemSendBufferSize();
		assertThat(systemSendBufferSize, is(0));
	}

	@Test
	public void getSystemRecvBufferSize() throws Exception {
		int systemRecvBufferSize = mockConnection.getSystemRecvBufferSize();
		assertThat(systemRecvBufferSize, is(recvBufferSize));
	}

	@Test
	public void getSystemRecvBufferSize_with_exception() throws Exception {
		reset(socket);
		doThrow(SocketException.class).when(socket).getReceiveBufferSize();
		int systemRecvBufferSize = mockConnection.getSystemRecvBufferSize();
		assertThat(systemRecvBufferSize, is(0));
	}

	@Test
	public void initRshModeServer_withException() {
		mockConnection.rsh(null);
		// p4ic4idea: original throws NPE.
		assertThrows(ConnectionException.class, () -> mockConnection.initRshModeServer());
	}

	// p4ic4idea: this fails if the rsh is not an executable file.
	@Test
	public void initRshModeServer_badFile()
			throws ConnectionException {
		mockConnection.rsh("not-a-valid-file");

		assertThrows(ConnectionException.class, () -> mockConnection.initRshModeServer());
	}

	@Test
	public void initRshModeServer()
			throws ConnectionException {
		// p4ic4idea: this fails if the rsh is not an executable file.
		mockConnection.rsh(Server.isRunningOnWindows() ? "cmd.exe" : "/bin/sh");

		mockConnection.initRshModeServer();
	}

	@Test
	public void initSocketBasedServer_orignal_socket_isNull()
			throws IOException, ConnectionException {
		mockConnection.socket(null);
		mockConnection.initSocketBasedServer();
		verify(rpcSocketPool).acquire();
	}

	@Test
	public void initSocketBasedServer_with_exceptions()
			throws Throwable {
		mockConnection.socket(null);

		reset(rpcSocketPool);
		doThrow(UnknownHostException.class).when(rpcSocketPool).acquire();
		// p4ic4idea: explicit change to throw a ConnectionException; was UnknownHostException
		assertThrows(ConnectionException.class, () -> mockConnection.initSocketBasedServer());

		reset(rpcSocketPool);
		doThrow(IOException.class).when(rpcSocketPool).acquire();
		// p4ic4idea: explicit change to throw a ConnectionException; was IOException
		assertThrows(ConnectionException.class, () -> mockConnection.initSocketBasedServer());

		// p4ic4idea: Throwable is never explicitly caught.  Try a runtime instead
		reset(rpcSocketPool);
		doThrow(RuntimeException.class).when(rpcSocketPool).acquire();
		assertThrows(ConnectionException.class, () -> mockConnection.initSocketBasedServer());
	}

	@Test
	public void initSocketBasedServer()
			throws ConnectionException {
		mockConnection.initSocketBasedServer();
	}

	@Test
	public void initRpcSocketInputAndOutputStreamIfSocketBasedServer()
			throws ConnectionException {
		mockConnection.initRpcSocketInputAndOutputStreamIfSocketBasedServer();
	}

	@Test
	public void initRpcSocketInputAndOutputStreamIfSocketBasedServer_with_exception() {
		mockConnection.socket(null);
		assertThrows(NullPointerError.class,
				() -> mockConnection.initRpcSocketInputAndOutputStreamIfSocketBasedServer());
	}

	@Test
	public void initSSL()
			throws SSLPeerUnverifiedException,
			CertificateNotYetValidException, CertificateExpiredException, ConnectionException {
		//Method initSSL = getPrivateMethod(RpcStreamConnection.class, "initSSL");
		PublicKey serverPubKey = mock(PublicKey.class);
		when(serverPubKey.getEncoded()).thenReturn(new byte[] { 'a', 'b', 'c' });

		X509Certificate certificate = mock(X509Certificate.class);
		when(certificate.getPublicKey()).thenReturn(serverPubKey);
		doNothing().when(certificate).checkValidity();

		SSLSession sslSession = mock(SSLSession.class);
		when(sslSession.isValid()).thenReturn(true);
		Certificate[] serverCerts = new Certificate[] { certificate };
		when(sslSession.getPeerCertificates()).thenReturn(serverCerts);

		SSLSocket sslSocket = mock(SSLSocket.class);
		when(sslSocket.getSession()).thenReturn(sslSession);
		mockConnection.socket(sslSocket);
		mockConnection.initSSL();

		reset(sslSession);
		when(sslSession.isValid()).thenReturn(true);
		assertThrows(SslException.class, () -> mockConnection.initSSL());

		reset(sslSession);
		when(sslSession.isValid()).thenReturn(true);
		when(sslSession.getPeerCertificates()).thenReturn(null);
		assertThrows(SslException.class, () -> mockConnection.initSSL());

		reset(sslSession);
		when(sslSession.isValid()).thenReturn(true);
		when(sslSession.getPeerCertificates()).thenReturn(serverCerts);
		reset(certificate);
		doNothing().when(certificate).checkValidity();
		when(certificate.getPublicKey()).thenReturn(null);
		assertThrows(SslException.class, () -> mockConnection.initSSL());

		reset(sslSession);
		when(sslSession.isValid()).thenReturn(true);
		when(sslSession.getPeerCertificates()).thenReturn(serverCerts);
		reset(certificate);
		doThrow(CertificateExpiredException.class).when(certificate).checkValidity();
		assertThrows(SslException.class, () -> mockConnection.initSSL());
	}

	@Test
	public void initSSL_with_exceptions() throws IllegalAccessException, IllegalArgumentException {
		SSLSocket sslSocket = mock(SSLSocket.class);
		doThrow(CertificateExpiredException.class).when(sslSocket).getSession();
		mockConnection.socket(sslSocket);
		assertThrows(SslException.class, () -> mockConnection.initSSL());

		reset(sslSocket);
		doThrow(CertificateNotYetValidException.class).when(sslSocket).getSession();
		assertThrows(SslException.class, () -> mockConnection.initSSL());

		reset(sslSocket);
		doThrow(NoSuchAlgorithmException.class).when(sslSocket).getSession();
		assertThrows(SslException.class, () -> mockConnection.initSSL());

		// getSession() cannot throw IOException
		//reset(sslSocket);
		//doThrow(IOException.class).when(sslSocket).getSession();
		//assertConnectionException(mockConnection, initSSL, IOException.class);
	}

	@Test
	public void continueReadIfGetPartialRead()
			throws IOException, ConnectionException {
		byte[] preambleBytes = { 'a', 'b', 0, 0, 0 };
		AtomicLong atomicLong = mock(AtomicLong.class);
		when(topInputStream.read(any(), anyInt(), anyInt())).thenReturn(3);
		int bytesRead = mockConnection.continueReadIfGetPartialRead(preambleBytes, 2, atomicLong);
		assertThat(bytesRead, is(5));
		verify(topInputStream).read(any(), anyInt(), anyInt());
	}

	@Test
	public void continueReadIfGetPartialRead_expecte_exception()
			throws InvocationTargetException, IllegalAccessException, IOException {
		byte[] preambleBytes = { 'a', 'b', 0, 0, 0 };
		AtomicLong atomicLong = mock(AtomicLong.class);
		when(topInputStream.read(any(), anyInt(), anyInt())).thenReturn(-1);
		assertThrows(ConnectionException.class,
				() -> mockConnection.continueReadIfGetPartialRead(preambleBytes, 2, atomicLong));
	}

	@Test
	public void continueReadIfGetPartialRead_dont_have_left_bytes_to_read()
			throws IOException, ConnectionException {
		int bytesRead = mockConnection.continueReadIfGetPartialRead(new byte[] { 'a', 'b' }, -1,
				new AtomicLong(0));
		assertThat(bytesRead, is(-1));
	}

	@Test
	public void continueReadIfIncompleteRead()
			throws IOException, ConnectionException {
		int packetBytesRead = 2;
		int payloadLength = 5;
		when(topInputStream.read(any(), anyInt(), anyInt())).thenReturn(3);
		int bytesRead = mockConnection.continueReadIfIncompleteRead(new AtomicLong(0), payloadLength,
				new byte[] { 'a', 'b', 0, 0, 0 }, packetBytesRead);
		assertThat(bytesRead, is(5));
		verify(topInputStream).read(any(), anyInt(), anyInt());
	}

	@Test
	public void continueReadIfIncompleteRead_dont_have_left_bytes_to_read()
			throws IOException, ConnectionException {
		int payloadLength = 2;
		int packetBytesRead = payloadLength + 1;
		int bytesRead = mockConnection.continueReadIfIncompleteRead(new AtomicLong(0), payloadLength,
				new byte[] { 'a', 'b' }, packetBytesRead);
		assertThat(bytesRead, is(packetBytesRead));
	}

	@Test
	public void continueReadIfIncompleteRead_expecte_exception()
			throws IOException {
		int packetBytesRead = 2;
		int payloadLength = packetBytesRead + 2;
		when(topInputStream.read(any(), anyInt(), anyInt())).thenReturn(-1);
		assertThrows(ConnectionException.class, () -> mockConnection.continueReadIfIncompleteRead(
				new AtomicLong(0), payloadLength, new byte[] { 'a', 'b' }, packetBytesRead));
	}

	@Test
	public void getRpcPacket() throws IOException, ConnectionException {
		RpcPacketFieldRule fieldRule = mock(RpcPacketFieldRule.class);
		IFilterCallback filterCallback = mock(IFilterCallback.class);

		reset(topInputStream);
		byte[] preambleBytes = { -116, -116, 0, 0, 0 };
		byte[] payloadBytes = { 120, 102, 105, 108, 101, 115, 0, 1, 0, 0, 0, 55, 0, 115, 101, 114, 118, 101, 114, 0, 1,
				0, 0, 0, 51, 0, 115, 101, 114, 118, 101, 114, 50, 0, 2, 0, 0, 0, 52, 49, 0, 115, 101, 114, 118, 101,
				114, 73, 68, 0, 0, 0, 0, 0, 0, 114, 101, 118, 118, 101, 114, 0, 1, 0, 0, 0, 57, 0, 116, 122, 111, 102,
				102, 115, 101, 116, 0, 4, 0, 0, 0, 55, 50, 48, 48, 0, 115, 110, 100, 98, 117, 102, 0, 6, 0, 0, 0, 51,
				49, 57, 52, 56, 55, 0, 114, 99, 118, 98, 117, 102, 0, 6, 0, 0, 0, 51, 49, 57, 52, 56, 56, 0, 102, 117,
				110, 99, 0, 8, 0, 0, 0, 112, 114, 111, 116, 111, 99, 111, 108, 0 };
		byte[] buf = new byte[preambleBytes.length + payloadBytes.length];
		System.arraycopy(preambleBytes, 0, buf, 0, preambleBytes.length);
		System.arraycopy(payloadBytes, 0, buf, preambleBytes.length, payloadBytes.length);
		topInputStream = new ByteArrayInputStream(buf);
		mockConnection.topInputStream(topInputStream);

		RpcPacket rpcPacket = mockConnection.getRpcPacket(fieldRule, filterCallback);
		assertNotNull(rpcPacket);
		assertThat(rpcPacket.getPacketLength(), is(payloadBytes.length));
	}

	@Test
	public void getRpcPacket_with_exceptions() throws IOException, ConnectionException {
		RpcPacketFieldRule fieldRule = mock(RpcPacketFieldRule.class);
		IFilterCallback filterCallback = mock(IFilterCallback.class);

		doThrow(IOException.class).when(topInputStream).read(any());
		assertThrows(ConnectionException.class, () -> mockConnection.getRpcPacket(fieldRule, filterCallback));

		reset(topInputStream);
		doThrow(ConnectionException.class).when(topInputStream).read(any());
		assertThrows(ConnectionException.class, () -> mockConnection.getRpcPacket(fieldRule, filterCallback));

		reset(topInputStream);
		doThrow(P4JavaError.class).when(topInputStream).read(any());
		assertThrows(P4JavaError.class, () -> mockConnection.getRpcPacket(fieldRule, filterCallback));

		// Throwable is never caught explicitly.  Try a runtime exception instead.
		reset(topInputStream);
		doThrow(RuntimeException.class).when(topInputStream).read(any());
		assertThrows(P4JavaError.class, () -> mockConnection.getRpcPacket(fieldRule, filterCallback));
	}
}