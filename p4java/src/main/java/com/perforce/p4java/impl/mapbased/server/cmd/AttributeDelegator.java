package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.rethrowFunction;
import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static com.perforce.p4java.impl.generic.core.file.FilePath.PathType.DEPOT;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.ATTRIBUTE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.common.function.FunctionWithException;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.SetFileAttributesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IAttributeDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Concrete implementation of the attribute command processing.
 */
public class AttributeDelegator extends BaseDelegator implements IAttributeDelegator {

    /**
     * Instantiate a new AttributeDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public AttributeDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IFileSpec> setFileAttributes(
            final List<IFileSpec> files,
            @Nonnull final Map<String, String> attributes,
            final SetFileAttributesOptions opts) throws P4JavaException {

        Validate.notNull(attributes);

        /*
         * Note the rather odd parameter processing below, required due to the
         * rather odd way attributes are passed to the server (or not) -- each
         * name must have a -n flag attached, and each value a corresponding -v
         * flag; it's unclear what happens when these don't match up, but never
         * mind... in any case, after some experimentation, it seems safest to
         * bunch all the names first, then the values. This may change with
         * further experimentation.
         */
        List<String> args = new ArrayList<>();
        for (String name : attributes.keySet()) {
            args.add("-n" + name);
        }

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (isNotBlank(entry.getValue())) {
                args.add("-v" + entry.getValue());
            }
        }

        /*
         * Note that this is the one command that doesn't adhere to the
         * (admittedly loose) rules about multiple returns and tag names, etc,
         * meaning that we anomalously return a list of result strings rather
         * than file specs; the reason underlying this is that each attribute
         * set causes a result row, meaning we may get multiple results back
         * from the server for the same file, and several error messages, etc.
         * -- all from the single request. This seems less than optimal to me...
         */
        List<Map<String, Object>> resultMaps = execMapCmdList(
                ATTRIBUTE,
                processParameters(
                        opts,
                        files,
                        args.toArray(new String[args.size()]),
                        true,
                        server),
                null);

        return buildSetFileAttributesFileSpecsFromCommandResultMaps(
                resultMaps,
                rethrowFunction(
                        new FunctionWithException<Map<String, Object>, IFileSpec>() {
                            @Override
                            public IFileSpec apply(Map<String, Object> map) throws P4JavaException {
                                return ResultListBuilder.handleFileReturn(map, server);
                            }
                        }
                )
        );
    }

    @Override
    public List<IFileSpec> setFileAttributes(
            final List<IFileSpec> files,
            @Nonnull final String attributeName,
            @Nonnull final InputStream inStream,
            final SetFileAttributesOptions opts) throws P4JavaException {

        Validate.notNull(inStream);
        Validate.notBlank(attributeName, "Attribute name shouldn't blank");
        /*
         * Note that we use the map argument here to pass in a single stream;
         * this can be expanded later if the server introduces multiple streams
         * for attributes (not likely for the attributes command, but other
         * commands may do this in the distant future, and anyway, it's the
         * thought that counts...).
         */
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put(IOptionsServer.ATTRIBUTE_STREAM_MAP_KEY, inStream);

        List<Map<String, Object>> resultMaps = execMapCmdList(
                ATTRIBUTE,
                processParameters(
                        opts,
                        files,
                        new String[]{"-i", "-n" + attributeName},
                        true,
                        server),
                inputMap);

        return buildSetFileAttributesFileSpecsFromCommandResultMaps(
                resultMaps,
                rethrowFunction(
                        new FunctionWithException<Map<String, Object>, IFileSpec>() {
                            @Override
                            public IFileSpec apply(Map<String, Object> map) throws P4JavaException {
                                return ResultListBuilder.handleFileReturn(map, server);
                            }
                        }
                )
        );
    }

    /**
     * Inner utility class to convert a resultmaps list into attribute filespec objects.
     * TODO: this doesn't belong in here, maybe filespecbuilder?
     */
    // p4ic4idea: remove private protections for unit tests
    List<IFileSpec> buildSetFileAttributesFileSpecsFromCommandResultMaps(
            @Nullable final List<Map<String, Object>> resultMaps,
            @Nonnull final Function<Map<String, Object>, IFileSpec> handle)
            throws AccessException, ConnectionException {

        List<IFileSpec> resultList = new ArrayList<>();
        if (nonNull(resultMaps)) {
            List<String> filesSeen = new ArrayList<>();
            for (Map<String, Object> map : resultMaps) {
                IFileSpec spec = handle.apply(map);
                if (spec.getOpStatus() == VALID) {
                    String file = spec.getAnnotatedPathString(DEPOT);
                    if (isNotBlank(file)) {
                        if (!filesSeen.contains(file)) {
                            filesSeen.add(file);
                            resultList.add(spec);
                        }
                    }
                } else {
                    resultList.add(spec);
                }
            }
        }
        return resultList;
    }
}