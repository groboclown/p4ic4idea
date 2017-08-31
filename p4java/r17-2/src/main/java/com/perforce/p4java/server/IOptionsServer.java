package com.perforce.p4java.server;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.callback.IFilterCallback;
import com.perforce.p4java.server.callback.IParallelCallback;
import com.perforce.p4java.server.callback.IStreamingCallback;

/**
 * An extension of the basic IServer interface to provide Options object-based
 * method access to Perforce server functionality and objects.
 * <p>
 * 
 * Note that unless otherwise noted, individual method options objects can be
 * null; if they're null, the individual method Javadoc will spell out what
 * default options apply (if any) in that case.
 * <p>
 * 
 * Note that in individual method Javadoc comments below, all method "throws"
 * clauses are assumed to throw the normal complement of RequestException,
 * ConnectionException, and AccessException with their usual semantics unless
 * otherwise noted. The three standard P4JavaException classes and the broad
 * causes for their being thrown are:
 * 
 * <pre>
 * ConnectionException if the Perforce server is unreachable or is not
 * 				connected.
 * RequestException if the Perforce server encounters an error during
 * 				its processing of the request.
 * AccessException if the Perforce server denies access to the caller.
 * </pre>
 */

public interface IOptionsServer extends IServer {
    /**
     * Issue an arbitrary P4Java command to the Perforce server and return the
     * results as a list of maps.
     * <p>
     * 
     * This method is intended for low-level commands in the spirit and format
     * of the p4 command line interpreter, and offers a simple way to issue
     * commands to the associated Perforce server without the overhead of the
     * more abstract Java interfaces and methods.
     * <p>
     * 
     * No guidance is given here on the format of the returned map; however, it
     * produces the same output as the p4 command line interpreter in -G (Python
     * map) mode.
     * <p>
     * 
     * Note that this method does not allow you to set "usage" options for the
     * command; these may be added later. Note also that although option
     * arguments passed to this method must be in a form recognized by the p4
     * command line interpreter, that does <i>not</i> mean the method is being
     * implemented by the interpreter -- the actual implementation depends on
     * the options used to get the server object in the first place from the
     * server factory.
     * 
     * @param cmdName
     *            the command to be issued; must be non-null, and correspond to
     *            a Perforce command recognized by P4Java and defined in
     *            CmdSpec.
     * @param cmdArgs
     *            the array of command arguments (options and file arguments,
     *            etc.) to be sent to the Perforce server. These must be in the
     *            form used by the corresponding p4 command line interpreter.
     *            Ignored if null.
     * @param inString
     *            an optional string to be sent to the server as standard input
     *            unchanged (this must be in the format expected by the server,
     *            typically as required when using the "-i" flag to the p4
     *            command line app for the same command). You must remember to
     *            issue the relevant command-specific option to enable this if
     *            needed.
     * @return a non-null Java Map of results; these results are as returned
     *         from issuing the command using the -G option with the p4 command
     *         line interpreter.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters
     * @since 2013.1
     */
    List<Map<String, Object>> execInputStringMapCmdList(String cmdName, String[] cmdArgs,
            String inString) throws P4JavaException;

