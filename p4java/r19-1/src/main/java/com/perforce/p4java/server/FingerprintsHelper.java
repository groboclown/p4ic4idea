package com.perforce.p4java.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is designed to lookup fingerprints from a trust file or the
 * in-memory fingerprints map. If a null trust file parameter is passed to the
 * methods, the in-memory fingerprints map will be used instead.
 * 
 */
public class FingerprintsHelper extends AbstractAuthHelper {

    private static final Fingerprint[] EMPTY = new Fingerprint[0];

    private static Map<String, String> fingerprintsMap = new ConcurrentHashMap<>();

    /**
     * Get the first found fingerprint value that matches the specified user
     * name and server address. The user name be non-null and the server address
     * must be non-null and be of the form server:port.
     * 
     * @param userName
     * @param serverAddress
     * @param trustFilePath
     * @return - fingerprint value or null if not found
     * @throws IOException
     *             - io exception from reading trust file
     */
    public static String getFingerprintValue(String userName, String serverAddress,
            String trustFilePath) throws IOException {
        String fingerprintValue = null;
        Fingerprint fingerprint = getFingerprint(userName, serverAddress, trustFilePath);
        if (fingerprint != null) {
            fingerprintValue = fingerprint.getFingerprintValue();
        }
        return fingerprintValue;
    }

    /**
     * Get all the fingerprints found in the file at the specified file path.
     * 
     * @param trustFilePath
     * @return - array of fingerprints found in the specified trust file at the
     *         specified path
     * @throws IOException
     *             - io exception from reading trust file
     */
    public static Fingerprint[] getFingerprints(String trustFilePath) throws IOException {
        File file = trustFilePath != null ? new File(trustFilePath) : null;
        Fingerprint[] fingerprints = getFingerprints(file);
        return fingerprints;
    }

    /**
     * Get all the fingerprints found in the specified file.
     * 
     * @param trustFile
     * @return - array of fingerprints found in the specified trust file
     * @throws IOException
     *             - io exception from reading trust file
     */
    public static Fingerprint[] getFingerprints(File trustFile) throws IOException {
        Fingerprint[] fingerprints = EMPTY;
        List<Map<String, String>> authList = trustFile != null ? getFileEntries(trustFile)
                : getMemoryEntries(fingerprintsMap);
        if (authList != null) {
            List<Fingerprint> fingerprintList = new ArrayList<Fingerprint>();
            for (Map<String, String> map : authList) {
                if (map != null) {
                    String serverAddress = map.get(SERVER_ADDRESS_MAP_KEY);
                    String userName = map.get(USER_NAME_MAP_KEY);
                    String fingerprintValue = map.get(AUTH_VALUE_MAP_KEY);
                    Fingerprint fingerprint = new Fingerprint(serverAddress, userName,
                            fingerprintValue);
                    fingerprintList.add(fingerprint);
                }
            }
            fingerprints = fingerprintList.toArray(new Fingerprint[0]);
        }
        return fingerprints;
    }

    /**
     * Get the first found fingerprint in the specified trust file that matches
     * the specified user name and server address. The server address must be
     * non-null and be of the form server:port. The user name may be null and if
     * that is the case the found first fingerprint for the specified server
     * address will be returned.
     * 
     * @param userName
     *            - possibly null user name to match against the found
     *            fingerprints
     * @param serverAddress
     *            - non-null server address
     * @param trustFilePath
     *            - path to trust file to search
     * @return - found fingerprint or null if not found
     * @throws IOException
     *             - io exception from reading trust file
     */
    public static Fingerprint getFingerprint(String userName, String serverAddress,
            String trustFilePath) throws IOException {
        Fingerprint foundFingerprint = null;
        if (serverAddress != null) {
            if (serverAddress.indexOf(':') == -1) {
                serverAddress = "localhost:" + serverAddress;
            }
            for (Fingerprint fingerprint : getFingerprints(trustFilePath)) {
                if (serverAddress.equals(fingerprint.getServerAddress())
                        && (userName == null || userName.equals(fingerprint.getUserName()))) {
                    foundFingerprint = fingerprint;
                    break;
                }
            }
        }
        return foundFingerprint;
    }

    /**
     * Get the first found fingerprint in the specified fingerprint file that
     * matches the specified server address. The server address must be non-null
     * and be of the form server:port.
     * 
     * @param serverAddress
     *            - non-null server address
     * @param trustFilePath
     *            - path to trust file to search
     * @return - found fingerprint or null if not found
     * @throws IOException
     *             - io exception from reading trust file
     */
    public static Fingerprint getFingerprint(String serverAddress, String trustFilePath)
            throws IOException {
        return getFingerprint(null, serverAddress, trustFilePath);
    }

