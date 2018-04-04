/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv.RpcHandler;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientMessage.ClientMessageId;
import com.perforce.p4java.impl.mapbased.rpc.func.helper.MD5Digester;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher.RpcPacketDispatcherResult;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcInputStream;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFile;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.impl.mapbased.rpc.func.client.ClientHelper.sendBackWrittenDataBytes;

/**
 * Implements some specialised file methods for sending file data back to the
 * Perforce server. May be refactored into other classes later when the scope of
 * the methods here is clearer -- HR.
 */
public class ClientSendFile {
	public static final String TRACE_PREFIX = "ClientSendFile";

	@SuppressWarnings("unused") // used for debugging
	private Properties props = null;

	private final ISystemFileCommandsHelper fileCommands = SysFileHelperBridge.getSysFileCommands();
	// Keeping track of file data progress info
	private String filePath = null;
	private long fileSize = 0;
	private long currentSize = 0;

	/**
	 * Create a new rpc file sender
	 */
	protected ClientSendFile(Properties props) {
		this.props = props;
	}
	
	private long sendStream(InputStream stream, RpcConnection connection, String handle, String write,
			MD5Digester digester, CommandEnv cmdEnv) throws ConnectionException, IOException {
		long fileLength = 0;
		
		Map<String, Object> sendMap = new HashMap<String, Object>();
		byte[] bytes = new byte[1024 * 64];
		int bytesRead;
		while ((bytesRead = stream.read(bytes)) > 0) {
			byte[] readBytes = new byte[bytesRead];
			
			System.arraycopy(bytes, 0, readBytes, 0, bytesRead);
			fileLength += bytesRead;
			sendMap.clear();
			sendMap.put(RpcFunctionMapKey.DATA, readBytes);
			sendMap.put(RpcFunctionMapKey.HANDLE, handle);

			RpcPacket sendPacket = RpcPacket.constructRpcPacket(write,
					sendMap, null);
			
			connection.putRpcPacket(sendPacket);
			digester.update(readBytes);

			currentSize = sendBackWrittenDataBytes(cmdEnv, filePath, fileSize, currentSize, bytesRead);
		}
		return fileLength;
	}
	
