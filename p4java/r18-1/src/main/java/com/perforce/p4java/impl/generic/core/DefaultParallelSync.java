package com.perforce.p4java.impl.generic.core;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import com.perforce.p4java.server.callback.IParallelCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Provides capability to perform sync in parallel.
 * The actual functionality is implemented as a server callback.
 */
public class DefaultParallelSync implements IParallelCallback {

	/**
	 * Default constructor
	 */
	public DefaultParallelSync() {

	}

	/**
	 * This function notifies the server that the sync can be done in parallel.
	 * <p>
	 * Invocation of this method spawns the threads required to support parallelism
	 *
	 * @param cmdEnv
	 * @param threads
	 * @param flags
	 * @param args
	 * @return
	 */
	@Override
	public boolean transmit(CommandEnv cmdEnv, int threads, HashMap<String, String> flags, ArrayList<String> args) {
		Thread[] parallelSyncThreads = new Thread[threads];
		try {
			for (int x = 0; x < threads; x++) {
				parallelSyncThreads[x] = new Thread(createRunnable(cmdEnv, x, flags, args));
				parallelSyncThreads[x].start();
			}
			for (int x = 0; x < threads; x++) {
				parallelSyncThreads[x].join();
			}
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	/**
	 * Helper method which creates a Runnable that performs the sync
	 *
	 * @param cmdEnv
	 * @param thread
	 * @param flags
	 * @param args
	 * @return
	 */
	private Runnable createRunnable(final CommandEnv cmdEnv, int thread,
	                                HashMap<String, String> flags, final List<String> args) {

		class RunnableSync implements Runnable {
			@Override
			public void run() {
				IOptionsServer server = null;
				try {
					Properties props = new Properties(cmdEnv.getServer().getProperties());
					server = ServerFactory.getOptionsServer(cmdEnv.getServer().getServerAddressDetails().getUri(), props);

					server.setCurrentServerInfo(cmdEnv.getServer().getCurrentServerInfo());
					server.setUserName(cmdEnv.getServer().getUserName());
					server.setAuthTicket(cmdEnv.getServer().getAuthTicket());
					server.setCurrentClient(cmdEnv.getServer().getCurrentClient());
					server.setWorkingDirectory(cmdEnv.getServer().getWorkingDirectory());
					server.setTrustFilePath(cmdEnv.getServer().getTrustFilePath());
					server.setTicketsFilePath(cmdEnv.getServer().getTicketsFilePath());
					server.connect();

					//pass the result to the handle result
					Map<String, Object>[] results = server.execMapCmd("transmit", args.toArray(new String[]{}), null);
					handleResults(results, cmdEnv);
				} catch (Throwable e) {
					throw new RuntimeException(e);
				} finally {
					if (server != null && server.isConnected()) {
						try {
							server.disconnect();
						} catch (P4JavaException ex) {
							throw new RuntimeException(ex);
						}
					}
				}
			}
		}
		return new RunnableSync();
	}

	/**
	 * Handles results on a per thread basis
	 *
	 * @param results
	 * @param cmdEnv
	 */
	private synchronized void handleResults(Map<String, Object>[] results, CommandEnv cmdEnv) {
		if (results != null) {
			for (Map<String, Object> item : results) {
				cmdEnv.handleResult(item);
			}
		}
	}
}