    /**
     * Issue an arbitrary P4Java command to the Perforce server and return the
     * results as a list of maps.
     * <p>
     * 
     * This method is intended for low-level commands in the spirit and format
     * of the p4 command line interpreter, and offers a simple way to issue
     * commands to the associated Perforce server without the overhead of the
     * more abstract Java interfaces and methods.
     * <p>
     * 
     * No guidance is given here on the format of the returned map; however, it
     * produces the same output as the p4 command line interpreter in -G (Python
     * map) mode.
     * <p>
     * 
     * Note that this method does not allow you to set "usage" options for the
     * command; these may be added later. Note also that although option
     * arguments passed to this method must be in a form recognized by the p4
     * command line interpreter, that does <i>not</i> mean the method is being
     * implemented by the interpreter -- the actual implementation depends on
     * the options used to get the server object in the first place from the
     * server factory.
     * 
     * @param cmdName
     *            the command to be issued; must be non-null, and correspond to
     *            a Perforce command recognized by P4Java and defined in
     *            CmdSpec.
     * @param cmdArgs
     *            the array of command arguments (options and file arguments,
     *            etc.) to be sent to the Perforce server. These must be in the
     *            form used by the corresponding p4 command line interpreter.
     *            Ignored if null.
     * @param inString
     *            an optional string to be sent to the server as standard input
     *            unchanged (this must be in the format expected by the server,
     *            typically as required when using the "-i" flag to the p4
     *            command line app for the same command). You must remember to
     *            issue the relevant command-specific option to enable this if
     *            needed.
     * @param filterCallback
     *            an optional filter callback to decide on skipping or keeping
     *            individual key/value pairs as part of the results map.
     * @return a non-null Java Map of results; these results are as returned
     *         from issuing the command using the -G option with the p4 command
     *         line interpreter.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters
     * @since 2013.1
     */
    List<Map<String, Object>> execInputStringMapCmdList(String cmdName, String[] cmdArgs,
            String inString, IFilterCallback filterCallback) throws P4JavaException;
    

    /**
     * Issue an arbitrary P4Java command to the Perforce server and return the
     * results as a list of maps.
     * <p>
     * 
     * This method is intended for low-level commands in the spirit and format
     * of the p4 command line interpreter, and offers a simple way to issue
     * commands to the associated Perforce server without the overhead of the
     * more abstract Java interfaces and methods.
     * <p>
     * 
     * No guidance is given here on the format of the returned map; however, it
     * produces the same output as the p4 command line interpreter in -G (Python
     * map) mode.
     * <p>
     * 
     * Note that this method does not allow you to set "usage" options for the
     * command; these may be added later. Note also that although option
     * arguments passed to this method must be in a form recognized by the p4
     * command line interpreter, that does <i>not</i> mean the method is being
     * implemented by the interpreter -- the actual implementation depends on
     * the options used to get the server object in the first place from the
     * server factory.
     * 
     * @param cmdName
     *            the command to be issued; must be non-null, and correspond to
     *            a Perforce command recognized by P4Java and defined in
     *            CmdSpec.
     * @param cmdArgs
     *            the array of command arguments (options and file arguments,
     *            etc.) to be sent to the Perforce server. These must be in the
     *            form used by the corresponding p4 command line interpreter.
     *            Ignored if null.
     * @param filterCallback
     *            an optional filter callback to decide on skipping or keeping
     *            individual key/value pairs as part of the results map.
     * @param parallelCallback
     *            an optional parallel sync/submit callback to provide a
     *            multi-threaded file transfer implementation.
     * @return a non-null Java Map of results; these results are as returned
     *         from issuing the command using the -G option with the p4 command
     *         line interpreter.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters
     * @since 2017.1
     */
    List<Map<String, Object>> execMapCmdList(String cmdName, String[] cmdArgs,
            IFilterCallback filterCallback,
            IParallelCallback parallelCallback) throws P4JavaException;;

    /**
     * Issue an arbitrary P4Java command to the Perforce server and return the
     * results as a list of maps without invoking any command callbacks.
     * <p>
     * 
     * Basically equivalent to execMapCmd with temporary disabling of any
     * ICommandCallback calls and / or listeners; this turns out to be useful
     * for various reasons we won't go into here...
     * <p>
     *
     * @param cmdName
     *            the command to be issued; must be non-null, and correspond to
     *            a Perforce command recognized by P4Java and defined in
     *            CmdSpec.
     * @param cmdArgs
     *            the array of command arguments (options and file arguments,
     *            etc.) to be sent to the Perforce server. These must be in the
     *            form used by the corresponding p4 command line interpreter.
     *            Ignored if null.
     * @param inMap
     *            an optional map to be sent to the server as standard input,
     *            using the Python map format (-G) form. You must remember to
     *            issue the relevant command-specific option to enable this if
     *            needed.
     * @return a non-null Java Map of results; these results are as returned
     *         from issuing the command using the -G option with the p4 command
     *         line interpreter.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters
     * @since 2013.1
     */
    List<Map<String, Object>> execQuietMapCmdList(String cmdName, String[] cmdArgs,
            Map<String, Object> inMap) throws P4JavaException;

