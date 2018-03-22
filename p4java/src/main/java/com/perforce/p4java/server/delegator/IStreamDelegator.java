package com.perforce.p4java.server.delegator;

import javax.annotation.Nonnull;

import com.perforce.p4java.core.IStream;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetStreamOptions;
import com.perforce.p4java.option.server.StreamOptions;

/**
 * Interface to handle the Stream command.
 */
public interface IStreamDelegator {
    /**
     * Create a new stream in the repository.
     *
     * @param stream non-null IStream object representing the stream to be created.
     * @return possibly-null operation result message string from the Perforce
     * server.
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2011.2
     */
    String createStream(@Nonnull IStream stream)
            throws P4JavaException;

    /**
     * Get an individual stream by stream path. Note that this method will
     * return a fake stream if you ask it for a non-existent stream, so it's not
     * the most useful of operations.
     *
     * @param streamPath non-null stream's path in a stream depot, of the form
     *                   //depotname/streamname
     * @return IStream non-null object corresponding to the named stream if it
     * exists and is retrievable; otherwise an IStream object that looks
     * real but does not, in fact, correspond to any known stream in the
     * repository.
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2011.2
     */
    IStream getStream(@Nonnull String streamPath)
            throws P4JavaException;

    /**
     * Get an individual stream by stream path. Note that this method will
     * return a fake stream if you ask it for a non-existent stream, so it's not
     * the most useful of operations.
     *
     * @param streamPath non-null stream's path in a stream depot, of the form
     *                   //depotname/streamname
     * @param opts       GetStreamOptions object describing optional parameters; if
     *                   null, no options are set.
     * @return IStream non-null object corresponding to the named stream if it
     * exists and is retrievable; otherwise an IStream object that looks
     * real but does not, in fact, correspond to any known stream in the
     * repository.
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2012.1
     */
    IStream getStream(String streamPath, GetStreamOptions opts)
            throws P4JavaException;

    /**
     * Update a Perforce stream spec on the Perforce server.
     *
     * @param stream non-null stream spec to be updated.
     * @param opts   StreamOptions object describing optional parameters; if null,
     *               no options are set.
     * @return non-null result message string from the Perforce server; this may
     * include form trigger output pre-pended and / or appended to the
     * "normal" message.
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2011.2
     */
    String updateStream(IStream stream, StreamOptions opts)
            throws P4JavaException;

    /**
     * Delete a Perforce stream spec from the Perforce server.
     *
     * @param streamPath non-null stream's path in a stream depot, of the form
     *                   //depotname/streamname
     * @param opts       StreamOptions object describing optional parameters; if null,
     *                   no options are set.
     * @return non-null result message string from the Perforce server; this may
     * include form trigger output pre-pended and / or appended to the
     * "normal" message
     * @throws P4JavaException if any error occurs in the processing of this method.
     * @since 2011.2
     */
    String deleteStream(String streamPath, StreamOptions opts)
            throws P4JavaException;
}
