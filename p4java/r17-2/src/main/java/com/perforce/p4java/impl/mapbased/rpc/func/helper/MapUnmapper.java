/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.helper;

import com.perforce.p4java.impl.mapbased.MapKeys;

import java.util.Map;

/**
 * Provides unmapping services to the P4Java RPC implementation.
 * These are not what they probably sound like -- they're basically
 * a way to serialise an input map for something like a changelist
 * or job onto a single byte buffer to be sent to the server as a
 * single data argument with newlines, tabs, etc.<p>
 * 
 * The need for this will probably go away when we refactor the upper
 * levels of P4Java to optimise and rationalise the use of maps overall.
 * 
 *
 */

public class MapUnmapper {

	/**
	 * Unmap a change list. Absolutely no sanity or other checks are done on
	 * the passed-in map...
	 * 
	 * @param inMap 
	 * @param strBuf 
	 */
	
	public static void unmapChangelistMap(Map<String, Object> inMap,
												StringBuffer strBuf) {
		if ((inMap != null) && (strBuf != null)) {
			strBuf.append(MapKeys.CHANGE_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.CHANGE_KEY) + MapKeys.DOUBLE_LF);
			
			Object client = inMap.get(MapKeys.CLIENT_KEY);
			if (client != null) {
				strBuf.append(MapKeys.CLIENT_KEY + MapKeys.COLON_SPACE + client + MapKeys.DOUBLE_LF);
			}
			Object user = inMap.get(MapKeys.USER_KEY);
			if (user != null) {
				strBuf.append(MapKeys.USER_KEY + MapKeys.COLON_SPACE + user + MapKeys.DOUBLE_LF);
			}
			Object type = inMap.get(MapKeys.TYPE_KEY);
			if (type != null) {
				strBuf.append(MapKeys.TYPE_KEY + MapKeys.COLON_SPACE + type + MapKeys.DOUBLE_LF);
			}
			Object status = inMap.get(MapKeys.STATUS_KEY);
			if (status != null) {
				strBuf.append(MapKeys.STATUS_KEY + MapKeys.COLON_SPACE + status + MapKeys.DOUBLE_LF);
			}
			Object date = inMap.get(MapKeys.DATE_KEY);
			if (date != null) {
				strBuf.append(MapKeys.DATE_KEY + MapKeys.COLON_SPACE + date + MapKeys.DOUBLE_LF);
			}
			
			String descr =  replaceNewlines((String) inMap.get(MapKeys.DESCRIPTION_KEY));
			strBuf.append(MapKeys.DESCRIPTION_KEY + MapKeys.COLON_LF + (descr == null ? MapKeys.EMPTY : descr) + MapKeys.LF);
			strBuf.append(MapKeys.FILES_KEY + MapKeys.COLON_LF);

			for (int i = 0; ; i++) {
				String fileStr = (String) inMap.get(MapKeys.FILES_KEY + i);
				
				if (fileStr != null) {
					strBuf.append(MapKeys.TAB + inMap.get(MapKeys.FILES_KEY + i) + MapKeys.LF);
				} else {
					break;
				}
			}
			
			strBuf.append(MapKeys.LF);

			for (int i = 0; ; i++) {
				String fileStr = (String) inMap.get(MapKeys.JOBS_KEY + i);
				
				if (fileStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.JOBS_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + inMap.get(MapKeys.JOBS_KEY + i) + MapKeys.LF);
				} else {
					break;
				}
			}
			
