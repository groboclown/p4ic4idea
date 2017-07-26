/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.CharsetConverter;
import com.perforce.p4java.Log;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.core.IMapEntry;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.ClientError;
import com.perforce.p4java.exception.ClientFileAccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.ProtocolError;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.generic.core.MapEntry;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.impl.mapbased.rpc.RpcServer;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv.RpcHandler;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientMessage.ClientMessageId;
import com.perforce.p4java.impl.mapbased.rpc.func.helper.MD5Digester;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher.RpcPacketDispatcherResult;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcOutputStream;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFile;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType.RpcServerTypeStringSpec;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.AppleFileHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;
import com.perforce.p4java.util.FilesHelper;

/**
 * Implements the simpler lower-level file commands that typically
 * correspond to system commands such as chmod, delete, etc.
 * 
 *
 */

public class ClientSystemFileCommands {
	
	public static final String TRACE_PREFIX = "ClientSystemFileCommands";
	
	public static final String DEFAULT_TMPFILE_PFX = "p4j";
	public static final String DEFAULT_TMPFILE_SFX = ".p4j";
	
	public static final String SYSTEM_TMPDIR_PROPS_KEY = "java.io.tmpdir";
	public static final String SYSTEM_TMPDIR_DEFAULT = "/tmp";
	
	public static final String PERMS_RW = "rw";
	
	// Keys for stuff left in the command environment between calls:
	
	protected static final String FILE_DELETE_ON_ERR_KEY = "deleteOnError";
	protected static final String FILE_OPEN_PATH_KEY = "openFilePath";
	protected static final String FILE_OPEN_TARGET_FILE_KEY = "targetFile";
	protected static final String FILE_OPEN_TMP_FILE_KEY = "tmpFile";
	protected static final String FILE_OPEN_TMP_STREAM_KEY = "tmpFileStream";
	protected static final String FILE_OPEN_TARGET_STREAM_KEY = "targetFileStream";
	protected static final String FILE_OPEN_ORIG_ARGS_KEY = "origArgs";
	protected static final String FILE_OPEN_MODTIME_KEY = "modTime";
	protected static final String FILE_OPEN_IS_SYMBOLIC_LINK_KEY = "isSymbolicLink";
	
	// Reconcile handler map key for 'skipAdd'
	protected static final String RECONCILE_HANDLER_SKIP_ADD_KEY = "skipAdd";
	
	// Reconcile handle
	private String reconcileHandle = null;
	
	private Properties props = null;
	private RpcServer server = null;
	private ClientIgnoreChecker checker = null;
	
	private String tmpDirName = null;
	
	private ISystemFileCommandsHelper fileCommands
					= SysFileHelperBridge.getSysFileCommands();

	// Keeping track of file data progress info
	private String filePath = null;
	private long fileSize = 0;
	private long currentSize = 0;
	
	protected ClientSystemFileCommands(Properties props, RpcServer server) {
		this.props = props;
		this.server = server;
		this.tmpDirName = RpcPropertyDefs.getProperty(this.props,
							PropertyDefs.P4JAVA_TMP_DIR_KEY,
									System.getProperty(SYSTEM_TMPDIR_PROPS_KEY));
		
		if (tmpDirName == null) {
			// This can really only happen if someone has nuked or played with
			// the JVM's system props before we get here... the default will
			// work for most non-Windows boxes in most cases, and may not be
			// needed in many cases anyway.
			
			tmpDirName = SYSTEM_TMPDIR_DEFAULT;
			
			Log.warn("Unable to get tmp name from P4 props or System; using "
					+ tmpDirName + " instead");
			
		}
	}

	/**
	 * Change the r/w (etc.) mode of a file locally.
	 */
	
	protected RpcPacketDispatcherResult chmodFile(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in chmodFile().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in chmodFile().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in chmodFile().");
		}

		String path = (String) resultsMap.get(RpcFunctionMapKey.PATH);
		String perms = (String) resultsMap.get(RpcFunctionMapKey.PERMS);
		String fileTypeStr = (String) resultsMap.get(RpcFunctionMapKey.TYPE);
		String time = (String) resultsMap.get(RpcFunctionMapKey.TIME);

		RpcPerforceFileType fileType = RpcPerforceFileType.decodeFromServerString(fileTypeStr);
		boolean fstSymlink = (fileType == RpcPerforceFileType.FST_SYMLINK);
		
		try {
			cmdEnv.newHandler();	// As per C++ API...
			
			File targetFile = new RpcPerforceFile(path, fileTypeStr);
						
			if (fileExists(targetFile, fstSymlink)) {
				// FIXME: proper time parsing -- HR.

				if ((time != null)) {
					targetFile.setLastModified(new Long(time));
				}
				
				if (perms != null) {
					// usually in form "rw", etc.; not entirely sure what
					// expected values there are, but inspection of the C++ API
					// suggests that there's "rw" and not much else -- anything
					// else is interpreted as meaning "read-only", which is how
					// we'll act until proven wrong...
										
					if (perms.equalsIgnoreCase(PERMS_RW)) {
						fileCommands.setWritable(path, true);
					} else {
						fileCommands.setWritable(path, false);
					}
				}
				
				switch (fileType) {
					case FST_XTEXT:
					case FST_XBINARY:
						fileCommands.setExecutable(path, true, true);
						break;
					default:
						break;
				}
			} else {
				cmdEnv.handleResult(
						new RpcMessage(
								ClientMessageId.CANT_CHMOD_FILE,
								MessageSeverityCode.E_INFO,
								MessageGenericCode.EV_CLIENT,
								new String[] {path}
							).toMap()
					);
			}
			
		} catch (NumberFormatException nfe) {
			throw new ProtocolError(
					"Unexpected conversion error in ClientSystemFileCommands.chmodFile: "
					+ nfe.getLocalizedMessage());
		// groboclown: allow real exception to be naturally handled
		//} catch (Exception exc) {
		//	// FIXME: better error handling here -- HR.
		//
		//	Log.exception(exc);
		//	throw new ConnectionException(exc.getLocalizedMessage(), exc);
		}
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * Open a client file for writing. We have to process things like NOCLOBBER,
	 * and ensure we write to a temp file if a file already exists, etc. Much of
	 * the logic here is straight from the C++ API, currently minus the OpenDiff
	 * functionality (which will probably be factored out elsewhere).<p>
	 * 
	 * We also have to leave the associated file descriptor (or channel equivalent)
	 * lying around for the rest of the function sequence to be able to pick up.<p>
	 * 
	 * Note that we also now implement the 10.2 sync (etc.) transfer integrity
	 * checks; this is actually fairly easy as it can be done on the raw (un-translated)
	 * data coming in to the write methods (as opposed to checkFile's version, which
	 * has to consider the translated data). We do most of this work in the RpcOutputStream
	 * and RpcPerforceFile classes after setting things up here; closeFile does the final
	 * round up and delivers the verdict. This all only happens if Server.nonCheckedSyncs
	 * is false.<p>
	 * 
	 * The temp file created here will be deleted in the subsequent closeFile() method call.
	 */
	