	/**
	 * Send a file's contents back to the Perforce server. Notably assumes a
	 * late model server...
	 *
	 * FIXME: digest stuff is not yet implemented -- HR.
	 * FIXME: error handling is typically nearly non-existent -- HR.
	 * FIXME: charset issues -- HR.
	 * FIXME: rework to use proper flush protocol implementation -- HR.
	 *
	 * @return - result
	 */
	protected RpcPacketDispatcherResult sendFile(RpcConnection rpcConnection, CommandEnv cmdEnv,
			Map<String, Object> resultsMap) {

		cmdEnv.newHandler();
		String clientPath = (String) resultsMap.get(RpcFunctionMapKey.PATH);
		String type = (String) resultsMap.get(RpcFunctionMapKey.TYPE);
		String perms = (String) resultsMap.get(RpcFunctionMapKey.PERMS);
		String handle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
		String open = (String) resultsMap.get(RpcFunctionMapKey.OPEN);
		String write = (String) resultsMap.get(RpcFunctionMapKey.WRITE);
		String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
		String decline = (String) resultsMap.get(RpcFunctionMapKey.DECLINE);
		@SuppressWarnings("unused") // used for debugging
		String serverDigest = (String) resultsMap.get(RpcFunctionMapKey.SERVERDIGEST);
		@SuppressWarnings("unused") // used for debugging
		String revertUnchanged = (String) resultsMap.get(RpcFunctionMapKey.REVERTUNCHANGED);
		String reopen = (String) resultsMap.get(RpcFunctionMapKey.REOPEN);
		RpcHandler handler = cmdEnv.getHandler(handle);

		if (handler == null) {
			handler = cmdEnv.new RpcHandler(handle, false, null);
			cmdEnv.addHandler(handler);
		}

		RpcPerforceFile file = null;
		InputStream inStream = null;
		MD5Digester digester = null;
		long fileLength = 0; // Oh for a Java unsigned type...
		long modTime = 0;

		try {
			try {
				file = new RpcPerforceFile(clientPath, type);
				modTime = file.lastModified();

				// Symlink to an non-existing target will reuturn '0' from the
				// 'File.lastModified()' method. Also, this method only returns
				// the
				// last modified time of it's target. So, we must use the new
				// 'SymbolicLinkHelper.getLastModifiedTime()' method to get the
				// last
				// modified time of the symlink (not it's target).
				if (file.getFileType() == RpcPerforceFileType.FST_SYMLINK) {
					// Java returns '0' if the file does not exist or if an I/O
					// error occurs
					// Use the symbolic link helper to get the last modified
					// time.
					modTime = SymbolicLinkHelper.getLastModifiedTime(clientPath);

					// If all else fails, use the current time milli.
					if (modTime == 0) {
						modTime = System.currentTimeMillis();
					}
				}

				// Initialize file data info for progress indicator
				filePath = clientPath != null && !clientPath.isEmpty() ? clientPath : null;

				fileSize = file.length();
				currentSize = 0;

				if (!handler.isError()) {
					// If everything's OK, send an open (lbr-Open) packet with
					// passed-through
					// map entries, then write the file to the server using
					// looped
					// lbr-WriteFile packets, then send a dm-SubmitFile.
					Map<String, Object> respMap = new HashMap<>();

					for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
						String key = entry.getKey();
						if (key != null && !RpcFunctionMapKey.FUNCTION.equalsIgnoreCase(key)) {
							respMap.put(entry.getKey(), entry.getValue());
						}
					}

					RpcPacket respPacket = RpcPacket.constructRpcPacket(open, respMap, null);

					rpcConnection.putRpcPacket(respPacket);

					// Note: as per job 036870, we need to defer the existence
					// testing for the
					// file to here (we could have detected its non-existence on
					// the first line
					// of the method) as we always need to send an lbr-Open
					// function back to
					// the server to keep things straight. This is apparently a
					// vestige of the
					// early protocol and state machine design -- HR.
					if (!file.exists() && file.getFileType() != RpcPerforceFileType.FST_SYMLINK) {
						handler.setError(true);
						cmdEnv.handleResult(new RpcMessage(ClientMessageId.OS_FILE_READ_ERROR,
								MessageSeverityCode.E_INFO, MessageGenericCode.EV_CLIENT,
								new String[] { "open for read", clientPath + ": No such file or directory" }).toMap());
					} else {
						// Now to send the file contents:
						String symbolicLinkTarget = null;
						// Check if the file is a symlink type
						if (file.getFileType() == RpcPerforceFileType.FST_SYMLINK) {
							// Read the target of the symlink
							if (SymbolicLinkHelper.isSymbolicLinkCapable()) {
								symbolicLinkTarget = SymbolicLinkHelper.readSymbolicLink(clientPath);
								// Appending "\n" for depot archive symlink file
								// storage
								// See job078811
								if (symbolicLinkTarget != null) {
									symbolicLinkTarget += "\n";
								}
							}

							if (symbolicLinkTarget == null) {
								handler.setError(true);
								cmdEnv.handleResult(
										new RpcMessage(ClientMessageId.FILE_SEND_ERROR, MessageSeverityCode.E_FAILED,
												MessageGenericCode.EV_CLIENT, new String[] { "symlink", clientPath })
														.toMap());
								return RpcPacketDispatcherResult.CONTINUE_LOOP;
							}
						}

						digester = new MD5Digester();
						
						Charset fileCharset = null;
						if (RpcPerforceFileType.FST_UTF16 == file.getFileType()
								|| RpcPerforceFileType.FST_XUTF16 == file.getFileType()) {
							fileCharset = CharsetDefs.UTF16;
						} else if (RpcPerforceFileType.FST_UTF8 == file.getFileType()
								|| RpcPerforceFileType.FST_XUTF8 == file.getFileType()) {
							fileCharset = CharsetDefs.UTF8;
						} else if (RpcPerforceFileType.FST_UNICODE == file.getFileType()
								|| RpcPerforceFileType.FST_XUNICODE == file.getFileType()) {
							// Server might have sent explicit charset here...
							fileCharset = rpcConnection.getClientCharset();
						}
						if ((!rpcConnection.isUnicodeServer() && fileCharset != CharsetDefs.UTF16)
								|| fileCharset == null
								|| fileCharset.equals(CharsetDefs.UTF8) ) {
							/* 
							 * Not unicode enabled p4d server and filetype is utf-16
							 * Or unicode enabled p4d server and filetype is not unicode or we're already in the target charset
							 */
							inStream = symbolicLinkTarget == null
									? new RpcInputStream(file, null)
									: new ByteArrayInputStream(symbolicLinkTarget.getBytes()); // need to convert symbolicLinkTarget to utf8
						} else {
							
							inStream = symbolicLinkTarget == null
									? new RpcInputStream(file, fileCharset)
									: new ByteArrayInputStream(symbolicLinkTarget.getBytes());
						}

						fileLength = sendStream(inStream, rpcConnection, handle, write, digester, cmdEnv);

						// All sent; now try to set the perms properly if
						// appropriate:
						if (!handler.isError() && (nonNull(perms) && isNull(reopen))) {
							boolean writable = perms.equalsIgnoreCase(ClientSystemFileCommands.PERMS_RW);
							fileCommands.setWritable(clientPath, writable);
						}
					}
				}

				// Now try to send a finalise message:
				String finalise = (handler.isError() ? decline : confirm);
				Map<String, Object> finaliseMap = new HashMap<>();

				for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
					String key = entry.getKey();
					if (nonNull(key) && !RpcFunctionMapKey.FUNCTION.equalsIgnoreCase(key)) {
						finaliseMap.put(entry.getKey(), entry.getValue());
					}
				}

				if (digester != null) {
					finaliseMap.put(RpcFunctionMapKey.DIGEST, digester.digestAs32ByteHex());
					finaliseMap.put(RpcFunctionMapKey.FILESIZE, String.valueOf(fileLength));
					if (modTime != 0) {
						finaliseMap.put(RpcFunctionMapKey.TIME, String.valueOf(modTime / 1000));
					}
				}

				RpcPacket finalisePacket = RpcPacket.constructRpcPacket(finalise, finaliseMap, null);

				rpcConnection.putRpcPacket(finalisePacket);

				// We now clear any errors, again as a vestige of earlier
				// protocol
				// design decisions; we'd normally let the error be ack'd back
				// to the
				// server as a client-side abort, but we need to let the server
				// think
				// everything went fine here and let it send its own error
				// message(s) -- HR.
				handler.setError(false);

				// Clear data file info for progress indicator
				filePath = null;
				fileSize = 0;
				currentSize = 0;
			} finally {
				if (nonNull(inStream)) {
					inStream.close();
				}
			}
		} catch (Exception exc) {
			Log.exception(exc);
			handler.setError(true);
			cmdEnv.handleResult(new RpcMessage(ClientMessageId.FILE_SEND_ERROR, MessageSeverityCode.E_FAILED,
					MessageGenericCode.EV_CLIENT, new String[] { clientPath, exc.getLocalizedMessage() }).toMap());
		}

		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
}