    /**
     * Issue a streaming map command to the Perforce server, using an optional
     * string for any input expected by the server (such as label or job specs,
     * etc.).
     * <p>
     * 
     * Streaming commands allow users to get each result from a suitably-issued
     * command as it comes in from the server, rather than waiting for the
     * entire command method to complete (and getting the results back as a
     * completed List or Map or whatever).
     * <p>
     * 
     * The results are sent to the user using the IStreamingCallback
     * handleResult method; see the IStreamingCallback Javadoc for details. The
     * payload passed to handleResult is usually the raw map gathered together
     * deep in the RPC protocol layer, and the user is assumed to have the
     * knowledge and technology to be able to parse it and use it suitably in
     * much the same way as a user unpacks or processes the results from the
     * other low-level exec methods like execMapCommand.
     * <p>
     * 
     * NOTE: 'streaming' here has nothing at all to do with Perforce 'streams',
     * which are (or will be) implemented elsewhere.
     * 
     * @param cmdName
     *            the command to be issued; must be non-null, and correspond to
     *            a Perforce command recognized by P4Java and defined in
     *            CmdSpec.
     * @param cmdArgs
     *            the array of command arguments (options and file arguments,
     *            etc.) to be sent to the Perforce server. These must be in the
     *            form used by the corresponding p4 command line interpreter.
     *            Ignored if null.
     * @param inString
     *            an optional string to be sent to the server as standard input
     *            unchanged (this must be in the format expected by the server,
     *            typically as required when using the "-i" flag to the p4
     *            command line app for the same command). You must remember to
     *            issue the relevant command-specific option to enable this if
     *            needed.
     * @param callback
     *            a non-null IStreamingCallback to be used to process the
     *            incoming results.
     * @param key
     *            an opaque integer key that is passed to the IStreamingCallback
     *            callback methods to identify the action as being associated
     *            with this specific call.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2013.1
     */
    void execInputStringStreamingMapCmd(String cmdName, String[] cmdArgs, String inString,
            IStreamingCallback callback, int key) throws P4JavaException;

    /**
     * Get the UsageOptions object associated with this server.
     * 
     * @return possibly-null UsageOptions object.
     */
    UsageOptions getUsageOptions();

    /**
     * Set the UsageOptions object associated with this server. Note that
     * changing this object (or its contents) while a server is busy can cause
     * issues.
     * 
     * @param opts
     *            non-null UsageOptions object to associate with this server.
     * @return the current server.
     */
    IOptionsServer setUsageOptions(UsageOptions opts);

    /**
     * Set the server's Perforce authentication ticket for the specified user to
     * the passed-in string.
     * <p>
     * 
     * @param userName
     *            non-null Perforce user name
     * @param authTicket
     *            possibly-null Perforce authentication ticket
     * @since 2011.2
     */
    void setAuthTicket(String userName, String authTicket);

    /**
     * Return the Perforce Server's authId.
     * 
     * This may be: addr:port or clusterId or authId If the connection hasn't
     * been made yet, this could be null.
     * 
     * @since 2016.1
     * @return possibly-null Perforce authentication id
     */
    String getAuthId();

    /**
     * Set the Perforce authentication tickets file path.
     * 
     * @param ticketsFilePath
     *            non-null Perforce auth tickets file path
     * @since 2013.1
     */
    void setTicketsFilePath(String ticketsFilePath);

    /**
     * Return the Perforce authentication tickets file path.
     * 
     * @return possibly-null Perforce auth tickets file path
     * @since 2013.1
     */
    String getTicketsFilePath();