	protected RpcPacketDispatcherResult openFile(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {
		
		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in openFile().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in openFile().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in openFile().");
		}
		
		String path = (String) resultsMap.get(RpcFunctionMapKey.PATH);
		String clientHandle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
		String modTime = (String) resultsMap.get(RpcFunctionMapKey.TIME);
		String noClobber = (String) resultsMap.get(RpcFunctionMapKey.NOCLOBBER);
		String perms = (String) resultsMap.get(RpcFunctionMapKey.PERMS);
		String fileTypeStr = (String) resultsMap.get(RpcFunctionMapKey.TYPE);
		String fileSizeStr = (String) resultsMap.get(RpcFunctionMapKey.FILESIZE);
		String digest = (String) resultsMap.get(RpcFunctionMapKey.DIGEST); // new 10.2 checked sync digest
		boolean useLocalDigester = useLocalDigester(digest, cmdEnv);

		// Initialize file data info for progress indicator
		filePath = path != null ? path : null;
		fileSize = fileSizeStr != null ? new Long(fileSizeStr) : 0;
		currentSize = 0;
		
		RpcPerforceFileType fileType = RpcPerforceFileType.decodeFromServerString(fileTypeStr);
		boolean fstSymlink = (fileType == RpcPerforceFileType.FST_SYMLINK);
		
		try {
			RpcPerforceFile targetFile = new RpcPerforceFile(path, fileTypeStr);
			RpcPerforceFile tmpFile = null;
			RpcHandler handler = cmdEnv.getHandler(clientHandle);	
			
			if (handler == null) {
				handler = cmdEnv.new RpcHandler(clientHandle, false, targetFile);
				cmdEnv.addHandler(handler);
			} else {
				// Clear out any current temp file mappings to ensure
				// files/streams aren't re-used
				handler.getMap().remove(FILE_OPEN_TMP_FILE_KEY);
				handler.getMap().remove(FILE_OPEN_TMP_STREAM_KEY);
				handler.getMap().remove(FILE_OPEN_MODTIME_KEY);
				handler.getMap().remove(FILE_OPEN_IS_SYMBOLIC_LINK_KEY);
			}
			handler.setError(false);
			handler.getMap().put(FILE_OPEN_ORIG_ARGS_KEY, resultsMap);
			handler.getMap().put(FILE_OPEN_MODTIME_KEY, modTime);
			
			if (targetFile.exists() && targetFile.isFile() && (noClobber != null)
					&& targetFile.canWrite()) {
				handler.setError(true);
				cmdEnv.handleResult(
							new RpcMessage(
									ClientMessageId.CANT_CLOBBER,
									MessageSeverityCode.E_FAILED,
									MessageGenericCode.EV_CLIENT,
									new String[] {path}
								).toMap()
						);
				return RpcPacketDispatcherResult.CONTINUE_LOOP;
			}
			
			// File is a symlink type
			if (fstSymlink) {
				// Return error message if this Java cannot handle symlinks
				if (!SymbolicLinkHelper.isSymbolicLinkCapable()) {
					handler.setError(true);
					cmdEnv.handleResult(
								new RpcMessage(
										ClientMessageId.CANT_CREATE_FILE_TYPE,
										MessageSeverityCode.E_FAILED,
										MessageGenericCode.EV_CLIENT,
										new String[] {"symlink", path}
									).toMap()
							);
					return RpcPacketDispatcherResult.CONTINUE_LOOP;
				}

				handler.getMap().put(FILE_OPEN_IS_SYMBOLIC_LINK_KEY, true);

				// This case is the target is an existing symlink
				// and the new file can not do indirect writes, but
				// we do need to replace the symlink...
				// so... delete the symlink and just
				// transfer normally...

				// XXX maybe we should move the symlink to
				// a temp name and move it back if the transfer
				// fails?
				if (fileExists(targetFile, fstSymlink)) {
					targetFile.delete();
				} else {
					if (!FilesHelper.mkdirs(targetFile)) {
						handler.setError(true);
						cmdEnv.handleResult(
								new RpcMessage(
										ClientMessageId.CANT_CREATE_DIR,
										MessageSeverityCode.E_FAILED,
										MessageGenericCode.EV_CLIENT,
										new String[] {path}
									).toMap()
							);
						return RpcPacketDispatcherResult.CONTINUE_LOOP;
					}
				}	

				// Add any other additional signals to the state map
				
				return RpcPacketDispatcherResult.CONTINUE_LOOP;
			}
			
			if (targetFile.exists() && targetFile.isFile() && !cmdEnv.isSyncInPlace()) {
				// If the target file exists and is not a special file
			    // i.e. a device, and we're not syncing in place, we write a
				// temp file and arrange for our closeFile() to rename it into place.
				
				String tmpFileName = RpcPerforceFile.createTempFileName(targetFile.getParent());
				tmpFile = new RpcPerforceFile(tmpFileName, fileTypeStr);
				handler.getMap().put(FILE_OPEN_TMP_FILE_KEY, tmpFile);
				
				if ((perms != null) && perms.equalsIgnoreCase(PERMS_RW)) {
					fileCommands.setWritable(tmpFileName, true);
				}
				
			} else if (!targetFile.exists()) {
				// Delete the non-existing targetFile... just in case it's a
				// symlink pointing to a non-existing target... this can happen
				// if the clientHandle is 'resolve' with 'at' (accept theirs)...
				// and the source type is 'text' (or something) while the target
				// type is a 'symlink'... which makes the 'type' field value to
				// be of the source type 'text'.
				// See job067571
				
				// Also, sync to a previous revision text when head is a symlink
				// pointing to a non-existing target.
				// See job068281
				
				// The fix to the above scenarios would be to call file delete
				// (can be redundant if the target file is truly not there)
				targetFile.delete();
				
				// See if we have the enclosing directories; if not,
				// we have to try to create them...
				
				if (!FilesHelper.mkdirs(targetFile)) {
					handler.setError(true);
					cmdEnv.handleResult(
							new RpcMessage(
									ClientMessageId.CANT_CREATE_DIR,
									MessageSeverityCode.E_FAILED,
									MessageGenericCode.EV_CLIENT,
									new String[] {path}
								).toMap()
						);
					
					return RpcPacketDispatcherResult.CONTINUE_LOOP;
				}
				
				try {
					if (!targetFile.createNewFile()) {
						Log.warn(TRACE_PREFIX + ".openFile: unable to create new target file");
					}
					
					// If the target file is a file that needs decoding, write the undecoded
					// output from the server to a tmp file, then arrange for the tmp file to
					// be decoded in closeFile(). Decoding is currently not necessary any more,
					// but may be resurrected in the future -- HR.
					
					if (!targetFile.canCopyAsIs()) {
						String tmpFileName = RpcPerforceFile.createTempFileName(tmpDirName);
						tmpFile = new RpcPerforceFile(tmpFileName, fileTypeStr);
						handler.getMap().put(FILE_OPEN_TMP_FILE_KEY, tmpFile);
					}
				} catch (IOException ioexc) {
					handler.setError(true);
					cmdEnv.handleResult(
							new RpcMessage(
									ClientMessageId.CANT_CREATE_FILE,
									MessageSeverityCode.E_FAILED,
									MessageGenericCode.EV_CLIENT,
									new String[] {path, ioexc.getLocalizedMessage()}
								).toMap()
						);
					
					return RpcPacketDispatcherResult.CONTINUE_LOOP;
				}
			} else if (targetFile.isFile()) {
				// Set writable so we can write in place; will
				// get reset back to read-only if needed in
				// closeFile.
				
				fileCommands.setWritable(path, true);
			}
			
			handler.getMap().put(FILE_OPEN_TARGET_FILE_KEY, targetFile);
						
			if ((perms != null) && perms.equalsIgnoreCase(PERMS_RW)) {
				fileCommands.setWritable(path, true);
			}
			
			if (fileType.isExecutable()) {
				fileCommands.setExecutable(path, true, true);
			}
			
			if (tmpFile != null) {
				RpcOutputStream tmpStream = new RpcOutputStream(tmpFile, rpcConnection.getClientCharset(),
																			rpcConnection.isUnicodeServer(),
																			useLocalDigester);
				if (useLocalDigester) {
					tmpStream.setServerDigest(digest);
				}
				handler.getMap().put(FILE_OPEN_TMP_STREAM_KEY, tmpStream);
			} else if (targetFile != null) {
				RpcOutputStream targetStream = new RpcOutputStream(targetFile, rpcConnection.getClientCharset(),
																			rpcConnection.isUnicodeServer(),
																			useLocalDigester);
				if (useLocalDigester) {
					targetStream.setServerDigest(digest);
				}
				handler.getMap().put(FILE_OPEN_TARGET_STREAM_KEY, targetStream);
			}
			
		} catch (P4JavaError p4je) {
			throw p4je;
		} catch (Exception exc) {
			Log.exception(exc);
			throw new P4JavaError(
					"Unexpected exception in ClientSystemFileCommands.openFile: "
					+ exc.getLocalizedMessage() + exc, exc);
		}
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * Write file contents to the target file. This method assumes that
	 * fileOpen has previously been called, and that the state map contains
	 * at least one valid file output stream to write bytes to.
	 */
	
	@SuppressWarnings("unchecked")
	protected RpcPacketDispatcherResult writeFile(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in writeFile().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in writeFile().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in writeFile().");
		}
		
		String clientHandle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
		RpcHandler handler = cmdEnv.getHandler(clientHandle);
		Map<String,Object> stateMap = cmdEnv.getStateMap();

		if (handler == null) {
			throw new NullPointerError("Null client handler in writeFile().");
		}
		
		if (handler.isError()) {
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}
		
		if (stateMap == null) {
			throw new NullPointerError(
					"Null state map in ClientSystemFileCommands.writeFile().");
		}

		Map<String, Object> origArgs = (Map<String, Object>) handler.getMap().get(FILE_OPEN_ORIG_ARGS_KEY);

		if (origArgs == null) {
			throw new NullPointerError(
					"Null original argument map ClientSystemFileCommands.writeFile() state map");
		}

		String path = (String) origArgs.get(RpcFunctionMapKey.PATH);

		// Check if the file is a symlink type
		if (handler.getMap().get(FILE_OPEN_IS_SYMBOLIC_LINK_KEY) != null
				&& (Boolean) handler.getMap().get(
						FILE_OPEN_IS_SYMBOLIC_LINK_KEY)) {
			if (path != null) {
				convertFileDataMap(resultsMap,
						cmdEnv.getRpcConnection().getClientCharset(),
						cmdEnv.getRpcConnection().isUnicodeServer());
				String data = (String) resultsMap.get(RpcFunctionMapKey.DATA);
				if (data != null) {
					// Remove any newline characters
					data = data.replaceAll("(\\r|\\n)", "");
					// Create the symlink
					Object link = SymbolicLinkHelper.createSymbolicLink(path, data);
					if (link == null) {
						// Return error message since the symlink creation failed.
						// Possibly the OS doesn't have support for symlinks.
						handler.setError(true);
						cmdEnv.handleResult(
									new RpcMessage(
											ClientMessageId.CANT_CREATE_FILE_TYPE,
											MessageSeverityCode.E_FAILED,
											MessageGenericCode.EV_CLIENT,
											new String[] {"symlink", path}
										).toMap()
								);
						return RpcPacketDispatcherResult.CONTINUE_LOOP;
					}
				}
				return RpcPacketDispatcherResult.CONTINUE_LOOP;
			}
		}		
		
		RpcOutputStream outStream =
			(RpcOutputStream) handler.getMap().get(
												FILE_OPEN_TMP_STREAM_KEY);
		
		if (outStream == null) {
			outStream = (RpcOutputStream) handler.getMap().get(
												FILE_OPEN_TARGET_STREAM_KEY);
		}
		
		if (outStream == null) {
			throw new P4JavaError(
					"No open file stream in ClientSystemFileCommands.writeFile()");
		}

		try {
			if ((outStream.getFD() != null) && outStream.getFD().valid()) {
				long bytesWritten = outStream.write(resultsMap);

				// Send back the data bytes written (accumulated)
				// This is for the progress indicator
				if (cmdEnv.getProtocolSpecs().isEnableProgress()) {
					if (fileSize > 0 && bytesWritten > 0) {
						currentSize += bytesWritten;
						Map<String, Object> dataSizeMap = new HashMap<String, Object>();
						dataSizeMap.put("path", filePath);
						dataSizeMap.put("fileSize", fileSize);
						dataSizeMap.put("currentSize", currentSize);
						cmdEnv.handleResult(dataSizeMap);
					}
				}
				
			} else {
				Log.error("output stream unexpectedly closed in writeFile");
				handler.setError(true);
			}
		} catch (IOException ioexc) {
			// FIXME: the message below will be misleading for tmp file writes -- HR.
			
			handler.setError(true);
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.FILE_WRITE_ERROR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[] {path == null ? "<unknown>" : path,
									ioexc.getLocalizedMessage()}
						).toMap()
				);
			
