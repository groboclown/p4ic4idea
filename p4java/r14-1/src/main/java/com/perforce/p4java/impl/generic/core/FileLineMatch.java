/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.core;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IFileLineMatch;
import com.perforce.p4java.impl.generic.core.file.FileSpec;

import java.util.Map;

/**
 * Implementation class of the {@link IFileLineMatch} interface
 */
public class FileLineMatch implements IFileLineMatch {

	protected String file = null;
	protected int revision = -1;
	protected int lineNumber = -1;
	protected String line = null;
	protected MatchType type = MatchType.MATCH;

	/**
	 * Explicit-value constructor; sets all fields to null or -1, type to MatchType.MATCH.
	 */
	public FileLineMatch(String file, int revision, String line, MatchType type) {
		this.file = file;
		this.revision = revision;
		this.line = line;
		this.type = type;
	}

	/**
	 * Create a file line match
	 * 
	 * @param map
	 */
	public FileLineMatch(Map<String, Object> map) {
		if (map != null) {
			this.file = (String) map.get("depotFile");
			this.revision = FileSpec.getRevFromString((String) map.get("rev"));
			if (map.get("line") != null) {
				try {
					this.lineNumber = Integer.parseInt((String) map.get("line"));
				} catch (NumberFormatException exc) {
					Log.warn("NumberFormatException in FileLineMatch map-based constructor: "
								+ exc.getLocalizedMessage());
				}
			}
			this.line = (String) map.get("matchedLine");
			this.type = MatchType.fromServerString((String) map.get("type"));
		}
	}

	/**
	 * @see com.perforce.p4java.core.IFileLineMatch#getDepotFile()
	 */
	public String getDepotFile() {
		return this.file;
	}

	/**
	 * @see com.perforce.p4java.core.IFileLineMatch#getLine()
	 */
	public String getLine() {
		return this.line;
	}

	/**
	 * @see com.perforce.p4java.core.IFileLineMatch#getRevision()
	 */
	public int getRevision() {
		return this.revision;
	}

	/**
	 * @see com.perforce.p4java.core.IFileLineMatch#getLineNumber()
	 */
	public int getLineNumber() {
		return this.lineNumber;
	}

	/**
	 * @see com.perforce.p4java.core.IFileLineMatch#getType()
	 */
	public MatchType getType() {
		return this.type;
	}

}
