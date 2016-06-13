/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;
import com.perforce.p4java.util.FilesHelper;

/**
 * This super class is designed to lookup auth entries from file or memory.
 * 
 */
public abstract class AbstractAuthHelper {

	protected static final String SERVER_ADDRESS_MAP_KEY = "serverAddress";
	protected static final String USER_NAME_MAP_KEY = "userName";
	protected static final String AUTH_VALUE_MAP_KEY = "authValue";

	public static final int DEFAULT_LOCK_TRY = 100; // 100 tries
	public static final long DEFAULT_LOCK_DELAY = 300000; // 300 seconds delay time
	public static final long DEFAULT_LOCK_WAIT = 1000; // 1 second wait time
	
	/**
	 * Get the auth entry in the specified auth map that matches the specified
	 * user name and server address. The user name be non-null and the server
	 * address must be non-null and be of the form server:port.
	 * 
	 * @param authMap
	 * @return - list of auth entries found in the specified auth map
	 */
	protected static synchronized Map<String, String> getMemoryEntry(String userName, String serverAddress, Map<String, String> authMap) {
		Map<String, String> entryMap = null;
		if (userName != null && serverAddress != null && authMap != null) {
			if (serverAddress.lastIndexOf(':') == -1) {
				serverAddress = "localhost:" + serverAddress;
			}
			String prefix = serverAddress + "=" + userName;
			if (authMap.containsKey(prefix)) {
				String authValue = authMap.get(prefix);
				entryMap = new HashMap<String,String>();
				entryMap.put(SERVER_ADDRESS_MAP_KEY, serverAddress);
				entryMap.put(USER_NAME_MAP_KEY, userName);
				entryMap.put(AUTH_VALUE_MAP_KEY, authValue);
			}
		}
		return entryMap;	
	}

	/**
	 * Get all the auth entries found in the specified auth store in memory.
	 * 
	 * @param authMap
	 * @return - list of auth entries found in the specified auth map
	 */
	protected static synchronized List<Map<String, String>> getMemoryEntries(Map<String, String> authMap) {
		List<Map<String, String>> authList = new ArrayList<Map<String, String>>();
		if (authMap != null) {
			for (Map.Entry<String, String> entry : authMap.entrySet()) {
			    String line = entry.getKey() + ":" + entry.getValue();
				// Auth entry pattern is:
				// server_address=user_name:auth_value
				int equals = line.indexOf('=');
				if (equals != -1) {
					int colon = line.indexOf(':', equals);
					if (colon != -1 && colon + 1 < line.length()) {
						String serverAddress = line.substring(0,
								equals);
						String userName = line.substring(equals + 1,
								colon);
						String authValue = line
								.substring(colon + 1);
						Map<String,String> entryMap = new HashMap<String,String>();
						entryMap.put(SERVER_ADDRESS_MAP_KEY, serverAddress);
						entryMap.put(USER_NAME_MAP_KEY, userName);
						entryMap.put(AUTH_VALUE_MAP_KEY, authValue);
						authList.add(entryMap);
					}
				}
			}
		}
		return authList;
	}

	/**
	 * Save the specified parameters as an entry into the specified auth
	 * map. This method will add or replace the current entry for the user name
	 * and server address in the auth map. If the specified auth value is null
	 * then the current entry (if exits) in the specified map will be removed.
	 * 
	 * @param userName
	 *            - non-null user name
	 * @param serverAddress
	 *            - non-null server address
	 * @param authValue
	 *            - possibly null auth value
	 * @param authMap
	 *            - non-null auth map
	 */
	protected static synchronized void saveMemoryEntry(String userName, String serverAddress,
			String authValue, Map<String, String> authMap) {
		if (userName != null && serverAddress != null && authMap != null) {
			if (serverAddress.lastIndexOf(':') == -1) {
				serverAddress = "localhost:" + serverAddress;
			}
			String prefix = serverAddress + "=" + userName;
			if (authValue != null) { // save entry
				authMap.put(prefix, authValue);
			} else {
				if (authMap.containsKey(prefix)) { // delete entry
					authMap.remove(prefix);
				}
			}
		}
	}
	
	/**
	 * Get all the auth entries found in the specified auth file.
	 * 
	 * @param authFile
	 * @return - list of auth entries found in the specified auth file
	 * @throws IOException
	 *             - io exception from reading auth file
	 */
	protected static synchronized List<Map<String, String>> getFileEntries(File authFile) throws IOException {
		List<Map<String, String>> authList = new ArrayList<Map<String, String>>();
		if (authFile != null && authFile.exists()) {
			BufferedReader reader = new BufferedReader(new FileReader(
					authFile));
			try {
				String line = reader.readLine();
				while (line != null) {
					// Auth entry pattern is:
					// server_address=user_name:auth_value
					int equals = line.indexOf('=');
					if (equals != -1) {
						int colon = line.indexOf(':', equals);
						if (colon != -1 && colon + 1 < line.length()) {
							String serverAddress = line.substring(0,
									equals);
							String userName = line.substring(equals + 1,
									colon);
							String authValue = line
									.substring(colon + 1);
							Map<String,String> map = new HashMap<String,String>();
							map.put(SERVER_ADDRESS_MAP_KEY, serverAddress);
							map.put(USER_NAME_MAP_KEY, userName);
							map.put(AUTH_VALUE_MAP_KEY, authValue);
							authList.add(map);
						}
					}
					line = reader.readLine();
				}
			} finally {
				reader.close();
			}
		}
		return authList;
	}

