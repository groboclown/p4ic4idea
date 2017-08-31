/**
 * 
 */
package com.perforce.p4java.impl.generic.core.file;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;

/**
 * Conveniently bundles up possible Perforce path-based metadata
 * annotations and associated operations. This includes revision,
 * label, changelist, and date annotations (at least). Most useful
 * for converting to / from string representations associated with
 * string file paths.
 */

public class PathAnnotations {
	
	/**
	 * The string used to start Perforce file revision metadata annotations.
	 */
	public static final String REV_PFX = "#";
	
	/**
	 * The string used to separate Perforce file revision metadata annotations.
	 */
	public static final String REV_SEP = ",#";
	
	/**
	 * The string used to start Perforce file label / changelist / date metadata
	 * annotations.
	 */
	public static final String NONREV_PFX = "@"; // non-revision prefix, i.e.
												 // label, date and changelist prefix...

	protected int startRevision = IFileSpec.NO_FILE_REVISION;
	protected int endRevision = IFileSpec.NO_FILE_REVISION;
	protected Date date = null;
	protected int changelistId = IChangelist.UNKNOWN;
	protected String label = null;
	
	/**
	 * Describes what to search for to see if any Perforce revision annotation metadata
	 * is in the path string.
	 */
	private static Pattern revMetadata = Pattern.compile(
										 "((#)(\\d+)$)"
										+ "|(#none$)"
										+ "|(#head$)"
										+ "|(#have$)"
										+ "|(#have,#head$)"
										+ "|((#)(\\d+),#(\\d+)$)"
										+ "|((#)(\\d+),#head$)"
										+ "|((#)(\\d+),#have$)"
										+ "|(#)have,(\\d+)$"
									);
	
	/**
	 * Describes what to search for to see if any Perforce non-revision annotation metadata
	 * is in the path string.
	 */
	private static Pattern nonRevMetadata = Pattern.compile(
										"((@)(\\d+)$)"
										+ "|((@)(\\d+\\/\\d+\\/\\d+)$)"
										+ "|((@)(\\d+\\/\\d+\\/\\d+:\\d+:\\d+:\\d+)$)"
										+ "|((@)([^@,%#]+)$)"
									);
	
	private static Pattern revNumNum = Pattern.compile("(#)(\\d+),#(\\d+)$");		// #2,#3
	private static Pattern revNumHead = Pattern.compile("(#)(\\d+),#head$");	// #3,#head
	private static Pattern revNumHave = Pattern.compile("(#)(\\d+),#have$");	// #3,#have
	private static Pattern revHaveNum = Pattern.compile("#have,#(\\d+)$");	// #have,#3
	private static Pattern revNum = Pattern.compile("(#)(\\d+)$");				// #3
	private static Pattern revNone = Pattern.compile("#none$");	// #none
	private static Pattern revHead = Pattern.compile("#head$");	// #head
	private static Pattern revHave = Pattern.compile("#have$");	// #have
	private static Pattern revHaveHead = Pattern.compile("#have,#head$");	// #have,#head
	
	private static Pattern nonrevChange = Pattern.compile("(@)(\\d+)$");	// @12345
	private static Pattern nonrevDateShort = Pattern.compile("(@)(\\d+\\/\\d+\\/\\d+)$");	// @yyyy/MM/dd
	private static Pattern nonrevDateFull = Pattern.compile("(@)(\\d+\\/\\d+\\/\\d+:\\d+:\\d+:\\d+)$"); // @yyyy/MM/dd:HH:mm:ss
	private static Pattern nonrevLabel = Pattern.compile("(@)([^@,%#]+)$");	// @labelname
	
	/**
	 * Construct a PathAnnotations object with default field values:<br>
	 * startRev, endRev = IFileSpec.NO_FILE_REVISION;<br>
	 * date, label = null;<br>
	 * changelist = IChangelist.UNKNOWN.
	 */
	public PathAnnotations() {
	}

	/**
	 * Construct a PathAnnotations object from explicit candidate field values.
	 */
	
	public PathAnnotations(int startRevision, int endRevision, Date date,
			int changelistId, String label) {
		this.startRevision = startRevision;
		this.endRevision = endRevision;
		this.date = date;
		this.changelistId = changelistId;
		this.label = label;
	}
	
	/**
	 * Construct a PathAnnotations object for a specific file spec. If fileSpec
	 * is null, this is equivalent to calling the default constructor.
	 */
	
	public PathAnnotations(IFileSpec fileSpec) {
		if (fileSpec != null) {
			this.startRevision = fileSpec.getStartRevision();
			this.endRevision = fileSpec.getEndRevision();
			this.date = fileSpec.getDate();
			this.changelistId = fileSpec.getChangelistId();
			this.label = fileSpec.getLabel();
		}
	}
	
	/**
	 * Construct a PathAnnotations object from the passed-in string,
	 * which is assumed to contain a path string with optional
	 * Perforce annotations. If pathStr is null, or contains no
	 * parseable annotations, this is equivalent to calling the
	 * default constructor.
	 */
	public PathAnnotations(String pathStr) {
		if (pathStr != null) {
			if (hasRevisionAnnotations(pathStr) >= 0) {
				extractRevisionData(pathStr, this);
			} else if (hasNonRevisionAnnotations(pathStr) >= 0) {
				extractNonRevisionData(pathStr, this);
			}
		}
	}
	
