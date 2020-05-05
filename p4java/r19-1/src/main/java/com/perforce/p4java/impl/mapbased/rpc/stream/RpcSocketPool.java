/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.stream;

import com.perforce.p4java.Log;
import com.perforce.p4java.impl.mapbased.rpc.stream.helper.RpcSocketHelper;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
public class RpcSocketPool {
	
	/**
	 * Shutdown handler for cleaning up before a socket is closed
	 */
	public static interface ShutdownHandler {

		/**
		 * Callback for before the socket is closed to do any pre-close work.
		 * Implementors should not directly close the socket parameter.
		 * 
		 * @param socket
		 */
		void shutdown(Socket socket);

	}

	/**
	 * Pool manager that closes sockets that have been left open for more than
	 * the idle time allowed.
	 * 
	 */
	private static class PoolManager implements Runnable {

		/**
		 * Socket idle time system property in milliseconds
		 */
		private static final String RPC_SOCKET_IDLE_TIME = "com.perforce.p4java.RPC_SOCKET_IDLE_TIME";

		/**
		 * Default idle time to close sockets - 30 seconds
		 */
		private static final int DEFAULT_SOCKET_IDLE_TIME = 30000;

		private int idleTime;
		private List<RpcSocketPool> pools;
		private boolean started = false;

		/**
		 * Create a new pool manager
		 */
		public PoolManager() {
			this.pools = new ArrayList<RpcSocketPool>();
			int time = DEFAULT_SOCKET_IDLE_TIME;
			String configuredTime = System.getProperty(RPC_SOCKET_IDLE_TIME);
			if (configuredTime != null) {
				try {
					time = Integer.parseInt(configuredTime);
				} catch (NumberFormatException nfe) {
					time = DEFAULT_SOCKET_IDLE_TIME;
				}
			}
			this.idleTime = time;
		}

		public void register(RpcSocketPool pool) {
			if (pool != null) {
				synchronized (this) {
					pools.add(pool);
				}
				if (started) {
					synchronized (this.pools) {
						this.pools.notify();
					}
				} else {
					start();
				}
			}
		}

		public void start() {
			started = true;
			Thread thread = new Thread(this);
			thread.setName("P4Java Socket Pool Manager");
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.setDaemon(true);
			thread.start();
		}

		public void unregister(RpcSocketPool pool) {
			if (pool != null) {
				synchronized (this) {
					pools.remove(pool);
				}
			}
		}

		/**
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			while (true) {
				while (this.pools.isEmpty()) {
					synchronized (this.pools) {
						try {
							this.pools.wait();
						} catch (InterruptedException e) {
							break;
						}
					}
				}
				RpcSocketPool[] pools = null;
				synchronized (this) {
					pools = this.pools.toArray(new RpcSocketPool[this.pools
							.size()]);
				}
				for (RpcSocketPool pool : pools) {
					pool.timeout(this.idleTime);
				}
				try {
					Thread.sleep(this.idleTime);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	private static PoolManager MANAGER = new PoolManager();

	private class SocketEntry {
		Socket socket;
		long releaseTime;

		/**
		 * Create a new socket entry with the specified socket with a release
		 * time of the current system time
		 * 
		 * @param socket
		 */
		public SocketEntry(Socket socket) {
			this.socket = socket;
			this.releaseTime = System.currentTimeMillis();
		}
	}

	private Properties socketProperties;
	private String host;
	private int port;
	private int size;
	private ShutdownHandler shutdownHandler;
	private Queue<SocketEntry> pool;
	private boolean secure = false;
	
	/**
	 * Create a new socket pool indicating whether it is secure (SSL) or not.
	 * 
	 * @param poolSize
	 * @param host
	 * @param port
	 * @param socketProperties
	 * @param shutdownHandler
	 * @param secure
	 */
	public RpcSocketPool(int poolSize, String host, int port,
			Properties socketProperties, ShutdownHandler shutdownHandler,
			boolean secure) {
		this(poolSize, host, port, socketProperties, shutdownHandler);
		this.secure = secure;
	}

