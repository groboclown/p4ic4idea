/*
 * Copyright (c) 2016, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java.server.delegator;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.SetFileAttributesOptions;

/**
 * Delegator class for the Java API version of the <i>p4 attribute</i> command.
 * Supported options are:
 * <ul><li></li><li></li></ul>
 * See the online <a href="https://www.perforce.com/perforce/doc.current/manuals/cmdref/p4_attribute.html">Perforce Helix documentation</a> for more details.
 */
public interface IAttributeDelegator {

    /**
     * Set file attributes on one or more files (unsupported). See the main
     * Perforce documentation for an explanation of file attributes, which are
     * potentially complex and difficult to use efficiently. Attributes can
     * currently only be retrieved using the getExtendedFiles (fstat) operation.
     * <p>
     * <p>
     * Note that this method only accepts String attribute values; if the
     * attribute is intended to be binary, use the setHexValue setter on the
     * associated SetFileAttributesOptions object and hexify the value, or,
     * alternatively, use the stream version of this method. String input this
     * way will be converted to bytes for the attributes before being sent to
     * the Perforce server using the prevailing character set. If this is a
     * problem, use hex encoding or the stream variant of this method
     * <p>
     * <p>
     * Note that attributes can only be removed from a file by setting the
     * appropriate value of the name / value pair passed-in through the
     * attributes map to null.
     * <p>
     * <p>
     * Note that the filespecs returned by this method, if valid, contain only
     * the depot path and version information; no other field can be assumed to
     * be valid. Note also that, while the p4 command line executable returns a
     * list of results that amounts to the cross product of files and
     * attributes, this method never returns more than one result for each file
     * affected.
     *
     * @param opts       SetFileAttributesOptions object describing optional
     *                   parameters; if null, no options are set.
     * @param attributes a non-null Map of attribute name / value pairs; if any value
     *                   is null, that attribute is removed.
     * @param files      non-null list of files to be affected
     * @return non-null but possibly empty list of filespec results for the
     * operation.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2011.1
     */
    public List<IFileSpec> setFileAttributes(
            List<IFileSpec> files,
            Map<String, String> attributes,
            SetFileAttributesOptions opts) throws P4JavaException;

    /**
     * Set a file attribute on one or more files using the passed-in input
     * stream as the source for the attribute's value (unsupported). See the
     * main Perforce documentation for an explanation of file attributes, which
     * are potentially complex and difficult to use efficiently. Attributes can
     * currently only be retrieved using the getExtendedFiles (fstat) operation.
     * <p>
     * <p>
     * This method is intended to allow for unmediated binary definitions of
     * file attribute contents, and is typically used for things like thumbnails
     * that are too big to be conveniently handled using hex conversion with the
     * strings-based version of this method. Absolutely no interpretation is
     * done on the stream -- it's bytes all the way... there is also no hard
     * limit to the size of the stream that contains the attribute value, but
     * the consequences on both the enclosing app and the associated Perforce
     * server of too-large attributes may be severe. Typical 8K thumbnails are
     * no problem at all, but something in the megabyte range or larger might be
     * problematic at both ends.
     * <p>
     * <p>
     * Note that this method will leave the passed-in stream open, but (in
     * general) the stream's read pointer will be at the end of the stream when
     * this method returns. You are responsible for closing the stream if
     * necessary after the call; you are also responsible for ensuring that the
     * read pointer is where you want it to be in the stream (i.e. where you
     * want the method to start reading the attribute value from) when you pass
     * in the stream. I/O errors while reading the stream will be logged, but
     * otherwise generally ignored -- you must check the actual results of this
     * operation yourself.
     * <p>
     * <p>
     * Note that the server currently only supports setting file attributes
     * using a stream for one filespec at a time, but for reasons of symmetry
     * you must pass in a list of (one) filespec. Note that this doesn't
     * necessarily mean only one <i>file</i> is affected in the depot, just that
     * only one file <i>spec</i> is used to specify the affected file(s).
     * <p>
     * <p>
     * Note that attributes can only be removed from a file by setting the
     * appropriate value of the name / value pair passed-in through the
     * attributes map to null.
     * <p>
     * <p>
     * Note that the filespecs returned by this method, if valid, contain only
     * the depot path and version information; no other field can be assumed to
     * be valid. Note also that, while the p4 command line executable returns a
     * list of results that amounts to the cross product of files and
     * attributes, this method never returns more than one result for each file
     * affected.
     *
     * @param opts          SetFileAttributesOptions object describing optional
     *                      parameters; if null, no options are set.
     * @param attributeName the non-null name of the attribute to be set.
     * @param inStream      non-null InputStream ready for reading the attribute value
     *                      from.
     * @param files         non-null list of files to be affected.
     * @return non-null but possibly empty list of filespec results for the
     * operation.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2011.1
     */
    public List<IFileSpec> setFileAttributes(
            List<IFileSpec> files,
            @Nonnull String attributeName,
            @Nonnull InputStream inStream,
            SetFileAttributesOptions opts) throws P4JavaException;
}
