/**
 * 
 */
package com.perforce.p4java.impl.generic.core.file;

import com.perforce.p4java.core.file.IFileSpec;

/**
 * Defines a file spec path for Perforce IFileSpec objects, and a bunch of useful
 * methods for extracting and appending Perforce file-related metadata (such as
 * revision information or label / changelist / date annotations) to and from paths
 * and path strings.
 * 
 * In Perforce terms, a file path specifies a particular file, but not a specific
 * revision of that file (either implicitly or explicitly). That is, FilePath objects
 * do not contain the possible "#" or "@" (etc.) specifiers suffixed to Perforce paths.
 * In P4Java terms, file path <i>strings</i> are typically string representations of paths
 * as understood above, but with version / changelist / etc. annotations attached.
 */

public class FilePath {
	
	/**
	 * Defines the various types a Perforce file path can have.
	 */
	
	public enum PathType {
		/**
		 * Unknown file path type.
		 */
		UNKNOWN,
		
		/**
		 * Path pseudo-type used to flag a path as being the
		 * "original" path spec for a file spec. See IFileSpec
		 * for an explanation of ORIGINAL path types.
		 */
		ORIGINAL,
		
		/**
		 * Path is a Perforce depot path type.
		 */
		DEPOT,
		
		/**
		 * Path is a Perforce client path type.
		 */
		CLIENT,
		
		/**
		 * Path is a Perforce local path type.
		 */
		LOCAL;
	};
	
	protected String pathString = null;
	protected PathType pathType = PathType.UNKNOWN;
	
	/**
	 * Default constructor -- sets path type field to UNKNOWN,
	 * path string to null.
	 */
	public FilePath() {
	}
	
	/**
	 * Construct a FilePath from explicit type and path string values. If the
	 * passed-in pathString contains version / date / changelist information,
	 * it is stripped from the path and ignored.
	 */
	public FilePath(PathType pathType, String pathString) {
		this(pathType, pathString, false);
	}
	
	/**
	 * Construct a FilePath from explicit type and path string values. If the
	 * ignoreAnnotations parameter is false and if the 
	 * passed-in pathString contains version / date / changelist information,
	 * it is stripped from the path and ignored; otherwise the pathString
	 * parameter is used-as is (allowing for embedded "#" and "@" characters,
	 * for instance).
	 */
	public FilePath(PathType pathType, String pathString, boolean ignoreAnnotations) {
		this.pathType = pathType;
		if (ignoreAnnotations) {
			this.pathString = pathString;
		} else {
			this.pathString = PathAnnotations.stripAnnotations(pathString);
		}
	}
	
	/**
	 * Annotate this path with the passed-in Perforce file metadata
	 * annotations. If either or both this.pathString and annotations
	 * is null, returns this.pathString.
	 */
	
	public String annotate(PathAnnotations annotations) {
		if ((annotations != null) && (this.pathString != null)) {
			return this.pathString + annotations.toString();
		}
		
		return this.pathString;
	}
	
	public String annotate(IFileSpec fileSpec) {
		if ((fileSpec != null) && (this.pathString != null)) {
			return this.pathString + new PathAnnotations(fileSpec);
		}
		return this.pathString;
	}
	
	public PathType getPathType() {
		return this.pathType;
	}
	
	/**
	 * Note: does NOT annotate by default!
	 */
	public String toString() {
		return this.pathString;
	}

	public String getPathString() {
		return pathString;
	}

	public void setPathString(String pathString) {
		this.pathString = pathString;
	}

	public void setPathType(PathType pathType) {
		this.pathType = pathType;
	}
}
