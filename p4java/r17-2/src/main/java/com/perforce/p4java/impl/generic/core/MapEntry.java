/**
 * 
 */
package com.perforce.p4java.impl.generic.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IMapEntry;

/**
 * Default implementation of the IMapEntry interface.
 */

public class MapEntry implements IMapEntry {
	protected static String elementPatternStr = "([^\"]\\S*|\".+?\")\\s*";
	protected static Pattern elementPattern = Pattern.compile(elementPatternStr);

	protected int order = ORDER_UNKNOWN;
	protected EntryType type = null;
	protected String left = null;
	protected String right = null;
	
	/**
	 * Default constructor. Sets all fields to null, and order
	 * to ORDER_UNKNOWN.
	 */
	public MapEntry() {
	}
	
	
	/**
	 * Construct a suitable MapEntry from the passed-in arguments, inferring
	 * the entry type from any suitable prefixes on the passed-in left string.
	 * Left and right strings have any type prefixes stripped from them.
	 */
	public MapEntry(int order, String left, String right) {
		this.order = order;
		if (right != null) {
			this.right = stripTypePrefix(right);
		}
		if (left != null) {
			this.type = EntryType.fromString(left);
			this.left = stripTypePrefix(left);
		}
	}
	
	/**
	 * Explicit-value constructor. Left and right strings have any type
	 * prefixes stripped from them before being assigned to the new entry.
	 */
	public MapEntry(int order, EntryType type, String left, String right) {
		this.order = order;
		this.type = type;
		if (left != null) {
			this.left = stripTypePrefix(left);
		}
		if (right != null) {
			this.right = stripTypePrefix(right);
		}
	}
	
	/**
	 * Attempts to construct a new MapEntry by parsing the passed-in string
	 * into type, left, and right components; assumes that the passed-in string
	 * is in the format specified by parseViewString (below). If the passed-in string
	 * is null, only the order field is set; the other fields are set to null or
	 * ORDER_UNKNOWN.
	 */
	public MapEntry(int order, String mappingStr) {
		this.order = order;
		if (mappingStr != null) {
			String[] entries = parseViewMappingString(mappingStr);
			this.type = EntryType.fromString(entries[0]);
			this.left = stripTypePrefix(entries[0]);
			this.right = entries[1];
		}
	}
	
	/**
	 * Copy constructor. Constructs a new MapEntry from the passed-in version.
	 * If entry is null, this is equivalent to calling the default constructor.
	 */
	public MapEntry(IMapEntry entry) {
		if (entry != null) {
			this.order = entry.getOrder();
			this.type = entry.getType();
			this.left = entry.getLeft();
			this.right = entry.getRight();
		}
	}
	
	/**
	 * @see com.perforce.p4java.core.IMapEntry#getOrder()
	 */
	public int getOrder() {
		return this.order;
	}
	
	/**
	 * NOTE: does not affect actual order in the list on its own...
	 * 
	 * @see com.perforce.p4java.core.IMapEntry#setOrder(int)
	 */
	public void setOrder(int position) {
		this.order = position;
	}

	/**
	 * @see com.perforce.p4java.core.IMapEntry#getType()
	 */
	public EntryType getType() {
		return type;
	}

	/**
	 * @see com.perforce.p4java.core.IMapEntry#setType(com.perforce.p4java.core.IMapEntry.EntryType)
	 */
	public void setType(EntryType type) {
		this.type = type;
	}

	/**
	 * @see com.perforce.p4java.core.IMapEntry#getLeft()
	 */
	public String getLeft() {
		return getLeft(false);
	}
	
	/**
	 * @see com.perforce.p4java.core.IMapEntry#getLeft(boolean)
	 */
	public String getLeft(boolean quoteBlanks) {
		if (quoteBlanks && (left != null) && (left.contains(" ") || left.contains("\t"))) {
			return "\"" + left + "\"";
		}
		return left;
	}

	/**
	 * @see com.perforce.p4java.core.IMapEntry#setLeft(java.lang.String)
	 */
	public void setLeft(String left) {
		this.left = left;
	}

	/**
	 * @see com.perforce.p4java.core.IMapEntry#getRight()
	 */
	public String getRight() {
		return getRight(false);
	}
	
	/**
	 * @see com.perforce.p4java.core.IMapEntry#getRight(boolean)
	 */
	public String getRight(boolean quoteBlanks) {
		if (quoteBlanks && (right != null) && (right.contains(" ") || right.contains("\t"))) {
			return "\"" + right + "\"";
		}
		return right;
	}
	
