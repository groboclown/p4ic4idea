/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

/**
 * This super class is designed to lookup auth entries from file or memory.
 * 
 */
public abstract class AbstractAuthHelper {

	protected static final String SERVER_ADDRESS_MAP_KEY = "serverAddress";
	protected static final String USER_NAME_MAP_KEY = "userName";
	protected static final String AUTH_VALUE_MAP_KEY = "authValue";

	
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
				serverAddress += "localhost:" + serverAddress;
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
				serverAddress += "localhost:" + serverAddress;
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
			String authValue, File authFile) throws IOException {
		if (userName != null && serverAddress != null && authFile != null) {
			if (serverAddress.lastIndexOf(':') == -1) {
				serverAddress += "localhost:" + serverAddress;
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
						renamed = copy(tempAuth, authFile);
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

	private static boolean copy(File source, File destination)
			throws IOException {
		boolean copied = false;
		if (source != null && destination != null) {
		    FileInputStream reader = null;
		    FileOutputStream writer = null;
			try {
				ISystemFileCommandsHelper helper = ServerFactory.getRpcFileSystemHelper();
				if (helper == null) {
					helper = SysFileHelperBridge.getSysFileCommands();
				}
				if (helper != null) {
					helper.setWritable(destination.getAbsolutePath(), true);
				}
				reader = new FileInputStream(source);
				writer = new FileOutputStream(destination);
				long targetCount = reader.getChannel().size();
				long transferCount = writer.getChannel().transferFrom(reader.getChannel(), 0, targetCount);
				copied = transferCount == targetCount;
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						Log.warn("reader close error in AbstractAuthHelper.copy(): "
								+ e.getLocalizedMessage());
						Log.exception(e);
					}
				}
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						Log.warn("writer close error in AbstractAuthHelper.copy(): "
								+ e.getLocalizedMessage());
						Log.exception(e);
					}
				}
			}
		}
		return copied;
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
}
