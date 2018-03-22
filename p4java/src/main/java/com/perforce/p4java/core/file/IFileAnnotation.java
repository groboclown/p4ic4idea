/**
 * 
 */
package com.perforce.p4java.core.file;

import java.util.List;

/**
 * Describes a Perforce file annotation as returned from the annotation command.
 * See the main Perforce documentation for the annotate command for detailed
 * descriptions of Perforce file version annotation.
 */

public interface IFileAnnotation {
	
	/**
	 * Get the lower version or change number for the associated annotation.
	 */
	int getLower();
	
	/**
	 * Get the upper version or change number for the associated annotation.
	 */
	int getUpper();
	
	/**
	 * Get the line being annotated. This will <i>not</i> include the associated newline
	 * character or any line ending processing, but may include any embedded carriage
	 * return characters if they exist.<p>
	 * 
	 * The returned line will never be null, but may be empty.
	 */
	String getLine();
	
	/**
	 * Get the line being annotated, with optional line ending processing based on
	 * client settings (if available). This method is aimed mostly at P4Eclipse and other
	 * specialized usage where file content lines need to be matched or compared
	 * with sync'd file contents (etc.), and may not be particularly useful for general
	 * users: when in doubt, use the simple getLine() method instead.<p>
	 * 
	 * The returned line will never be null, but may be empty.
	 * 
	 * @param processLineEndings if true, and if a client is associated with this
	 * 			annotation (i.e. it was created by one of the IServer or IFileSpec
	 * 			annotation methods, and a client was associated with the server at the
	 * 			time the IServer or IFileSpec call was made), the returned string will
	 * 			attempt to honor client line end settings where possible; if there are no
	 * 			available settings, the local line ending is appended. If processLineEndings
	 * 			is not true, the results are identical to calling the no-argument getLine method.
	 */
	String getLine(boolean processLineEndings);
	
	/**
	 * Get the associated depot path for this annotation, if one is available.
	 * May be null in certain (very unusual) circumstances.
	 */
	String getDepotPath();
	
	/**
	 * Get a list of all contributing integration annotations for this annotation,
	 * if such a list exists. In the absence of the use of the GetFileAnnotationsOptions
	 * followAllIntegrations option (corresponding to annotate -I), this method will
	 * always return null; otherwise, if it is not null, it will contain a list of
	 * IFileAnnotation objects representing the contributing integrations. These
	 * IFileAnnotation objects will have a non-negative ordering (see getOrdering()),
	 * and null line data.
	 * 
	 * @since 2011.1
	 * @return possibly-null list of contributing integrations.
	 */
	List<IFileAnnotation> getAllIntegrations();
	
	/**
	 * Get the ordering of this annotation in the associated allIntegrations
	 * list, if it exists. If the returned value is -1, this annotation is not
	 * part of an integrations list (and is therefore a data annotation).
	 * 
	 * @since 2011.1
	 */
	
	int getOrdering();
}