	/**
	 * @see com.perforce.p4java.core.IMapEntry#setRight(java.lang.String)
	 */
	public void setRight(String right) {
		this.right = right;
	}

	/**
	 * @see com.perforce.p4java.core.IMapEntry#toString(java.lang.String)
	 */
	public String toString(String sepString, boolean quoteBlanks) {
		StringBuilder retVal = new StringBuilder();
		boolean quoteLeft = false;
		boolean quoteRight = false;
		
		if (quoteBlanks) {
			if (this.left != null && this.left.contains(" ")) {
				quoteLeft = true;
			}
			if (this.right != null && this.right.contains(" ")) {
				quoteRight = true;
			}
		}
		if (quoteLeft) retVal.append("\"");
		if (this.type != null && this.type != EntryType.INCLUDE) {
			retVal.append(type);
		}
		if (this.left != null) {
			retVal.append(left);
		}
		if (quoteLeft) retVal.append("\"");

		if ((sepString != null) && (this.right != null)) {
			retVal.append(sepString);
		}
		if (this.right != null) {
			if (quoteRight) retVal.append("\"");
			retVal.append(this.right);
			if (quoteRight)retVal.append("\"");
		}
		return retVal.toString();
	}
	
	/**
	 * An alias for this.toString(" ", true).
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return toString(" ", true);
	}
	
	/**
	 * Strip any Perforce entry type prefix from the passed-in string. If the
	 * string is null, this returns null; if there's no such prefix, the original
	 * string is returned.
	 */
	public static String stripTypePrefix(String str) {
		if (str != null) {
			if (str.startsWith(IMapEntry.EXCLUDE_PREFIX)) {
				return str.substring(IMapEntry.EXCLUDE_PREFIX.length());
			} else if (str.startsWith(IMapEntry.OVERLAY_PREFIX)){
				return str.substring(IMapEntry.OVERLAY_PREFIX.length());
			} else {
				return str;
			}
		}
		
		return null;
	}
	
	/**
	 * Attempt to parse a string to get left and right view mapping
	 * elements out of it along with the optional EntryType spec
	 * on any left view strings.<p>
	 * 
	 * The incoming string format is described semi-formally as follows:
	 * <pre>
	 * [whitespace] leftentry [whitespace rightentry] [whitespace]
	 * where leftentry = ([entrytype] non-whitespace-string) | (quote [entrytype] anystring quote)
	 * and rightentry = (non-whitespace-string) | (quote anystring quote)
	 * </pre>
	 * Even less formally, if a left or right string has embedded spaces in it,
	 * it should be quoted with a double quote character; any left-entry entry
	 * type character must be <i>within</i> the quotes if they exist. The quotes
	 * are always stripped from the associated element before being returned.<p>
	 * 
	 * The left string is returned as the first element of the returned
	 * array; the right (if it exists) is the second. Either or both can
	 * be null, but the array itself will never be null. The left string
	 * will still contain any entry type spec prepended (if it exists),
	 * and will need further processing to get the entry type (and or remove
	 * the entry type character).
	 * 
	 * @param if not null, string to be parsed; if null, this method returns
	 * 			an empty (but not null) array
	 * @return non-null two-element string array; element 0 contains the left
	 * 			element, element 1 contains the right. Either or both can be null,
	 * 			but except in pathological cases, it's unusual for the left to be
	 * 			null and the right to be non-null.
	 */
	public static String[] parseViewMappingString(String str) {
		String[] retVal = new String[] { null, null };
		
		if (str != null) {
			Matcher mat = elementPattern.matcher(str);
			
			int fields = 0;
			while (mat.find()) {
				if (mat.groupCount() > 0) {
					if (mat.group(1) != null) {
						if (mat.group(1).startsWith("\"")) { // strip the quotes
							retVal[fields] = mat.group(1).replaceAll("^\"|\"$", "");
						} else {
							retVal[fields] = mat.group(1);
						}
						fields++;
					}
				}
			}
			if ((fields < 1) || (fields > 3)) {
				Log.warn("Bad view map field count in MapEntry.parseViewString: '"
						+ str + "' (" + fields + ")");
			} 
		}
		
		return retVal;
	}

	/**
	 * Put double quotes around file path with whitespace. If quotes exist,
	 * don't quoted again.
	 * 
	 * @param str with whitespace
	 * @return quoted str with whitespace
	 */
	protected static String quoteWhitespaceString(String str) {
		if (str != null) {
			if (str.contains(" ") || str.contains("\t")) {
				if (!str.startsWith("\"")) {
					str = "\"" + str;
				}
				if (!str.endsWith("\"")) {
					str = str + "\"";
				}
			}
		}
		return str;
	}
}
