package com.perforce.p4java.server;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class is designed to lookup authentication tickets from a tickets file
 * or the in-memory tickets map. If a null tickets file parameter is passed to
 * the methods, the in-memory tickets map will be used instead.
 */
public class AuthTicketsHelper extends AbstractAuthHelper {
    private static final AuthTicket[] EMPTY = new AuthTicket[0];
    private static final Map<String, String> ticketsMap = new ConcurrentHashMap<>();

    /**
     * Get the first found ticket value that matches the specified user name and
     * server address. The user name be non-null and the server address must be
     * non-null and be of the form server:port.
     *
     * @return - ticket value or null if not found
     * @throws IOException - io exception from reading tickets file
     */
    public static String getTicketValue(
            final String userName,
            final String serverAddress,
            final String ticketsFilePath) throws IOException {

        String ticketValue = null;
        AuthTicket ticket = getTicket(
                userName,
                serverAddress,
                ticketsFilePath);

        if (nonNull(ticket)) {
            ticketValue = ticket.getTicketValue();
        }
        return ticketValue;
    }

    /**
     * Get the first found ticket in the specified ticket file that matches the
     * specified user name and server address. The server address must be
     * non-null and be of the form server:port. The user name may be null and if
     * that is the case the found first ticket for the specified server address
     * will be returned.
     *
     * @param userName        - possibly null user name to match against the found tickets
     * @param serverAddress   - non-null server address
     * @param ticketsFilePath - path to tickets file to search
     * @return - found ticket or null if not found
     * @throws IOException - io exception from reading tickets file
     */
    public static AuthTicket getTicket(
            final String userName,
            final String serverAddress,
            final String ticketsFilePath) throws IOException {

        String p4Port = serverAddress;
        AuthTicket foundTicket = null;
        if (nonNull(serverAddress)) {
            if (serverAddress.indexOf(':') == -1) {
                p4Port = "localhost:" + serverAddress;
            }

            for (AuthTicket ticket : getTickets(ticketsFilePath)) {
                if (p4Port.equals(ticket.getServerAddress())
                        && (isBlank(userName) || userName.equals(ticket.getUserName()))) {

                    foundTicket = ticket;
                    break;
                }
            }
        }
        return foundTicket;
    }

    /**
     * Get all the tickets found in the file at the specified file path.
     *
     * @return - array of tickets found in the specified tickets file at the specified path
     * @throws IOException - io exception from reading tickets file
     */
    public static AuthTicket[] getTickets(final String ticketsFilePath) throws IOException {
        File file = isNotBlank(ticketsFilePath) ? new File(ticketsFilePath) : null;
        return getTickets(file);
    }

    /**
     * Get all the tickets found in the specified file.
     *
     * @return - array of tickets found in the specified tickets file
     * @throws IOException - io exception from reading tickets file
     */
    public static AuthTicket[] getTickets(final File ticketsFile) throws IOException {
        AuthTicket[] tickets = EMPTY;
        List<Map<String, String>> authList = nonNull(ticketsFile) ? getFileEntries(ticketsFile) : getMemoryEntries(ticketsMap);
        if (nonNull(authList)) {
            List<AuthTicket> ticketList = new CopyOnWriteArrayList<>();
            for (Map<String, String> map : authList) {
                if (nonNull(map)) {
                    String serverAddress = map.get(SERVER_ADDRESS_MAP_KEY);
                    String userName = map.get(USER_NAME_MAP_KEY);
                    String ticketValue = map.get(AUTH_VALUE_MAP_KEY);
                    AuthTicket ticket = new AuthTicket(
                            serverAddress,
                            userName,
                            ticketValue);
                    ticketList.add(ticket);
                }
            }
            tickets = ticketList.toArray(new AuthTicket[ticketList.size()]);
        }

        return tickets;
    }

    /**
     * Get the first found ticket in the specified ticket file that matches the
     * specified server address. The server address must be non-null and be of
     * the form server:port.
     *
     * @param serverAddress   - non-null server address
     * @param ticketsFilePath - path to tickets file to search
     * @return - found ticket or null if not found
     * @throws IOException - io exception from reading tickets file
     */
    public static AuthTicket getTicket(
            final String serverAddress,
            final String ticketsFilePath) throws IOException {

        return getTicket(null, serverAddress, ticketsFilePath);
    }

