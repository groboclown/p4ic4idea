package com.perforce.p4java.server;

import com.perforce.p4java.Log;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;
import com.perforce.p4java.util.FilesHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwIOException;
import static com.perforce.p4java.common.base.StringHelper.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lastIndexOf;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substring;

/**
 * This super class is designed to lookup auth entries from file or memory.
 */
public abstract class AbstractAuthHelper {

	public static final int DEFAULT_LOCK_TRY = 100; // 100 tries
	public static final long DEFAULT_LOCK_DELAY = 300000; // 300 seconds delay
	// time
	public static final long DEFAULT_LOCK_WAIT = 1; // 1 millisecond wait time
	protected static final String SERVER_ADDRESS_MAP_KEY = "serverAddress";
	protected static final String USER_NAME_MAP_KEY = "userName";
	protected static final String AUTH_VALUE_MAP_KEY = "authValue";

	private final static Object lock = new Object();

	/**
	 * Get the auth entry in the specified auth map that matches the specified
	 * user name and server address. The user name be non-null and the server
	 * address must be non-null and be of the form server:port.
	 *
	 * @return - list of auth entries found in the specified auth map
	 */
	protected static Map<String, String> getMemoryEntry(final String userName, final String serverAddress,
	                                                    final Map<String, String> authMap) {

		String p4Port = serverAddress;
		Map<String, String> entryMap = new ConcurrentHashMap<>();
		if (isNotBlank(userName) && isNotBlank(serverAddress) && nonNull(authMap)) {
			if (serverAddress.lastIndexOf(':') == -1) {
				p4Port = "localhost:" + serverAddress;
			}
			String prefix = p4Port + "=" + userName;
			if (authMap.containsKey(prefix)) {
				String authValue = authMap.get(prefix);
				entryMap.put(SERVER_ADDRESS_MAP_KEY, p4Port);
				entryMap.put(USER_NAME_MAP_KEY, userName);
				entryMap.put(AUTH_VALUE_MAP_KEY, authValue);
			}
		}
		return entryMap;
	}

	/**
	 * Get all the auth entries found in the specified auth store in memory.
	 *
	 * @return - list of auth entries found in the specified auth map
	 */
	protected static List<Map<String, String>> getMemoryEntries(final Map<String, String> authMap) {
		List<Map<String, String>> authList = new CopyOnWriteArrayList<>();
		if (nonNull(authMap)) {
			for (Map.Entry<String, String> entry : authMap.entrySet()) {
				String line = entry.getKey() + ":" + entry.getValue();
				// Auth entry pattern is:
				// server_address=user_name:auth_value
				int equals = line.indexOf('=');
				popularAuthEntry(equals, line, authList);
			}
		}
		return authList;
	}

	private static void popularAuthEntry(final int equals, final String line,
	                                     final List<Map<String, String>> authList) {

		if (equals != -1) {
			int colon = indexOf(line, ':', equals);
			if (colon != -1 && colon + 1 < line.length()) {
				String serverAddress = substring(line, 0, equals);
				String userName = substring(line, equals + 1, colon);
				String authValue = substring(line, colon + 1);
				Map<String, String> map = new ConcurrentHashMap<>();
				map.put(SERVER_ADDRESS_MAP_KEY, serverAddress);
				map.put(USER_NAME_MAP_KEY, userName);
				map.put(AUTH_VALUE_MAP_KEY, authValue);
				authList.add(map);
			}
		}
	}

