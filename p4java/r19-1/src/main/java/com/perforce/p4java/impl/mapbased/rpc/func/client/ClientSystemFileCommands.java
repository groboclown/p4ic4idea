/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import com.perforce.p4java.CharsetConverter;
import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.FileDecoderException;
import com.perforce.p4java.exception.FileEncoderException;
import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSeverityCode;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.ProtocolError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv.RpcHandler;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.impl.mapbased.rpc.RpcServer;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientMessage.ClientMessageId;
import com.perforce.p4java.impl.mapbased.rpc.handles.ClientFile;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacket;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketDispatcher.RpcPacketDispatcherResult;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcOutputStream;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceDigestType;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFile;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType.RpcServerTypeStringSpec;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.AppleFileHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;
import com.perforce.p4java.util.FilesHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.perforce.p4java.impl.mapbased.rpc.func.client.ClientHelper.sendBackWrittenDataBytes;

/**
 * Implements the simpler lower-level file commands that typically
 * correspond to system commands such as chmod, delete, etc.
 */

public class ClientSystemFileCommands {

	public static final String TRACE_PREFIX = "ClientSystemFileCommands";

	public static final String DEFAULT_TMPFILE_PFX = "p4j";
	public static final String DEFAULT_TMPFILE_SFX = ".p4j";

	public static final String SYSTEM_TMPDIR_PROPS_KEY = "java.io.tmpdir";
	public static final String SYSTEM_TMPDIR_DEFAULT = "/tmp";

	public static final String PERMS_RW = "rw";

	// Reconcile handler map key for 'skipAdd'
	protected static final String RECONCILE_HANDLER_SKIP_ADD_KEY = "skipAdd";

	private Properties props = null;
	private RpcServer server = null;

	private String tmpDirName = null;

	private ISystemFileCommandsHelper fileCommands
			= SysFileHelperBridge.getSysFileCommands();

	// Keeping track of file data progress info
	private String filePath = null;
	private long fileSize = 0;
	private long currentSize = 0;

	private ClientSystemFileMatchCommands fileMatchCommands;

