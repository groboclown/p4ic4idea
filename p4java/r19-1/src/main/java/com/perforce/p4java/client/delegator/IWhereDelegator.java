package com.perforce.p4java.client.delegator;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;

import java.util.List;

public interface IWhereDelegator {

	/**
	 * For each of the passed-in file specs, show how the named file maps through
	 * the client view.<p>
	 * <p>
	 * <b>Note that this method will return an empty list unless the client is the
	 * IServer object's current client.</b><p>
	 * <p>
	 * The returned IFiles contain all three of the depot, client, and local
	 * file paths of the corresponding fileSpec array element where appropriate, or the
	 * usual server-generated error message if there was no corresponding mapping. The
	 * main exception to this is the case when no filespecs are given (i.e. the fileSpecs
	 * array is null or empty), when the mappings for '...' (all files in the current directory
	 * and below) are returned (if they exist).
	 *
	 * @param fileSpecs a list of Perforce file specifications; can be empty or even
	 *                  null (see explanation above).
	 * @return a non-null (but possibly empty) list of IFileSpec for the input
	 * filespecs.
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 *                             connected.
	 * @throws AccessException     if the Perforce server denies access to the caller
	 */

	List<IFileSpec> where(List<IFileSpec> fileSpecs)
			throws ConnectionException, AccessException;

	/**
	 * For each of the passed-in file specs, show how the named file maps through
	 * the client view.<p>
	 * <p>
	 * <b>Note that this method will return an empty list unless the client is the
	 * IServer object's current client.</b><p>
	 * <p>
	 * The returned IFiles contain all three of the depot, client, and local
	 * file paths of the corresponding fileSpec array element where appropriate, or the
	 * usual server-generated error message if there was no corresponding mapping. The
	 * main exception to this is the case when no filespecs are given (i.e. the fileSpecs
	 * array is null or empty), when the mappings for '...' (all files in the current directory
	 * and below) are returned (if they exist).
	 *
	 * Assumes depot syntax is used for original path.
	 *
	 * @param fileSpecs a list of Perforce file specifications; can be empty or even
	 *                  null (see explanation above).
	 * @return a non-null (but possibly empty) list of IFileSpec for the input
	 * filespecs.
	 */

	List<IFileSpec> localWhere(List<IFileSpec> fileSpecs);

}