	/**
	 * Create a new socket pool with a max pool size, host, port, and socket
	 * properties, and an optional shutdown handler
	 * 
	 * @param poolSize
	 * @param host
	 * @param port
	 * @param socketProperties
	 * @param shutdownHandler
	 */
	public RpcSocketPool(int poolSize, String host, int port,
			Properties socketProperties, ShutdownHandler shutdownHandler) {
		this.size = poolSize;
		this.host = host;
		this.port = port;
		this.socketProperties = socketProperties;
		this.pool = new LinkedList<SocketEntry>();
		this.shutdownHandler = shutdownHandler;
		MANAGER.register(this);
	}

	/**
	 * Acquire a socket to the configured server address
	 * 
	 * @return - socket
	 * @throws IOException
	 */
	public Socket acquire() throws IOException {
		Socket socket = null;
		synchronized (this.pool) {
			SocketEntry entry = this.pool.poll();
			if (entry != null) {
				socket = entry.socket;
			}
		}
		if (!isAlive(socket)) {
			quietClose(socket);
			socket = RpcSocketHelper.createSocket(this.host, this.port, this.socketProperties, this.secure);
		}
		return socket;
	}

	private void quietClose(Socket socket) {
		if (socket != null) {
			try {
				socket.getInputStream().close();
			} catch (IOException e) {
			}
			try {
				socket.getOutputStream().close();
			} catch (IOException e) {
			}
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	private boolean isAlive(Socket socket) {
		return socket != null && socket.isBound() && !socket.isClosed()
				&& socket.isConnected() && !socket.isInputShutdown()
				&& !socket.isOutputShutdown();
	}

	/**
	 * Release a socket back to the pool as no longer using
	 * 
	 * @param socket
	 * @param shutdownHandler
	 * @throws IOException
	 */
	public void release(Socket socket, ShutdownHandler shutdownHandler)
			throws IOException {
		if (isAlive(socket)) {
			boolean close = false;
			synchronized (this.pool) {
				if (this.pool.size() < size) {
					this.pool.add(new SocketEntry(socket));
				} else {
					close = true;
				}
			}
			if (close) {
				if (shutdownHandler != null) {
					shutdownHandler.shutdown(socket);
				}
				if (!socket.isClosed()) {
					socket.getInputStream().close();
				}
				if (!socket.isClosed()) {
					socket.getOutputStream().close();
				}
				socket.close();
			}
		}
	}

	private void close(Socket socket) throws IOException {
		if (socket != null) {
			if (!socket.isClosed()) {
				socket.getInputStream().close();
			}
			if (!socket.isClosed()) {
				socket.getOutputStream().close();
			}
			socket.close();
		}
	}

	/**
	 * Disconnect all sockets from the specified host and port
	 */
	public void disconnect() {
		Socket[] sockets = null;
		try {
			synchronized (this.pool) {
				sockets = new Socket[this.pool.size()];
				int count = 0;
				for (SocketEntry entry : this.pool) {
					sockets[count] = entry.socket;
					count++;
				}
				this.pool.clear();
			}
			for (Socket socket : sockets) {
				if (this.shutdownHandler != null) {
					this.shutdownHandler.shutdown(socket);
				}
				try {
					close(socket);
				} catch (IOException e) {
					Log.exception(e);
				}
			}
		} finally {
			MANAGER.unregister(this);
		}
	}

	/**
	 * Timeout any sockets idle for greater than or equal to the milliseconds
	 * value specified
	 * 
	 * @param idleDuration
	 */
	public void timeout(int idleDuration) {
		synchronized (this.pool) {
			long openTime;
			List<SocketEntry> closed = new ArrayList<SocketEntry>();
			for (SocketEntry entry : this.pool) {
				openTime = System.currentTimeMillis() - entry.releaseTime;
				if (openTime >= idleDuration) {
					if (this.shutdownHandler != null) {
						this.shutdownHandler.shutdown(entry.socket);
					}
					quietClose(entry.socket);
					closed.add(entry);
				}
			}
			this.pool.removeAll(closed);
		}
	}
}
