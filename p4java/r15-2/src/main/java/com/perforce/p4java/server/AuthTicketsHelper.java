package com.perforce.p4java.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is designed to lookup authentication tickets from a tickets file
 * or the in-memory tickets map. If a null tickets file parameter is passed to
 * the methods, the in-memory tickets map will be used instead.
 * 
 */
public class AuthTicketsHelper extends AbstractAuthHelper {

	private static final AuthTicket[] EMPTY = new AuthTicket[0];

	private static Map<String, String> ticketsMap = 
			Collections.synchronizedMap(new HashMap<String, String>());

	/**
	 * Get the first found ticket value that matches the specified user name and
	 * server address. The user name be non-null and the server address must be
	 * non-null and be of the form server:port.
	 * 
	 * @param userName
	 * @param serverAddress
	 * @param ticketsFilePath
	 * @return - ticket value or null if not found
	 * @throws IOException
	 *             - io exception from reading tickets file
	 */
	public static String getTicketValue(String userName, String serverAddress,
			String ticketsFilePath) throws IOException {
		String ticketValue = null;
		AuthTicket ticket = getTicket(userName, serverAddress, ticketsFilePath);
		if (ticket != null) {
			ticketValue = ticket.getTicketValue();
		}
		return ticketValue;
	}

	/**
	 * Get all the tickets found in the file at the specified file path.
	 * 
	 * @param ticketsFilePath
	 * @return - array of tickets found in the specified tickets file at the
	 *         specified path
	 * @throws IOException
	 *             - io exception from reading tickets file
	 */
	public static AuthTicket[] getTickets(String ticketsFilePath)
			throws IOException {
		File file = ticketsFilePath != null ? new File(ticketsFilePath) : null;
		AuthTicket[] tickets = getTickets(file);
		return tickets;
	}

	/**
	 * Get all the tickets found in the specified file.
	 * 
	 * @param ticketsFile
	 * @return - array of tickets found in the specified tickets file
	 * @throws IOException
	 *             - io exception from reading tickets file
	 */
	public static synchronized AuthTicket[] getTickets(File ticketsFile)
			throws IOException {
		AuthTicket[] tickets = EMPTY;
		List<Map<String, String>> authList = ticketsFile != null ? 
				getFileEntries(ticketsFile) : getMemoryEntries(ticketsMap);
		if (authList != null) {
			List<AuthTicket> ticketList = new ArrayList<AuthTicket>();
			for (Map<String, String> map : authList) {
				if (map != null) {
					String serverAddress = map.get(SERVER_ADDRESS_MAP_KEY);
					String userName = map.get(USER_NAME_MAP_KEY);
					String ticketValue = map.get(AUTH_VALUE_MAP_KEY);
					AuthTicket ticket = new AuthTicket(serverAddress,
							userName, ticketValue);
					ticketList.add(ticket);
				}
			}
			tickets = ticketList.toArray(new AuthTicket[0]);
		}
		return tickets;
	}

	/**
	 * Get the first found ticket in the specified ticket file that matches the
	 * specified user name and server address. The server address must be
	 * non-null and be of the form server:port. The user name may be null and if
	 * that is the case the found first ticket for the specified server address
	 * will be returned.
	 * 
	 * @param userName
	 *            - possibly null user name to match against the found tickets
	 * @param serverAddress
	 *            - non-null server address
	 * @param ticketsFilePath
	 *            - path to tickets file to search
	 * @return - found ticket or null if not found
	 * @throws IOException
	 *             - io exception from reading tickets file
	 */
	public static AuthTicket getTicket(String userName, String serverAddress,
			String ticketsFilePath) throws IOException {
		AuthTicket foundTicket = null;
		if (serverAddress != null) {
			if (serverAddress.indexOf(':') == -1) {
				serverAddress = "localhost:" + serverAddress;
			}
			for (AuthTicket ticket : getTickets(ticketsFilePath)) {
				if (serverAddress.equals(ticket.getServerAddress())
						&& (userName == null || userName.equals(ticket
								.getUserName()))) {
					foundTicket = ticket;
					break;
				}
			}
		}
		return foundTicket;
	}

	/**
	 * Get the first found ticket in the specified ticket file that matches the
	 * specified server address. The server address must be non-null and be of
	 * the form server:port.
	 * 
	 * @param serverAddress
	 *            - non-null server address
	 * @param ticketsFilePath
	 *            - path to tickets file to search
	 * @return - found ticket or null if not found
	 * @throws IOException
	 *             - io exception from reading tickets file
	 */
	public static AuthTicket getTicket(String serverAddress,
			String ticketsFilePath) throws IOException {
		return getTicket(null, serverAddress, ticketsFilePath);
	}