    /**
     * Set the Perforce trust file path.
     * 
     * @param trustFilePath
     *            non-null Perforce trust file path
     * @since 2013.1
     */
    void setTrustFilePath(String trustFilePath);

    /**
     * Return the Perforce trust file path.
     * 
     * @return possibly-null Perforce trust file path
     * @since 2013.1
     */
    String getTrustFilePath();

    /**
     * Issue an arbitrary P4Java command to the Perforce server and get the
     * results as a stream.
     * <p>
     * 
     * This method is intended for low-level commands in the spirit and format
     * of the p4 command line interpreter, and offers a simple way to issue
     * commands to the associated Perforce server without the overhead of the
     * more abstract Java interfaces and methods.
     * <p>
     * 
     * Note that this method is intended for things like getting file contents,
     * and may have unpredictable results on commands not originally expected to
     * return i/o streams.
     * <p>
     * 
     * Note that this method does not allow you to set "usage" options for the
     * command; these may be added later. Note also that although option
     * arguments passed to this method must be in a form recognized by P4Java
     * (as defined by the CmdSpec enum), that does <i>not</i> mean the method is
     * being implemented by the interpreter -- the actual implementation depends
     * on the options used to get the server object in the first place from the
     * server factory.
     * 
     * @param cmdName
     *            the command to be issued; must be non-null, and correspond to
     *            a Perforce command recognized by P4Java and defined in
     *            CmdSpec.
     * @param cmdArgs
     *            the array of command arguments (options and file arguments,
     *            etc.) to be sent to the Perforce server. These must be in the
     *            form used by the corresponding p4 command line interpreter.
     *            Ignored if null.
     * @param inMap
     *            an optional map to be sent to the server as standard input,
     *            using the Python map format (-G) form. You must remember to
     *            issue the relevant command-specific option to enable this if
     *            needed.
     * @return an InputStream on the command output. This will never be null,
     *         but it may be empty. You <i>must</i> properly close this stream
     *         after use or temporary files may be left lying around the VM's
     *         java.io.tmpdir area.
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     * @since 2013.1
     */
    InputStream execStreamCmd(String cmdName, String[] cmdArgs, Map<String, Object> inMap)
            throws P4JavaException;

    /**
     * Issue an arbitrary P4Java command to the Perforce server and return the
     * results as a stream.
     * <p>
     * 
     * This method is intended for low-level commands in the spirit and format
     * of the p4 command line interpreter, and offers a simple way to issue
     * commands to the associated Perforce server without the overhead of the
     * more abstract Java interfaces and methods.
     * <p>
     * 
     * Note that this method is intended for things like getting file contents,
     * and may have unpredictable results on commands not originally expected to
     * return i/o streams.
     * <p>
     * 
     * Note that this method does not allow you to set "usage" options for the
     * command; these may be added later. Note also that although option
     * arguments passed to this method must be in a form recognized by the p4
     * command line interpreter, that does <i>not</i> mean the method is being
     * implemented by the interpreter -- the actual implementation depends on
     * the options used to get the server object in the first place from the
     * server factory.
     * 
     * @param cmdName
     *            the command to be issued; must be non-null, and correspond to
     *            a Perforce command recognized by P4Java and defined in
     *            CmdSpec.
     * @param cmdArgs
     *            the array of command arguments (options and file arguments,
     *            etc.) to be sent to the Perforce server. These must be in the
     *            form used by the corresponding p4 command line interpreter.
     *            Ignored if null.
     * @param inString
     *            an optional string to be sent to the server as standard input
     *            unchanged (this must be in the format expected by the server,
     *            typically as required when using the "-i" flag to the p4
     *            command line app for the same command). You must remember to
     *            issue the relevant command-specific option to enable this if
     *            needed.
     * @return an InputStream on the command output. This will never be null,
     *         but it may be empty. You <i>must</i> properly close this stream
     *         after use or temporary files may be left lying around the VM's
     *         java.io.tmpdir area.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters
     * @since 2013.1
     */

