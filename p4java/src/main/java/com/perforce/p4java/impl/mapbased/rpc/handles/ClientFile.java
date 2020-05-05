package com.perforce.p4java.impl.mapbased.rpc.handles;

import com.perforce.p4java.impl.mapbased.rpc.CommandEnv.RpcHandler;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcOutputStream;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFile;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ClientFile extends AbstractHandle {

	private static final String FILE_OPEN_TMP_FILE_KEY = "tmpFile";
	private static final String FILE_OPEN_TMP_STREAM_KEY = "tmpFileStream";
	private static final String FILE_OPEN_TARGET_STREAM_KEY = "targetFileStream";
	private static final String FILE_DELETE_ON_ERR_KEY = "deleteOnError";
	private static final String FILE_OPEN_PATH_KEY = "openFilePath";
	private static final String FILE_OPEN_ORIG_ARGS_KEY = "origArgs";
	private static final String FILE_OPEN_MODTIME_KEY = "modTime";
	private static final String FILE_IS_DIFF_KEY = "isDiff";
	private static final String FILE_DIFF_NAME_KEY = "diffName";
	private static final String FILE_DIFF_FLAGS_KEY = "diffFlags";
	private static final String FILE_MATCH_DICT_KEY = "matchDct";
	private static final String FILE_SERVER_DIGEST_KEY = "serverDigest";
	private static final String FILE_INDIRECT_KEY = "indirect";
	private static final String FILE_SYM_TARGET_KEY = "symTarget";

	private String localDigest;

	@Override
	public String getHandleType() {
		return "ClientFile";
	}

	public ClientFile(RpcHandler rpcHandler) {
		super(rpcHandler);
	}

	public RpcPerforceFile getFile() {
		return (RpcPerforceFile) rpcHandler.getFile();
	}

	public void setFile(RpcPerforceFile file) {
		rpcHandler.setFile(file);
	}

	public RpcPerforceFile getTmpFile() {
		return (RpcPerforceFile) rpcHandler.getMap().get(FILE_OPEN_TMP_FILE_KEY);
	}

	public RpcOutputStream getStream() {
		return (RpcOutputStream) rpcHandler.getMap().get(FILE_OPEN_TARGET_STREAM_KEY);
	}

	public RpcOutputStream getTmpStream() {
		return (RpcOutputStream) rpcHandler.getMap().get(FILE_OPEN_TMP_STREAM_KEY);
	}

	public void Close() {
		try {
			if (getTmpStream() != null) {
				if (getTmpStream().getLocalDigester() != null) {
					localDigest = getTmpStream().getLocalDigester().digestAs32ByteHex();
				}
				getTmpStream().close();
			}

			if (getStream() != null) {
				if (getStream().getLocalDigester() != null) {
					localDigest = getStream().getLocalDigester().digestAs32ByteHex();
				}
				getStream().close();
			}
		} catch (IOException e) {
			setError(true);
		}
	}

	public boolean hasFile() {
		return getFile() != null || getTmpFile() != null;
	}

	public long getModTime() {
		if (!rpcHandler.getMap().containsKey(FILE_OPEN_MODTIME_KEY)) {
			return 0;
		}
		return (long) rpcHandler.getMap().get(FILE_OPEN_MODTIME_KEY);
	}

	public long statModTime() {
		return getFile().lastModified() / 1000;
	}

	public void setModTime(long modtime) {
		rpcHandler.getMap().put(FILE_OPEN_MODTIME_KEY, modtime);
	}

	public void setModTime(String modTime) {
		try {
			rpcHandler.getMap().put(FILE_OPEN_MODTIME_KEY, Long.parseLong(modTime));
		} catch (NumberFormatException nfe) {
			// noop
		}
	}

	public void setArgs(Map<String, Object> resultsMap) {
		rpcHandler.getMap().put(FILE_OPEN_ORIG_ARGS_KEY, resultsMap);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getArgs() {
		return (Map<String, Object>) rpcHandler.getMap().get(FILE_OPEN_ORIG_ARGS_KEY);
	}

	public void setDiff(int i) {
		rpcHandler.getMap().put(FILE_IS_DIFF_KEY, i);
	}

	public void setDeleteOnClose(boolean del) {

	}

	public void setDiffName(String path) {
		rpcHandler.getMap().put(FILE_DIFF_NAME_KEY, path);
	}

	public void setDiffFlags(String flags) {
		rpcHandler.getMap().put(FILE_DIFF_FLAGS_KEY, flags);
	}

	public boolean isSymlink() {
		return getFile().getFileType() == RpcPerforceFileType.FST_SYMLINK;
	}

	public void MakeGlobalTemp() {
	}

	public void setTmpFile(RpcPerforceFile rpcPerforceFile) {
		rpcHandler.getMap().put(FILE_OPEN_TMP_FILE_KEY, rpcPerforceFile);
	}

	public void createStream(boolean useLocalDigester, RpcConnection rpcConnection, String digest) throws IOException {
		if (getTmpFile() != null) {
			RpcOutputStream tmpStream = new RpcOutputStream(getTmpFile(), rpcConnection, useLocalDigester);
			if (useLocalDigester) {
				tmpStream.setServerDigest(digest);
			}
			rpcHandler.getMap().put(FILE_OPEN_TMP_STREAM_KEY, tmpStream);
		} else if (getFile() != null) {
			RpcOutputStream targetStream = new RpcOutputStream(getFile(), rpcConnection, useLocalDigester);
			if (useLocalDigester) {
				targetStream.setServerDigest(digest);
			}
			rpcHandler.getMap().put(FILE_OPEN_TARGET_STREAM_KEY, targetStream);
		}
	}

	@SuppressWarnings("unchecked")
	public HashMap<String, String> getMatchDict() {
		if (!rpcHandler.getMap().containsKey(FILE_MATCH_DICT_KEY)) {
			HashMap<String, String> dict = new HashMap<String, String>();
			rpcHandler.getMap().put(FILE_MATCH_DICT_KEY, dict);
			return dict;
		}
		return (HashMap<String, String>) rpcHandler.getMap().get(FILE_MATCH_DICT_KEY);
	}

	public void setError(boolean error) {
		rpcHandler.setError(error);
	}

	public boolean isError() {
		return rpcHandler.isError();
	}

	public boolean isDiff() {
		if (rpcHandler.getMap().containsKey(FILE_IS_DIFF_KEY)) {
			if ((Integer) rpcHandler.getMap().get(FILE_IS_DIFF_KEY) == 1) {
				return true;
			}
		}
		return false;
	}

	public String getServerDigest() {
		return (String) rpcHandler.getMap().get(FILE_SERVER_DIGEST_KEY);
	}

	public void setServerDigest(String digest) {
		rpcHandler.getMap().put(FILE_SERVER_DIGEST_KEY, digest);
	}

	public String getDigest() {
		return localDigest;
	}

	public boolean isIndirect() {
		return rpcHandler.getMap().containsKey(FILE_INDIRECT_KEY) &&
				(boolean) rpcHandler.getMap().get(FILE_INDIRECT_KEY);
	}

	public void setIndirect(boolean indirect) {
		rpcHandler.getMap().put(FILE_INDIRECT_KEY, indirect);
	}

	public String getSymTarget() {
		return (String) rpcHandler.getMap().get(FILE_SYM_TARGET_KEY);
	}

	public void setSymTarget(String symTarget) {
		File file = new File(symTarget);
		rpcHandler.getMap().put(FILE_SYM_TARGET_KEY, file.getAbsolutePath());
	}
}