			Log.error("failed write for file " + (path == null ? "<unknown>" : path)
												+ "; exception follows...");
			Log.exception(ioexc);
		}
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	private void writeToStream(byte[] sourceBytes, int start, int length,
			OutputStream stream) throws IOException {
		if (ClientLineEnding.CONVERT_TEXT) {
			for (int i = start; i < length; i++) {
				if (sourceBytes[i] == ClientLineEnding.FST_L_LF_BYTES[0]) {
					stream.write(ClientLineEnding.FST_L_LOCAL_BYTES);
				} else {
					stream.write(sourceBytes[i]);
				}
			}
		} else {
			stream.write(sourceBytes, start, length);
		}
	}
	
	private void translate(byte[] sourceBytes, CharsetConverter converter,
			int length, OutputStream stream) throws IOException {
		int start = 0;

		if (ClientLineEnding.CONVERT_TEXT) {
			ByteArrayOutputStream converted = new ByteArrayOutputStream();
			writeToStream(sourceBytes, start, length, converted);
			sourceBytes = converted.toByteArray();
			start = 0;
			length = sourceBytes.length;
		}

		ByteBuffer from = ByteBuffer.wrap(sourceBytes);
		if (length > 0) {
			ByteBuffer converted = converter.convert(from);
			if (converted != null) {
				// Update byte array for converted buffer values
				sourceBytes = converted.array();
				start = converted.position();
				length = converted.limit();
			}
		} else {
			// Zero length array denotes last writeText call for
			// the printed file
			byte[] underflow = converter.clearUnderflow();
			if (underflow != null) {
				// Write underflow
				ByteBuffer converted = converter.convert(ByteBuffer
						.wrap(underflow));
				if (converted != null) {
					sourceBytes = converted.array();
					start = converted.position();
					length = converted.limit();
				}

				// Underflow denotes a failure since it should
				// convert completely
				if (converter.clearUnderflow() != null) {
					throw new ClientError(
							"Translation of text output failed to charset "
									+ converter.getToCharsetName());
				}
			}
		}

		if (length > 0) {
			stream.write(sourceBytes, start, length);
		}
	}
	
	/**
	 * Handles the client-OutputText command.<p>
	 * Basically uses writeBinary (below) with the twist that we
	 * currently throw an exception when we see the trans option (which
	 * we haven't implemented yet).<p>
	 * 
	 * Note that -- like the C++ API -- no line end munging is performed -- we
	 * just throw what's coming at us straight back to whoever's catching it...
	 */
	protected RpcPacketDispatcherResult writeText(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap)
									throws ConnectionException {
		
		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in writeText().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in writeText().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in writeText().");
		}

		String trans  = (String) resultsMap.get(RpcFunctionMapKey.TRANS);
		
		if ((trans != null) && !trans.equalsIgnoreCase("no")) {
			throw new P4JavaError(
				"trans arg not 'no' or null in writeText: " + trans);
		}
				
		final String handlerName = "writeText";
		
		RpcHandler handler = cmdEnv.getHandler(handlerName);
		
		if (handler == null) {
			handler = cmdEnv.new RpcHandler(handlerName, false, null);
			cmdEnv.addHandler(handler);
		}
		
		if (handler.isError()) {
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}
		
		Map<String,Object> stateMap = cmdEnv.getStateMap();
		
		RpcOutputStream outStream = getTempOutputStream(cmdEnv);
		
		if (outStream == null) {
			throw new NullPointerError(
					"Null output stream in writeText state map");
		}

		try {
			if ((outStream.getFD() != null) && outStream.getFD().valid()) {
				byte[] sourceBytes = (byte[]) resultsMap
						.get(RpcFunctionMapKey.DATA);
				int len = sourceBytes.length;
				int start = 0;

				// Check for trans being null here as it was already checked to
				// be either null or 'no' so null here signifies it is not 'no'.
				if (trans == null) {
					CharsetConverter converter = (CharsetConverter) stateMap
							.get(RpcServer.RPC_TMP_CONVERTER_KEY);
					if (converter == null) {
						Charset charset = rpcConnection.getClientCharset();
						
						// Look inside results map vector to see if file is
						// utf-16 since the previous fstat into message will
						// set it if necessary
						for (Map<String, Object> map : cmdEnv
								.getResultMaps()) {
							if (map.containsKey(MapKeys.TYPE_LC_KEY)) {
								String type = map.get(MapKeys.TYPE_LC_KEY)
										.toString();
								if (MapKeys.UTF16_LC_KEY.equals(type)) {
									charset = CharsetDefs.UTF16;
									break;
								}
							}
						}
						
						// Convert if client charset is not UTF-8
						if (charset != CharsetDefs.UTF8) {
							converter = new CharsetConverter(
									CharsetDefs.UTF8, charset);
							stateMap.put(
									RpcServer.RPC_TMP_CONVERTER_KEY,
									converter);
						}
					}
					if (converter != null) {
						translate(sourceBytes, converter, len, outStream);
					} else {
						writeToStream(sourceBytes, start, len, outStream);
					}
				} else if (len > 0) {
					writeToStream(sourceBytes, start, len, outStream);
				}
			} else {
				Log.error("output stream unexpectedly closed in writeText");
				handler.setError(true);
			}
		} catch (IOException ioexc) {
			handler.setError(true);
			cmdEnv.getResultMaps().add(
					new RpcMessage(
							ClientMessageId.FILE_WRITE_ERROR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[] {"tmp file",
									ioexc.getLocalizedMessage()}
						).toMap()
				);
		} 
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * A specialised method to handle the client-OutputBinary command.<p>
	 * 
	 * Note that this method fakes a handler to keep state around, and assumes
	 * that the target (tmp) stream has been passed-in by the higher levels in
	 * the cmdEnv state map.
	 */
	protected RpcPacketDispatcherResult writeBinary(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap)
									throws ConnectionException {
				
		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in writeBinary().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in writeBinary().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in writeBinary().");
		}

		final String handlerName = "writeBinary";
		
		RpcHandler handler = cmdEnv.getHandler(handlerName);
		
		if (handler == null) {
			handler = cmdEnv.new RpcHandler(handlerName, false, null);
			cmdEnv.addHandler(handler);
		}
		
		if (handler.isError()) {
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}
		
		@SuppressWarnings("unused") // used for debugging
		Map<String,Object> stateMap = cmdEnv.getStateMap();
		
		RpcOutputStream outStream = getTempOutputStream(cmdEnv);
		
		if (outStream == null) {
			throw new NullPointerError(
					"Null output stream in writeText state map");
		}

		try {
			if ((outStream.getFD() != null) && outStream.getFD().valid()) {
				outStream.write(resultsMap);
			} else {
				Log.error("output stream unexpectedly closed in writeBinary");
				handler.setError(true);
			}
		} catch (IOException ioexc) {
			handler.setError(true);
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.FILE_WRITE_ERROR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[] {"tmp file",
									ioexc.getLocalizedMessage()}
						).toMap()
				);
		} 
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * Close a file that was opened earlier for writing. Depending
	 * on circumstances, this may involve moving a temporary file
	 * and / or deleting other files, etc., and stitching up permissions,
	 * etc. (we are often not allowed to change a file's executable bits
	 * in places like /tmp, etc., so we do it here...).
	 */
	
	@SuppressWarnings("unchecked")
	protected RpcPacketDispatcherResult closeFile(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {

		//FIXME(S): permissions, cleanup -- HR.
				
		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in closeFile().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in closeFile().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in closeFile().");
		}

		String commit = (String) resultsMap.get(RpcFunctionMapKey.COMMIT);
		String clientHandle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
		RpcHandler handler = cmdEnv.getHandler(clientHandle);
		Map<String, Object> stateMap = cmdEnv.getStateMap();
		String serverDigest = null;
		String localDigest = null;

		// Clear data file info for progress indicator
		filePath = null;
		fileSize = 0;
		currentSize = 0;
		
		if (handler == null) {
			throw new NullPointerError("Null client handler in closeFile().");
		}
		
		if (handler.isError()) {
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}
		
		if (stateMap == null) {
			throw new NullPointerError(
					"Null state map in ClientSystemFileCommands.closeFile().");
		}

		// Return if it is a symlink
		if (handler.getMap().get(FILE_OPEN_IS_SYMBOLIC_LINK_KEY) != null
				&& (Boolean) handler.getMap().get(
						FILE_OPEN_IS_SYMBOLIC_LINK_KEY)) {
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}
		
		RpcOutputStream tmpStream = (RpcOutputStream) handler.getMap().get(
														FILE_OPEN_TMP_STREAM_KEY);
		RpcOutputStream targetStream = (RpcOutputStream) handler.getMap().get(
														FILE_OPEN_TARGET_STREAM_KEY);
		RpcPerforceFile tmpFile
				= (RpcPerforceFile) handler.getMap().get(FILE_OPEN_TMP_FILE_KEY);
		RpcPerforceFile targetFile
				= (RpcPerforceFile) handler.getMap().get(FILE_OPEN_TARGET_FILE_KEY);
		
		if (targetFile == null) {
			throw new NullPointerError(
				"Null target file ClientSystemFileCommands.closeFile() state map");
		}
		
		
		if (commit != null) {
			Map<String, Object> origArgs = (Map<String, Object>) handler.getMap().get(
															FILE_OPEN_ORIG_ARGS_KEY);
			
			if (origArgs == null) {
				throw new NullPointerError(
					"Null original argument map ClientSystemFileCommands.closeFile() state map");
			}
			
			String perms = (String) origArgs.get(RpcFunctionMapKey.PERMS);
			String modTimeStr = null;
			if (handler.getMap().containsKey(FILE_OPEN_MODTIME_KEY)) {
				modTimeStr = (String) handler.getMap().get(FILE_OPEN_MODTIME_KEY);
			}
			
			try {
				if (tmpStream != null) {
					if (tmpFile == null) {
						throw new NullPointerError(
							"Null tmp file ClientSystemFileCommands.writeFile() state map");
					}

					// Before rename (move) tmp file to target file,
					// We must make sure the tmp stream is closed...
					// in case that the stream is buffered...
					// which might still contain data...
					// so, we must call close() before the File.rename()
					// See job068751
					try {
						tmpStream.flush();
						tmpStream.close();
					} catch (IOException e) {
						Log.error("Flushing or closing stream failed in closeFile(); tmp file: "
										+ tmpFile.getName());
					}
					
					try {
						// Need to rename tmp file to target file.
						if (!tmpFile.renameTo(targetFile)) {
							Log.warn("Rename file failed in closeFile(); so, now will try to copy the file...");
							// If a straight up rename fails, then try
							// copying the tmp file onto the target file.
							// This rename problem seems to happen on Windows
							// when file size exceeds 2GB.
							// See job080437
							FilesHelper.copy(tmpFile, targetFile);
							Log.warn("Copy file succeeded in closeFile().");
						}
					} catch (IOException e) {
						// Total failure occurred - was unable to rename
						// or even copy the file to its target.
						Log.error("Rename/copy failed completely in closeFile(); tmp file: "
								+ tmpFile.getName()
								+ "; target file: "
								+ targetFile.getName());
					}

				} else {
					// Was written in-place; nothing to do here...
					if (targetStream != null) {
						try {
							targetStream.flush();
						} catch (IOException e) {
								Log.error("Flushing stream failed in closeFile(); tmp file: "
												+ tmpFile.getName());
						}
					}
				}
				
				if (tmpStream != null) {
					serverDigest = tmpStream.getServerDigest();
					if (tmpStream.getLocalDigester() != null) {
						try {
							tmpStream.flush();
						} catch (IOException e) {
							Log.error("Flushing stream failed in closeFile(); tmp file: "
									+ tmpFile.getName());
						}
						localDigest = tmpStream.getLocalDigester().digestAs32ByteHex();
					}
				} else if (targetStream != null) {
					serverDigest = targetStream.getServerDigest();
					if (targetStream.getLocalDigester() != null) {
						try {
							targetStream.flush();
						} catch (IOException e) {
							Log.error("Flushing stream failed in closeFile(); target file: "
									+ targetFile.getName());
						}
						localDigest = targetStream.getLocalDigester().digestAs32ByteHex();
						
						// (pallen) close targetStream before setting modtime
						try {
							targetStream.close();
						} catch (IOException ioexc) {
							Log.warn("target file close error in ClientSystemFileCommands.closeFile(): "
									+ ioexc.getLocalizedMessage());
						}
					}
				}
				
				if ((serverDigest != null) && !cmdEnv.isNonCheckedSyncs()) {
					if (!serverDigest.equals(localDigest)) {
						handler.setError(true);
						cmdEnv.handleResult(
								new RpcMessage(
										ClientMessageId.DIGEST_MISMATCH,
										MessageSeverityCode.E_FAILED,
										MessageGenericCode.EV_CLIENT,
										new String[] {
													targetFile.getPath(),
													serverDigest,
													localDigest
												}
									).toMap()
							);
						return RpcPacketDispatcherResult.CONTINUE_LOOP;
					}
				}

				// Handle AppleSingle file ('apple').
				// Extract data fork and resource fork.
				// -------------------------------------------------------------
			    // Type        Client Use              Server Storage
			    // ----        ----------              --------------
			    // apple       Mac resource + data     compressed AppleSingle
				// -------------------------------------------------------------
				if (targetFile.getFileType() == RpcPerforceFileType.FST_APPLEFILE) {
					AppleFileHelper.extractFile(targetFile);
				}

				if (modTimeStr != null) {
					try {
						long modTime = new Long(modTimeStr);
						
						if (modTime > 0) {
							targetFile.setLastModified(modTime * 1000);
						}
					} catch (Exception exc) {
						Log.warn("Unable to set target file modification time: " + exc);
					}
				}
				
				if (perms.equalsIgnoreCase(PERMS_RW)) {
					fileCommands.setWritable(targetFile.getPath(), true);
				} else {
					fileCommands.setWritable(targetFile.getPath(), false);
				}
				
				if (targetFile.getFileType().isExecutable()) {
					// See job075630
					// Set exec bit for Owner, Group and World.
					fileCommands.setExecutable(targetFile.getPath(), true, false);
				}
			} finally {
				try {
					if (tmpStream != null) {
						tmpStream.close();
					}	
				} catch (IOException ioexc) {
					Log.warn("tmp file close error in ClientSystemFileCommands.closeFile(): "
							+ ioexc.getLocalizedMessage());
				}
				try {
					if (targetStream != null) {
						targetStream.close();
					}
				} catch (IOException ioexc) {
					Log.warn("target file close error in ClientSystemFileCommands.closeFile(): "
							+ ioexc.getLocalizedMessage());
				}
				if (tmpFile != null) {
					if (tmpFile.exists() && !tmpFile.delete()) {
						Log.warn("Unable to delete tmp file '"
								+ tmpFile.getPath()
								+ "' in ClientSystemFileCommands.closeFile() -- unknown cause");
					}
				}
			}
		}
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * Move a file from one location to another. Supports the new 2009.1 smart
	 * move command.
	 */
	protected RpcPacketDispatcherResult moveFile(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in moveFile().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in moveFile().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in moveFile().");
		}
		
		String clientPath = (String) resultsMap.get(RpcFunctionMapKey.PATH); // fromFile
		String targetPath = (String) resultsMap.get(RpcFunctionMapKey.PATH2); // toFile
		String type = (String) resultsMap.get(RpcFunctionMapKey.TYPE); // fromFile
		String targetType = (String) resultsMap.get(RpcFunctionMapKey.TYPE2); // toFile
		String clientHandle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
		String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
		String rmdir = (String) resultsMap.get(RpcFunctionMapKey.RMDIR);

		RpcPerforceFileType fromFileType = RpcPerforceFileType.decodeFromServerString(type);
		RpcPerforceFileType targetFileType = RpcPerforceFileType.decodeFromServerString(targetType);

		boolean fromFstSymlink = (fromFileType == RpcPerforceFileType.FST_SYMLINK);
		boolean toFstSymlink = (targetFileType == RpcPerforceFileType.FST_SYMLINK);
		
		RpcPerforceFile fromFile = new RpcPerforceFile(clientPath, type);
		RpcPerforceFile toFile = new RpcPerforceFile(targetPath, targetType);
		
		RpcHandler handler = cmdEnv.getHandler(clientHandle);	
		
		if (handler == null) {
			handler = cmdEnv.new RpcHandler(clientHandle, false, toFile);
			cmdEnv.addHandler(handler);
		}
		handler.setError(false);
		
		if (!fileExists(fromFile, fromFstSymlink)) {
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.FILE_NONEXISTENT,
							MessageSeverityCode.E_INFO,
							MessageGenericCode.EV_CLIENT,
							new String[] {clientPath}
						).toMap()
				);
			
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}
		
		boolean caseSensitive
					= !(cmdEnv.getServerProtocolSpecsMap().containsKey(RpcFunctionMapKey.NOCASE));
		
		if (fileExists(toFile, toFstSymlink) && (!caseSensitive || !clientPath.equalsIgnoreCase(targetPath))) {
			// Target file exists, but this could be a case change, in which case allow this only if
			// the server is case sensitive (logic copied directly from the C++ equivalent)
			// Not sure about the logic here -- seems odd to allow this... (HR).
			
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.CANT_CLOBBER,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[] {targetPath}
						).toMap()
				);
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}
		
		// May need to stitch up target directories:
		
		if (!FilesHelper.mkdirs(toFile)) {
			handler.setError(true);
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.CANT_CREATE_DIR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[] {targetPath}
						).toMap()
				);
			
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}
		
		if (fromFile.renameTo(toFile, true)) {
			// Now construct a suitable response back to the server:
			
			resultsMap.remove(RpcFunctionMapKey.FUNCTION);
			
			RpcPacket respPacket = RpcPacket.constructRpcPacket(
								confirm,
								resultsMap,
								null);
			
			rpcConnection.putRpcPacket(respPacket);
		} else {			
			// Was unable to rename or even copy the file to its target;
			// usually the sign of permissions problems, etc., that we can't
			/// fix on the fly, so report it to the user and the log and don't
			// ack a confirm back to the server...
			
			Log.error("Rename failed completely in moveFile (cause unknown); source file: "
					+ clientPath
					+ "; target file: "
					+ targetPath);
			
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.FILE_MOVE_ERROR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[] {clientPath, "(cause unknown)"}
						).toMap()
				);
		}
		
		if (rmdir != null) {
			// Attempt to nuke the containing directory. Errors
			// here are ignored, which is the behaviour in the C++
			// API (we log the errors, though, which is more than the
			// C++ folks do).
			
			File dir = fromFile.getParentFile();
			
			if (dir != null) {
				if (!dir.delete()) {
					Log.warn("Unable to delete parent directory for delete for file '"
							+ clientPath + "'; (unknown cause)");
				}
			} else {
				Log.warn("Unable to open parent directory for delete for file '"
						+ clientPath + "'; (no parent directory)");
			}
		}
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	protected RpcPacketDispatcherResult deleteFile(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {
		
		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in deleteFile().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in deleteFile().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in deleteFile().");
		}

		String clientPath = (String) resultsMap.get(RpcFunctionMapKey.PATH);
		String noClobber = (String) resultsMap.get(RpcFunctionMapKey.NOCLOBBER);
		String rmDir = (String) resultsMap.get(RpcFunctionMapKey.RMDIR);
		String fileTypeStr = (String) resultsMap.get(RpcFunctionMapKey.TYPE);
		
		RpcPerforceFileType fileType = RpcPerforceFileType.decodeFromServerString(fileTypeStr);
		boolean fstSymlink = (fileType == RpcPerforceFileType.FST_SYMLINK);
		
		File file = new File(clientPath);
		
		// Ignore non-existing files for the "client-DeleteFile" function
		// See job074183
		if (!fileExists(file, fstSymlink)) {
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}
		
		if (file.exists() && file.isFile() && (noClobber != null)
				&& file.canWrite()) {
			cmdEnv.handleResult(
						new RpcMessage(
								ClientMessageId.CANT_CLOBBER,
								MessageSeverityCode.E_FAILED,
								MessageGenericCode.EV_CLIENT,
								new String[] {clientPath}
							).toMap()
					);
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}
		
		boolean deleteFailed = false;
		
		if (!file.delete()) {
			deleteFailed = true;
			
			// On some systems (Mac OS X, at least) delete won't work on read-only files;
			// try again, but first change file to writable:

			if (!file.canWrite() && fileCommands.setWritable(clientPath, true)) {
				deleteFailed = !file.delete();
				// We don't reset permissions here because the file is already 
				// tampered with and we don't know the exact permissions in any case...
				// (this may be revisited later).
			}
		}
		
		if (deleteFailed) {	
			// Unfortunately we have no ack or anything to give to the
			// server on file delete, so we just let this go with a warning...
			
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.CANT_DELETE_FILE,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[] {clientPath}
						).toMap()
				);
			
			return RpcPacketDispatcherResult.CONTINUE_LOOP;	
		}
		
		if (rmDir != null) {
			// Attempt to nuke the containing directory and it's upstream parent
			// directories (if empty), and stops at the client's root directory.
			// Errors here are ignored, which is the behaviour in the C++ API
			// (we log the errors, though, which is more than the C++ folks do).

			File clientRootDir = new File(this.server.getCurrentClient().getRoot());
			File dir = file;
			do {
				dir = dir.getParentFile();
				if (dir != null) {
					if (!dir.delete()) {
						Log.warn("Unable to delete parent directory for delete for file '"
								+ clientPath + "'; (unknown cause)");
						// Stop when unable to delete the parent directory
						break;
					}
				} else {
					Log.warn("Unable to open parent directory for delete for file '"
							+ clientPath + "' (unknown cause)");
					// Stop when unable to open the parent directory
					break;
				}
			
			} while (!dir.getAbsoluteFile().equals(clientRootDir));
			
		}
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * The infamous checkFile omnibus method, used to, well, check files
	 * on the Perforce client side. Basically copied and transliterated
	 * into Java from the C++ original; not all of it currently makes
	 * sense in a Java environment, but this will be fixed (HR).<p>
	 * 
	 * Much of the work happens off-stage in the support methods
	 * elsewhere.
	 * 
	 * What follows is copied from the C++ API:<p>
	 * 
	 * This routine, for compatibility purposes, has several modes.<p>
	 *
	 * 1.	If clientType is set, we know the type and we're checking to see
	 *	if the file exists and (if digest is set) if the file has the same
	 *	fingerprint.  We return this in "status" with a value of "missing",
	 *	"exists", or "same".  This starts around version 1742.<p>
	 *
	 * 2.	If clientType is unset, we're looking for the type of the file,
	 *	and we'll return it in "type".  This is sort of overloaded, 'cause
	 *	it can also get set with pseudo-types like "missing".  In this
	 *	case, we use the "xfiles" protocol check to make sure we don't
	 *	return something the server doesn't expect.<p>
	 * <pre>
	 *	- xfiles unset: return text, binary.
	 * 	- xfiles >= 0: also return xtext, xbinary.
	 *	- xfiles >= 1: also return symlink.
	 *	- xfiles >= 2; also return resource (mac resource file).  
	 *	- xfiles >= 3; also return ubinary
	 *	- xfiles >= 4; also return apple
	 * </pre>
	 *	If forceType is set, we'll use that in preference over what
	 *	we've discovered.  We still check the file (to make sure they're
	 *	not adding a directory, and so they get to right warning if
	 *	they add an empty file), but we'll just override that back to
	 *	the (typemap's) forceType.<p>
	 *
	 * We map empty/missing/unreadable into forceType/"text".
	 * 
	 */
	
	protected RpcPacketDispatcherResult checkFile(RpcConnection rpcConnection,
			CommandEnv cmdEnv, Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in checkFile().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in checkFile().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in checkFile().");
		}

		String clientPath = (String) resultsMap.get(RpcFunctionMapKey.PATH);
		String clientType = (String) resultsMap.get(RpcFunctionMapKey.TYPE);
		String forceType = (String) resultsMap.get(RpcFunctionMapKey.FORCETYPE);
		String digest = (String) resultsMap.get(RpcFunctionMapKey.DIGEST);
		String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
		String ignore = (String) resultsMap.get(RpcFunctionMapKey.IGNORE);
		
		// 2012.1 server asks the client to do ignore checks (on add), in the
		// case of client forced file type (i.e 'p4 add -t binary file1.xyz'),
		// ignore == client-Ack, do quick confirm
		if (ignore != null) {

			// Do ignore checking, reject file matching ignore patterns
			if (isIgnore(new File(clientPath), rpcConnection.getClientCharset(), cmdEnv)) {
				return RpcPacketDispatcherResult.CONTINUE_LOOP;
			}
			
		    // Client forced file type.
			// "ignore == client-Ack"
			// Skip client-type checking.
		    // Just ack, return confirm, echoing incoming args.
			if (ignore.length() > 0) {
				RpcFunctionSpec funcSpec = RpcFunctionSpec.decode(ignore);
				if (funcSpec == RpcFunctionSpec.CLIENT_ACK) {
					
					Map<String,Object> respMap = new HashMap<String, Object>();
					for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
						if ((entry.getKey() != null) && !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.FUNCTION)) {
							respMap.put(entry.getKey(), entry.getValue());
						}
					}
					RpcPacket respPacket = RpcPacket.constructRpcPacket(
							confirm,
							respMap,
							null);
					rpcConnection.putRpcPacket(respPacket);
					
					return RpcPacketDispatcherResult.CONTINUE_LOOP;
				}
			}
		}
		
		String status = "exists";
		String nType = (clientType == null) ? "text" : clientType;

		RpcPerforceFileType fileType = null;
		boolean fstSymlink = false;

		if (clientType != null) {
			RpcPerforceFile file = new RpcPerforceFile(clientPath, clientType);
			fileType = RpcPerforceFileType.decodeFromServerString(clientType);
			fstSymlink = (fileType == RpcPerforceFileType.FST_SYMLINK);
			
			/*
		     * If we do know the type, we want to know if it's missing.
		     * If it isn't missing and a digest is given, we want to know if
		     * it is the same.
		     */

			if (!fileExists(file, fstSymlink)) {
				status = "missing";
			} else if (digest != null) {
				// Calculate actual file digest; if same, we assume the file's
				// the same as on the server.
				
				MD5Digester digester = new MD5Digester();
				
				if (digester != null) {
					Charset digestCharset = null;
					boolean convertLineEndings = false;
					//Only worry about encoding if unicode
					switch (fileType) {
						case FST_UTF16:
							digestCharset = CharsetDefs.UTF16;
							// fallthrough
						case FST_UNICODE:
							if( digestCharset == null) {
								digestCharset = rpcConnection.getClientCharset();
							}
							// fallthrough
						case FST_XTEXT:
							// fallthrough
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
					String digestStr = digester.digestFileAs32ByteHex(file,
							digestCharset, convertLineEndings, file.getLineEnding());
					
					if ((digestStr != null) && digestStr.equals(digest)) {
						status = "same";
					}
				}
			}
		} else {			
			// Infer the file type, since it's not given.
			File file = new File(clientPath);
			fileType = RpcPerforceFileType.inferFileType(file,
					cmdEnv.getRpcConnection().isUnicodeServer(),
					cmdEnv.getRpcConnection().getClientCharset());
			fstSymlink = (fileType == RpcPerforceFileType.FST_SYMLINK);

			if (!fileExists(file, fstSymlink)) {
				status = "missing";
				
				cmdEnv.handleResult(
						new RpcMessage(
								ClientMessageId.FILE_MISSING_ASSUMING_TYPE,
								MessageSeverityCode.E_INFO,
								MessageGenericCode.EV_CLIENT,
								new String[] {clientPath, nType}
								).toMap()
					);
			}
			
			String serverXLevelStr = (String) cmdEnv.getServerProtocolSpecsMap().get("xfiles");
			int serverXLevel = 0;
			if (serverXLevelStr != null) {
				try {
					serverXLevel = new Integer(serverXLevelStr);
				} catch (NumberFormatException nfe) {
					throw new ProtocolError(
							"Unexpected number conversion exception in "
							+ TRACE_PREFIX + ".checkFile: "
							+ nfe.getLocalizedMessage(), nfe);
				}
			}
				
			RpcServerTypeStringSpec spec
						= RpcPerforceFileType.getServerFileTypeString(
													clientPath,
													fileType,
													forceType, serverXLevel);
			
			
			if (spec.getServerTypeString() == null) {
				
				// Signals an error that can't be recovered from (there's just
				// no server file type string): Just put the associated error message
				// into the results vector and return.
				
				cmdEnv.handleResult(spec.getMsg().toMap());
				
				return RpcPacketDispatcherResult.CONTINUE_LOOP;
				
			} else if (spec.getMsg() != null) {
				
				// non-fatal error to report:
				
				cmdEnv.handleResult(spec.getMsg().toMap());
			}
			
			nType = spec.getServerTypeString();
		}
		
		// Now construct a suitable response for the server; this
		// means copying the incoming args, appending or changing
		// "type" and "status" if necessary, and changing the
		// function type to dm-OpenFile.
		
		Map<String,Object> respMap = new HashMap<String, Object>();
		
		respMap.put(RpcFunctionMapKey.TYPE, nType);
		respMap.put(RpcFunctionMapKey.STATUS, status);
		
		for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
			if ((entry.getKey() != null) && !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.FUNCTION)
					&& !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.TYPE)
					&& !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.STATUS)) {
				respMap.put(entry.getKey(), entry.getValue());
			}
		}
		
		RpcPacket respPacket = RpcPacket.constructRpcPacket(
				confirm,
				respMap,
				null);
		
		rpcConnection.putRpcPacket(respPacket);
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * "inquire" about file, for 'p4 reconcile' <p>
	 * 
	 * This routine performs clientCheckFile's scenario 1 checking, but also
	 * saves the list of files that are in the depot so they can be compared to
	 * the list of files on the client when reconciling later for add.
	 */
	protected RpcPacketDispatcherResult reconcileEdit(
			RpcConnection rpcConnection, CommandEnv cmdEnv,
			Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in reconcileEdit().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in reconcileEdit().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in reconcileEdit().");
		}

		String clientPath = (String) resultsMap.get(RpcFunctionMapKey.PATH);
		String clientType = (String) resultsMap.get(RpcFunctionMapKey.TYPE);
		String digest = (String) resultsMap.get(RpcFunctionMapKey.DIGEST);
		String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
		String handle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);

		this.reconcileHandle = handle;
		
		RpcHandler handler = cmdEnv.getHandler(handle);
		
		if (handler == null) {
			handler = cmdEnv.new RpcHandler(handle, false, null);
			cmdEnv.addHandler(handler);
		}
		
		@SuppressWarnings("unchecked")
		Map<String, File> skipFilesMap = (Map<String, File>)handler.getMap().get(RECONCILE_HANDLER_SKIP_ADD_KEY);
		if (skipFilesMap == null) {
			skipFilesMap = new HashMap<String, File>();
		}
		
		String status = "exists";
		String nType = (clientType == null) ? "text" : clientType;
		
		RpcPerforceFileType fileType = RpcPerforceFileType.decodeFromServerString(clientType);
		boolean fstSymlink = (fileType == RpcPerforceFileType.FST_SYMLINK);

		/*
	     * If we do know the type, we want to know if it's missing.
	     * If it isn't missing and a digest is given, we want to know if
	     * it is the same.
	     */
		
		File file = new File(clientPath);
		
		if (!fileExists(file, fstSymlink)) {
			status = "missing";
		} else if (RpcPerforceFileType.isProbablySymLink(file)) {
			skipFilesMap.put(file.getAbsolutePath(), file);
		} else if (digest != null) {
			// Calculate actual file digest; if same, we assume the file's
			// the same as on the server.
			
			MD5Digester digester = new MD5Digester();
			
			if (digester != null) {
				Charset digestCharset = null;
				boolean convertLineEndings = false;
				//Only worry about encoding if unicode
				switch (fileType) {
					case FST_UTF16:
						digestCharset = CharsetDefs.UTF16;
						// fall through
					case FST_UNICODE:
						if( digestCharset == null) {
							digestCharset = rpcConnection.getClientCharset();
						}
						// fall through
					case FST_XTEXT:
						// fall through
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
				String digestStr = digester.digestFileAs32ByteHex(file,
						digestCharset, convertLineEndings);
				
				if ((digestStr != null) && digestStr.equals(digest)) {
					status = "same";
				}
			}
		} else {
			skipFilesMap.put(file.getAbsolutePath(), file);
		}
		
		handler.getMap().put(RECONCILE_HANDLER_SKIP_ADD_KEY, skipFilesMap);
		
		// Now construct a suitable response for the server; this
		// means copying the incoming args, appending or changing
		// "type" and "status" if necessary, and changing the
		// function type to server-ReconcileFile.
		
		Map<String,Object> respMap = new HashMap<String, Object>();
		
		respMap.put(RpcFunctionMapKey.TYPE, nType);
		respMap.put(RpcFunctionMapKey.STATUS, status);
		
		for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
			if ((entry.getKey() != null) && !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.FUNCTION)
					&& !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.TYPE)
					&& !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.STATUS)) {
				respMap.put(entry.getKey(), entry.getValue());
			}
		}
		
		RpcPacket respPacket = RpcPacket.constructRpcPacket(
				confirm,
				respMap,
				null);
		
		rpcConnection.putRpcPacket(respPacket);
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}
	
	/**
	 * Reconcile add confirm - scans the directory (local syntax) and returns
	 * files in the directory using the full path. This supports traversing
	 * sub-directories.<p>
	 * 
	 * TODO: Check MapApi; if no mapping, continue; Currently, just rely on
	 * server side MapApi validation.
	 */
	protected RpcPacketDispatcherResult reconcileAdd(
			RpcConnection rpcConnection, CommandEnv cmdEnv,
			Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in reconcileAdd().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in reconcileAdd().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in reconcileAdd().");
		}

		String dir = (String) resultsMap.get(RpcFunctionMapKey.DIR);
		String traverse = (String) resultsMap.get(RpcFunctionMapKey.TRAVERSE);
		String skipIgnore = (String) resultsMap.get(RpcFunctionMapKey.SKIP_IGNORE);
		String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
		String handle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
		
		ViewMap<IMapEntry> viewMap = new ViewMap<IMapEntry>();
		for (int i = 0; resultsMap.get(RpcFunctionMapKey.MAP_TABLE + i) != null; i++) {
			String entry = (String)resultsMap.get(RpcFunctionMapKey.MAP_TABLE + i);
			if (entry != null) {
				// Put double quotes around file path with whitespace
				if (entry.contains(" ") || entry.contains("\t")) {
					if (!entry.startsWith("\"")) {
						entry = "\"" + entry;
					}
					if (!entry.endsWith("\"")) {
						entry = entry + "\"";
					}
				}
				IMapEntry mapEntry = new MapEntry(i, entry);
				viewMap.addEntry(mapEntry);
			}
		}
		
		boolean isTraverse = (traverse != null && !traverse.equalsIgnoreCase("0")) ? true : false;
		boolean isSkipIgnore = (skipIgnore != null && !skipIgnore.equalsIgnoreCase("0")) ? true : false;
		
		Map<String, File> addFilesMap = new HashMap<String, File>();
		// Recursive call
		traverseDirs(new File(dir), isTraverse, isSkipIgnore, addFilesMap,
				viewMap, rpcConnection.isUnicodeServer(),
				rpcConnection.getClientCharset(), cmdEnv);

		// If we have a list of files we know are in the depot already,
		// filter them out of our list of files to add
		RpcHandler handler = cmdEnv.getHandler(handle);
		if (handler != null) {
			@SuppressWarnings("unchecked")
			Map<String, File> skipFilesMap = (Map<String, File>)handler.getMap().get(RECONCILE_HANDLER_SKIP_ADD_KEY);
			if (skipFilesMap != null) {
				for (Map.Entry<String, File> entry : skipFilesMap.entrySet()) {
				    String key = entry.getKey();
				    addFilesMap.remove(key);
				}
			}
		}
		
		// Now construct a suitable response for the server; this
		// means copying the incoming args, appending or changing
		// "type" and "status" if necessary, and changing the
		// function type to server-ReconcileAdds.
		
		Map<String,Object> respMap = new HashMap<String, Object>();
		
		int i = 0;
		for (Map.Entry<String, File> entry : addFilesMap.entrySet()) {
			if (entry.getKey() != null) {
				respMap.put(RpcFunctionMapKey.FILE + i++, entry.getKey());
			}
		}
		
		for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
			if ((entry.getKey() != null) && !entry.getKey().equalsIgnoreCase(RpcFunctionMapKey.FUNCTION)) {
				respMap.put(entry.getKey(), entry.getValue());
			}
		}
		
		RpcPacket respPacket = RpcPacket.constructRpcPacket(
				confirm,
				respMap,
				null);
		
		rpcConnection.putRpcPacket(respPacket);
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}

	/**
	 * Reconcile flush - remove the skip add files map from the reconcile handler.
	 */
	protected RpcPacketDispatcherResult reconcileFlush(
			RpcConnection rpcConnection, CommandEnv cmdEnv,
			Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in reconcileFlush().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in reconcileFlush().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in reconcileFlush().");
		}

		RpcHandler handler = cmdEnv.getHandler(this.reconcileHandle);
		
		if (handler != null) {
			handler.getMap().remove(RECONCILE_HANDLER_SKIP_ADD_KEY);
			handler = null;
		}
		
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}

	/**
	 * Recursively (optional) traverse the directory tree for files.<p>
	 * 
	 * TODO: Check MapApi; if no mapping, continue. Currently, just rely on
	 * server side MapApi validation.
	 */
	private void traverseDirs(File file, boolean traverse, boolean skipIgnore,
			Map<String, File> addFilesMap, ViewMap<IMapEntry> viewMap,
			boolean unicode, Charset charset, CommandEnv cmdEnv) {
    	if (addFilesMap == null) {
    		throw new IllegalArgumentException("Must pass in a non-null 'files' list as a parameter.");
    	}

		if (file == null || !file.exists()) {
			return;
		}

		// If this is a file, not a directory, and not to be ignored,
		// save the filename and return.
		if (file.isFile()) {
			if (skipIgnore || !isIgnore(file, charset, cmdEnv)) {
				addFilesMap.put(file.getAbsolutePath(), file);
			}
			return;
		}

		// If this is a symlink to a directory, and not to be ignored,
		// save the filename and return.
		if (file.isDirectory() && RpcPerforceFileType.isProbablySymLink(file)) {
			if (skipIgnore || !isIgnore(file, charset, cmdEnv)) {
				addFilesMap.put(file.getAbsolutePath(), file);
			}
			return;
		}

		// This is a directory to be scanned.
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.isDirectory()) { // Directory
						if (RpcPerforceFileType.isProbablySymLink(file)) {
							// TODO: Check MapApi; if no mapping, continue.
							// Currently, rely on server side MapApi validation.
							
							if (skipIgnore || !isIgnore(f, charset, cmdEnv)) {
								addFilesMap.put(f.getAbsolutePath(), f);
							}
						} else if (traverse) {
							// Recursive call
							traverseDirs(f, traverse, skipIgnore, addFilesMap,
									viewMap, unicode, charset, cmdEnv);
						}
					} else { // File
						// TODO: Check MapApi; if no mapping, continue.
						// Currently, rely on server side MapApi validation.
						
						if (skipIgnore || !isIgnore(f, charset, cmdEnv)) {
							addFilesMap.put(f.getAbsolutePath(), f);
						}
					}
				}
			}
		}
	}

	/**
	 * Check if the file or symbolic link exists.
	 */
	private boolean fileExists(File file, boolean fstSymlink) {
		
		if (file != null) {
			if (file.exists()) {
				return true;
			} else if (fstSymlink) {
				return SymbolicLinkHelper.exists(file.getPath());
			}
		}
		
		return false;
	}

	/**
	 * Check if the file should be ignored.
	 */
	private boolean isIgnore(File file, Charset charset, CommandEnv cmdEnv) {
		// Do ignore checking, reject file matching ignore patterns
		if (getChecker(charset) != null) {
			try {
				if (checker.match(file)) {
					cmdEnv.handleResult(new RpcMessage(
							ClientMessageId.CANT_ADD_FILE_TYPE,
							MessageSeverityCode.E_INFO,
							MessageGenericCode.EV_CLIENT, new String[] {
									file.getAbsolutePath(), "ignored" }).toMap());
					return true;
				}
			} catch (FileNotFoundException e) {
				Log.error("Exception occurred during ignore files checking: "
						+ e);
			} catch (IOException e) {
				Log.error("Exception occurred during ignore files checking: "
						+ e);
			}
		}
		
		return false;
	}
	
	/**
	 * Return the temp RPC output stream. If it doesn't exist, try to create a
	 * new one only if the command is run from a "streamCmd" method or tracking
	 * is enabled.
	 */
	public RpcOutputStream getTempOutputStream(CommandEnv cmdEnv)
			throws ConnectionException {
		if (cmdEnv == null) {
			throw new NullPointerError(
					"Null command env in ClientSystemFileCommands.getTempOutputStream()");
		}
		if (cmdEnv.getStateMap() == null) {
			throw new NullPointerError(
					"Null command env state map in ClientSystemFileCommands.getTempOutputStream()");
		}
		if (cmdEnv.getProtocolSpecs() == null) {
			throw new NullPointerError(
					"Null command env protocol specs in ClientSystemFileCommands.getTempOutputStream()");
		}

		RpcOutputStream outStream = (RpcOutputStream) cmdEnv.getStateMap().get(
				RpcServer.RPC_TMP_OUTFILE_STREAM_KEY);

		if (outStream == null) {
			if (cmdEnv.isStreamCmd() || cmdEnv.getProtocolSpecs().isEnableTracking()) {
				try {
					String tmpFileName = RpcPerforceFile
							.createTempFileName(RpcPropertyDefs.getProperty(
									this.server.getProperties(),
									PropertyDefs.P4JAVA_TMP_DIR_KEY,
									System.getProperty("java.io.tmpdir")));
					RpcPerforceFile tmpFile = new RpcPerforceFile(tmpFileName, RpcPerforceFileType.FST_BINARY);
					outStream = new RpcOutputStream(tmpFile);
					// Set the new temp RPC output stream to the command env state map
					cmdEnv.getStateMap().put(RpcServer.RPC_TMP_OUTFILE_STREAM_KEY, outStream);
				} catch (IOException ioexc) {
					Log.error("tmp file creation error: " + ioexc.getLocalizedMessage());
					Log.exception(ioexc);
					// p4ic4idea: altered exception to be more precise
					throw new ClientFileAccessException("Unable to create temporary file for Perforce file retrieval; " +
							"reason: " + ioexc.getLocalizedMessage(),
							ioexc);
				}
			}
		}

		return outStream;
	}

	public Map<String, Object> convertFileDataMap(Map<String, Object> map, Charset charset, boolean isUnicodeServer) {

		if (map != null) {
			String dataString = null;
			byte[] dataBytes = null;

			try {
				dataBytes = (byte[]) map.get(RpcFunctionMapKey.DATA);
			} catch (Throwable thr) {
				Log.exception(thr);
			}
			
			if (dataBytes != null) {
				try {
					dataString = new String(dataBytes, charset == null ?
							RpcConnection.NON_UNICODE_SERVER_CHARSET_NAME :
									(isUnicodeServer ? CharsetDefs.UTF8_NAME : charset.name()));
					map.put(RpcFunctionMapKey.DATA, dataString);
				} catch (UnsupportedEncodingException e) {
					Log.exception(e);
				}
			}
		}

		return map;
	}

    /**
     * Recursively get all files in a directory.<p>
     * 
     * Note: must pass in a non-null 'files' list as a parameter.
     */
    public static void getFiles(File dir, FilenameFilter filter, List<File> files) {
    	if (files == null) {
    		throw new IllegalArgumentException("Must pass in a non-null 'files' list as a parameter.");
    	}
    	if (dir != null) {
	        if (dir.isDirectory()) {
	            String[] children = dir.list(filter);
	            if (children != null) {
		            for (int i=0; i<children.length; i++) {
		            	getFiles(new File(dir, children[i]), filter, files);
		            }
	            }
	        } else {
	        	files.add(dir);
	        }
    	}
    }        

    /**
     * Check if it should use the local digester
     */
    private boolean useLocalDigester(String serverDigest, CommandEnv cmdEnv) {
		return ((serverDigest != null) && !cmdEnv.isNonCheckedSyncs());
	}
	
    /**
     * Return the client ignore checker; create a new one if it doesn't exist.
     */
	private ClientIgnoreChecker getChecker(Charset charset) {
		if (this.checker == null) {
			if (this.server != null) {
				if (this.server.getCurrentClient() != null
						&& this.server.getIgnoreFileName() != null) {
					this.checker = new ClientIgnoreChecker(this.server
							.getCurrentClient().getRoot(),
							this.server.getIgnoreFileName(), charset);
				}
			}
		}
		return this.checker;
	}
}