	/**
	 * Save the specified parameters as an entry into the specified auth
	 * file. This method will replace the current entry for the user name and
	 * server address in the auth file. If a current entry is not found then
	 * the specified entry will be appended to the file. If the specified auth
	 * value is null then the current entry in the specified file will be
	 * removed if found.
	 * 
	 * @param userName
	 *            - non-null user name
	 * @param serverAddress
	 *            - non-null server address
	 * @param authValue
	 *            - possibly null auth value
	 * @param authFile
	 *            - non-null file
	 * @throws IOException
	 */
	protected static synchronized void saveFileEntry(String userName, String serverAddress,
			String authValue, File authFile, int lockTry, long lockDelay, long lockWait) throws IOException {
		if (userName != null && serverAddress != null && authFile != null) {
			// Create parent directories if necessary
			if (!authFile.exists()) {
				FilesHelper.mkdirs(authFile);
			}
			// Create lock file
			File lockFile = new File(authFile.getAbsolutePath() + ".lck");
			if (!createLockFile(lockFile, lockTry, lockDelay, lockWait)) {
				return;
			}
			
			if (serverAddress.lastIndexOf(':') == -1) {
				serverAddress = "localhost:" + serverAddress;
			}
			String prefix = serverAddress + "=" + userName + ":";
			String value = null;
			if (authValue != null) {
				value = prefix + authValue;
			}
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(authFile));
			} catch (FileNotFoundException fnfe) {
				// File is non-existent or not readable so ignored contents
				reader = null;
			}

			// Put contents in temp file
			File tempAuth = File.createTempFile("p4auth", ".txt");
			PrintWriter writer = new PrintWriter(tempAuth, "utf-8");
			boolean renamed = false;

			try {
				boolean processed = false;

				// Only add current auth file content if a reader was
				// successfully created
				if (reader != null) {
					String line = reader.readLine();
					while (line != null) {
						// Replace existing entry in the auth file
						if (!processed && line.startsWith(prefix)) {
							// value being null means that the entry should be
							// removed
							if (value != null) {
								writer.println(value);
							}
							processed = true;
						} else {
							writer.println(line);
						}
						line = reader.readLine();
					}
				}
				if (!processed && value != null) {
					writer.println(value);
				}
			} finally {
				writer.flush();
				writer.close();
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						// ignore
					}
				}
				try {
					// Rename to original auth file if no exceptions occur
					renamed = tempAuth.renameTo(authFile);
					if (!renamed) {
						// If a straight up rename fails then try to copy the new
						// auth file into the current p4 auth file. This seems to
						// happen on windows.
						renamed = FilesHelper.copy(tempAuth, authFile);
					}
				} finally {
					if (tempAuth.exists()) {
						if (!tempAuth.delete()) {
							Log.warn("Unable to delete temp auth file '"
									+ tempAuth.getPath()
									+ "' in AbstractAuthHelper.saveFileEntry() -- unknown cause");
						}
					}
				}
				// Delete lock file
				if (lockFile != null) {
					if (lockFile.exists()) {
						if (!lockFile.delete()) {
							lockFile.deleteOnExit();
							Log.error("Error deleting auth lock file: "
									+ lockFile.getAbsolutePath());
						}
					}
				}
			}

			// Update read bit of actual auth file
			updateReadBit(authFile);

			if (!renamed) {
				throw new IOException("P4 auth file: "
						+ authFile.getAbsolutePath()
						+ " could not be overwritten.");
			}

		}
	}

	private static void updateReadBit(File file) throws IOException {
		if (file != null) {
			// The goal is to set the file permissions bits to only have owner
			// read set (-r-------- or 400) but currently
			// java.io.File.setReadOnly may leave the group and other read bits
			// set. Try to leverage the registered helper to clear the remaining
			// read bits.Document this in the release notes.
			file.setReadOnly();
			ISystemFileCommandsHelper helper = ServerFactory
					.getRpcFileSystemHelper();
			if (helper == null) {
				helper = SysFileHelperBridge.getSysFileCommands();
			}
			if (helper != null) {
				helper.setOwnerReadOnly(file.getAbsolutePath());
			}
		}
	}
	
	private static boolean createLockFile(File lockFile, int lockTry, long lockDelay, long lockWait) {
		lockTry = lockTry < 1 ? DEFAULT_LOCK_TRY : lockTry;
		lockDelay = lockDelay < 1 ? DEFAULT_LOCK_DELAY : lockDelay;
		lockWait = lockWait < 1 ? DEFAULT_LOCK_WAIT : lockWait;
		
		if (lockFile != null) {
			while (lockTry-- > 0) {
				// Lock file exists
				if (lockFile.lastModified() > 0) {
					// Delete "old" lock file
					if ((System.currentTimeMillis() - lockFile.lastModified()) > lockDelay) {
						if (!lockFile.delete()) {
							lockFile.deleteOnExit();
							Log.error("Error deleting auth lock file: "
									+ lockFile.getAbsolutePath());
							return false;
						}
					} else { // Lock file is "new", so wait for other process/thread to finish with it
						try {
							Thread.sleep(lockWait);
						} catch (InterruptedException e) {
							Log.error("Error waiting for auth lock file: "
									+ e.getLocalizedMessage());
						}
					}
				} else { // Lock file doesn't exist, so create it
					try {
						if (lockFile.createNewFile()) {
							return true;
						}
					} catch (IOException e) {
						Log.error("Error creating new auth lock file: "
								+ lockFile.getAbsolutePath() + ": "
								+ e.getLocalizedMessage());
					}
				}
			}
			// Too many retries
			Log.error("Error creating new auth lock file after retries: "
					+ lockFile.getAbsolutePath());
		}
		
		return false;
	}
}
