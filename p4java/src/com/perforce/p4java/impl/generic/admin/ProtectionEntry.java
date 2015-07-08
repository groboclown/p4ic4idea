/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.admin;

import java.util.Map;

import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.impl.generic.core.MapEntry;

/**
 * Default IProtectionEntry implementation class.
 * <p>
 * 
 * Note that the order of this protection entry in the protections table is part
 * of the protection entry key when pass to the server for updating the
 * protections table.
 * <p>
 * 
 * When exclusionary mappings are used, order is relevant: the exclusionary
 * mapping overrides any matching protections listed above it in the table. No
 * matter what access level is being denied in the exclusionary protection, all
 * the access levels for the matching users, files, and IP addresses are denied.
 * <p>
 * 
 * <pre>
 * Protections0: super user p4java * //depot/...
 * Protections1: write group p4users * //depot/project1/...
 * Protections2: write group p4users * -//depot/project1/build/...
 * Protections3: read user p4jtestuser * //depot/...
 * Protections4: read user p4jtestuser * -//depot/topsecret/...
 * </pre>
 */

public class ProtectionEntry extends MapEntry implements IProtectionEntry {

	/**
	 * The protection mode for this entry. The permission level or right being
	 * granted or denied. Each permission level includes all the permissions
	 * above it, except for 'review'. Each permission only includes the specific
	 * right and no lesser rights. This approach enables you to deny individual
	 * rights without having to re-grant lesser rights. Modes prefixed by '='
	 * are rights. All other modes are permission levels.
	 */
	private String mode = null;
	
	/**
	 * If true, this protection entry applies to a group.
	 */
	private boolean group = false;

	/**
	 * The IP address of a client host; can include wildcards.
	 */
	private String host = null;
	
	/**
	 * A Perforce group or user name; can include wildcards.
	 */
	private String name = null;
	
	/**
	 * Default constructor -- sets all fields to null, zero, or false.
	 */
	public ProtectionEntry() {
		super();
	}
	
	/**
	 * Explicit-value constructor.
	 */
	
	public ProtectionEntry(int order, String mode, boolean group, String host,
			String name, String path, boolean pathExcluded) {

		super(order, null);

		this.mode = mode;
		this.group = group;
		this.host = host;
		this.name = name;
		if (path != null) {
			String[] entries = parseViewMappingString(quoteWhitespaceString(path));
			this.type = EntryType.fromString(entries[0]);
			this.left = stripTypePrefix(entries[0]);
			this.right = entries[1];
		}
		this.left = quoteWhitespaceString(this.left);

		if (pathExcluded) {
			this.type = EntryType.EXCLUDE;
		}
	}
	
	/**
	 * Constructs a ProtectionEntry from the passed-in map; this map
	 * must have come from a Perforce IServer method call or it may fail.
	 * If map is null, equivalent to calling the default constructor.
	 */
	
	public ProtectionEntry(Map<String, Object> map, int order) {
		super(order, null);

		if (map != null) {
			this.host = (String) map.get("host");
			String pathStr = (String) map.get("depotFile");
			if (pathStr != null) {
				String[] entries = parseViewMappingString(quoteWhitespaceString(pathStr));
				this.type = EntryType.fromString(entries[0]);
				this.left = stripTypePrefix(entries[0]);
				this.right = entries[1];
			}
			this.left = quoteWhitespaceString(this.left);
			this.mode = (String) map.get("perm");
			this.name = (String) map.get("user");

			if (map.containsKey("isgroup")) {
				this.group = true;
			} else {
				this.group = false;
			}
			
			if (map.containsKey("unmap")) {
				this.type = EntryType.EXCLUDE;
			}
		}
	}
	
	/**
	 * @see com.perforce.p4java.admin.IProtectionEntry#getHost()
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * @see com.perforce.p4java.admin.IProtectionEntry#getMode()
	 */
	public String getMode() {
		return this.mode;
	}

	/**
	 * @see com.perforce.p4java.admin.IProtectionEntry#getName()
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @see com.perforce.p4java.admin.IProtectionEntry#getPath()
	 */
	public String getPath() {
		if (this.isPathExcluded()) {
			return addExclude(this.left);
		} else if (this.type == EntryType.OVERLAY) {
			return addOverlay(this.left);
		} else {
			return this.left;
		}
	}

	/**
	 * @see com.perforce.p4java.admin.IProtectionEntry#isGroup()
	 */
	public boolean isGroup() {
		return this.group;
	}

	/**
	 * @see com.perforce.p4java.admin.IProtectionEntry#setGroup(boolean)
	 */
	public void setGroup(boolean group) {
		this.group = group;
	}

	/**
	 * @see com.perforce.p4java.admin.IProtectionEntry#setHost(String)
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @see com.perforce.p4java.admin.IProtectionEntry#setMode(String)
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * @see com.perforce.p4java.admin.IProtectionEntry#setName(String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @see com.perforce.p4java.admin.IProtectionEntry#setPath(String)
	 */
	public void setPath(String path) {
		this.left = quoteWhitespaceString(path);
	}

	/**
	 * @see com.perforce.p4java.admin.IProtectionEntry#isPathExcluded()
	 */
	public boolean isPathExcluded() {
		return (this.type == EntryType.EXCLUDE);
	}

	/**
	 * @see com.perforce.p4java.admin.IProtectionEntry#setPathExcluded(boolean)
	 */
	public void setPathExcluded(boolean pathExcluded) {
		if (pathExcluded) {
			this.type = EntryType.EXCLUDE;
		}
	}

	/**
	 * Add exclude ('-') to a string. If it is a double quoted string, add the
	 * exclude immediately after the first double quote char.
	 * 
	 * @param str with quotes
	 * @return exclude in quoted str
	 */
	private String addExclude(String str) {
		if (str != null) {
			if (str.startsWith("\"")) {
				str = "\"-" + str.substring(1);
			} else {
				str = "-" + str;
			}
		}
		return str;
	}
	
	/**
	 * Add overlay ('+') to a string. If it is a double quoted string, add the
	 * overlay immediately after the first double quote char.
	 * 
	 * @param str with quotes
	 * @return overlay in quoted str
	 */
	private String addOverlay(String str) {
		if (str != null) {
			if (str.startsWith("\"")) {
				str = "\"+" + str.substring(1);
			} else {
				str = "+" + str;
			}
		}
		return str;
	}

	/**
	 * Returns string representation of the protection entry.
	 * 
	 * @return the string representation of the protection entry
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (mode != null) {
			sb.append(mode);
		}
		if (isGroup()) {
			sb.append(" ").append("group");
		} else {
			sb.append(" ").append("user");
		}
		if (name != null) {
			sb.append(" ").append(name);
		}
		if (host != null) {
			sb.append(" ").append(host);
		}
		if (left != null) {
			sb.append(" ").append(getPath());
		}
		return sb.toString();
	}
}
