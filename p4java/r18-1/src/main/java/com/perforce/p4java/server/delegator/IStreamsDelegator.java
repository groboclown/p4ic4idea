package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetStreamsOptions;

/**
 * Interface to handle the Streams command.
 */
public interface IStreamsDelegator {
    /**
     * Get a list of all summary Perforce streams known to the Perforce server.
     * <p>
     * <p>
     * Note that the IStreamSummary objects returned here do not have stream
     * paths. You must call the getStream method on a specific stream to get
     * valid paths for a stream.
     *
     * @param streamPaths if specified, the list of streams is limited to those matching
     *                    the supplied list of stream paths, of the form
     *                    //depotname/streamname
     * @param opts        object describing optional parameters; if null, no options are
     *                    set.
     * @return non-null (but possibly-empty) list of IStreamSummary objects.
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2011.2
     */
    List<IStreamSummary> getStreams(
            List<String> streamPaths,
            GetStreamsOptions opts) throws P4JavaException;
}
