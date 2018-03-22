/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core;

/**
 * Defines an individual file path in the stream view. Each view mapping is of
 * the form:
 * <p>
 * 
 * <path_type> <view_path> [<depot_path>]
 * <p>
 * 
 * where <path_type> is a single keyword, <view_path> is a file path with no
 * leading slashes, and the optional <depot_path> is a file path beginning with
 * '//'. Both <view_path> and <depot_path> may contain trailing wildcards, but
 * no leading or embedded wildcards. Lines in the Paths field may appear in any
 * order. A duplicated <view_path> overrides its preceding entry.
 * <p>
 * 
 * Paths are inherited by child stream views. A child stream's paths can
 * downgrade the inherited view, but not upgrade it. (For instance, a child
 * stream can downgrade a shared path to an isolated path, but it can't upgrade
 * an isolated path to a shared path.) Note that <depot_path> is relevant only
 * when <path_type> is 'import' or 'import+'.
 */

public interface IStreamViewMapping extends IMapEntry {

	/**
	 * Defines the possible path types.
	 * <p>
	 * 
	 * share: <view_path> will be included in client views and in branch views.
	 * Files in this path are accessible to workspaces, can be submitted to the
	 * stream, and can be integrated with the parent stream.
	 * <p>
	 * 
	 * isolate: <view_path> will be included in client views but not in branch
	 * views. Files in this path are accessible to workspaces, can be submitted
	 * to the stream, but are not integratable with the parent stream.
	 * <p>
	 * 
	 * import: <view_path> will be included in client views but not in branch
	 * views. Files in this path are mapped as in the parent stream's view (the
	 * default) or to <depot_path> (optional); they are accessible to
	 * workspaces, but can not be submitted or integrated to the stream.
	 * <p>
	 * 
	 * exclude: <view_path> will be excluded from client views and branch views.
	 * Files in this path are not accessible to workspaces, and can't be
	 * submitted or integrated to the stream.
	 * <p>
	 */
	public enum PathType {
		SHARE("share"),
		ISOLATE("isolate"),
		IMPORT("import"),
		IMPORTPLUS("import+"),
		EXCLUDE("exclude"),
		UNKNOWN("unknown");
		
		private String type = null;
		
		private PathType(String type) {
			this.type = type;
		}

		public String getValue() {
			return this.type;
		}

		@Override
	    public String toString() {
	        return this.getValue();
	    }
		
		/**
		 * Return a suitable Path type as inferred from the passed-in
		 * string, which is assumed to be the string form of a Path type.
		 * Otherwise return the UNKNOWN type
		 */
		public static PathType fromString(String str) {
			if (str != null) {
				for (PathType pt : PathType.values()) {
					if (str.equalsIgnoreCase(pt.type)) {
						return pt;
					}
				}
				return UNKNOWN;
			}
			return null;
		}
	};

	/**
	 * Get the stream view path type
	 */
	PathType getPathType();

	/**
	 * Set a stream view path type
	 */
	void setPathType(PathType pathType);

	/**
	 * Get a stream view entry's view path; this corresponds to the left entry
	 * of the associated mapping.
	 */
	String getViewPath();

	/**
	 * Set a stream view entry's view path; this corresponds to the left entry
	 * of the associated mapping.
	 */
	void setViewPath(String viewPath);

	/**
	 * Get a stream view entry's optional depot path; this corresponds to the
	 * right entry of the associated mapping.
	 */
	String getDepotPath();

	/**
	 * Set a stream view entry's optional depot path; this corresponds to the
	 * right entry of the associated mapping.
	 */
	void setDepotPath(String depotPath);
};