	protected ClientSystemFileCommands(Properties props, RpcServer server,
	                                   ClientSystemFileMatchCommands fileMatchCommands) {
		this.props = props;
		this.server = server;
		this.fileMatchCommands = fileMatchCommands;
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
			cmdEnv.newHandler();    // As per C++ API...

			File targetFile = new RpcPerforceFile(path, fileTypeStr);

			if (RpcPerforceFile.fileExists(targetFile, fstSymlink)) {

				if ((time != null)) {
					targetFile.setLastModified(new Long(time) * 1000);
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
								new String[]{path}
						).toMap()
				);
			}
		} catch (NumberFormatException nfe) {
			throw new ProtocolError(
					"Unexpected conversion error in ClientSystemFileCommands.chmodFile: "
							+ nfe.getLocalizedMessage());
		} catch (Exception exc) {
			// FIXME: better error handling here -- HR.

			Log.exception(exc);
			throw new ConnectionException(exc.getLocalizedMessage(), exc);
		}

		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}

	/**
	 * Open a client file for writing. We have to process things like NOCLOBBER,
	 * and ensure we write to a temp file if a file already exists, etc. Much of
	 * the logic here is straight from the C++ API, currently minus the OpenDiff
	 * functionality (which will probably be factored out elsewhere).<p>
	 * <p>
	 * We also have to leave the associated file descriptor (or channel equivalent)
	 * lying around for the rest of the function sequence to be able to pick up.<p>
	 * <p>
	 * Note that we also now implement the 10.2 sync (etc.) transfer integrity
	 * checks; this is actually fairly easy as it can be done on the raw (un-translated)
	 * data coming in to the write methods (as opposed to checkFile's version, which
	 * has to consider the translated data). We do most of this work in the RpcOutputStream
	 * and RpcPerforceFile classes after setting things up here; closeFile does the final
	 * round up and delivers the verdict. This all only happens if Server.nonCheckedSyncs
	 * is false.<p>
	 * <p>
	 * The temp file created here will be deleted in the subsequent closeFile() method call.
	 */

	protected RpcPacketDispatcherResult openFile(RpcConnection rpcConnection,
	                                             CommandEnv cmdEnv, Map<String, Object> resultsMap)
			throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in openFile().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in openFile().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in openFile().");
		}

		String function = (String) resultsMap.get(RpcFunctionMapKey.FUNCTION);
		String clientPath = (String) resultsMap.get(RpcFunctionMapKey.PATH);
		String clientHandle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
		String modTime = (String) resultsMap.get(RpcFunctionMapKey.TIME);
		String noClobber = (String) resultsMap.get(RpcFunctionMapKey.NOCLOBBER);
		String perms = (String) resultsMap.get(RpcFunctionMapKey.PERMS);
		String fileTypeStr = (String) resultsMap.get(RpcFunctionMapKey.TYPE);
		String fileSizeStr = (String) resultsMap.get(RpcFunctionMapKey.FILESIZE);
		String diffFlags = (String) resultsMap.get(RpcFunctionMapKey.DIFF_FLAGS);
		String digest = (String) resultsMap.get(RpcFunctionMapKey.DIGEST);
		String digestType = (String) resultsMap.get(RpcFunctionMapKey.DIGESTTYPE);

		RpcPerforceFileType fileType = RpcPerforceFileType.decodeFromServerString(fileTypeStr);
		boolean fstSymlink = (fileType == RpcPerforceFileType.FST_SYMLINK);

		// clear syncTime
		cmdEnv.setSyncTime(0);

		// Initialize file data info for progress indicator
		filePath = clientPath != null ? clientPath : null;
		fileSize = fileSizeStr != null ? new Long(fileSizeStr) : 0;
		currentSize = 0;
		boolean doChecksum = false;

		try {

			RpcHandler handler = cmdEnv.getHandler(clientHandle);

			if (handler == null) {
				handler = cmdEnv.new RpcHandler(clientHandle, false, new RpcPerforceFile(clientPath, fileTypeStr));
				cmdEnv.addHandler(handler);
			} else {
				handler.setFile(new RpcPerforceFile(clientPath, fileTypeStr));
				handler.getMap().clear();
			}

			if (digestType != null) {
				// Requires check on digest if present, hence
				// before opening the file, check if it exist
				if (new File(clientPath).exists()) {
					doChecksum = true;
				}
			}

			// Create file object for writing.
			// Set binary/text/etc file type

			ClientFile cfile = new ClientFile(handler);
			cfile.setArgs(resultsMap);
			cfile.setModTime(modTime);

			if (clientHandle.equals("sync")) {
				handler.setError(false);
			}

			RpcFunctionSpec f = RpcFunctionSpec.decode(function);
			if (f.equals(RpcFunctionSpec.CLIENT_OPENDIFF) || f.equals(RpcFunctionSpec.CLIENT_OPENMATCH)) {

				// Set up to be a diff
				// We save the real name as the altFile and
				// replace the real name with a temp file.

				cfile.setDiff(1);
				cfile.setDeleteOnClose(true);
				cfile.setDiffName(clientPath);

				// Save diffFlags for diff operation.

				if (diffFlags != null) {
					cfile.setDiffFlags(diffFlags);
				}

				// Make temp dir

				cfile.MakeGlobalTemp();

				if (function.equalsIgnoreCase(RpcFunctionSpec.CLIENT_OPENMATCH.toString())) {
					fileMatchCommands.openMatch(rpcConnection, cmdEnv, resultsMap, cfile);
				}
			} else {
				if (doChecksum) {
					if (!rpcConnection.getDigest(fileType, cfile.getFile(), RpcPerforceDigestType.GetType(digestType)).equals(digest)) {
						handler.setError(true);
						cmdEnv.handleResult(
								new RpcMessage(
										ClientMessageId.NO_MODIFIED_FILE,
										MessageSeverityCode.E_FAILED,
										MessageGenericCode.EV_CLIENT,
										new String[]{"update", clientPath}
								).toMap());
						return RpcPacketDispatcherResult.CONTINUE_LOOP;
					}
				}

				// Handle noclobber.
				if (cfile.getFile().exists()
						&& cfile.getFile().isFile()
						&& (noClobber != null)
						&& cfile.getFile().canWrite()) {
					handler.setError(true);
					cmdEnv.handleResult(
							new RpcMessage(
									ClientMessageId.CANT_CLOBBER,
									MessageSeverityCode.E_FAILED,
									MessageGenericCode.EV_CLIENT,
									new String[]{clientPath}
							).toMap()
					);
					return RpcPacketDispatcherResult.CONTINUE_LOOP;
				}

				if (cfile.getFile().fileExists(cfile.getFile(), fstSymlink) && cfile.getFile().isFile() && !cmdEnv.isSyncInPlace()) {
					// If the target file exists and is not a special file
					// i.e. a device, and we're not syncing in place, we write a
					// temp file and arrange for our closeFile() to rename it into place.

					String tmpFileName = RpcPerforceFile.createTempFileName(cfile.getFile().getParent());
					cfile.setTmpFile(new RpcPerforceFile(tmpFileName, fileTypeStr));

					if ((perms != null) && perms.equalsIgnoreCase(PERMS_RW)) {
						fileCommands.setWritable(tmpFileName, true);
					}
				} else if (cfile.getFile().isSymlink()) {
					// This case is the target is an existing symlink
					// and the new file can not do indirect writes, but
					// we do need to replace the symlink...
					// so... delete the symlink and just
					// transfer normally...

					// XXX maybe we should move the symlink to
					// a temp name and move it back if the transfer
					// fails?

					cfile.getFile().delete();

					// for symlinks in Perforce skip createStream
					if (cfile.isSymlink()) {
						return RpcPacketDispatcherResult.CONTINUE_LOOP;
					}
				} else if (cfile.getFile().isFile()) {
					// Set writable so we can write in place; will
					// get reset back to read-only if needed in
					// closeFile.

					fileCommands.setWritable(clientPath, true);
				} else {
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
					cfile.getFile().delete();

					// See if we have the enclosing directories; if not,
					// we have to try to create them...

					if (!FilesHelper.mkdirs(cfile.getFile())) {
						handler.setError(true);
						cmdEnv.handleResult(
								new RpcMessage(
										ClientMessageId.CANT_CREATE_DIR,
										MessageSeverityCode.E_FAILED,
										MessageGenericCode.EV_CLIENT,
										new String[]{clientPath}
								).toMap()
						);

						return RpcPacketDispatcherResult.CONTINUE_LOOP;
					}

					try {

						// If the server file type is SYMLINK then setSymlink in ClientFile for use in writeFile.
						if (fstSymlink) {
							return RpcPacketDispatcherResult.CONTINUE_LOOP;
						} else {
							if (!cfile.getFile().createNewFile()) {
								Log.warn(TRACE_PREFIX + ".openFile: unable to create new target file");
							}
						}

						// If the target file is a file that needs decoding, write the undecoded
						// output from the server to a tmp file, then arrange for the tmp file to
						// be decoded in closeFile(). Decoding is currently not necessary any more,
						// but may be resurrected in the future -- HR.

						if (!cfile.getFile().canCopyAsIs()) {
							String tmpFileName = RpcPerforceFile.createTempFileName(tmpDirName);
							cfile.setTmpFile(new RpcPerforceFile(tmpFileName, fileTypeStr));
						}
					} catch (IOException ioexc) {
						handler.setError(true);
						cmdEnv.handleResult(
								new RpcMessage(
										ClientMessageId.CANT_CREATE_FILE,
										MessageSeverityCode.E_FAILED,
										MessageGenericCode.EV_CLIENT,
										new String[]{clientPath, ioexc.getLocalizedMessage()}
								).toMap()
						);

						return RpcPacketDispatcherResult.CONTINUE_LOOP;
					}
				}

				if ((perms != null) && perms.equalsIgnoreCase(PERMS_RW)) {
					fileCommands.setWritable(clientPath, true);
				}

				if (cfile.getFile().getFileType().isExecutable()) {
					fileCommands.setExecutable(clientPath, true, true);
				}
			}

			boolean useLocalDigester = digestType == null && digest != null && !cmdEnv.isNonCheckedSyncs() && !cfile.isSymlink();
			if (useLocalDigester) {
				cfile.setServerDigest(digest);
			}

			cfile.createStream(useLocalDigester, rpcConnection, digest);
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

	protected RpcPacketDispatcherResult writeFile(RpcConnection rpcConnection,
	                                              CommandEnv cmdEnv, Map<String, Object> resultsMap)
			throws ConnectionException {

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
		Map<String, Object> stateMap = cmdEnv.getStateMap();

		if (handler == null) {
			throw new NullPointerError("Null client handler in writeFile().");
		}
		ClientFile cfile = new ClientFile(handler);

		if (handler.isError()) {
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}

		if (stateMap == null) {
			throw new NullPointerError(
					"Null state map in ClientSystemFileCommands.writeFile().");
		}

		Map<String, Object> origArgs = cfile.getArgs();

		if (origArgs == null) {
			throw new NullPointerError(
					"Null original argument map ClientSystemFileCommands.writeFile() state map");
		}

		String path = (String) origArgs.get(RpcFunctionMapKey.PATH);

		// Check if the file is a symlink type
		if (cfile.isSymlink()) {

			RpcPerforceFile tmpPath = cfile.getTmpFile();
			String linkPath = path;
			if (tmpPath != null) {
				try {
					linkPath = tmpPath.toString();
					cfile.getTmpStream().close();
					tmpPath.delete();
				} catch (IOException e) {
					handler.setError(true);
					cmdEnv.handleResult(
							new RpcMessage(
									ClientMessageId.CANT_DELETE_FILE,
									MessageSeverityCode.E_FAILED,
									MessageGenericCode.EV_CLIENT,
									new String[]{"symlink tmpFile", linkPath}
							).toMap()
					);
				}
			}

			if (linkPath != null) {
				convertFileDataMap(resultsMap,
						cmdEnv.getRpcConnection().getClientCharset(),
						cmdEnv.getRpcConnection().isUnicodeServer());
				String data = (String) resultsMap.get(RpcFunctionMapKey.DATA);
				if (data != null) {
					// Remove any newline characters
					data = data.replaceAll("(\\r|\\n)", "");

					// stash symlink target for closeFile CheckFilePath operation using validatePath()
					cfile.setSymTarget(data);

					// Create the symlink
					Object link = SymbolicLinkHelper.createSymbolicLink(linkPath, data);
					if (link == null) {
						// Return error message since the symlink creation failed.
						// Possibly the OS doesn't have support for symlinks.
						handler.setError(true);
						cmdEnv.handleResult(
								new RpcMessage(
										ClientMessageId.CANT_CREATE_FILE_TYPE,
										MessageSeverityCode.E_FAILED,
										MessageGenericCode.EV_CLIENT,
										new String[]{"symlink", path}
								).toMap()
						);
						return RpcPacketDispatcherResult.CONTINUE_LOOP;
					}
				}
				return RpcPacketDispatcherResult.CONTINUE_LOOP;
			}
		}

		RpcOutputStream outStream = cfile.getTmpStream();

		if (outStream == null) {
			outStream = cfile.getStream();
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
				currentSize = sendBackWrittenDataBytes(cmdEnv, filePath, fileSize, currentSize, bytesWritten);
			} else {
				Log.error("output stream unexpectedly closed in writeFile");
				handler.setError(true);
			}
		} catch (FileDecoderException e) {
			handler.setError(true);
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.FILE_DECODER_ERROR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[]{path == null ? "<unknown>" : path}
					).toMap()
			);

			Log.error("failed to decode file " + (path == null ? "<unknown>" : path) + "; exception follows...");
			Log.exception(e);
		} catch (FileEncoderException e) {
			handler.setError(true);
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.FILE_ENCODER_ERROR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[]{path == null ? "<unknown>" : path}
					).toMap()
			);

			Log.error("failed to encode file " + (path == null ? "<unknown>" : path) + "; exception follows...");
			Log.exception(e);
		} catch (IOException e) {
			handler.setError(true);
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.FILE_WRITE_ERROR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[]{path == null ? "<unknown>" : path,
									e.getLocalizedMessage()}
					).toMap()
			);

			Log.error("failed write for file " + (path == null ? "<unknown>" : path) + "; exception follows...");
			Log.exception(e);
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
	                       int length, OutputStream stream) throws IOException, FileDecoderException, FileEncoderException {
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
	 * <p>
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

		String trans = (String) resultsMap.get(RpcFunctionMapKey.TRANS);

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

		Map<String, Object> stateMap = cmdEnv.getStateMap();

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
							new String[]{"tmp file",
									ioexc.getLocalizedMessage()}
					).toMap()
			);
		} catch (FileDecoderException e) {
			handler.setError(true);
			cmdEnv.getResultMaps().add(
					new RpcMessage(
							ClientMessageId.FILE_WRITE_ERROR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[]{"tmp file",
									e.getLocalizedMessage()}
					).toMap()
			);
		} catch (FileEncoderException e) {
			handler.setError(true);
			cmdEnv.getResultMaps().add(
					new RpcMessage(
							ClientMessageId.FILE_WRITE_ERROR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[]{"tmp file",
									e.getLocalizedMessage()}
					).toMap()
			);
		}

		return RpcPacketDispatcherResult.CONTINUE_LOOP;
	}

	/**
	 * A specialised method to handle the client-OutputBinary command.<p>
	 * <p>
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
				Map<String, Object> stateMap = cmdEnv.getStateMap();

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
							new String[]{"tmp file",
									ioexc.getLocalizedMessage()}
					).toMap()
			);
		} catch (FileDecoderException e) {
			handler.setError(true);
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.FILE_WRITE_ERROR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[]{"tmp file",
									e.getLocalizedMessage()}
					).toMap()
			);
		} catch (FileEncoderException e) {
			handler.setError(true);
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.FILE_WRITE_ERROR,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[]{"tmp file",
									e.getLocalizedMessage()}
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

	protected RpcPacketDispatcherResult closeFile(RpcConnection rpcConnection,
	                                              CommandEnv cmdEnv, Map<String, Object> resultsMap)
			throws ConnectionException {

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

		if (cmdEnv.isNullSync()) {
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}

		String function = (String) resultsMap.get(RpcFunctionMapKey.FUNCTION);
		String commit = (String) resultsMap.get(RpcFunctionMapKey.COMMIT);
		String clientHandle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);

		RpcHandler handler = cmdEnv.getHandler(clientHandle);

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

		ClientFile cfile = new ClientFile(handler);

		// Check for illegal symlinks
		//
		// Block symlinks outside the workspace if filesys.restictsymlinks=1
		// and P4CLIENTROOT or DVCS are in use.

		validatePath(rpcConnection, cmdEnv, cfile);

		// Close file, and then diff/rename as appropriate.

		if (cfile.hasFile()) {
			cfile.Close();
		}

		// Stat file and record syncTime

		if (cfile.hasFile()) {
			long modTime = cfile.getModTime();
			if (modTime != 0) {
				cmdEnv.setSyncTime(modTime);
			} else {
				cmdEnv.setSyncTime(cfile.statModTime());
			}
		}

		if (!cfile.isError() && cfile.getServerDigest() != null && commit != null) {
			if (!cfile.getDigest().equals(cfile.getServerDigest())) {
				cmdEnv.handleResult(
						new RpcMessage(
								ClientMessageId.DIGEST_MISMATCH,
								MessageSeverityCode.E_FAILED,
								MessageGenericCode.EV_CLIENT,
								new String[]{cfile.getFile().getName(), cfile.getDigest(), cfile.getServerDigest()}
						).toMap()
				);
				return RpcPacketDispatcherResult.CONTINUE_LOOP;
			}
		}

		if (cfile.getFile() == null) {
			throw new NullPointerError(
					"Null target file ClientSystemFileCommands.closeFile() state map");
		}

		if (cfile.isError()) {
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		} else if (cfile.isDiff()) {
			RpcFunctionSpec f = RpcFunctionSpec.decode(function);
			if (f.equals(RpcFunctionSpec.CLIENT_CLOSEMATCH)) {
				// Pass off control to clientFindMatch.
				// Don't delete handle yet, clientAckMatch needs it.

				fileMatchCommands.closeMatch(rpcConnection, cmdEnv, resultsMap, cfile);
				return RpcPacketDispatcherResult.CONTINUE_LOOP;
			}
			//TODO: Still need to port this
			//FileSys *f2 = client->GetUi()->File( f->file->GetType() );
			//f2->SetContentCharSetPriv(f->file->GetContentCharSetPriv() );
			//f2->Set( f->diffName );
			//client->GetUi()->Diff( f->file, f2, 0, f->diffFlags.Text(), e );
			//delete f2;
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		} else if (commit == null) {
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		} else {
			//TODO npoole: Move the logic for direct/indirect file handling into ClientFile
			Map<String, Object> origArgs = cfile.getArgs();

			if (origArgs == null) {
				throw new NullPointerError(
						"Null original argument map ClientSystemFileCommands.closeFile() state map");
			}

			String perms = (String) origArgs.get(RpcFunctionMapKey.PERMS);

			try {
				if (cfile.getTmpStream() != null) {
					if (cfile.getTmpFile() == null) {
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
						cfile.getTmpStream().flush();
						cfile.getTmpStream().close();
					} catch (IOException e) {
						Log.error("Flushing or closing stream failed in closeFile(); tmp file: "
								+ cfile.getFile().getName());
					}

					try {
						// Need to rename tmp file to target file.
						if (!cfile.getTmpFile().renameTo(cfile.getFile())) {
							Log.warn("Rename file failed in closeFile(); so, now will try to copy the file...");
							// If a straight up rename fails, then try
							// copying the tmp file onto the target file.
							// This rename problem seems to happen on Windows
							// when file size exceeds 2GB.
							// See job080437
							FilesHelper.copy(cfile.getTmpFile(), cfile.getFile());
							Log.warn("Copy file succeeded in closeFile().");
						}
					} catch (IOException e) {
						// Total failure occurred - was unable to rename
						// or even copy the file to its target.
						Log.error("Rename/copy failed completely in closeFile(); tmp file: "
								+ cfile.getFile().getName()
								+ "; target file: "
								+ cfile.getFile().getName());
						handler.setError(true);
						cmdEnv.handleResult(
								new RpcMessage(
										ClientMessageId.FILE_WRITE_ERROR,
										MessageSeverityCode.E_FAILED,
										MessageGenericCode.EV_CLIENT,
										new String[]{cfile.getFile().getName(), e.getLocalizedMessage()}
								).toMap()
						);

						return RpcPacketDispatcherResult.CONTINUE_LOOP;
					}
				} else {
					// Was written in-place; nothing to do here...
					if (cfile.getStream() != null) {
						try {
							cfile.getStream().flush();
						} catch (IOException e) {
							Log.error("Flushing stream failed in closeFile(); tmp file: "
									+ cfile.getFile().getName());
							handler.setError(true);
							cmdEnv.handleResult(
									new RpcMessage(
											ClientMessageId.FILE_WRITE_ERROR,
											MessageSeverityCode.E_FAILED,
											MessageGenericCode.EV_CLIENT,
											new String[]{cfile.getFile().getName(), e.getLocalizedMessage()}
									).toMap()
							);

							return RpcPacketDispatcherResult.CONTINUE_LOOP;
						}
					}
				}

				if (cfile.getTmpStream() != null) {
					serverDigest = cfile.getTmpStream().getServerDigest();
					if (cfile.getDigest() != null) {
						try {
							cfile.getTmpStream().flush();
						} catch (IOException e) {
							Log.error("Flushing stream failed in closeFile(); tmp file: "
									+ cfile.getFile().getName());
							handler.setError(true);
							cmdEnv.handleResult(
									new RpcMessage(
											ClientMessageId.FILE_WRITE_ERROR,
											MessageSeverityCode.E_FAILED,
											MessageGenericCode.EV_CLIENT,
											new String[]{cfile.getFile().getName(), e.getLocalizedMessage()}
									).toMap()
							);

							return RpcPacketDispatcherResult.CONTINUE_LOOP;
						}
						localDigest = cfile.getDigest();
					}
				} else if (cfile.getStream() != null) {
					serverDigest = cfile.getStream().getServerDigest();
					if (cfile.getDigest() != null) {
						try {
							cfile.getStream().flush();
						} catch (IOException e) {
							Log.error("Flushing stream failed in closeFile(); target file: "
									+ cfile.getFile().getName());
							handler.setError(true);
							cmdEnv.handleResult(
									new RpcMessage(
											ClientMessageId.FILE_WRITE_ERROR,
											MessageSeverityCode.E_FAILED,
											MessageGenericCode.EV_CLIENT,
											new String[]{cfile.getFile().getName(), e.getLocalizedMessage()}
									).toMap()
							);

							return RpcPacketDispatcherResult.CONTINUE_LOOP;
						}
						localDigest = cfile.getDigest();

						// (pallen) close targetStream before setting modtime
						try {
							cfile.getStream().close();
						} catch (IOException e) {
							Log.warn("target file close error in ClientSystemFileCommands.closeFile(): "
									+ e.getLocalizedMessage());
							handler.setError(true);
							cmdEnv.handleResult(
									new RpcMessage(
											ClientMessageId.FILE_WRITE_ERROR,
											MessageSeverityCode.E_FAILED,
											MessageGenericCode.EV_CLIENT,
											new String[]{cfile.getFile().getName(), e.getLocalizedMessage()}
									).toMap()
							);

							return RpcPacketDispatcherResult.CONTINUE_LOOP;
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
										new String[]{
												cfile.getFile().getPath(),
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
				if (cfile.getFile().getFileType() == RpcPerforceFileType.FST_APPLEFILE) {
					AppleFileHelper.extractFile(cfile.getFile());
				}

				if (cfile.getModTime() > 0) {
					try {
						cfile.getFile().setLastModified(cfile.getModTime() * 1000);
					} catch (Exception exc) {
						Log.warn("Unable to set target file modification time: " + exc);
					}
				}

				if (perms != null) {
					if (perms.equalsIgnoreCase(PERMS_RW)) {
						fileCommands.setWritable(cfile.getFile().getPath(), true);
					} else {
						if (cfile.isSymlink()) {
							fileCommands.setWritable(cfile.getFile().getPath(), true);
						} else {
							fileCommands.setWritable(cfile.getFile().getPath(), false);
						}
					}
				}

				if (cfile.getFile().getFileType().isExecutable()) {
					// See job075630
					// Set exec bit for Owner, Group and World.
					fileCommands.setExecutable(cfile.getFile().getPath(), true, false);
				}
			} finally {
				try {
					if (cfile.getTmpStream() != null) {
						cfile.getTmpStream().close();
					}
				} catch (IOException ioexc) {
					Log.warn("tmp file close error in ClientSystemFileCommands.closeFile(): "
							+ ioexc.getLocalizedMessage());
				}
				try {
					if (cfile.getStream() != null) {
						cfile.getStream().close();
					}
				} catch (IOException ioexc) {
					Log.warn("target file close error in ClientSystemFileCommands.closeFile(): "
							+ ioexc.getLocalizedMessage());
				}
				if (cfile.getTmpFile() != null) {
					if (cfile.getTmpFile().exists() && !cfile.getTmpFile().delete()) {
						Log.warn("Unable to delete tmp file '"
								+ cfile.getTmpFile().getPath()
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

		if (!RpcPerforceFile.fileExists(fromFile, fromFstSymlink)) {
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.FILE_NONEXISTENT,
							MessageSeverityCode.E_INFO,
							MessageGenericCode.EV_CLIENT,
							new String[]{clientPath}
					).toMap()
			);

			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}

		boolean caseSensitive
				= !(cmdEnv.getServerProtocolSpecsMap().containsKey(RpcFunctionMapKey.NOCASE));

		if (RpcPerforceFile.fileExists(toFile, toFstSymlink) && (!caseSensitive || !clientPath.equalsIgnoreCase(targetPath))) {
			// Target file exists, but this could be a case change, in which case allow this only if
			// the server is case sensitive (logic copied directly from the C++ equivalent)
			// Not sure about the logic here -- seems odd to allow this... (HR).

			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.CANT_CLOBBER,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[]{targetPath}
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
							new String[]{targetPath}
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
							new String[]{clientPath, "(cause unknown)"}
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
			    /*
			     *  Don't delete parent directories that are symbolic links. This mimics the
                 *  server behaviour that prevents a subsequent sync filling a disc when there
                 *  were symbolic links.
                 */
				if (!SymbolicLinkHelper.isSymbolicLink(dir.getAbsolutePath()) && !dir.delete()) {
					Log.stats("Unable to delete parent directory for delete for file '"
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
		String clientHandle = (String) resultsMap.get(RpcFunctionMapKey.HANDLE);
		String rmDir = (String) resultsMap.get(RpcFunctionMapKey.RMDIR);
		String fileTypeStr = (String) resultsMap.get(RpcFunctionMapKey.TYPE);
		String digest = (String) resultsMap.get(RpcFunctionMapKey.DIGEST);
		String digestType = (String) resultsMap.get(RpcFunctionMapKey.DIGESTTYPE);

		// clear syncTime

		cmdEnv.setSyncTime(0);

		RpcPerforceFileType fileType = RpcPerforceFileType.decodeFromServerString(fileTypeStr);

		// If fileType is null, then test for symlink in fileExists()
		boolean fstSymlink = (fileType == null || fileType == RpcPerforceFileType.FST_SYMLINK);

		File file = new File(clientPath);

		// Ignore non-existing files for the "client-DeleteFile" function
		// See job074183
		// Don't try to unlink a directory. It won't work, and it will be
		// confusing. Worse, it might mess up directory permissions.
		//
		if (!RpcPerforceFile.fileExists(file, fstSymlink)) {
			return RpcPacketDispatcherResult.CONTINUE_LOOP;
		}

		// Don't delete modified files noclobber allwrite (digestType set)
		if (digestType != null) {
			if (!rpcConnection.getDigest(fileType, file, RpcPerforceDigestType.GetType(digestType)).equals(digest)) {
				RpcHandler handler = cmdEnv.getHandler(clientHandle);

				if (handler == null) {
					handler = cmdEnv.new RpcHandler(clientHandle, false, new RpcPerforceFile(clientPath, fileTypeStr));
					cmdEnv.addHandler(handler);
				} else {
					handler.setFile(new RpcPerforceFile(clientPath, fileTypeStr));
					handler.getMap().clear();
				}

				handler.setError(true);
				cmdEnv.handleResult(
						new RpcMessage(
								ClientMessageId.NO_MODIFIED_FILE,
								MessageSeverityCode.E_FAILED,
								MessageGenericCode.EV_CLIENT,
								new String[]{"delete", clientPath}
						).toMap());
				return RpcPacketDispatcherResult.CONTINUE_LOOP;
			}
		}

		// Don't clobber poor file
		// noclobber, handle new to 99.1
		// be safe about clientHandle being set
		if (file.exists() && file.isFile() && (noClobber != null)
				&& file.canWrite() && clientHandle != null && !fstSymlink) {
			RpcHandler handler = cmdEnv.getHandler(clientHandle);

			if (handler == null) {
				handler = cmdEnv.new RpcHandler(clientHandle, false, new RpcPerforceFile(clientPath, fileTypeStr));
				cmdEnv.addHandler(handler);
			} else {
				handler.setFile(new RpcPerforceFile(clientPath, fileTypeStr));
				handler.getMap().clear();
			}

			handler.setError(true);
			cmdEnv.handleResult(
					new RpcMessage(
							ClientMessageId.CANT_CLOBBER,
							MessageSeverityCode.E_FAILED,
							MessageGenericCode.EV_CLIENT,
							new String[]{clientPath}
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
							new String[]{clientPath}
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
				    /*
				     *  Don't delete parent directories that are symbolic links. This mimics the
				     *  server behaviour that prevents a subsequent sync filling a disc when there
				     *  were symbolic links.
				     */
					if (!SymbolicLinkHelper.isSymbolicLink(dir.getAbsolutePath()) && !dir.delete()) {
						Log.stats("Unable to delete parent directory for delete for file '"
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
	 * <p>
	 * Much of the work happens off-stage in the support methods
	 * elsewhere.
	 * <p>
	 * What follows is copied from the C++ API:<p>
	 * <p>
	 * This routine, for compatibility purposes, has several modes.<p>
	 * <p>
	 * 1.	If clientType is set, we know the type and we're checking to see
	 * if the file exists and (if digest is set) if the file has the same
	 * fingerprint.  We return this in "status" with a value of "missing",
	 * "exists", or "same".  This starts around version 1742.<p>
	 * <p>
	 * 2.	If clientType is unset, we're looking for the type of the file,
	 * and we'll return it in "type".  This is sort of overloaded, 'cause
	 * it can also get set with pseudo-types like "missing".  In this
	 * case, we use the "xfiles" protocol check to make sure we don't
	 * return something the server doesn't expect.<p>
	 * <pre>
	 * 	- xfiles unset: return text, binary.
	 * 	- xfiles >= 0: also return xtext, xbinary.
	 * 	- xfiles >= 1: also return symlink.
	 * 	- xfiles >= 2; also return resource (mac resource file).
	 * 	- xfiles >= 3; also return ubinary
	 * 	- xfiles >= 4; also return apple
	 * </pre>
	 * If forceType is set, we'll use that in preference over what
	 * we've discovered.  We still check the file (to make sure they're
	 * not adding a directory, and so they get to right warning if
	 * they add an empty file), but we'll just override that back to
	 * the (typemap's) forceType.<p>
	 * <p>
	 * We map empty/missing/unreadable into forceType/"text".
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
		String digestType = (String) resultsMap.get(RpcFunctionMapKey.DIGESTTYPE);
		String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);
		String ignore = (String) resultsMap.get(RpcFunctionMapKey.IGNORE);
		String fileSize = (String) resultsMap.get(RpcFunctionMapKey.FILESIZE);
		String scanSize = (String) resultsMap.get(RpcFunctionMapKey.SCANSIZE);
		String checkLinks = (String) resultsMap.get(RpcFunctionMapKey.CHECKLINKS);
		String checkLinksNs = (String) resultsMap.get(RpcFunctionMapKey.CHECKLINKSN);
		int checkLinksN = 0;
		try {
			checkLinksN = checkLinksNs != null ? Integer.valueOf(checkLinksNs) : 0;
		} catch (NumberFormatException nfe) {
		}

		if (digest != null && digestType != null) {
			return checkFileGraph(rpcConnection, cmdEnv, resultsMap);
		}

		// For adding files,  checkSize is a maximum (or use alt type)
		// For flush,  checkSize is an optimization check on binary files.

		// checksize defined later

		// Check for symbolic link in path

		if (checkLinks != null) {
			File ps = new File(clientPath);

			// Don't allow opening a file for add if it is a symlink to
			// a directory.  job092324 said this was too restrictive, so
			// only do it if filesys.checklinks < 3.

			if (SymbolicLinkHelper.isSymbolicLink(ps.getAbsolutePath()) && checkLinksN < 3) {
				//TODO npoole: check that this actually sees symlinked directories like this
				if (ps.isDirectory()) {
					//msg.Set( MsgClient::CheckFileBadPath )
					//		<< clientPath << fs->Name();
					//client->GetUi()->Message( &msg );
					//cmdEnv.setError();
					return RpcPacketDispatcherResult.CONTINUE_LOOP;
				}
			}

			while ((ps = ps.getParentFile()) != null) {
				if (SymbolicLinkHelper.isSymbolicLink(ps.getAbsolutePath())) {
					//msg.Set( MsgClient::CheckFileBadPath )
					//		<< clientPath << fs->Name();
					//client->GetUi()->Message( &msg );
					//cmdEnv.setError();
					return RpcPacketDispatcherResult.CONTINUE_LOOP;
				}

				if (checkLinks.equals(ps.getAbsolutePath())) {
					break;
				}
			}
		}

		// 2012.1 server asks the client to do ignore checks (on add), in the
		// case of client forced file type (i.e 'p4 add -t binary file1.xyz'),
		// ignore == client-Ack, do quick confirm
		if (ignore != null) {

			// Do ignore checking, reject file matching ignore patterns
			if (fileMatchCommands.isIgnore(new File(clientPath), rpcConnection.getClientCharset(), cmdEnv)) {
				return RpcPacketDispatcherResult.CONTINUE_LOOP;
			}

			// Client forced file type.
			// "ignore == client-Ack"
			// Skip client-type checking.
			// Just ack, return confirm, echoing incoming args.
			if (ignore.length() > 0) {
				RpcFunctionSpec funcSpec = RpcFunctionSpec.decode(ignore);
				if (funcSpec == RpcFunctionSpec.CLIENT_ACK) {

					if (confirm.length() > 0) {
						Map<String, Object> respMap = new HashMap<String, Object>();
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
					}

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

			if (!RpcPerforceFile.fileExists(file, fstSymlink)) {
				status = "missing";
			} else if (digest != null) {
				// Calculate actual file digest; if same, we assume the file's the same as on the server.
				String digestStr = rpcConnection.getDigest(fileType, file);
				if ((digestStr != null) && digestStr.equals(digest)) {
					status = "same";
				}
			}
		} else {
			int scan = -1;
			if (scanSize != null) {
				try {
					scan = Integer.parseInt(scanSize);
				} catch (NumberFormatException e) {
				}
			}

			// Infer the file type, since it's not given.
			File file = new File(clientPath);
			fileType = RpcPerforceFileType.inferFileType(file, scan,
					cmdEnv.getRpcConnection().isUnicodeServer(),
					cmdEnv.getRpcConnection().getClientCharset());
			fstSymlink = (fileType == RpcPerforceFileType.FST_SYMLINK);

			if (!RpcPerforceFile.fileExists(file, fstSymlink)) {
				status = "missing";

				cmdEnv.handleResult(
						new RpcMessage(
								ClientMessageId.FILE_MISSING_ASSUMING_TYPE,
								MessageSeverityCode.E_INFO,
								MessageGenericCode.EV_CLIENT,
								new String[]{clientPath, nType}
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

			boolean overSize = false;

			RpcServerTypeStringSpec spec
					= RpcPerforceFileType.getServerFileTypeString(
					clientPath,
					overSize,
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

		Map<String, Object> respMap = new HashMap<String, Object>();

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

	protected RpcPacketDispatcherResult checkFileGraph(RpcConnection rpcConnection,
	                                                   CommandEnv cmdEnv, Map<String, Object> resultsMap)
			throws ConnectionException {

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
		String digest = (String) resultsMap.get(RpcFunctionMapKey.DIGEST);
		String digestType = (String) resultsMap.get(RpcFunctionMapKey.DIGESTTYPE);
		String confirm = (String) resultsMap.get(RpcFunctionMapKey.CONFIRM);

		String status = "exists";

		File file = new File(clientPath);

		if (!RpcPerforceFile.fileExists(file, true)) {
			status = "missing";
		} else {
			if (rpcConnection.getDigest(RpcPerforceFileType.decodeFromServerString(clientType), file,
					RpcPerforceDigestType.GetType(digestType)).equals(digest)) {
				status = "same";
			}
		}

		Map<String, Object> respMap = new HashMap<String, Object>();
		respMap.put(RpcFunctionMapKey.TYPE, clientType);
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

	protected RpcPacketDispatcherResult convertFile(
			RpcConnection rpcConnection, CommandEnv cmdEnv,
			Map<String, Object> resultsMap) throws ConnectionException {

		if (rpcConnection == null) {
			throw new NullPointerError("Null rpcConnection in convertFile().");
		}
		if (cmdEnv == null) {
			throw new NullPointerError("Null cmdEnv in convertFile().");
		}
		if (resultsMap == null) {
			throw new NullPointerError("Null resultsMap in convertFile().");
		}

		String clientPath = (String) resultsMap.get(RpcFunctionMapKey.PATH);
		String clientType = (String) resultsMap.get(RpcFunctionMapKey.TYPE);
		String perms = (String) resultsMap.get(RpcFunctionMapKey.PERMS);
		String fromCS = (String) resultsMap.get(RpcFunctionMapKey.CHARSET + 1);
		String toCS = (String) resultsMap.get(RpcFunctionMapKey.CHARSET + 2);

		if (clientPath == null) {
			throw new NullPointerException("Missing path");
		}
		if (perms == null) {
			throw new NullPointerException("Missing perms");
		}
		if (fromCS == null || toCS == null) {
			throw new NullPointerException("Missing charset");
		}

		//TODO: Still need to port this
		/*
		int size = FileSys::BufferSize();
        StrBuf bu, bt;
        char *b = bu.Alloc( size );
        int l, statFlags;

        FileSys *f = 0;
        FileSys *t = 0;

        CharSetCvt::CharSet cs1 = CharSetCvt::Lookup( fromCS->Text() );
        CharSetCvt::CharSet cs2 = CharSetCvt::Lookup( toCS->Text() );
        if( cs2 == CharSetApi::CSLOOKUP_ERROR ||
            cs1 == CharSetApi::CSLOOKUP_ERROR )
            goto convertFileFinish;

        f = ClientSvc::File( client, e );
        f->SetContentCharSetPriv( cs1 );
        if( e->Test() )
            goto convertFileFinish;

        statFlags = f->Stat();
        if( !( statFlags & FSF_EXISTS  ) ||
             ( statFlags & FSF_SYMLINK )  )
        {
            e->Set( MsgClient::FileOpenError );
            goto convertFileFinish;
		}

        t = client->GetUi()->File( f->GetType() );
        t->MakeLocalTemp( f->Name() );
        t->SetContentCharSetPriv( cs2 );

        f->Open( FOM_READ, e );
        f->Translator( CharSetCvt::FindCachedCvt( cs1, CharSetCvt::UTF_8 ) );

        t->Open( FOM_WRITE, e );
        t->Translator( CharSetCvt::FindCachedCvt( CharSetCvt::UTF_8, cs2 ) );

        if( e->Test() )
            goto convertFileFinish;

        while( ( l = f->Read( b, size, e ) ) && !e->GetErrorCount() )
            t->Write( b, l, e );

        // Translation errors are info, and FileSys::Close clears
        // info messages, so we need to trap those here since we
        // don't want to go ahead with a mangled file.

        if( e->GetErrorCount() )
        {
            e->Set( MsgSupp::ConvertFailed )
                << clientPath
                << fromCS
                << toCS;
            client->OutputError( e );
            f->Close( e );
            t->Close( e );
            t->Unlink( e );
            delete f;
            delete t;
            return;
			}

        f->Close( e );
        t->Close( e );

        if( e->Test() )
        {
            t->Unlink( e );
            goto convertFileFinish;
			}

        t->Rename( f, e );
        f->Chmod( perms->Text(), e );

    convertFileFinish:
        if( e->GetErrorCount() )
        {
            e->Set( MsgSupp::ConvertFailed )
                << clientPath
                << fromCS
                << toCS;
            client->OutputError( e );
			}
        delete f;
        delete t;
	 */
		return RpcPacketDispatcherResult.CONTINUE_LOOP;
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
					outStream = RpcOutputStream.getTmpOutputStream(tmpFile);
					// Set the new temp RPC output stream to the command env state map
					cmdEnv.getStateMap().put(RpcServer.RPC_TMP_OUTFILE_STREAM_KEY, outStream);
				} catch (IOException ioexc) {
					Log.error("tmp file creation error: " + ioexc.getLocalizedMessage());
					Log.exception(ioexc);
					throw new ConnectionException("Unable to create temporary file for Perforce file retrieval; " +
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

	private boolean validatePath(RpcConnection rpcConnection, CommandEnv cmdEnv, ClientFile cfile) {

		if (cfile == null) {
			// cfile is null - something bad happened - return false
			return false;
		}

		String clientPath = server.getClientPath();
		if (clientPath == null) {
			// P4CLIENT path unset - return true
			return true;
		}

		// file to verify
		String file = cfile.getFile().getAbsolutePath();

		// or symlink
		if (cfile.isSymlink() && rpcConnection.getFilesysRestrictedSymlinks() == 1) {
			if (!SymbolicLinkHelper.isSymbolicLinkCapable()) {
				cfile.setError(true);
				cmdEnv.handleResult(
						new RpcMessage(
								ClientMessageId.CANT_CREATE_FILE_TYPE,
								MessageSeverityCode.E_FAILED,
								MessageGenericCode.EV_CLIENT,
								new String[]{"symlink", cfile.getFile().getAbsolutePath()}
						).toMap()
				);
				// Symlink not supported - return false
				return false;
			}

			if (cfile.getSymTarget() != null) {
				file = cfile.getSymTarget();
			}
		}

		List<String> paths = Arrays.asList(clientPath.split(";"));
		for (String path : paths) {
			path = path.trim();
			if (file.startsWith(path)) {
				// Symlink or File is under P4CLIENTPATH - return true
				return true;
			}
		}

		cfile.setError(true);
		cmdEnv.handleResult(
				new RpcMessage(
						ClientMessageId.NOT_UNDER_CLIENT_PATH,
						MessageSeverityCode.E_FAILED,
						MessageGenericCode.EV_CLIENT,
						new String[]{file, clientPath}
				).toMap()
		);
		// Symlink or File is outside P4CLIENTPATH - return false
		return false;
	}
}
