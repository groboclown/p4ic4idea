/**
 * 
 */
package com.perforce.p4java.core.file;

import com.perforce.p4java.core.IServerResource;

/**
 * Defines the broadest operations available on file specs returned
 * from Perforce file operations.<p>
 * 
 * IFileOperationResult is the base interface for the IFileSpec family of
 * interfaces, and is used to signal and store the synchronous error returns
 * from methods that return file specs and lists of file specs. This is needed
 * due to the Perforce server's penchant for returning info and error messages
 * intertwined with "normal" file specs; this interface can be used to determine
 * the status of a specific returned file spec.<p>
 * 
 * In general, if an IFileOperationResult object has been returned from a server
 * or client (etc.) method, the getOpStatus() method will indicate whether the
 * associated values are "valid" (i.e. were filled-in or whatever by the underlying
 * server) or represent an error or informational event. In the latter cases,
 * the getStatusMessage() method returns the associated server message, and the
 * other fields in the file spec are not guaranteed to contain useful values.<p>
 * 
 * Note that although IFileOperationResult extends the IServerResource interface, current
 * implementations are not refreshable, updateable, or completable through that interface,
 * and isComplete() will currently always return true.
 */

public interface IFileOperationResult extends IServerResource {
	
	/**
	 * Return the particular Perforce operation status associated with the
	 * specific Perforce file spec operation.
	 * 
	 * @return non-null FileSpecOpStatus for the specific file spec return.
	 */
	FileSpecOpStatus getOpStatus();
	
	/**
	 * Return the status message associated with the operation (this may be an
	 * error or informational message, depending on the operation). Will only be
	 * meaningful if the associated op status returned by getOpStatus() is neither
	 * UNKNOWN nor VALID.
	 * 
	 * @return possibly-null Perforce operation message.
	 */
	String getStatusMessage();
	
	/**
	 * Get the Perforce severity code associated with the operation result. Will only
	 * be meaningful if the associated op status returned by getOpStatus() is neither
	 * UNKNOWN nor VALID.
	 * 
	 * @return integer severity code.
	 */
	int getSeverityCode();
	
	/**
	 * Get the Perforce generic code associated with the operation result. Will only
	 * be meaningful if the associated op status returned by getOpStatus() is neither
	 * UNKNOWN nor VALID.
	 * 
	 * @return integer generic code.
	 */
	int getGenericCode();
	
	/**
	 * Get the Perforce raw code associated with the operation result. Will only
	 * be meaningful if the associated op status returned by getOpStatus() is neither
	 * UNKNOWN nor VALID, and the status was set as the result of a server-side issue.
	 */
	int getRawCode();
	
	/**
	 * Get the Perforce 'unqieu' code associated with the operation result. Will only
	 * be meaningful if the associated op status returned by getOpStatus() is neither
	 * UNKNOWN nor VALID, and the status was set as the result of a server-side issue.
	 */
	int getUniqueCode();
	
	/**
	 * Get the Perforce sub system code associated with the operation result. Will only
	 * be meaningful if the associated op status returned by getOpStatus() is neither
	 * UNKNOWN nor VALID, and the status was set as the result of a server-side issue.
	 */
	int getSubSystem();
	
	/**
	 * Get the Perforce 'sub' code associated with the operation result. Will only
	 * be meaningful if the associated op status returned by getOpStatus() is neither
	 * UNKNOWN nor VALID, and the status was set as the result of a server-side issue.
	 */
	int getSubCode();
}