    /**
     * Save the specified ticket as an entry into the specified tickets file.
     * This method will replace the current entry for the user name and server
     * address in the tickets file. If a current entry is not found then the
     * specified entry will be appended to the file.
     *
     * @param ticket          - non-null ticket
     * @param ticketsFilePath - non-null path
     */
    public static void saveTicket(
            final AuthTicket ticket,
            final String ticketsFilePath) throws IOException {

        File file = isNotBlank(ticketsFilePath) ? new File(ticketsFilePath) : null;
        saveTicket(ticket, file);
    }

    /**
     * Save the specified ticket as an entry into the specified tickets file.
     * This method will replace the current entry for the user name and server
     * address in the tickets file. If a current entry is not found then the
     * specified entry will be appended to the file.
     *
     * @param ticket      - non-null ticket
     * @param ticketsFile - non-null file
     */
    public static void saveTicket(
            final AuthTicket ticket,
            final File ticketsFile) throws IOException {

        if (nonNull(ticket)) {
            saveTicket(
                    ticket.getUserName(),
                    ticket.getServerAddress(),
                    ticket.getTicketValue(),
                    ticketsFile);
        }
    }

    /**
     * Save the specified parameters as an entry into the specified tickets
     * file. This method will replace the current entry for the user name and
     * server address in the tickets file. If a current entry is not found then
     * the specified entry will be appended to the file.
     *
     * @param userName        - non-null user name
     * @param serverAddress   - non-null server address
     * @param ticketValue     - non-null ticket value
     * @param ticketsFilePath - non-null file path
     */
    public static void saveTicket(
            final String userName,
            final String serverAddress,
            final String ticketValue,
            final String ticketsFilePath) throws IOException {

        File file = isNotBlank(ticketsFilePath) ? new File(ticketsFilePath) : null;
        saveTicket(userName, serverAddress, ticketValue, file);
    }

    /**
     * Save the specified parameters as an entry into the specified tickets
     * file. This method will replace the current entry for the user name and
     * server address in the tickets file. If a current entry is not found then
     * the specified entry will be appended to the file. If the specified ticket
     * value is null then the current entry in the specified file will be
     * removed if found.
     *
     * @param userName      - non-null user name
     * @param serverAddress - non-null server address
     * @param ticketValue   - possibly null ticket value
     * @param ticketsFile   - non-null file
     */
    public static void saveTicket(
            final String userName,
            final String serverAddress,
            final String ticketValue,
            final File ticketsFile) throws IOException {

        saveTicket(userName, serverAddress, ticketValue, ticketsFile, 0, 0, 0);
    }

    /**
     * Save the specified parameters as an entry into the specified tickets
     * file. This method will replace the current entry for the user name and
     * server address in the tickets file. If a current entry is not found then
     * the specified entry will be appended to the file.
     *
     * @param userName        - non-null user name
     * @param serverAddress   - non-null server address
     * @param ticketValue     - non-null ticket value
     * @param ticketsFilePath - non-null file path
     * @param lockTry         - number of tries for locking
     * @param lockDelay       - delay time (ms) for locking
     * @param lockWait        - wait time (ms) for other process/thread to finish locking
     */
    public static void saveTicket(
            final String userName,
            final String serverAddress,
            final String ticketValue,
            final String ticketsFilePath,
            final int lockTry,
            final long lockDelay,
            final long lockWait) throws IOException {

        File file = isNotBlank(ticketsFilePath) ? new File(ticketsFilePath) : null;
        saveTicket(
                userName,
                serverAddress,
                ticketValue,
                file,
                lockTry,
                lockDelay,
                lockWait);
    }

    /**
     * Save the specified parameters as an entry into the specified tickets
     * file. This method will replace the current entry for the user name and
     * server address in the tickets file. If a current entry is not found then
     * the specified entry will be appended to the file. If the specified ticket
     * value is null then the current entry in the specified file will be
     * removed if found.
     *
     * @param userName      - non-null user name
     * @param serverAddress - non-null server address
     * @param ticketValue   - possibly null ticket value
     * @param ticketsFile   - non-null file
     * @param lockTry       - number of tries for locking
     * @param lockDelay     - delay time (ms) for locking
     * @param lockWait      - wait time (ms) for other process/thread to finish locking
     */
    public static void saveTicket(
            final String userName,
            final String serverAddress,
            final String ticketValue,
            final File ticketsFile,
            final int lockTry,
            final long lockDelay,
            final long lockWait) throws IOException {

        if (nonNull(ticketsFile)) {
            saveFileEntry(
                    userName,
                    serverAddress,
                    ticketValue,
                    ticketsFile,
                    lockTry,
                    lockDelay,
                    lockWait);
        } else {
            saveMemoryEntry(userName, serverAddress, ticketValue, ticketsMap);
        }
    }
}
