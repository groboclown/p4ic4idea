package com.perforce.p4java.impl.mapbased.rpc.func.client;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.client.ParallelSyncOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.server.IServer;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sean Shou
 * @since 26/08/2016
 */
public class ClientHelper {
	private ClientHelper() { /* util */ }

	/**
	 * Send back the data bytes written (accumulated)
	 * This is for the progress indicator
	 */
	public static long sendBackWrittenDataBytes(
			final CommandEnv cmdEnv,
			final String filePath,
			final long fileSize,
			final long currentSize,
			final long bytesRead) {
		long totalReadSize = currentSize;
		if (cmdEnv.getProtocolSpecs().isEnableProgress()) {
			if (fileSize > 0 && bytesRead > 0) {
				totalReadSize += bytesRead;
				Map<String, Object> dataSizeMap = new HashMap<String, Object>();
				dataSizeMap.put("path", filePath);
				dataSizeMap.put("fileSize", fileSize);
				dataSizeMap.put("currentSize", totalReadSize);
				cmdEnv.handleResult(dataSizeMap);
			}
		}

		return totalReadSize;
	}

	/**
	 * Helper method that build the parallel sync options
	 *
	 * @param pSyncOpts
	 * @return String
	 * @throws P4JavaException
	 */
	public static String[] buildParallelOptions(IServer serverImpl, List<IFileSpec> fileSpecs, SyncOptions syncOpts,
	                                            ParallelSyncOptions pSyncOpts) throws P4JavaException {

		StringBuilder parallelOptionsBuilder = new StringBuilder();
		parallelOptionsBuilder.append("--parallel=");
		if (pSyncOpts.getNumberOfThreads() > 0) {
			parallelOptionsBuilder.append("threads=" + pSyncOpts.getNumberOfThreads());
		} else {
			parallelOptionsBuilder.append("threads=0");
		}
		if (pSyncOpts.getMinimum() > 0) {
			parallelOptionsBuilder.append(",min=" + pSyncOpts.getMinimum());
		}
		if (pSyncOpts.getMinumumSize() > 0) {
			parallelOptionsBuilder.append(",minsize=" + pSyncOpts.getMinumumSize());
		}
		if (pSyncOpts.getBatch() > 0) {
			parallelOptionsBuilder.append(",batch=" + pSyncOpts.getBatch());
		}
		if (pSyncOpts.getBatchSize() > 0) {
			parallelOptionsBuilder.append(",batchSize=" + pSyncOpts.getBatchSize());
		}

		String[] syncOptions = Parameters.processParameters(syncOpts, fileSpecs, serverImpl);
		String[] po = {parallelOptionsBuilder.toString()};

		String[] mergedOptions = ArrayUtils.addAll(po, syncOptions);

		return mergedOptions;
	}
}
