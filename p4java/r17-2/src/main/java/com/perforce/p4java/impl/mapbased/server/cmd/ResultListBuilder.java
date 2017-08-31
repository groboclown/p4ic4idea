/*
 * Copyright (c) 2016, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java.impl.mapbased.server.cmd;

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

public class ResultListBuilder {
    public static <T> List<T> buildNonNullObjectListFromCommandResultMaps(
            final List<Map<String, Object>> resultMaps, final Function<Map, T> construct) throws P4JavaException {
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
            final List<Map<String, Object>> resultMaps, final Function<Map, T> construct) throws P4JavaException {
        List<T> objectList = new ArrayList<>();
        if (resultMaps != null) {
            for (Map<String, Object> map : resultMaps) {
                if (!ResultMapParser.handleErrorStr(map) && isBlank(ResultMapParser.getErrorOrInfoStr(map))) {
                    T object = construct.apply(map);
                    objectList.add(object);
                }
            }
        }
        return objectList;
    }

    public static <T> T buildNullableObjectFromNonInfoMessageCommandResultMaps(
            final List<Map<String, Object>> resultMaps, final Function<Map, T> construct) throws RequestException, AccessException {
        T obj = null;
        if (resultMaps != null) {
            for (Map<String, Object> map : resultMaps) {
                if (map != null) {
                    if (!ResultMapParser.handleErrorStr(map) && !ResultMapParser.isInfoMessage(map)) {
                        obj = construct.apply(map);
                    }
                }
            }
        }
        return obj;
    }

    public static List<IExtendedFileSpec> buildNonNullExtendedFileSpecListFromCommandResultMaps(
            final List<Map<String, Object>> resultMaps, final IOptionsServer server)
            throws RequestException, AccessException, ConnectionException {
        List<IExtendedFileSpec> specList = new ArrayList<>();
        if (resultMaps != null) {
            for (Map<String, Object> map : resultMaps) {
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
                String errStr = ResultMapParser.handleFileErrorStr(map);
                if (isBlank(errStr)) {
                    if (isContainsValidRevisionSpecificInformation(map)) {
                        specList.add(new ExtendedFileSpec(map, server, -1));
                    }
                } else {
                    FileSpecOpStatus fileSpecOpStatus = ERROR;
                    if (ResultMapParser.isInfoMessage(map)) {
                        fileSpecOpStatus = INFO;
                    }
                    specList.add(new ExtendedFileSpec(fileSpecOpStatus, errStr));
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

        if (nonNull(map)) {
            String errStr = ResultMapParser.handleFileErrorStr(map);
            if (isBlank(errStr)) {
                return new FileSpec(map, server, -1);
            } else {
                String codeStr = parseCode0ErrorString(map);
                if (ResultMapParser.isInfoMessage(map)) {
                    if (ignoreInfo) {
                        return new FileSpec(map, server, -1);
                    } else {
                        return new FileSpec(FileSpecOpStatus.INFO, errStr, codeStr);
                    }
                } else {
                    return new FileSpec(FileSpecOpStatus.ERROR, errStr, codeStr);
                }
            }
        }
        return null;
    }

    public static IFileSpec handleFileReturn(
            final Map<String, Object> map, final IServer server)
            throws AccessException, ConnectionException {

        if (nonNull(map)) {
            String errStr = ResultMapParser.handleFileErrorStr(map);
            if (isBlank(errStr)) {
                return new FileSpec(map, server, -1);
            } else {
                FileSpecOpStatus specOpStatus = FileSpecOpStatus.ERROR;
                if (ResultMapParser.isInfoMessage(map)) {
                    specOpStatus = FileSpecOpStatus.INFO;
                }

                String codeStr = parseCode0ErrorString(map);
                return new FileSpec(specOpStatus, errStr, codeStr);
            }
        }
        return null;
    }
}
