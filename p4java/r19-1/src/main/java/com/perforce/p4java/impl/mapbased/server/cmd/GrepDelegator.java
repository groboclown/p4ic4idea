package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwRequestExceptionIfConditionFails;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.GREP;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.perforce.p4java.core.IFileLineMatch;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.FileLineMatch;
import com.perforce.p4java.option.server.MatchingLinesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IGrepDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation of 'p4 grep'.
 */
public class GrepDelegator extends BaseDelegator implements IGrepDelegator {

    /**
     * Instantiates a new grep delegator.
     *
     * @param server
     *            the server
     */
    public GrepDelegator(final IOptionsServer server) {
        super(server);
    }

    /**
     * Get list of matching lines in the specified file specs. This method
     * implements the p4 grep command; for full semantics, see the separate p4
     * documentation and / or the GrepOptions Javadoc.
     *
     * @param fileSpecs
     *            file specs to search for matching lines
     * @param pattern
     *            non-null string pattern to be passed to the grep command
     * @param options
     *            - Options to grep command
     * @return - non-null but possibly empty list of file line matches
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     */
    public List<IFileLineMatch> getMatchingLines(final List<IFileSpec> fileSpecs,
            final String pattern, final MatchingLinesOptions options) throws P4JavaException {

        return getMatchingLines(fileSpecs, pattern, null, options);
    }

    /**
     * Get list of matching lines in the specified file specs. This method
     * implements the p4 grep command; for full semantics, see the separate p4
     * documentation and / or the GrepOptions Javadoc.
     * <p>
     *
     * This method allows the user to retrieve useful info and warning message
     * lines the Perforce server may generate in response to things like
     * encountering a too-long line, etc., by passing in a non-null infoLines
     * parameter.
     *
     * @param fileSpecs
     *            file specs to search for matching lines
     * @param pattern
     *            non-null string pattern to be passed to the grep command
     * @param infoLines
     *            if not null, any "info" lines returned from the server (i.e.
     *            warnings about exceeded line lengths, etc.) will be put into
     *            the passed-in list in the order they are received.
     * @param options
     *            - Options to grep command
     * @return - non-null but possibly empty list of file line matches
     * @throws P4JavaException
     *             if any error occurs in the processing of this method.
     * @since 2011.1
     */
    public List<IFileLineMatch> getMatchingLines(@Nonnull final List<IFileSpec> fileSpecs,
            @Nonnull final String pattern, @Nullable final List<String> infoLines,
            final MatchingLinesOptions options) throws P4JavaException {

        Validate.notNull(fileSpecs);
        Validate.notBlank(pattern, "Match pattern string shouldn't null or empty");

        List<Map<String, Object>> resultMaps = execMapCmdList(GREP,
                processParameters(options, fileSpecs, "-e" + pattern, server), null);

        List<IFileLineMatch> specList = new ArrayList<>();
        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                String message = ResultMapParser.getErrorStr(map);
                throwRequestExceptionIfConditionFails(isBlank(message), parseCode0ErrorString(map),
                        message);
                message = ResultMapParser.getErrorOrInfoStr(map);
                if (isBlank(message)) {
                    specList.add(new FileLineMatch(map));
                } else if (nonNull(infoLines)) {
                    infoLines.add(message);
                }
            }
        }
        return specList;
    }
}