	/**
	 * Strip any Perforce file metadata annotations from the passed-in string.
	 * Returns the string as-is if there were no annotations or if the string
	 * was null. Will return the empty (not null) string if there was no actual
	 * path part of the string and pathStr wasn't null.
	 */
	public static String stripAnnotations(String pathStr) {
		if (pathStr != null) {
			int i = -1;
			if ((i = hasRevisionAnnotations(pathStr)) >= 0) {
				return pathStr.substring(0, i);
			} else if ((i = hasNonRevisionAnnotations(pathStr)) >= 0) {
				return pathStr.substring(0, i);
			}
		}
		
		return pathStr;
	}
	
	/**
	 * Get all Perforce metadata annotations from the passed-in string. If
	 * there are no annotations, or the passed-in pathStr argument is null,
	 * returns null.
	 */
	public static PathAnnotations getAnnotations(String pathStr) {
		if ((pathStr != null) && (PathAnnotations.hasPerforceAnnotations(pathStr))) {
			return new PathAnnotations(pathStr);
		}
		
		return null;
	}
	
	/**
	 * Return non-negative index of first non-revision Perforce annotations match in the
	 * string if the passed-in string is not null has Perforce
	 * revision annotations appended to it. Otherwise returns -1.
	 */
	public static int hasRevisionAnnotations(String pathStr) {
		if (pathStr != null) {
			if (pathStr.contains(REV_PFX)) {
				Matcher mat = revMetadata.matcher(pathStr);
				if (mat.find()) {
					return mat.start();
				}
			}
		}
		
		return -1;
	}
	
	/**
	 * Return non-negative index of first non-revision Perforce annotations match in the
	 * string if the passed-in string is not null and has label, changelist, date,
	 * etc., specs appended. Otherwise returns -1.
	 */
	public static int hasNonRevisionAnnotations(String pathStr) {
		if (pathStr != null) {
			if (pathStr.contains(NONREV_PFX)) {
				Matcher mat = nonRevMetadata.matcher(pathStr);
				if (mat.find()) {
					return mat.start();
				}
			}
		}
		
		return -1;
	}
	
	/**
	 * Return true if the passed-in path is not null and has Perforce file
	 * metadata appended to it.
	 */
	public static boolean hasPerforceAnnotations(String pathStr) {
		return (hasRevisionAnnotations(pathStr) >= 0)
							|| (hasNonRevisionAnnotations(pathStr) >= 0);
	}
	
	/**
	 * Extract any start / end revision info from the passed-in pathStr and put it into
	 * the passed-in pathAnnotations object. Returns true if it found any parseable
	 * revision information, false otherwise.<p>
	 * 
	 * If either or both pathStr and pathAnnotations is null, returns false.
	 */
	public static boolean extractRevisionData(String pathStr, PathAnnotations pathAnnotations) {
		/*
		 * #n
		 * #head
		 * #n,#m
		 * #n,#head
		 * #none
		 * #have
		 * #have,#head (is this legal?)
		 */
		
		try {
			// Note: match order is significant below; don't change it unless
			// you're sure you know what you're doing...
	
			if ((pathStr != null) && (pathAnnotations != null)) {
	
				Matcher mat = revNumNum.matcher(pathStr);
				
				if (mat.find()) {
					pathAnnotations.startRevision = new Integer(mat.group(2));
					pathAnnotations.endRevision = new Integer(mat.group(3));
					return true;
				}
				
				mat = revNumHead.matcher(pathStr);
					
				if (mat.find()) {
					pathAnnotations.startRevision = new Integer(mat.group(2));
					pathAnnotations.endRevision = IFileSpec.HEAD_REVISION;
					return true;
				}
				
				mat = revNumHave.matcher(pathStr);
				
				if (mat.find()) {
					pathAnnotations.startRevision = new Integer(mat.group(2));
					pathAnnotations.endRevision = IFileSpec.HAVE_REVISION;
					return true;
				}
				
				mat = revHaveNum.matcher(pathStr);
				
				if (mat.find()) {
					pathAnnotations.startRevision = IFileSpec.HAVE_REVISION;
					pathAnnotations.endRevision = new Integer(mat.group(1));
					return true;
				}
				
				mat = revHaveHead.matcher(pathStr);
				
				if (mat.find()) {
					pathAnnotations.startRevision = IFileSpec.HAVE_REVISION;
					pathAnnotations.endRevision = IFileSpec.HEAD_REVISION;
					return true;
				}
				
				mat = revNum.matcher(pathStr);
	
				if (mat.find()) {
					pathAnnotations.startRevision = IFileSpec.NO_FILE_REVISION;
					pathAnnotations.endRevision = new Integer(mat.group(2));
					return true;
				}
				
				mat = revHead.matcher(pathStr);
	
				if (mat.find()) {
					pathAnnotations.startRevision = IFileSpec.NO_FILE_REVISION;
					pathAnnotations.endRevision = IFileSpec.HEAD_REVISION;
					return true;
				}
				
				mat = revHave.matcher(pathStr);
	
				if (mat.find()) {
					pathAnnotations.startRevision = IFileSpec.NO_FILE_REVISION;
					pathAnnotations.endRevision = IFileSpec.HAVE_REVISION;
					return true;
				}
				
				mat = revNone.matcher(pathStr);
	
				if (mat.find()) {
					pathAnnotations.startRevision = IFileSpec.NO_FILE_REVISION;
					pathAnnotations.endRevision = IFileSpec.NONE_REVISION;
					return true;
				}
			}
		} catch (Throwable thr) {
			Log.error("Unexpected exception in PathAnnotations.extractRevisionData; "
									+ "pathStr: " + pathStr + "; message: "
									+ thr.getLocalizedMessage());
			Log.exception(thr);
		}
		
		return false;
	}
	