			strBuf.append(MapKeys.LF);
		}
	}
	
	/**
	 * Unmap a job. Jobs basically have free format defined by the associated
	 * jobspec (which we don't have access to here), so we have to try to the
	 * best we can with what we've got -- which is to dump the map to the strbuf
	 * while guessing at things like string formats, etc. This may prove error-prone
	 * in the long run.
	 * 
	 * @param inMap 
	 * @param strBuf 
	 */
	public static void unmapJobMap(Map<String, Object> inMap,
									StringBuffer strBuf) {
		if ((inMap != null) && (strBuf != null)) {
			for (Map.Entry<String, Object> entry : inMap.entrySet()) {
				strBuf.append(entry.getKey() + MapKeys.COLON_SPACE + replaceNewlines((String) entry.getValue()) + MapKeys.DOUBLE_LF);
			}
		}
	}
	
	/**
	 * Unmap a client map. Similar in intent and execution to unmapJobMap.
	 * 
	 * @param inMap 
	 * @param strBuf 
	 */
	public static void unmapClientMap(Map<String, Object> inMap,
									StringBuffer strBuf) {
		if ((inMap != null) && (strBuf != null)) {
			strBuf.append(MapKeys.CLIENT_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.CLIENT_KEY) + MapKeys.DOUBLE_LF);
			
			Object owner = inMap.get(MapKeys.OWNER_KEY);
			if (owner != null) {
				strBuf.append(MapKeys.OWNER_KEY + MapKeys.COLON_SPACE + owner + MapKeys.DOUBLE_LF);
			}
			
			// Fix for job036074, a null Host should not be turned into the
			// "null" string but should be omitted from the map string.
			Object host = inMap.get(MapKeys.HOST_KEY);
			if( host != null) {
			    strBuf.append(MapKeys.HOST_KEY + MapKeys.COLON_SPACE + host.toString() + MapKeys.DOUBLE_LF);
			}
			
			if (inMap.containsKey(MapKeys.UPDATE_KEY)) {
				strBuf.append(MapKeys.UPDATE_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.UPDATE_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.ACCESS_KEY)) {
				strBuf.append(MapKeys.ACCESS_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.ACCESS_KEY) + MapKeys.DOUBLE_LF);
			}
			
			if (inMap.containsKey(MapKeys.OPTIONS_KEY)) {
				strBuf.append(MapKeys.OPTIONS_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.OPTIONS_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.SUBMITOPTIONS_KEY)) {
				strBuf.append(MapKeys.SUBMITOPTIONS_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.SUBMITOPTIONS_KEY) + MapKeys.DOUBLE_LF);
			}
			strBuf.append(MapKeys.ROOT_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.ROOT_KEY) + MapKeys.DOUBLE_LF);
			if (inMap.containsKey(MapKeys.LINEEND_KEY)) {
				strBuf.append(MapKeys.LINEEND_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.LINEEND_KEY) + MapKeys.DOUBLE_LF);
			}
			String descr = replaceNewlines((String) inMap.get(MapKeys.DESCRIPTION_KEY));
			strBuf.append(MapKeys.DESCRIPTION_KEY + MapKeys.COLON_LF + (descr == null ? MapKeys.EMPTY : descr) + MapKeys.LF);
			strBuf.append(MapKeys.VIEW_KEY + MapKeys.COLON_LF);

			for (int i = 0; ; i++) {
				String fileStr = (String) inMap.get(MapKeys.VIEW_KEY + i);
				
				if (fileStr != null) {
					strBuf.append(MapKeys.TAB + inMap.get(MapKeys.VIEW_KEY + i) + MapKeys.LF);
				} else {
					break;
				}
			}
			
			strBuf.append(MapKeys.LF);

			for (int i = 0; ; i++) {
				String fileStr = (String) inMap.get(MapKeys.ALTROOTS_KEY + i);
				
				if (fileStr != null) {
					if (i == 0 ) {
						strBuf.append(MapKeys.ALTROOTS_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + inMap.get(MapKeys.ALTROOTS_KEY + i) + MapKeys.LF);
				} else {
					break;
				}
			}
			
			strBuf.append(MapKeys.LF);

			if (inMap.containsKey(MapKeys.STREAM_KEY)) {
				strBuf.append(MapKeys.STREAM_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.STREAM_KEY) + MapKeys.DOUBLE_LF);
			}
			
			if (inMap.containsKey(MapKeys.SERVERID_KEY)) {
				strBuf.append(MapKeys.SERVERID_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.SERVERID_KEY) + MapKeys.DOUBLE_LF);
			}

			if (inMap.containsKey(MapKeys.STREAMATCHANGE_KEY)) {
				strBuf.append(MapKeys.STREAMATCHANGE_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.STREAMATCHANGE_KEY) + MapKeys.DOUBLE_LF);
			}

			if (inMap.containsKey(MapKeys.TYPE_KEY)) {
				strBuf.append(MapKeys.TYPE_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.TYPE_KEY) + MapKeys.DOUBLE_LF);
			}
		}
	}
	
	/**
	 * Unmap a Perforce user map.
	 * 
	 * @param inMap 
	 * @param strBuf 
	 */
	public static void unmapUserMap(Map<String, Object> inMap,
			StringBuffer strBuf) {
		if ((inMap != null) && (strBuf != null)) {
			strBuf.append(MapKeys.USER_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.USER_KEY) + MapKeys.DOUBLE_LF);
			if (inMap.containsKey(MapKeys.EMAIL_KEY)) {
				strBuf.append(MapKeys.EMAIL_KEY + MapKeys.COLON_SPACE
									+ inMap.get(MapKeys.EMAIL_KEY) + MapKeys.DOUBLE_LF);
			}		
			if (inMap.containsKey(MapKeys.FULLNAME_KEY)) {
				strBuf.append(MapKeys.FULLNAME_KEY + MapKeys.COLON_SPACE
									+ inMap.get(MapKeys.FULLNAME_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.JOBVIEW_KEY)) {
				strBuf.append(MapKeys.JOBVIEW_KEY + MapKeys.COLON_SPACE
									+ inMap.get(MapKeys.JOBVIEW_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.PASSWORD_KEY)) {
				strBuf.append(MapKeys.PASSWORD_KEY + MapKeys.COLON_SPACE
									+ inMap.get(MapKeys.PASSWORD_KEY) + MapKeys.DOUBLE_LF);
			}
			
			if (inMap.containsKey(MapKeys.TYPE_KEY)) {
				strBuf.append(MapKeys.TYPE_KEY + MapKeys.COLON_SPACE
									+ inMap.get(MapKeys.TYPE_KEY) + MapKeys.DOUBLE_LF);
			}

			for (int i = 0; ; i++) {
				String mapStr = (String) inMap.get(MapKeys.REVIEWS_KEY + i);
				
				if (mapStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.REVIEWS_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + mapStr + MapKeys.LF);
				} else {
					break;
				}
			}
		}
	}
	
	/**
	 * Unmap a Perforce user group map.
	 * 
	 * @param inMap 
	 * @param strBuf 
	 */
	public static void unmapUserGroupMap(Map<String, Object> inMap,
							StringBuffer strBuf) {
		if ((inMap != null) && (strBuf != null)) {
			strBuf.append(MapKeys.GROUP_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.GROUP_KEY) + MapKeys.DOUBLE_LF);
			if (inMap.containsKey(MapKeys.MAXRESULTS_KEY)) {
				strBuf.append(MapKeys.MAXRESULTS_KEY + MapKeys.COLON_SPACE
									+ inMap.get(MapKeys.MAXRESULTS_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.MAXSCANROWS_KEY)) {
				strBuf.append(MapKeys.MAXSCANROWS_KEY + MapKeys.COLON_SPACE
									+ inMap.get(MapKeys.MAXSCANROWS_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.MAXLOCKTIME_KEY)) {
				strBuf.append(MapKeys.MAXLOCKTIME_KEY + MapKeys.COLON_SPACE
									+ inMap.get(MapKeys.MAXLOCKTIME_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.TIMEOUT_KEY)) {
				strBuf.append(MapKeys.TIMEOUT_KEY + MapKeys.COLON_SPACE
									+ inMap.get(MapKeys.TIMEOUT_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.PASSWORD_TIMEOUT_KEY)) {
				strBuf.append(MapKeys.PASSWORD_TIMEOUT_KEY + MapKeys.COLON_SPACE
									+ inMap.get(MapKeys.PASSWORD_TIMEOUT_KEY) + MapKeys.DOUBLE_LF);
			}

			for (int i = 0; ; i++) {
				String mapStr = (String) inMap.get(MapKeys.SUBGROUPS_KEY + i);
				
				if (mapStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.SUBGROUPS_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + mapStr + MapKeys.LF);
				} else {
					break;
				}
			}
			for (int i = 0; ; i++) {
				String mapStr = (String) inMap.get(MapKeys.OWNERS_KEY + i);
				
				if (mapStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.OWNERS_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + mapStr + MapKeys.LF);
				} else {
					break;
				}
			}
			for (int i = 0; ; i++) {
				String mapStr = (String) inMap.get(MapKeys.USERS_KEY + i);
				
				if (mapStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.USERS_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + mapStr + MapKeys.LF);
				} else {
					break;
				}
			}
		}
	}
	
	/**
	 * Unmap a Label Perforce label.
	 * 
	 * @param inMap 
	 * @param strBuf 
	 */
	public static void unmapLabelMap(Map<String, Object> inMap,
									StringBuffer strBuf) {
		if ((inMap != null) && (strBuf != null)) {
			strBuf.append(MapKeys.LABEL_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.LABEL_KEY) + MapKeys.DOUBLE_LF);
			strBuf.append(MapKeys.OWNER_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.OWNER_KEY) + MapKeys.DOUBLE_LF);
			if (inMap.containsKey(MapKeys.UPDATE_KEY)) {
				strBuf.append(MapKeys.UPDATE_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.UPDATE_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.ACCESS_KEY)) {
				strBuf.append(MapKeys.ACCESS_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.ACCESS_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.REVISION_KEY)) {
				strBuf.append(MapKeys.REVISION_KEY + MapKeys.COLON_SPACE
									+ inMap.get(MapKeys.REVISION_KEY) + MapKeys.DOUBLE_LF);
			}
			String descr =  replaceNewlines((String) inMap.get(MapKeys.DESCRIPTION_KEY));
			strBuf.append(MapKeys.DESCRIPTION_KEY + MapKeys.COLON_LF + (descr == null ? MapKeys.EMPTY : descr) + MapKeys.LF);
			if (inMap.containsKey(MapKeys.OPTIONS_KEY)) {
				strBuf.append(MapKeys.OPTIONS_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.OPTIONS_KEY) + MapKeys.DOUBLE_LF);
			}

			for (int i = 0; ; i++) {
				String mapStr = (String) inMap.get(MapKeys.VIEW_KEY + i);
				
				if (mapStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.VIEW_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + mapStr + MapKeys.LF);
				} else {
					break;
				}
			}
		}
	}
	
	/**
	 * Unmap a BranchSpec spec.
	 * 
	 * @param inMap 
	 * @param strBuf 
	 */
	public static void unmapBranchMap(Map<String, Object> inMap,
											StringBuffer strBuf) {
		if ((inMap != null) && (strBuf != null)) {
			strBuf.append(MapKeys.BRANCH_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.BRANCH_KEY) + MapKeys.DOUBLE_LF);
			
			if (inMap.containsKey(MapKeys.OWNER_KEY)) {
				strBuf.append(MapKeys.OWNER_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.OWNER_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.UPDATE_KEY)) {
				strBuf.append(MapKeys.UPDATE_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.UPDATE_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.ACCESS_KEY)) {
				strBuf.append(MapKeys.ACCESS_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.ACCESS_KEY) + MapKeys.DOUBLE_LF);
			}
			String descr =  replaceNewlines((String) inMap.get(MapKeys.DESCRIPTION_KEY));
			strBuf.append(MapKeys.DESCRIPTION_KEY + MapKeys.COLON_LF + (descr == null ? MapKeys.EMPTY : descr) + MapKeys.LF);
			if( inMap.containsKey(MapKeys.OPTIONS_KEY) ) {
				strBuf.append(MapKeys.OPTIONS_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.OPTIONS_KEY) + MapKeys.DOUBLE_LF);
			}

			for (int i = 0; ; i++) {
				String mapStr = (String) inMap.get(MapKeys.VIEW_KEY + i);
				
				if (mapStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.VIEW_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + mapStr + MapKeys.LF);
				} else {
					break;
				}
			}
		}
	}
	
	/**
	 * Unmap a depot map.
	 * 
	 * @param inMap 
	 * @param strBuf 
	 */
	public static void unmapDepotMap(Map<String, Object> inMap,
											StringBuffer strBuf) {
		if ((inMap != null) && (strBuf != null)) {
			strBuf.append(MapKeys.DEPOT_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.DEPOT_KEY) + MapKeys.DOUBLE_LF);
			if (inMap.containsKey(MapKeys.OWNER_KEY)) {
				strBuf.append(MapKeys.OWNER_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.OWNER_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.DATE_KEY)) {
				strBuf.append(MapKeys.DATE_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.DATE_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.TYPE_KEY)) {
				strBuf.append(MapKeys.TYPE_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.TYPE_KEY) + MapKeys.DOUBLE_LF);
			}
			String descr = replaceNewlines((String) inMap.get(MapKeys.DESCRIPTION_KEY));
			strBuf.append(MapKeys.DESCRIPTION_KEY + MapKeys.COLON_LF + (descr == null ? MapKeys.EMPTY : descr) + MapKeys.LF);

			if (inMap.containsKey(MapKeys.ADDRESS_KEY)) {
				strBuf.append(MapKeys.ADDRESS_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.ADDRESS_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.SUFFIX_KEY)) {
				strBuf.append(MapKeys.SUFFIX_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.SUFFIX_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.STREAM_DEPTH)) {
				strBuf.append(MapKeys.STREAM_DEPTH + MapKeys.COLON_SPACE + inMap.get(MapKeys.STREAM_DEPTH) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.MAP_KEY)) {
				strBuf.append(MapKeys.MAP_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.MAP_KEY) + MapKeys.DOUBLE_LF);
			}

			for (int i = 0; ; i++) {
				String mapStr = (String) inMap.get(MapKeys.SPEC_MAP_KEY + i);
				
				if (mapStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.SPEC_MAP_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + mapStr + MapKeys.LF);
				} else {
					break;
				}
			}
		}
	}
	
	/**
	 * Unmap a list of protection entries.
	 * 
	 * @param inMap 
	 * @param strBuf 
	 */
	public static void unmapProtectionEntriesMap(Map<String, Object> inMap,
														StringBuffer strBuf) {
		if ((inMap != null) && (strBuf != null)) {
			for (int i = 0; ; i++) {
				String mapStr = (String) inMap.get(MapKeys.PROTECTIONS_KEY + i);
				
				if (mapStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.PROTECTIONS_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + mapStr + MapKeys.LF);
				} else {
					break;
				}
			}
		}
	}

	/**
	 * Unmap a stream map.
	 * 
	 * @param inMap 
	 * @param strBuf 
	 */
	public static void unmapStreamMap(Map<String, Object> inMap,
											StringBuffer strBuf) {
		if ((inMap != null) && (strBuf != null)) {
			if (inMap.containsKey(MapKeys.STREAM_KEY)) {
				strBuf.append(MapKeys.STREAM_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.STREAM_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.TYPE_KEY)) {
				strBuf.append(MapKeys.TYPE_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.TYPE_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.PARENT_KEY)) {
				strBuf.append(MapKeys.PARENT_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.PARENT_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.NAME_KEY)) {
				strBuf.append(MapKeys.NAME_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.NAME_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.OWNER_KEY)) {
				strBuf.append(MapKeys.OWNER_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.OWNER_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.UPDATE_KEY)) {
				strBuf.append(MapKeys.UPDATE_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.UPDATE_KEY) + MapKeys.DOUBLE_LF);
			}
			if (inMap.containsKey(MapKeys.ACCESS_KEY)) {
				strBuf.append(MapKeys.ACCESS_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.ACCESS_KEY) + MapKeys.DOUBLE_LF);
			}
			String descr =  replaceNewlines((String) inMap.get(MapKeys.DESCRIPTION_KEY));
			strBuf.append(MapKeys.DESCRIPTION_KEY + MapKeys.COLON_LF + (descr == null ? MapKeys.EMPTY : descr) + MapKeys.LF);
			if( inMap.containsKey(MapKeys.OPTIONS_KEY) ) {
				strBuf.append(MapKeys.OPTIONS_KEY + MapKeys.COLON_SPACE + inMap.get(MapKeys.OPTIONS_KEY) + MapKeys.DOUBLE_LF);
			}

			for (int i = 0; ; i++) {
				String mapStr = (String) inMap.get(MapKeys.PATHS_KEY + i);
				
				if (mapStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.PATHS_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + mapStr + MapKeys.LF);
				} else {
					break;
				}
			}
			for (int i = 0; ; i++) {
				String mapStr = (String) inMap.get(MapKeys.REMAPPED_KEY + i);
				
				if (mapStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.REMAPPED_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + mapStr + MapKeys.LF);
				} else {
					break;
				}
			}
			for (int i = 0; ; i++) {
				String mapStr = (String) inMap.get(MapKeys.IGNORED_KEY + i);
				
				if (mapStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.IGNORED_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + mapStr + MapKeys.LF);
				} else {
					break;
				}
			}

		}
	}

	/**
	 * Unmap a list of trigger entries.
	 * 
	 * @param inMap 
	 * @param strBuf 
	 */
	public static void unmapTriggerEntriesMap(Map<String, Object> inMap,
														StringBuffer strBuf) {
		if ((inMap != null) && (strBuf != null)) {
			for (int i = 0; ; i++) {
				String mapStr = (String) inMap.get(MapKeys.TRIGGERS_KEY + i);
				
				if (mapStr != null) {
					if (i == 0) {
						strBuf.append(MapKeys.TRIGGERS_KEY + MapKeys.COLON_LF);
					}
					strBuf.append(MapKeys.TAB + mapStr + MapKeys.LF);
				} else {
					break;
				}
			}
		}
	}

	/**
	 * Replace all but the last newline in the incoming string with
	 * newline / tab pairs. Useful for various multi-line form inputs.
	 * 
	 * @param str 
	 * @return replaced string 
	 */
	public static String replaceNewlines(String str) {
		if (str != null) {
			String[] strs = str.split(MapKeys.LF);
			
			if (strs.length == 1) {
				return MapKeys.TAB + str + MapKeys.LF;
			} else {
				StringBuilder retStr = new StringBuilder();
				
				for (String s : strs) {
					retStr.append(MapKeys.TAB);
					retStr.append(s);
					retStr.append(MapKeys.LF);
				}
				
				return retStr.toString();
			}
		}
		
		return null;
	}
}
