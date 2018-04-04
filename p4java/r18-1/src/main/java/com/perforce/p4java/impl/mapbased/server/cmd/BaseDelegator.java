/*
 * Copyright (c) 2016, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.P4JavaExceptions.throwRequestExceptionIfPerforceServerVersionOldThanExpected;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IOptionsServer;
import org.apache.commons.lang3.Validate;

/**
 *
 */
public abstract class BaseDelegator {
    protected static int MAX_LIMIT_SUPPORTED_MIN_VERSION = 20061;
    protected static int USER_RESTRICTIONS_SUPPORTED_MIN_VERSION = 20062;
    protected static int QUERY_EXPRESSIONS_SUPPORTED_MIN_VERSION = 20082;

    /** The server object */

    final IOptionsServer server;

    /**
     * 
     * Basic constructor, taking a server object.
     * 
     * @param server
     *            - an instance of the currently effective server implementaion
     * 
     */

    BaseDelegator(IOptionsServer server) {
        this.server = server;
    }

    /**
     * 
     * Utility method for constructing an IFileSpec object from a key value pair
     * map, a client
     * 
     * and the current server.
     * 
     * @param map
     *            The result map from an execcmd operation
     * 
     * @param client
     * 
     * @return An IFileSpec object
     * 
     * @throws AccessException
     *             Insufficient privileges for the server
     * 
     * @throws ConnectionException
     *             Cannot connect to the server
     * 
     */

    IFileSpec handleFileReturn(final Map<String, Object> map, final IClient client)

            throws AccessException, ConnectionException {

        if (map != null) {

            String errStr = server.handleFileErrorStr(map);

            if (isBlank(errStr)) {

                return new FileSpec(map, server, -1);

            } else {

                FileSpecOpStatus specOpStatus = FileSpecOpStatus.ERROR;

                if (server.isInfoMessage(map)) {

                    specOpStatus = FileSpecOpStatus.INFO;

                }

                String codeStr = parseCode0ErrorString(map);

                return new FileSpec(specOpStatus, errStr, codeStr);

            }

        }

        return null;

    }

    /**
     * 
     * Run the given command against the real server method.
     * 
     * @param cmdSpec
     *            The command being run; e.g. VERIFY
     * 
     * @param cmdArgs
     *            The parameters transformed into an array of arguments
     * 
     * @param inMap
     *            Optional, used to provide input data to the server command;
     *            e.g. spec data
     * 
     * @return A list of result map objects
     * 
     * @throws ConnectionException
     *             When there is a problem connecting to the server
     * 
     * @throws AccessException
     *             When privileges are insufficient to run the command
     * 
     */
    List<Map<String, Object>> execMapCmdList(
            @Nonnull final CmdSpec cmdSpec,
            String[] cmdArgs,
            Map<String, Object> inMap)
            throws ConnectionException, AccessException, RequestException {

        Validate.notNull(cmdSpec);
        return server.execMapCmdList(cmdSpec.toString(), cmdArgs, inMap);
    }

    /**
     * 
     * Run the given streaming command against the real server method.
     * 
     * @param cmdSpec
     *            The command being run; e.g. VERIFY
     * 
     * @param cmdArgs
     *            The parameters transformed into an array of arguments
     * 
     * @return the stream returned by the server command
     * 
     * @throws ConnectionException
     *             When there is a problem connecting to the server
     * 
     * @throws AccessException
     *             When privileges are insufficient to run the command
     * 
     */
    InputStream execStreamCmd(final CmdSpec cmdSpec, String[] cmdArgs)
            throws ConnectionException, RequestException, AccessException {
        Validate.notNull(cmdSpec);
        return server.execStreamCmd(cmdSpec.toString(), cmdArgs);
    }

    void checkMinSupportedPerforceVersion(final String userName, final int maxLimit,
                                          final String queryString, final String clientOrLabel)
            throws RequestException, ConnectionException {

        int serverVersion = server.getServerVersion();

        if (isNotBlank(userName)) {
            throwRequestExceptionIfPerforceServerVersionOldThanExpected(
                    serverVersion >= USER_RESTRICTIONS_SUPPORTED_MIN_VERSION,
                    "user restrictions '%s' for %s lists are not supported by this version of the Perforce server",
                    userName, clientOrLabel);
        }

        if (maxLimit > 0) {
            throwRequestExceptionIfPerforceServerVersionOldThanExpected(
                    serverVersion >= MAX_LIMIT_SUPPORTED_MIN_VERSION,
                    "max limit '%s' for %s lists are not supported by this version of the Perforce server",
                    maxLimit, clientOrLabel);
        }

        if (isNotBlank(queryString)) {
            throwRequestExceptionIfPerforceServerVersionOldThanExpected(
                    serverVersion >= QUERY_EXPRESSIONS_SUPPORTED_MIN_VERSION,
                    "query expressions '%s' for %s lists are not supported by this version of the Perforce server",
                    queryString, clientOrLabel);
        }
    }
}