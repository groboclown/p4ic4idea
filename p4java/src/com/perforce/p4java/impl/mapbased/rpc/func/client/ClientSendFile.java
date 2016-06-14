/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.CharsetConverter;
import com.perforce.p4java.Log;
import com.perforce.p4java.ILookahead;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
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
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcLineEndFilterInputStream;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFile;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;

/**
 * Implements some specialised file methods for sending file
 * data back to the Perforce server. May be refactored into
 * other classes later when the scope of the methods here
 * is clearer -- HR.
 * 
 *
 */

public class ClientSendFile {
	
	/**
	 * TRACE_PREFIX
	 */
	public static final String TRACE_PREFIX = "ClientSendFile";
	
	/**
	 * DEFAULT_SENDBUF_SIZE
	 */
	public static final int DEFAULT_SENDBUF_SIZE = 1024;	// in bytes
	
	@SuppressWarnings("unused")  // used for debugging
	private Properties props = null;
	
	private ISystemFileCommandsHelper fileCommands = SysFileHelperBridge.getSysFileCommands();
	
	// Keeping track of file data progress info
	private String filePath = null;
	private long fileSize = 0;
	private long currentSize = 0;
	
	/**
	 * Create a new rpc file sender 
	 * 
	 * @param props
	 */
	protected ClientSendFile(Properties props) {
		this.props = props;
	}
	