    InputStream execInputStringStreamCmd(String cmdName, String[] cmdArgs, String inString)
            throws P4JavaException;

    /**
     * Return the fingerprint for the Perforce SSL connection.
     * <p>
     * 
     * Note that this fingerprint is generated from the connection, it may not
     * be the same as the one (if any) stored in the trust file.
     * 
     * @return possibly-null fingerprint for the Perforce SSL connection.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2012.1
     */
    String getTrust() throws P4JavaException;

    /**
     * Approve and add the fingerprint for the Perforce SSL connection. The
     * fingerprint or replacement will be stored in the trust file. If the
     * attribute TrustOptions.isReplacement() is true, then the replacement
     * fingerprint will be stored. Otherwise, the normal fingerprint is stored.
     * <p>
     * 
     * Note that an exception would be thrown if there is an identity change
     * detected. If you want to trust the new key use the 'force' option.
     * 
     * @param opts
     *            TrustOptions object describing optional parameters; if null,
     *            no options are set.
     * @return non-null result message string from the trust operation; this may
     *         include the fingerprint for the Perforce server public key.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2012.1
     */
    String addTrust(TrustOptions opts) throws P4JavaException;

    /**
     * Approve and add the specified fingerprint for the Perforce SSL
     * connection. The fingerprint will be stored in the trust file.
     * 
     * @param fingerprintValue
     *            non-null fingerprint value to be added.
     * @return non-null result message string from the trust operation; this may
     *         include the fingerprint for the Perforce server public key.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2012.1
     */
    String addTrust(String fingerprintValue) throws P4JavaException;

    /**
     * Approve and add the specified fingerprint or replacement for the Perforce
     * SSL connection. The fingerprint or replacement will be stored in the
     * trust file. If the attribute TrustOptions.isReplacement() is true, then
     * the replacement fingerprint will be stored. Otherwise, the normal
     * fingerprint is stored.
     * 
     * @param fingerprintValue
     *            non-null fingerprint value to be added.
     * @param opts
     *            TrustOptions object describing optional parameters; if null,
     *            no options are set.
     * @return non-null result message string from the trust operation; this may
     *         include the fingerprint for the Perforce server public key.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2015.1
     */
    String addTrust(String fingerprintValue, TrustOptions opts) throws P4JavaException;

    /**
     * Remove the fingerprint for the Perforce SSL connection. The fingerprint
     * will removed from the trust file.
     * 
     * @return non-null result message string from the trust operation; this may
     *         include the fingerprint for the Perforce server public key.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2012.1
     */
    String removeTrust() throws P4JavaException;

    /**
     * Remove the fingerprint or replacement for the Perforce SSL connection.
     * The fingerprint or replacement will removed from the trust file. If the
     * attribute TrustOptions.isReplacement() is true, then the replacement
     * fingerprint will be removed. Otherwise the normal fingerprint is removed.
     * 
     * @param opts
     *            TrustOptions object describing optional parameters; if null,
     *            no options are set.
     * @return non-null result message string from the trust operation; this may
     *         include the fingerprint for the Perforce server public key.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2015.1
     */
    String removeTrust(TrustOptions opts) throws P4JavaException;

    /**
     * List all fingerprints in the trust file.
     * 
     * @return non-null list of known fingerprints in the trust file.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2012.1
     */
    List<Fingerprint> getTrusts() throws P4JavaException;

    /**
     * List all fingerprints or replacements in the trust file. If the attribute
     * TrustOptions.isReplacement() is true, then replacement fingerprints will
     * be returned. Otherwise, normal fingerprints are returned.
     * 
     * @param opts
     *            TrustOptions object describing optional parameters; if null,
     *            no options are set.
     * @return non-null list of fingerprints in the trust file.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2015.1
     */
    List<Fingerprint> getTrusts(TrustOptions opts) throws P4JavaException;

}