	/**
	 * Extract any non-revision info from the passed-in pathStr and put it into
	 * the passed-in pathAnnotations object. Returns true if it found any parseable
	 * label / changelist / date (etc.) information, false otherwise.<p>
	 * 
	 * Note that the candidate string "@2009/09/12" is ambiguous -- it could be
	 * either a date or a label by a generous reading of the Perforce specs -- but
	 * we take the "if it looks like a duck..." approach here and parse it as a date
	 * if at all possible. Similarly for the string "@12345" which could be a changelist
	 * ID or a label ID -- we parse it as a changelist ID if at all possible.<p>
	 * 
	 * If either or both pathStr and pathAnnotations is null, returns false.
	 */
	public static boolean extractNonRevisionData(String pathStr, PathAnnotations pathAnnotations) {
		//				@date:			@2009/08/15
		//				@changelistid:	@34567
		//				@label:			@Label10A
		
		// NOTE: the order of matching below is critical; don't change it unless
		// you know what you're doing and why you're doing it...
		
		if ((pathStr != null) && (pathAnnotations != null)) {
			try {
				Matcher mat = nonrevChange.matcher(pathStr);
				
				if (mat.find()) {
					pathAnnotations.changelistId = new Integer(mat.group(2));
					return true;
				}
				
				mat = nonrevDateShort.matcher(pathStr);
				
				if (mat.find()) {
					pathAnnotations.date = new SimpleDateFormat("yyyy/MM/dd").parse(mat.group(2));// HH:mm:ss					
					return true;
				}
				
				mat = nonrevDateFull.matcher(pathStr);
				
				if (mat.find()) {
					pathAnnotations.date = new SimpleDateFormat("yyyy/MM/dd:HH:mm:ss").parse(mat.group(2));// HH:mm:ss					
					return true;
				}
				
				mat = nonrevLabel.matcher(pathStr);
				
				if (mat.find()) {
					pathAnnotations.label = mat.group(2);
					return true;
				}
			} catch (Exception exc) {
				Log.error("Unexpected parse exception in PathAnnotations.extractNonRevData: "
						+ exc.getLocalizedMessage());
				Log.exception(exc);
			}
		}
		
		return false;
	}
	
	/**
	 * Return a Perforce-standard string representation of this
	 * annotation. Will return an empty (not null) string if there's
	 * nothing to represent.
	 */
	
	public String toString() {
		if ((startRevision != IFileSpec.NO_FILE_REVISION)
					&& (endRevision != IFileSpec.NO_FILE_REVISION)) {
			return REV_PFX + revString(startRevision) + REV_SEP + revString(endRevision);
		} else if (endRevision != IFileSpec.NO_FILE_REVISION) {
			return REV_PFX + revString(endRevision);
		} else if (getChangelistId() != IChangelist.UNKNOWN) {
			return NONREV_PFX + getChangelistId();
		} else if (getLabel() != null) {
			return NONREV_PFX + getLabel();
		} else if (getDate() != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd:HH:mm:ss");
			StringBuffer sBuf = new StringBuffer();
				
			sdf.format(getDate(), sBuf, new FieldPosition(0));
			
			return NONREV_PFX + sBuf.toString();
		}
		
		return "";
	}

	public int getStartRevision() {
		return startRevision;
	}

	public void setStartRevision(int startRev) {
		this.startRevision = startRev;
	}

	public int getEndRevision() {
		return endRevision;
	}

	public void setEndRevision(int endRev) {
		this.endRevision = endRev;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public void setChangelistId(int changelistId) {
		this.changelistId = changelistId;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	private static String revString(int revision) {
		switch (revision) {
			case IFileSpec.NO_FILE_REVISION:
				return IFileSpec.NO_REVISION_STRING;
				
			case IFileSpec.NONE_REVISION:
				return IFileSpec.NONE_REVISION_STRING;
				
			case IFileSpec.HAVE_REVISION:
				return IFileSpec.HAVE_REVISION_STRING;
				
			case IFileSpec.HEAD_REVISION:
				return IFileSpec.HEAD_REVISION_STRING;
				
			default: return "" + revision;
		}
	}
}