	/**
	 * Save the specified ticket as an entry into the specified tickets file.
	 * This method will replace the current entry for the user name and server
	 * address in the tickets file. If a current entry is not found then the
	 * specified entry will be appended to the file.
	 * 
	 * @param ticket
	 *            - non-null ticket
	 * @param ticketsFilePath
	 *            - non-null path
	 * @throws IOException
	 */
	public static void saveTicket(AuthTicket ticket, String ticketsFilePath)
			throws IOException {
		File file = ticketsFilePath != null ? new File(ticketsFilePath) : null;
		saveTicket(ticket, file);
	}

	/**
	 * Save the specified ticket as an entry into the specified tickets file.
	 * This method will replace the current entry for the user name and server
	 * address in the tickets file. If a current entry is not found then the
	 * specified entry will be appended to the file.
	 * 
	 * @param ticket
	 *            - non-null ticket
	 * @param ticketsFile
	 *            - non-null file
	 * @throws IOException
	 */
	public static void saveTicket(AuthTicket ticket, File ticketsFile)
			throws IOException {
		if (ticket != null) {
			saveTicket(ticket.getUserName(), ticket.getServerAddress(),
					ticket.getTicketValue(), ticketsFile);
		}
	}

	/**
	 * Save the specified parameters as an entry into the specified tickets
	 * file. This method will replace the current entry for the user name and
	 * server address in the tickets file. If a current entry is not found then
	 * the specified entry will be appended to the file.
	 * 
	 * @param userName
	 *            - non-null user name
	 * @param serverAddress
	 *            - non-null server address
	 * @param ticketValue
	 *            - non-null ticket value
	 * @param ticketsFilePath
	 *            - non-null file path
	 * @throws IOException
	 */
	public static void saveTicket(String userName, String serverAddress,
			String ticketValue, String ticketsFilePath) throws IOException {
		File file = ticketsFilePath != null ? new File(ticketsFilePath) : null;
		saveTicket(userName, serverAddress, ticketValue, file);
	}

	/**
	 * Save the specified parameters as an entry into the specified tickets
	 * file. This method will replace the current entry for the user name and
	 * server address in the tickets file. If a current entry is not found then
	 * the specified entry will be appended to the file.
	 * 
	 * @param userName
	 *            - non-null user name
	 * @param serverAddress
	 *            - non-null server address
	 * @param ticketValue
	 *            - non-null ticket value
	 * @param ticketsFilePath
	 *            - non-null file path
	 * @param lockTry
	 *            - number of tries for locking
	 * @param lockDelay
	 *            - delay time (ms) for locking
	 * @param lockWait
	 *            - wait time (ms) for other process/thread to finish locking
	 * @throws IOException
	 */
	public static void saveTicket(String userName, String serverAddress,
			String ticketValue, String ticketsFilePath, int lockTry,
			long lockDelay, long lockWait) throws IOException {
		File file = ticketsFilePath != null ? new File(ticketsFilePath) : null;
		saveTicket(userName, serverAddress, ticketValue, file,
				lockTry, lockDelay, lockWait);
	}

	/**
	 * Save the specified parameters as an entry into the specified tickets
	 * file. This method will replace the current entry for the user name and
	 * server address in the tickets file. If a current entry is not found then
	 * the specified entry will be appended to the file. If the specified ticket
	 * value is null then the current entry in the specified file will be
	 * removed if found.
	 * 
	 * @param userName
	 *            - non-null user name
	 * @param serverAddress
	 *            - non-null server address
	 * @param ticketValue
	 *            - possibly null ticket value
	 * @param ticketsFile
	 *            - non-null file
	 * @throws IOException
	 */
	public static synchronized void saveTicket(String userName, String serverAddress,
			String ticketValue, File ticketsFile) throws IOException {
		saveTicket(userName, serverAddress, ticketValue, ticketsFile, 0, 0, 0);
	}

	/**
	 * Save the specified parameters as an entry into the specified tickets
	 * file. This method will replace the current entry for the user name and
	 * server address in the tickets file. If a current entry is not found then
	 * the specified entry will be appended to the file. If the specified ticket
	 * value is null then the current entry in the specified file will be
	 * removed if found.
	 * 
	 * @param userName
	 *            - non-null user name
	 * @param serverAddress
	 *            - non-null server address
	 * @param ticketValue
	 *            - possibly null ticket value
	 * @param ticketsFile
	 *            - non-null file
	 * @param lockTry
	 *            - number of tries for locking
	 * @param lockDelay
	 *            - delay time (ms) for locking
	 * @param lockWait
	 *            - wait time (ms) for other process/thread to finish locking
	 * @throws IOException
	 */
	public static synchronized void saveTicket(String userName, String serverAddress,
			String ticketValue, File ticketsFile, int lockTry, long lockDelay, long lockWait)
					throws IOException {
		if (ticketsFile != null) {
			saveFileEntry(userName, serverAddress, ticketValue, ticketsFile,
					lockTry, lockDelay, lockWait);
		} else {
			saveMemoryEntry(userName, serverAddress, ticketValue, ticketsMap);
		}
	}
}
