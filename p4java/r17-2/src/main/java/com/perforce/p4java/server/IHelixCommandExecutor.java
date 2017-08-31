package com.perforce.p4java.server;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.callback.IFilterCallback;

public interface IHelixCommandExecutor {

    List<Map<String, Object>> execMapCmdList(
            @Nonnull CmdSpec cmdSpec,
            String[] cmdArgs,
            Map<String, Object> inMap) throws ConnectionException, AccessException;

    /**
     * Issue an arbitrary P4Java command to the Perforce server and return the
     * results as a list of maps.
     * <p>
     * <p>
     * This method is intended for low-level commands in the spirit and format
     * of the p4 command line interpreter, and offers a simple way to issue
     * commands to the associated Perforce server without the overhead of the
     * more abstract Java interfaces and methods.
     * <p>
     * <p>
     * No guidance is given here on the format of the returned map; however, it
     * produces the same output as the p4 command line interpreter in -G (Python
     * map) mode.
     * <p>
     * <p>
     * Note that this method does not allow you to set "usage" options for the
     * command; these may be added later. Note also that although option
     * arguments passed to this method must be in a form recognized by the p4
     * command line interpreter, that does <i>not</i> mean the method is being
     * implemented by the interpreter -- the actual implementation depends on
     * the options used to get the server object in the first place from the
     * server factory.
     *
     * @param cmdName the command to be issued; must be non-null, and correspond to
     *                a Perforce command recognized by P4Java and defined in
     *                CmdSpec.
     * @param cmdArgs the array of command arguments (options and file arguments,
     *                etc.) to be sent to the Perforce server. These must be in the
     *                form used by the corresponding p4 command line interpreter.
     *                Ignored if null.
     * @param inMap   an optional map to be sent to the server as standard input,
     *                using the Python map format (-G) form. You must remember to
     *                issue the relevant command-specific option to enable this if
     *                needed.
     * @return a non-null Java List of results; these results are as returned
     * from issuing the command using the -G option with the p4 command
     * line interpreter.
     * @throws ConnectionException
     *             if the Perforce server is unreachable or is not connected.
     * @throws RequestException
     *             if the Perforce server encounters an error during its
     *             processing of the request
     * @throws AccessException
     *             if the Perforce server denies access to the caller
     * @since 2013.1
     */
    List<Map<String, Object>> execMapCmdList(
            String cmdName,
            String[] cmdArgs,
            Map<String, Object> inMap)
            throws ConnectionException, AccessException, RequestException;

    /**
     * Issue an arbitrary P4Java command to the Perforce server and return the
     * results as a list of maps.
     * <p>
     * <p>
     * This method is intended for low-level commands in the spirit and format
     * of the p4 command line interpreter, and offers a simple way to issue
     * commands to the associated Perforce server without the overhead of the
     * more abstract Java interfaces and methods.
     * <p>
     * <p>
     * No guidance is given here on the format of the returned map; however, it
     * produces the same output as the p4 command line interpreter in -G (Python
     * map) mode.
     * <p>
     * <p>
     * Note that this method does not allow you to set "usage" options for the
     * command; these may be added later. Note also that although option
     * arguments passed to this method must be in a form recognized by the p4
     * command line interpreter, that does <i>not</i> mean the method is being
     * implemented by the interpreter -- the actual implementation depends on
     * the options used to get the server object in the first place from the
     * server factory.
     *
     * @param cmdName        the command to be issued; must be non-null, and correspond to
     *                       a Perforce command recognized by P4Java and defined in
     *                       CmdSpec.
     * @param cmdArgs        the array of command arguments (options and file arguments,
     *                       etc.) to be sent to the Perforce server. These must be in the
     *                       form used by the corresponding p4 command line interpreter.
     *                       Ignored if null.
     * @param inMap          an optional map to be sent to the server as standard input,
     *                       using the Python map format (-G) form. You must remember to
     *                       issue the relevant command-specific option to enable this if
     *                       needed.
     * @param filterCallback an optional filter callback to decide on skipping or keeping
     *                       individual key/value pairs as part of the results map.
     * @return a non-null Java List of results; these results are as returned
     * from issuing the command using the -G option with the p4 command
     * line interpreter.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2013.1
     */
    List<Map<String, Object>> execMapCmdList(
            String cmdName,
            String[] cmdArgs,
            Map<String, Object> inMap,
            IFilterCallback filterCallback) throws P4JavaException;