    /**
     * Save the specified fingerprint as an entry into the specified trust file.
     * This method will replace the current entry for the user name and server
     * address in the trust file. If a current entry is not found then the
     * specified entry will be appended to the file.
     * 
     * @param fingerprint
     *            - non-null fingerprint
     * @param trustFilePath
     *            - non-null path
     * @throws IOException
     */
    public static void saveFingerprint(Fingerprint fingerprint, String trustFilePath)
            throws IOException {
        File file = trustFilePath != null ? new File(trustFilePath) : null;
        saveFingerprint(fingerprint, file);
    }

    /**
     * Save the specified fingerprint as an entry into the specified trust file.
     * This method will replace the current entry for the user name and server
     * address in the trust file. If a current entry is not found then the
     * specified entry will be appended to the file.
     * 
     * @param fingerprint
     *            - non-null fingerprint
     * @param trustFile
     *            - non-null file
     * @throws IOException
     */
    public static void saveFingerprint(Fingerprint fingerprint, File trustFile) throws IOException {
        if (fingerprint != null) {
            saveFingerprint(fingerprint.getUserName(), fingerprint.getServerAddress(),
                    fingerprint.getFingerprintValue(), trustFile);
        }
    }

    /**
     * Save the specified parameters as an entry into the specified trust file.
     * This method will replace the current entry for the user name and server
     * address in the trust file. If a current entry is not found then the
     * specified entry will be appended to the file.
     * 
     * @param userName
     *            - non-null user name
     * @param serverAddress
     *            - non-null server address
     * @param fingerprintValue
     *            - non-null fingerprint value
     * @param trustFilePath
     *            - non-null file path
     * @throws IOException
     */
    public static void saveFingerprint(String userName, String serverAddress,
            String fingerprintValue, String trustFilePath) throws IOException {
        File file = trustFilePath != null ? new File(trustFilePath) : null;
        saveFingerprint(userName, serverAddress, fingerprintValue, file);
    }

    /**
     * Save the specified parameters as an entry into the specified trust file.
     * This method will replace the current entry for the user name and server
     * address in the trust file. If a current entry is not found then the
     * specified entry will be appended to the file.
     * 
     * @param userName
     *            - non-null user name
     * @param serverAddress
     *            - non-null server address
     * @param fingerprintValue
     *            - non-null fingerprint value
     * @param trustFilePath
     *            - non-null file path
     * @param lockTry
     *            - number of tries for locking
     * @param lockDelay
     *            - delay time (ms) for locking
     * @param lockWait
     *            - wait time (ms) for other process/thread to finish locking
     * @throws IOException
     */
    public static void saveFingerprint(String userName, String serverAddress,
            String fingerprintValue, String trustFilePath, int lockTry, long lockDelay,
            long lockWait) throws IOException {
        File file = trustFilePath != null ? new File(trustFilePath) : null;
        saveFingerprint(userName, serverAddress, fingerprintValue, file, lockTry, lockDelay,
                lockWait);
    }

    /**
     * Save the specified parameters as an entry into the specified trust file.
     * This method will replace the current entry for the user name and server
     * address in the trust file. If a current entry is not found then the
     * specified entry will be appended to the file. If the specified
     * fingerprint value is null then the current entry in the specified file
     * will be removed if found.
     * 
     * @param userName
     *            - non-null user name
     * @param serverAddress
     *            - non-null server address
     * @param fingerprintValue
     *            - possibly null fingerprint value
     * @param trustFile
     *            - non-null file
     * @throws IOException
     */
    public static void saveFingerprint(String userName, String serverAddress,
            String fingerprintValue, File trustFile) throws IOException {
        saveFingerprint(userName, serverAddress, fingerprintValue, trustFile, 0, 0, 0);
    }

    /**
     * Save the specified parameters as an entry into the specified trust file.
     * This method will replace the current entry for the user name and server
     * address in the trust file. If a current entry is not found then the
     * specified entry will be appended to the file. If the specified
     * fingerprint value is null then the current entry in the specified file
     * will be removed if found.
     * 
     * @param userName
     *            - non-null user name
     * @param serverAddress
     *            - non-null server address
     * @param fingerprintValue
     *            - possibly null fingerprint value
     * @param trustFile
     *            - non-null file
     * @param lockTry
     *            - number of tries for locking
     * @param lockDelay
     *            - delay time (ms) for locking
     * @param lockWait
     *            - wait time (ms) for other process/thread to finish locking
     * @throws IOException
     */
    public static void saveFingerprint(String userName, String serverAddress,
            String fingerprintValue, File trustFile, int lockTry, long lockDelay, long lockWait)
            throws IOException {
        if (trustFile != null) {
            saveFileEntry(userName, serverAddress, fingerprintValue, trustFile, lockTry, lockDelay,
                    lockWait);
        } else {
            saveMemoryEntry(userName, serverAddress, fingerprintValue, fingerprintsMap);
        }
    }
}
