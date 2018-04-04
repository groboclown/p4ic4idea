/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.admin;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.HOST;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.IS_GROUP;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.PERM;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.UNMAP;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.USER;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
  public ProtectionEntry(final int order,
                         final String mode,
                         final boolean group,
                         final String host,
                         final String name,
                         final String path,
                         final boolean pathExcluded) {

    super(order, null);

    this.mode = mode;
    this.group = group;
    this.host = host;
    this.name = name;
    if (isNotBlank(path)) {
      String[] entries = parseViewMappingString(quoteWhitespaceString(path));
      type = EntryType.fromString(entries[0]);
      left = stripTypePrefix(entries[0]);
      right = entries[1];
    }
    left = quoteWhitespaceString(left);

    if (pathExcluded) {
      type = EntryType.EXCLUDE;
    }
  }

  /**
   * Constructs a ProtectionEntry from the passed-in map; this map
   * must have come from a Perforce IServer method call or it may fail.
   * If map is null, equivalent to calling the default constructor.
   */

  public ProtectionEntry(final Map<String, Object> map,
                         final int order) {
    super(order, null);

    if (nonNull(map)) {
      host = parseString(map, HOST);
      String pathStr = parseString(map, DEPOT_FILE);
      if (isNotBlank(pathStr)) {
        String[] entries = parseViewMappingString(quoteWhitespaceString(pathStr));
        type = EntryType.fromString(entries[0]);
        left = stripTypePrefix(entries[0]);
        right = entries[1];
      }
      left = quoteWhitespaceString(left);
      mode = parseString(map, PERM);
      name = parseString(map, USER);

      group = map.containsKey(IS_GROUP);
      if (map.containsKey(UNMAP)) {
        type = EntryType.EXCLUDE;
      }
    }
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override

  public String getMode() {
    return mode;
  }

  @Override

  public String getName() {
    return name;
  }

  @Override

  public String getPath() {
    if (isPathExcluded()) {
      return addExclude(left);
    } else if (type == EntryType.OVERLAY) {
      return addOverlay(left);
    } else {
      return left;
    }
  }

  @Override

  public boolean isGroup() {
    return this.group;
  }

  @Override

  public void setGroup(boolean group) {
    this.group = group;
  }

  @Override

  public void setHost(String host) {
    this.host = host;
  }

  @Override

  public void setMode(String mode) {
    this.mode = mode;
  }

  @Override

  public void setName(String name) {
    this.name = name;
  }

  @Override

  public void setPath(String path) {
    left = quoteWhitespaceString(path);
  }

  @Override

  public boolean isPathExcluded() {
    return type == EntryType.EXCLUDE;
  }

  @Override

  public void setPathExcluded(boolean pathExcluded) {
    if (pathExcluded) {
      type = EntryType.EXCLUDE;
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
    return buildDiffSyntaxString(str, "-");
  }

  /**
   * Add overlay ('+') to a string. If it is a double quoted string, add the
   * overlay immediately after the first double quote char.
   *
   * @param str with quotes
   * @return overlay in quoted str
   */
  private String addOverlay(String str) {
    return buildDiffSyntaxString(str, "+");
  }

  private String buildDiffSyntaxString(String str, String syntax) {
    if (isNotBlank(str)) {
      if (str.startsWith("\"")) {
        str = "\"" + syntax + str.substring(1);
      } else {
        str = syntax + str;
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
    if (isNotBlank(mode)) {
      sb.append(mode);
    }
    if (isGroup()) {
      sb.append(SPACE).append("group");
    } else {
      sb.append(SPACE).append(USER);
    }
    if (isNotBlank(name)) {
      sb.append(SPACE).append(name);
    }
    if (isNotBlank(host)) {
      sb.append(SPACE).append(host);
    }
    if (isNotBlank(left)) {
      sb.append(SPACE).append(getPath());
    }
    return sb.toString();
  }
}