    /**
     * Issue an arbitrary P4Java command to the Perforce server and get the
     * results as a stream.
     * <p>
     * <p>
     * This method is intended for low-level commands in the spirit and format
     * of the p4 command line interpreter, and offers a simple way to issue
     * commands to the associated Perforce server without the overhead of the
     * more abstract Java interfaces and methods.
     * <p>
     * <p>
     * Note that this method is intended for things like getting file contents,
     * and may have unpredictable results on commands not originally expected to
     * return i/o streams.
     * <p>
     * <p>
     * Note that this method does not allow you to set "usage" options for the
     * command; these may be added later. Note also that although option
     * arguments passed to this method must be in a form recognized by P4Java
     * (as defined by the CmdSpec enum), that does <i>not</i> mean the method is
     * being implemented by the interpreter -- the actual implementation depends
     * on the options used to get the server object in the first place from the
     * server factory.
     *
     * @param cmdName the command to be issued; must be non-null, and correspond to
     *                a Perforce command recognized by P4Java and defined in
     *                CmdSpec.
     * @param cmdArgs the array of command arguments (options and file arguments,
     *                etc.) to be sent to the Perforce server. These must be in the
     *                form used by the corresponding p4 command line interpreter.
     *                Ignored if null.
     * @return an InputStream on the command output. This will never be null,
     * but it may be empty. You <i>must</i> properly close this stream
     * after use or temporary files may be left lying around the VM's
     * java.io.tmpdir area.
     * @throws ConnectionException if the Perforce server is unreachable or is not connected.
     * @throws RequestException    if the Perforce server encounters an error during its
     *                             processing of the request
     * @throws AccessException     if the Perforce server denies access to the caller
     */

    InputStream execStreamCmd(String cmdName, String[] cmdArgs)
            throws ConnectionException, RequestException, AccessException;

    /**
     * @deprecated use {@link com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser#handleFileErrorStr(Map)}
     */
    @Deprecated
    String handleFileErrorStr(Map<String, Object> map)
            throws ConnectionException, AccessException;

    /**
     * <p>
     * Gets the info/warning/error/fatal message from the passed-in Perforce
     * command results map. If no info/warning/error/fatal message found in the
     * results map it returns null.
     * </p>
     * <p>
     * Note that the minimum severity code is MessageSeverityCode.E_INFO.
     * Therefore, only message with severity code >= MessageSeverityCode.E_INFO
     * will be returned.
     * <p>
     * <p>
     * RPC impl errors come across the wire as a map in the form usually like
     * this:
     * <p>
     * <pre>
     * fmt0=Access for user '%user%' has not been enabled by 'p4 protect'.,
     * func=client-Message, user=nouser, code0=822483067
     * </pre>
     * <p>
     * Note that the code0 entry will be used to get the severity level; the
     * fmt0 entry contains the message.
     * <p>
     *
     * @param map Perforce command results map
     * @return possibly-null info/warning/error/fatal string
     * @since 2011.2
     * @deprecated use {@link com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser#getErrorOrInfoStr(Map)}
     */
    @Deprecated
    String getErrorOrInfoStr(Map<String, Object> map);

    /**
     * @deprecated use {@link com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser#handleErrorStr(Map)}
     */
    @Deprecated
    boolean handleErrorStr(Map<String, Object> map)
            throws RequestException, AccessException;

    /**
     * <p>
     * Gets the error/fatal message from the passed-in Perforce command results
     * map. If no error/fatal message found in the results map it returns null.
     * </p>
     * <p>
     * Note that the minimum severity code is MessageSeverityCode.E_FAILED.
     * Therefore, only message with severity code >=
     * MessageSeverityCode.E_FAILED will be returned.
     * <p>
     * <p>
     * RPC impl errors come across the wire as a map in the form usually like
     * this:
     * <p>
     * <pre>
     * fmt0=Access for user '%user%' has not been enabled by 'p4 protect'.,
     * func=client-Message, user=nouser, code0=822483067
     * </pre>
     * <p>
     * Note that the code0 entry will be used to get the severity level; the
     * fmt0 entry contains the message.
     * <p>
     *
     * @param map Perforce command results map
     * @return possibly-null error/fatal string
     * @since 2011.2
     * @deprecated use {@link com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser#getErrorStr(Map)}
     */
    @Deprecated
    String getErrorStr(Map<String, Object> map);

    /**
     * @deprecated use {@link com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser#isAuthFail(String)}
     */
    @Deprecated
    boolean isAuthFail(String errStr);

    /**
     * <p>
     * Gets the info message from the passed-in Perforce command results map. If
     * no info message found in the results map it returns null.
     * </p>
     * <p>
     * Note that the severity code is MessageSeverityCode.E_INFO. Therefore,
     * only message with severity code = MessageSeverityCode.E_INFO will be
     * returned.
     * <p>
     * <p>
     * RPC impl errors come across the wire as a map in the form usually like
     * this:
     * <p>
     * <pre>
     * fmt0=Access for user '%user%' has not been enabled by 'p4 protect'.,
     * func=client-Message, user=nouser, code0=822483067
     * </pre>
     * <p>
     * Note that the code0 entry will be used to get the severity level; the
     * fmt0 entry contains the message.
     * <p>
     *
     * @param map Perforce command results map
     * @return possibly-null info string
     * @since 2011.2
     * @deprecated use {@link com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser#getInfoStr(Map)}
     */
    @Deprecated
    String getInfoStr(Map<String, Object> map);

    /**
     * Checks if is info message.
     *
     * @param map the map
     * @return true, if is info message
     * @deprecated use {@link com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser#isInfoMessage(Map)}
     */
    @Deprecated
    boolean isInfoMessage(Map<String, Object> map);
}