	/**
	 * Save the specified parameters as an entry into the specified auth map.
	 * This method will add or replace the current entry for the user name and
	 * server address in the auth map. If the specified auth value is null then
	 * the current entry (if exits) in the specified map will be removed.
	 *
	 * @param userName      - non-null user name
	 * @param serverAddress - non-null server address
	 * @param authValue     - possibly null auth value
	 * @param authMap       - non-null auth map
	 */
	protected static void saveMemoryEntry(final String userName, final String serverAddress, final String authValue,
	                                      final Map<String, String> authMap) {

		if (isNotBlank(userName) && isNotBlank(serverAddress) && nonNull(authMap)) {
			String p4Port = serverAddress;
			if (serverAddress.lastIndexOf(':') == -1) {
				p4Port = "localhost:" + serverAddress;
			}
			String prefix = p4Port + "=" + userName;
			if (isNotBlank(authValue)) { // save entry
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
	 * @return - list of auth entries found in the specified auth file
	 * @throws IOException - io exception from reading auth file
	 */
	protected static List<Map<String, String>> getFileEntries(final File authFile) throws IOException {

		List<Map<String, String>> authList = new CopyOnWriteArrayList<>();
		if (nonNull(authFile) && authFile.exists()) {

			try (BufferedReader reader = new BufferedReader(new FileReader(authFile))) {
				String line = reader.readLine();
				while (line != null) {
					// Auth entry pattern is:
					// server_address=user_name:auth_value
					int equals = indexOf(line, '=');
					popularAuthEntry(equals, line, authList);
					line = reader.readLine();
				}
			}
		}
		return authList;
	}

	/**
	 * Save the specified parameters as an entry into the specified auth file.
	 * This method will replace the current entry for the user name and server
	 * address in the auth file. If a current entry is not found then the
	 * specified entry will be appended to the file. If the specified auth value
	 * is null then the current entry in the specified file will be removed if
	 * found.
	 *
	 * @param userName      - non-null user name
	 * @param serverAddress - non-null server address
	 * @param authValue     - possibly null auth value
	 * @param authFile      - non-null file
	 */
	protected static void saveFileEntry(final String userName, final String serverAddress, final String authValue,
	                                    final File authFile, final int lockTry, final long lockDelay, final long lockWait) throws IOException {

		if (isNotBlank(userName) && isNotBlank(serverAddress) && nonNull(authFile)) {
			String p4Port = firstMatch(lastIndexOf(serverAddress, ':') == -1, "localhost:" + serverAddress,
					serverAddress);

			Path authFilePath = authFile.toPath();

			synchronized (lock) {
				if (Files.notExists(authFilePath)) {
					Files.createDirectories(authFilePath.getParent());
					createFileIgnoreIfFileAlreadyExists(authFilePath);
				}
				File lockFile = createLockFileIfNotExist(authFile);
				boolean locked = false;
				try (RandomAccessFile lockFileRandomAccessor = new RandomAccessFile(lockFile, "rw");
				     FileChannel fileChannel = lockFileRandomAccessor.getChannel();
				     FileLock lock = tryLockFile(fileChannel, lockFile, lockTry, lockWait)) {

					if (nonNull(lock) && lock.isValid()) {
						locked = true;
						String authValuePrefix = format("%s=%s:", p4Port, userName);
						String newAuthValue = firstMatch(isNotBlank(authValue), authValuePrefix + authValue, EMPTY);

						try {
							readAuthFileContentPlusNewAuthValueAndWriteToTempAuthFile(authFile, authValuePrefix,
									newAuthValue);
							updateReadBit(authFile);
						} catch (IOException e) {
							e.printStackTrace();
							throwIOException(e, "P4TICKETS file: %s could not be overwritten.",
									authFile.getAbsolutePath());
						}

						// Update read bit of actual auth file
						updateReadBit(authFile);
					}
				} finally {
					if (locked) {
						Files.deleteIfExists(lockFile.toPath());
					}
				}
			}
		}
	}

	private static void createFileIgnoreIfFileAlreadyExists(Path filePath) throws IOException {
		if (!Files.exists(filePath)) {
			Files.createFile(filePath);
		}
	}

	private static File createLockFileIfNotExist(@Nonnull final File authFile) throws IOException {
		File lockFile = new File(authFile.getAbsolutePath() + ".lck");
		Path lockFilePath = lockFile.toPath();
		createFileIgnoreIfFileAlreadyExists(lockFilePath);

		return lockFile;
	}

	private static void readAuthFileContentPlusNewAuthValueAndWriteToTempAuthFile(final File authFile,
	                                                                              final String authValuePrefix, final String newAuthValue) throws IOException {

		File tempAuth = File.createTempFile("p4auth_" + System.currentTimeMillis(), ".txt");
		try (BufferedReader reader = new BufferedReader(new FileReader(authFile));
		     PrintWriter writer = new PrintWriter(tempAuth, "utf-8")) {
			boolean processed = false;
			// Only add current auth file content if a reader was
			// successfully created
			String possibleValidAuthValue;
			while ((possibleValidAuthValue = reader.readLine()) != null) {
				boolean isExistingAuthValueEntry = !processed && startsWith(possibleValidAuthValue, authValuePrefix);
				if (isExistingAuthValueEntry) {
					// newAuthValue being null means that the entry should be
					// removed
					if (isNotBlank(newAuthValue)) {
						writer.println(newAuthValue);
					}
					processed = true;
				} else {
					writer.println(possibleValidAuthValue);
				}
			}
			if (!processed && isNotBlank(newAuthValue)) {
				writer.println(newAuthValue);
			}
			writer.flush();
		}

		if (authFile.exists()) {
			authFile.setWritable(true);
		}

		try {
			Files.move(tempAuth.toPath(), authFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			if (!FilesHelper.copy(tempAuth, authFile)) {
				throwIOException("P4 auth file: %s could not be overwritten.", authFile.getAbsolutePath());
			}
		} finally {
			if (tempAuth.exists()) {
				tempAuth.delete();
			}
		}
	}

	private static FileLock tryLockFile(@Nullable final FileChannel lockFileChannel, @Nonnull final File lockFile,
	                                    final int lockTry, final long lockWait) throws IOException {

		int lockTries = firstMatch(lockTry < 1, DEFAULT_LOCK_TRY, lockTry);
		long lockWaits = firstMatch(lockWait < 1, DEFAULT_LOCK_WAIT, lockWait);

		String currentThreadName = Thread.currentThread().getName();

		FileLock fileLock = null;
		if (nonNull(lockFileChannel)) {
			do {
				try {
					Log.info("-----%s thread try to get lock", currentThreadName);
					fileLock = lockFileChannel.tryLock();
					if (fileLock.isValid()) {
						Log.info("=====%s thread get lock successfully\r\n", currentThreadName);
					}

					if (isNull(fileLock)) {
						System.out.println("did not get the lock");
					}
				} catch (IllegalStateException e) {
					// ignore, means locked by other process at this moment
				}

				if (nonNull(fileLock) && fileLock.isValid()) {
					break;
				} else {
					if (lockFile.lastModified() > 0) {
						try {
							Log.info("-----%s thread put to sleep, and it will retry lock %s times", currentThreadName,
									lockTries - 1);
							TimeUnit.SECONDS.sleep(lockWaits);
						} catch (InterruptedException e) {
							Log.error("Error waiting for auth lock file: %s", e.getLocalizedMessage());
						}
					}
				}
			} while (lockTries-- > 0);

			if (isNull(fileLock) || !fileLock.isValid()) {
				throwIOException("Error creating new auth lock file \"%s\" after retries: %s",
						lockFile.getAbsolutePath(), lockTry);
			}
		}

		return fileLock;
	}

	private static <T> T firstMatch(boolean expression, T first, T second) {
		if (expression) {
			return first;
		} else {
			return second;
		}
	}

	private static void updateReadBit(@Nullable final File file) throws IOException {
		if (nonNull(file)) {
			// The goal is to set the file permissions bits to only have owner
			// read set (-r-------- or 400) but currently
			// java.io.File.setReadOnly may leave the group and other read bits
			// set. Try to leverage the registered helper to clear the remaining
			// read bits.Document this in the release notes.
			file.setReadOnly();
			ISystemFileCommandsHelper helper = ServerFactory.getRpcFileSystemHelper();
			if (isNull(helper)) {
				helper = SysFileHelperBridge.getSysFileCommands();
			}
			if (nonNull(helper)) {
				helper.setOwnerReadOnly(file.getAbsolutePath());
			}
		}
	}
}