	private long sendRaw(InputStream stream, RpcConnection connection,
			String handle, String write, MD5Digester digester,
			CommandEnv cmdEnv)
			throws ConnectionException, IOException {
		long fileLength = 0;

		Map<String, Object> sendMap = new HashMap<String, Object>();
		byte[] bytes = new byte[DEFAULT_SENDBUF_SIZE];
		int bytesRead = 0;
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
			
			// Send back the data bytes written (accumulated)
			// This is for the progress indicator
			if (cmdEnv.getProtocolSpecs().isEnableProgress()) {
				if (fileSize > 0 && bytesRead > 0) {
					currentSize += bytesRead;
					Map<String, Object> dataSizeMap = new HashMap<String, Object>();
					dataSizeMap.put("path", filePath);
					dataSizeMap.put("fileSize", fileSize);
					dataSizeMap.put("currentSize", currentSize);
					cmdEnv.handleResult(dataSizeMap);
				}
			}
		}
		return fileLength;
	}
	
	private ILookahead createLookahead(final InputStream stream,
			Charset charset) {
		CharsetConverter newlineConverter = new CharsetConverter(
				CharsetDefs.DEFAULT, charset, true);
		ByteBuffer newlineBuffer = newlineConverter.convert(CharBuffer
				.wrap(new char[] { ClientLineEnding.FST_L_LF_CHAR }));
		final int lookaheadLength = newlineBuffer.limit();
		ILookahead lookahead = new ILookahead() {

			public byte[] bytesToAdd(char lastDecodedChar) {
				byte[] add = null;
				if (lastDecodedChar == ClientLineEnding.FST_L_CR_CHAR) {
					add = new byte[lookaheadLength];
					try {
						int read = stream.read(add);
						if (read != add.length) {
							byte[] realAdd = new byte[read];
							System.arraycopy(add, 0, realAdd, 0, read);
							add = realAdd;
						}
					} catch (IOException e) {
						add = null;
					}
				}
				return add;
			}
		};
		return lookahead;
	}
	
	private long sendConverted(Charset charset,final InputStream stream,
			RpcConnection connection, String handle, String write,
			MD5Digester digester, CommandEnv cmdEnv) throws ConnectionException,
			IOException {
		long fileLength = 0;
		
		Map<String, Object> sendMap = new HashMap<String, Object>();
		byte[] bytes = new byte[DEFAULT_SENDBUF_SIZE];
		int bytesRead = 0;

		ILookahead lookahead = null;
		if (ClientLineEnding.CONVERT_TEXT) {
			lookahead = createLookahead(stream, charset);
		}
		
		CharsetConverter converter = new CharsetConverter(charset, CharsetDefs.UTF8);
		while ((bytesRead = stream.read(bytes)) > 0) {
			ByteBuffer inBuffer = ByteBuffer.wrap(bytes, 0, bytesRead);
			ByteBuffer converted = converter.convert(inBuffer, lookahead);
			byte[] sendBytes = converted.array();
			bytesRead = converted.limit();
			int start = converted.position();
			
			//Send packet if at least one byte of data was converted
			if (bytesRead > 0) {
				// Convert line endings if necessary
				if (ClientLineEnding.CONVERT_TEXT) {
					// Create intermediate stream for converting line ending
					// based on the last byte buffer of converted data
					ByteArrayInputStream byteStream = new ByteArrayInputStream(
							sendBytes, 0, bytesRead);
					RpcLineEndFilterInputStream lineEndStream = new RpcLineEndFilterInputStream(
							byteStream, null);
					bytesRead = lineEndStream.read(sendBytes, 0, bytesRead);
					lineEndStream.close();
				}
				byte[] dataBytes = new byte[bytesRead];
				System.arraycopy(sendBytes, start, dataBytes, 0, bytesRead);
				fileLength += bytesRead;
				sendMap.clear();
				sendMap.put(RpcFunctionMapKey.DATA, dataBytes);
				sendMap.put(RpcFunctionMapKey.HANDLE, handle);

				RpcPacket sendPacket = RpcPacket.constructRpcPacket(
						write, sendMap, null);

				connection.putRpcPacket(sendPacket);
				digester.update(dataBytes);

				// Send back the data bytes written (accumulated)
				// This is for the progress indicator
				if (cmdEnv.getProtocolSpecs().isEnableProgress()) {
					if (fileSize > 0 && bytesRead > 0) {
						currentSize += bytesRead;
						Map<String, Object> dataSizeMap = new HashMap<String, Object>();
						dataSizeMap.put("path", filePath);
						dataSizeMap.put("fileSize", fileSize);
						dataSizeMap.put("currentSize", currentSize);
						cmdEnv.handleResult(dataSizeMap);
					}
				}
			}
		}
		return fileLength;
	}
	
	/**
	 * Send a file's contents back to the Perforce server. Notably
	 * assumes a late model server...
	 * 
	 * FIXME: digest stuff is not yet implemented -- HR.
	 * FIXME: error handling is typically nearly non-existent -- HR.
	 * FIXME: charset issues -- HR.
	 * FIXME: rework to use proper flush protocol implementation -- HR.
	 * 
	 * @param rpcConnection 
	 * @param cmdEnv 
	 * @param resultsMap 
	 * @return - result
	 * @throws ConnectionException 
	 */
	protected RpcPacketDispatcherResult sendFile(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {
		
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
		RpcPerforceFileType fileType = RpcPerforceFileType.decodeFromServerString(type);
		
		RpcHandler handler = cmdEnv.getHandler(handle);
		
		if (handler == null) {
			handler = cmdEnv.new RpcHandler(handle, false, null);
			cmdEnv.addHandler(handler);
		}

		RpcPerforceFile file = null;
		InputStream inStream = null;
		MD5Digester digester = null;
		long fileLength = 0;	// Oh for a Java unsigned type...
		long modTime = 0;
		
		try {
			file = new RpcPerforceFile(clientPath, fileType);
			modTime = file.lastModified();
			
			// Symlink to an non-existing target will reuturn '0' from the
			// 'File.lastModified()' method. Also, this method only returns the
			// last modified time of it's target. So, we must use the new 
			// 'SymbolicLinkHelper.getLastModifiedTime()' method to get the last
			// modified time of the symlink (not it's target).
			if (fileType == RpcPerforceFileType.FST_SYMLINK) {
				// Java returns '0' if the file does not exist or if an I/O error occurs
				// Use the symbolic link helper to get the last modified time.
				modTime = SymbolicLinkHelper.getLastModifiedTime(clientPath);

				 // If all else fails, use the current time milli.
				if (modTime == 0) {
					modTime = System.currentTimeMillis();
				}
			}
			
			// Initialize file data info for progress indicator
			filePath = clientPath != null ? clientPath : null;
			fileSize = file.length();
			currentSize = 0;
			
			if (!handler.isError()) {
			
				// If everything's OK, send an open (lbr-Open) packet with passed-through
				// map entries, then write the file to the server using looped
				// lbr-WriteFile packets, then send a dm-SubmitFile.
				
				Map<String,Object> respMap = new HashMap<String, Object>();
	
				for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
					if ((entry.getKey() != null) && !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.FUNCTION)) {
						respMap.put(entry.getKey(), entry.getValue());
					}
				}
				
				RpcPacket respPacket = RpcPacket.constructRpcPacket(
																open,
																respMap,
																null);
				
				rpcConnection.putRpcPacket(respPacket);
				
				// Note: as per job 036870, we need to defer the existence testing for the
				// file to here (we could have detected its non-existence on the first line
				// of the method) as we always need to send an lbr-Open function back to
				// the server to keep things straight. This is apparently a vestige of the
				// early protocol and state machine design -- HR.
				
				if (!file.exists() && fileType != RpcPerforceFileType.FST_SYMLINK) {
					handler.setError(true);
					cmdEnv.handleResult(
							new RpcMessage(
									ClientMessageId.OS_FILE_READ_ERROR,
									MessageSeverityCode.E_INFO,
									MessageGenericCode.EV_CLIENT,
									new String[] {"open for read", clientPath + ": No such file or directory"}
								).toMap()
						);
				} else {
					// Now to send the file contents:

					String symbolicLinkTarget = null;

					// Check if the file is a symlink type
					if (fileType == RpcPerforceFileType.FST_SYMLINK) {
						
						// Read the target of the symlink
						if (SymbolicLinkHelper.isSymbolicLinkCapable()) {
							symbolicLinkTarget = SymbolicLinkHelper.readSymbolicLink(clientPath);
							// Appending "\n" for depot archive symlink file storage
							// See job078811
							if (symbolicLinkTarget != null) {
								symbolicLinkTarget += "\n";
							}
						}
						
						if (symbolicLinkTarget == null) {
							handler.setError(true);
							cmdEnv.handleResult(
										new RpcMessage(
												ClientMessageId.FILE_SEND_ERROR,
												MessageSeverityCode.E_FAILED,
												MessageGenericCode.EV_CLIENT,
												new String[] {"symlink", clientPath}
											).toMap()
									);
							return RpcPacketDispatcherResult.CONTINUE_LOOP;
						}
						
					}
					
					digester = new MD5Digester();
					
					Charset charset = null;
					if (RpcPerforceFileType.FST_UTF16 == fileType) {
						charset = CharsetDefs.UTF16;
					} else if (RpcPerforceFileType.FST_UNICODE == fileType) {
						charset = rpcConnection.getClientCharset();
					}
					if ((!rpcConnection.isUnicodeServer() && charset != CharsetDefs.UTF16) || charset == null || charset.equals(CharsetDefs.UTF8)) {
						inStream = symbolicLinkTarget != null ? new ByteArrayInputStream(
								symbolicLinkTarget.getBytes())
								: new RpcInputStream(file);
						fileLength = sendRaw(inStream, rpcConnection, handle,
								write, digester, cmdEnv);
					} else {
						inStream = symbolicLinkTarget != null ? new ByteArrayInputStream(
								symbolicLinkTarget.getBytes())
								: new FileInputStream(file);
						fileLength = sendConverted(charset, inStream,
								rpcConnection, handle, write, digester, cmdEnv);
					}
					
					// All sent; now try to set the perms properly if appropriate:
					
					if (!handler.isError() && (perms != null) && (reopen == null)) {
						if (perms.equalsIgnoreCase(ClientSystemFileCommands.PERMS_RW)) {
							fileCommands.setWritable(clientPath, true);
						} else {
							fileCommands.setWritable(clientPath, false);
						}
					}
				}
			}
			
			// Now try to send a finalise message:
			
			String finalise = (handler.isError() ? decline : confirm);
			Map<String, Object> finaliseMap = new HashMap<String,Object>();			
			
			for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
				if ((entry.getKey() != null) && !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.FUNCTION)) {
					finaliseMap.put(entry.getKey(), entry.getValue());
				}
			}
			
			if (digester != null) {
				finaliseMap.put(RpcFunctionMapKey.DIGEST, digester.digestAs32ByteHex());
				finaliseMap.put(RpcFunctionMapKey.FILESIZE, "" + fileLength);
				if (modTime != 0) {
					finaliseMap.put(RpcFunctionMapKey.TIME, "" + (modTime / 1000));
				}
			}
			
			RpcPacket finalisePacket = RpcPacket.constructRpcPacket(
														finalise,
														finaliseMap,
														null);
			
			rpcConnection.putRpcPacket(finalisePacket);
			
			// We now clear any errors, again as a vestige of earlier protocol
			// design decisions; we'd normally let the error be ack'd back to the
			// server as a client-side abort, but we need to let the server think
			// everything went fine here and let it send its own error message(s) -- HR.
			
			handler.setError(false);
			
			
			// Clear data file info for progress indicator
			filePath = null;
			fileSize = 0;
			currentSize = 0;
			
		} catch (Exception exc) {
			Log.exception(exc);
			handler.setError(true);
			cmdEnv.handleResult(
						new RpcMessage(
							ClientMessageId.FILE_SEND_ERROR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[] {clientPath, exc.getLocalizedMessage()}
						).toMap()
				);
		} finally {
			try {
				if (inStream != null) {
					inStream.close();
				}
			} catch (IOException exc) {
				Log.warn("Unexpected exception on send file close; file: '" 
								+ clientPath + "'; message: "+ exc.getLocalizedMessage());
			}
		}
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
}
