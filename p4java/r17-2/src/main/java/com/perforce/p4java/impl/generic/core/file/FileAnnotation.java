/**
 * 
 */
package com.perforce.p4java.impl.generic.core.file;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.client.IClientSummary.ClientLineEnd;
import com.perforce.p4java.core.file.IFileAnnotation;

/**
 * Simple generic implementation class for IFileAnnotation interface.
 */
public class FileAnnotation implements IFileAnnotation {

	private int upper = 0;
	private int lower = 0;
	private String depotPath = null;
	private String line = null;
	private ClientLineEnd lineEnd = null;
	private boolean hadLineEnd = false;
	private static final String localLineEndStr = System.getProperty("line.separator", "\n");
	private List<IFileAnnotation> contributingSources = null;
	private int ordering = -1;
	
	/**
	 * Default all-field constructor.
	 */
	public FileAnnotation(int upper, int lower, String depotPath,
			String line, ClientLineEnd lineEnd) {
		this.upper = upper;
		this.lower = lower;
		this.depotPath = depotPath;
		this.line = line;
		this.lineEnd = lineEnd;
		handleLineEnding();
	}
	
	private void handleLineEnding() {
		// Data comes back with unwanted trailing newline; get rid of this here:
		if (this.line != null) {
			// Data can come back with \r\n on windows servers
			// and \n on mac/unix servers
			if (this.line.endsWith("\r\n")) {
				this.line = this.line.substring(0, this.line.length() - 2);
				hadLineEnd = true;
			} else if (this.line.endsWith("\n")) {
				this.line = this.line.substring(0, this.line.length() - 1);
				hadLineEnd = true;
			}
		}
	}
	
	/**
	 * Construct a suitable FileAnnotation object from the passed-in
	 * map; this map must be in the format and use the fields returned from
	 * a Perforce server annotate command.<p>
	 * 
	 * Leave lineEnd null for normal use.
	 */
	public FileAnnotation(Map<String, Object> map, String depotPath, ClientLineEnd lineEnd) {
		if (map != null) {
			try {
				lower = new Integer((String) map.get("lower"));
				upper = new Integer((String) map.get("upper"));
				this.lineEnd = lineEnd;
				this.line = (String) map.get("data");
				handleLineEnding();
			} catch (Throwable thr) {
				Log.warn("bad conversion in FileAnnotation constructor; map: " + map);
				Log.exception(thr);
			}
		}
		
		this.depotPath = depotPath; // May, of course, be null...
	}
	
	/**
	 * Create a new non-data file annotation with the passed-in parameters.
	 */
	
	public FileAnnotation(int order, String depotPath, int upper, int lower) {
		this.ordering = order;
		this.depotPath = depotPath;
		this.upper = upper;
		this.lower = lower;
	}
	
	/**
	 * Add an integration annotation to the contributingSources list. If the list
	 * is null, a new list will be created; otherwise it's added to the end of the list.
	 */
	public void addIntegrationAnnotation(IFileAnnotation annotation) {
		if (annotation != null) {
			if (this.contributingSources == null) {
				this.contributingSources = new ArrayList<IFileAnnotation>();
			}
			this.contributingSources.add(annotation);
		}
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileAnnotation#getDepotPath()
	 */
	public String getDepotPath() {
		return depotPath;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileAnnotation#getLine()
	 */
	public String getLine() {
		return this.line;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileAnnotation#getLine(boolean)
	 */
	public String getLine(boolean processLineEndings) {		
		if (processLineEndings && this.line != null && hadLineEnd) {
			if (lineEnd != null) {
				switch (lineEnd) {
				case UNIX:
				case SHARE:
					return this.line + "\n";
				case MAC:
					return this.line + "\r";
				case WIN:
					return this.line + "\r\n";
				case LOCAL:
				default:
					return this.line + localLineEndStr;
				}
			} else {
				return this.line + localLineEndStr;
			}
		}
		return this.line;
	}
	
	/**
	 * @see com.perforce.p4java.core.file.IFileAnnotation#getContributingSources()
	 */
	public List<IFileAnnotation> getAllIntegrations() {
		return this.contributingSources;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileAnnotation#getLower()
	 */
	public int getLower() {
		return lower;
	}

	/**
	 * @see com.perforce.p4java.core.file.IFileAnnotation#getUpper()
	 */
	public int getUpper() {
		return upper;
	}

	public void setUpper(int upper) {
		this.upper = upper;
	}

	public void setLower(int lower) {
		this.lower = lower;
	}

	public void setDepotPath(String depotPath) {
		this.depotPath = depotPath;
	}

	public void setLine(String line) {
		this.line = line;
	}

	public ClientLineEnd getLineEnd() {
		return lineEnd;
	}

	public void setLineEnd(ClientLineEnd lineEnd) {
		this.lineEnd = lineEnd;
	}

	public int getOrdering() {
		return ordering;
	}

	public FileAnnotation setOrdering(int ordering) {
		this.ordering = ordering;
		return this;
	}
}
