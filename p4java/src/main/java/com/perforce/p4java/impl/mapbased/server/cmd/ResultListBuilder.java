/*
 * Copyright (c) 2016, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.isContainsValidRevisionSpecificInformation;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static com.perforce.p4java.core.file.FileSpecOpStatus.ERROR;
import static com.perforce.p4java.core.file.FileSpecOpStatus.INFO;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.ExtendedFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.IServerMessage;

public class ResultListBuilder {
    public static <T> List<T> buildNonNullObjectListFromCommandResultMaps(
            // p4ic4idea: use more precise function generics
            final List<Map<String, Object>> resultMaps, final Function<Map<String, Object>, T> construct) throws P4JavaException {
        List<T> objectList = new ArrayList<>();
        if (resultMaps != null) {
            for (Map<String, Object> map : resultMaps) {
                if (!ResultMapParser.handleErrorStr(map)) {
                    T object = construct.apply(map);
                    objectList.add(object);
                }
            }
        }
        return objectList;
    }

    public static <T> List<T> buildNonNullObjectListFromNonMessageCommandResultMaps(
            // p4ic4idea: use more precise function generics
            final List<Map<String, Object>> resultMaps, final Function<Map<String, Object>, T> construct) throws P4JavaException {
        List<T> objectList = new ArrayList<>();
        if (resultMaps != null) {
            for (Map<String, Object> map : resultMaps) {
                // p4ic4idea: use a server message
                IServerMessage infoMessage = ResultMapParser.getHandledErrorInfo(map);
                if (nonNull(map) && isNull(infoMessage)) {
                    T object = construct.apply(map);
                    objectList.add(object);
                }
            }
        }
        return objectList;
    }

    public static <T> T buildNullableObjectFromNonInfoMessageCommandResultMaps(
            // p4ic4idea: use more precise function generics
            final List<Map<String, Object>> resultMaps, final Function<Map<String, Object>, T> construct) throws RequestException, AccessException {
        T obj = null;
        if (resultMaps != null) {
            for (Map<String, Object> map : resultMaps) {
                // p4ic4idea: use a server message
                IServerMessage message = ResultMapParser.toServerMessage(map);
                ResultMapParser.handleErrors(message);
                if (nonNull(message) && !message.isInfoOrError()) {
                    obj = construct.apply(map);
                }
            }
        }
        return obj;
    }

    public static List<IExtendedFileSpec> buildNonNullExtendedFileSpecListFromCommandResultMaps(
            final List<Map<String, Object>> resultMaps, final IOptionsServer server)
            throws RequestException, AccessException, ConnectionException {
        List<IExtendedFileSpec> specList = new ArrayList<>();
        // p4ic4idea: use server messages
        for (Map<String, Object> map: resultMaps) {
            // We do this by hand for the statFiles case; this may be
            // included in the generic handler later -- HR.
            // Note: as of 10.1 or so, fstats on shelved files may return
            // a "special" fstat info message (usually the last message)
            // that
            // contains only the description field of the associated
            // changelist
            // (see fstat -e documentation for this); therefore we carefully
            // weed
            // out any return map here that has no depot path and a "desc"
            // field
            // -- HR (see also job040680).
            if (nonNull(map)) {
                IServerMessage message = ResultMapParser.toServerMessage(map);
                ResultMapParser.handleFileErrors(message);
                if (nonNull(message) && message.isError()) {
                    specList.add(new ExtendedFileSpec(ERROR, message));
                } else if (nonNull(message) && message.isInfoOrError()) {
                    specList.add(new ExtendedFileSpec(INFO, message));
                } else {
                    if (isContainsValidRevisionSpecificInformation(map)) {
                        specList.add(new ExtendedFileSpec(map, server, -1));
                    }
                }
            }
        }
        return specList;
    }

    public static IFileSpec handleIntegrationFileReturn(
            final Map<String, Object> map,
            final IServer server) throws AccessException, ConnectionException {

        return handleIntegrationFileReturn(map, false, server);
    }

    public static IFileSpec handleIntegrationFileReturn(final Map<String, Object> map,
                                                        final boolean ignoreInfo, final IServer server) throws AccessException, ConnectionException {
        // p4ic4idea: use a server message.
        IServerMessage message = ResultMapParser.toServerMessage(map);
        if (nonNull(message)) {
            ResultMapParser.handleFileErrors(message);
            if (message.isError()) {
                return new FileSpec(FileSpecOpStatus.ERROR, message);
            } else if (message.isInfoOrError()) {
                if (ignoreInfo) {
                    return new FileSpec(map, server, -1);
                } else {
                    return new FileSpec(FileSpecOpStatus.INFO, message);
                }
            } else  {
                return new FileSpec(map, server, -1);
            }
        }
        return null;
    }

    public static IFileSpec handleFileReturn(
            final Map<String, Object> map, final IServer server)
            throws AccessException, ConnectionException {
        // p4ic4idea: use a server message.
        IServerMessage message = ResultMapParser.toServerMessage(map);
        if (nonNull(message)) {
            ResultMapParser.handleFileErrors(message);
            if (message.isError()) {
                return new FileSpec(FileSpecOpStatus.ERROR, message);
            } else if (message.isInfoOrError()) {
                return new FileSpec(FileSpecOpStatus.INFO, message);
            } else {
                return new FileSpec(map, server, -1);
            }
        }
        return null;
    }
}
