/**
 * 
 */
package com.perforce.p4java.impl.generic.sys;

/**
 * An interface that specifies a set of useful file-level functions
 * (such as recognizing a symbolic link or setting writable or
 * executable bits) that are not always implemented by vanilla
 * Java installations, and that can be used by P4Java to implement
 * these file operations when the surrounding JVm or JDK does not
 * do a good job.<p>
 * 
 * Java native P4Java implementations have to cope with the
 * fact that most vanilla JDK 5 implementations have poor or
 * even non-existent support for file-level metadata manipulations;
 * JDK 6 is not much better, but does allow you to set a file
 * writable and executable (albeit without honoring the Unix umask
 * settings). In order to allow JDK 5 installations to use the P4Java
 * NIO / RPC implementations, this interface is provided in conjunction
 * with the server factory's setRpcFileSystemHelper method to allow 
 * users to specify a helper class (or classes) that implement these
 * functions using native libraries or external functions (such as Eclipse's
 * file helpers). Note that this class can be safely ignored if you
 * are using the p4 command line protocol implementation, and can
 * typically be ignored with all protocol implementations if you are
 * using JDK 6 or later.<p>
 * 
 * Methods here concentrate on mundane file mode changes and metadata,
 * mostly, but may be added to as time goes by. The semantics of the
 * individual methods below are intended to be the same as those for
 * the corresponding methods defined on JDK 6's File class (where they
 * exist), with broad interpretations allowed.<p>
 * 
 * Note that all methods defined below <i>must</i> be implemented
 * in a thread-safe way and not cause undue thread blocking, as they're
 * called at crucial stages of the underlying RPC implementation. Note also
 * that the supplied SystemFileCommands implementation <i>must</i>
 * work safely even if it's just instantiated once per P4Java installation
 * for all requests (i.e. it must work as a singleton).
 */

public interface ISystemFileCommandsHelper {

	/**
	 * Set the file's permissions to allow or disallow writing to it.
	 * 
	 * @param fileName non-null path or name of the target file.
	 * @param writable if true, allow the file to be written to; if false,
	 * 			set the file read-only (or, more accurately, not writable).
	 * @return true iff the set permission operation succeeded. May return false
	 * 			if the file didn't exist or the operation failed or an exception was
	 * 			caught, etc.
	 */
	boolean setWritable(String fileName, boolean writable);
	
	/**
	 * Set the file's permissions to allow or disallow reading from it.
	 * 
	 * @param fileName non-null path or name of the target file.
	 * 			set the file read-only.
	 * @param readable if true, allow the file to be read; if false,
	 * 			set the file to not be readable
	 * @param ownerOnly true to only set the read-only bit for the owner
	 * @return true iff the set permission operation succeeded. May return false
	 * 			if the file didn't exist or the operation failed or an exception was
	 * 			caught, etc.
	 */
	boolean setReadable(String fileName, boolean readable, boolean ownerOnly);
	
	/**
	 * Set the file's read permissions only allow reading by owner.
	 * 
	 * @param fileName non-null path or name of the target file.
	 * 			set the file read-only.
	 * @return true iff the set permission operation succeeded. May return false
	 * 			if the file didn't exist or the operation failed or an exception was
	 * 			caught, etc.
	 */
	boolean setOwnerReadOnly(String fileName);
	
	/**
	 * Set the file's permissions to allow or disallow it to be executed.
	 * 
	 * @param fileName non-null path or name of the target file.
	 * @param executable if true, allow the file to be executed; if false,
	 * 			set the file not executable.
	 * @param ownerOnly true to only set the executable bit for the owner
	 * @return true iff the set permission operation succeeded. May return false
	 * 			if the file didn't exist or the operation failed or an exception was
	 * 			caught, etc.
	 */
	boolean setExecutable(String fileName, boolean executable, boolean ownerOnly);
	
	/**
	 * Return true iff the file exists and is executable.
	 * 
	 * @param fileName non-null path or name of the target file.
	 * @return true iff the file existed and is executable.
	 */
	boolean canExecute(String fileName);
	
	/**
	 * Return true iff the file exists and is a symbolic link.<p>
	 * 
	 * This method is guaranteed to only be called when we suspect
	 * a specific file may be a symbolic link (i.e. we've exhausted
	 * other possibilities) and it is safe for this method to return
	 * false if it can't implement the associated plumbing or it
	 * simply can't tell whether the file is a symlink or not.
	 * 
	 * @param fileName non-null path or name of the target file.
	 * @return true iff the file exists and is a symbolic link.
	 */
	boolean isSymlink(String fileName);
}
