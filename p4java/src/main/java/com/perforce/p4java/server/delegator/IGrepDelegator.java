package com.perforce.p4java.server.delegator;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.perforce.p4java.core.IFileLineMatch;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.MatchingLinesOptions;

// p4ic4idea: use IServerMessage rather than string info messages.
import com.perforce.p4java.server.IServerMessage;

/**
 * The Interface for 'p4 grep' implementations.
 */
public interface IGrepDelegator {

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
    List<IFileLineMatch> getMatchingLines(List<IFileSpec> fileSpecs,
            String pattern, MatchingLinesOptions options) throws P4JavaException;
    
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
    List<IFileLineMatch> getMatchingLines(@Nonnull List<IFileSpec> fileSpecs,
            // p4ic4idea: switch infoLines to an IServerMessage list.
            @Nonnull String pattern, @Nullable List<IServerMessage> infoLines,
            MatchingLinesOptions options) throws P4JavaException;
}
