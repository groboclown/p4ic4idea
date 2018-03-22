/*
 * Copyright (c) 2016, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetInterchangesOptions;

/**
 *
 */
public interface IInterchangesDelegator {

    /**
     * Returns a list of changelists that have not been integrated from a set of
     * source files to a set of target files.
     *
     * @param fromFile
     *            if non-null, use this as the from-file specification.
     * @param toFile
     *            if non-null, use this as the to-file specification.
     * @param opts
     *            GetInterchangesOptions object describing optional parameters;
     *            if null, no options are set.
     * @return non-null (but possibly empty) list of qualifying changelists.
     *         Note that the changelists returned here may not have all fields
     *         set (only description, ID, date, user, and client are known to be
     *         properly set by the server for this command)
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     */
    List<IChangelist> getInterchanges(IFileSpec fromFile, IFileSpec toFile,
            GetInterchangesOptions opts) throws P4JavaException;

    /**
     * Returns a list of changelists that have not been integrated from a set of
     * source files to a set of target files.
     * <p>
     *
     * Note that depending on the specific options passed-in the fromFileList
     * can be null or one file spec; the toFileList can be null, one or more
     * file specs. The full semantics of this operation are found in the main
     * 'p4 help interchanges' documentation.
     * <p>
     *
     * @param branchSpecName
     *            if non-null and not empty, use this as the branch spec name.
     * @param fromFileList
     *            if non-null and not empty, and biDirectional is true, use this
     *            as the from file list.
     * @param toFileList
     *            if non-null and not empty, use this as the to file list.
     * @param opts
     *            GetInterchangesOptions object describing optional parameters;
     *            if null, no options are set.
     * @return non-null (but possibly empty) list of qualifying changelists.
     *         Note that the changelists returned here may not have all fields
     *         set (only description, ID, date, user, and client are known to be
     *         properly set by the server for this command)
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     */
    List<IChangelist> getInterchanges(String branchSpecName, List<IFileSpec> fromFileList,
            List<IFileSpec> toFileList, GetInterchangesOptions opts) throws P4JavaException;
}
