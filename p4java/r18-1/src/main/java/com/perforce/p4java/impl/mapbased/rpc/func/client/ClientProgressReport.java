/*
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;
import com.perforce.p4java.impl.mapbased.rpc.RpcServer;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;
import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.callback.IProgressCallback;

/**
 * Report the progress of the command tick by tick.
 */

public class ClientProgressReport {
	
	public static final String TRACE_PREFIX = "ClientProgressReport";

	private static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
	private static final String DATE_PATTERN2 = "yyyy/MM/dd";

	private static final Map<String, CmdSpec> cmdMap = new HashMap<String, CmdSpec>();
	static {
		cmdMap.put(CmdSpec.FSTAT.toString(), CmdSpec.FSTAT);
		cmdMap.put(CmdSpec.FILES.toString(), CmdSpec.FILES);
		cmdMap.put(CmdSpec.SYNC.toString(), CmdSpec.SYNC);
		cmdMap.put(CmdSpec.JOBS.toString(), CmdSpec.JOBS);
		cmdMap.put(CmdSpec.CHANGES.toString(), CmdSpec.CHANGES);
		cmdMap.put(CmdSpec.USERS.toString(), CmdSpec.USERS);
	}

	protected RpcServer server = null;
	
	public ClientProgressReport(RpcServer server) {
		this.server = server;
	}
	
	/**
	 * Do a quick and dirty probabilistic progress report. Does not try to be
	 * too clever, but it does need to know something about the calling context
	 * to be able to send something useful back. If the tick() callback is
	 * called, this will return whatever the tick call returned, otherwise
	 * it returns true (which is usually interpreted to mean "keep going"...
	 */
	public boolean report(IProgressCallback progressCallback, int cmdCallBackKey,
			RpcFunctionSpec funcSpec, CommandEnv cmdEnv, Map<String, Object> resultsMap) {
		
		if (resultsMap != null) {
			// (We "know" cmdEnv, funcSpec, and progressCallback can't be null (famous last words...)).
			CmdSpec cmdSpec = cmdMap.get(cmdEnv.getCmdSpec().getCmdName());
			
			String tickMarker = getTickMarker(cmdSpec, resultsMap);
			
			switch (funcSpec) {
				case CLIENT_FSTATINFO:
					// Only want to output stuff when it's significant; we can't always
					// get this right, but if we're in an fstat or files command, this is
					// probably the place to output something, even though we can't get
					// a useful path here...
					if (cmdSpec != null) {
						if (cmdMap.containsKey(cmdSpec.toString())) {
							return progressCallback.tick(cmdCallBackKey, tickMarker);
						}
					}
					break;
					
				case CLIENT_OPENFILE:
				case CLIENT_SENDFILE:
				case CLIENT_OUTPUTBINARY:
				case CLIENT_OUTPUTTEXT:
					return progressCallback.tick(cmdCallBackKey, tickMarker);
				case CLIENT_MESSAGE:
					tickMarker = server.getErrorOrInfoStr(resultsMap);
					return progressCallback.tick(cmdCallBackKey, tickMarker);
				default:
					break;
			}
		}
		
		return true;
	}
	
	/**
	 * Struggle mightily to get something useful to send back to the
	 * consumer as a tick marker. This is sometimes difficult because
	 * the Perforce server isn't particularly consistent in map key
	 * name usage, etc....
	 */
	private String getTickMarker(CmdSpec cmdSpec, Map<String, Object> resultsMap) {
		String tickMarker = null;
		if (cmdSpec != null) {
			StringBuilder sb = null;
			switch (cmdSpec) {
			case FSTAT:
				final String[] fstatFields = { RpcFunctionMapKey.DEPOT_FILE,
						RpcFunctionMapKey.CLIENT_FILE, RpcFunctionMapKey.ISMAPPED,
						RpcFunctionMapKey.HEADACTION, RpcFunctionMapKey.HEADTYPE,
						RpcFunctionMapKey.HEADTIME, RpcFunctionMapKey.HEADREV,
						RpcFunctionMapKey.HEADCHANGE, RpcFunctionMapKey.HEADMODTIME,
						RpcFunctionMapKey.HAVEREV };
				sb = new StringBuilder();
				for (String f : fstatFields) {
					if (resultsMap.get(f) != null) {
						if (sb.length() > 0) {
							sb.append(CommandEnv.LINE_SEPARATOR);
						}
						sb.append("... ").append(f).append(" ").append((String)resultsMap.get(f));
					}
				}
				break;

			case FILES:
				sb = new StringBuilder();
				if (resultsMap.get(RpcFunctionMapKey.DEPOT_FILE) != null) {
					sb.append((String)resultsMap.get(RpcFunctionMapKey.DEPOT_FILE));
				}
				if (resultsMap.get(RpcFunctionMapKey.REV) != null) {
					sb.append("#").append((String)resultsMap.get(RpcFunctionMapKey.REV));
				}
				if (resultsMap.get(RpcFunctionMapKey.ACTION) != null) {
					sb.append(" - ").append((String)resultsMap.get(RpcFunctionMapKey.ACTION));
				}
				if (resultsMap.get(RpcFunctionMapKey.CHANGE) != null) {
					sb.append(" change ").append((String)resultsMap.get(RpcFunctionMapKey.CHANGE));
				}
				if (resultsMap.get(RpcFunctionMapKey.TYPE) != null) {
					sb.append(" (").append((String)resultsMap.get(RpcFunctionMapKey.TYPE)).append(")");
				}
				break;

			case SYNC:
				sb = new StringBuilder();
				if (resultsMap.get(RpcFunctionMapKey.TOTALFILESIZE) != null) {
					sb.append("... ").append("totalFileSize ").append((String)resultsMap.get(RpcFunctionMapKey.TOTALFILESIZE));
				}
				if (resultsMap.get(RpcFunctionMapKey.TOTALFILECOUNT) != null) {
					if (sb.length() > 0) {
						sb.append(CommandEnv.LINE_SEPARATOR);
					}
					sb.append("... ").append("totalFileCount ").append((String)resultsMap.get(RpcFunctionMapKey.TOTALFILECOUNT));
				}
				if (sb.length() > 0) {
					sb.append(CommandEnv.LINE_SEPARATOR);
				}
				if (resultsMap.get(RpcFunctionMapKey.DEPOT_FILE) != null) {
					sb.append((String)resultsMap.get(RpcFunctionMapKey.DEPOT_FILE));
				}
				if (resultsMap.get(RpcFunctionMapKey.REV) != null) {
					sb.append("#").append((String)resultsMap.get(RpcFunctionMapKey.REV));
				}
				if (resultsMap.get(RpcFunctionMapKey.ACTION) != null) {
					sb.append(" - ").append((String)resultsMap.get(RpcFunctionMapKey.ACTION));
				}
				if (resultsMap.get(RpcFunctionMapKey.CLIENT_FILE) != null) {
					sb.append(" as ").append((String)resultsMap.get(RpcFunctionMapKey.CLIENT_FILE));
				}
				break;

			case JOBS:
				sb = new StringBuilder();
				if (resultsMap.get(MapKeys.JOB_KEY) != null) {
					sb.append((String)resultsMap.get(MapKeys.JOB_KEY));
				}
				if (resultsMap.get(MapKeys.DATE_KEY) != null) {
					try {
						Date date = parseDate((String)resultsMap.get(MapKeys.DATE_KEY), DATE_PATTERN);
						String dateStr = formatDate(date, DATE_PATTERN2);
						sb.append(" on ").append(dateStr);
					} catch (ParseException e) {
						// Do nothing
					}
				}
				if (resultsMap.get(MapKeys.USER_KEY) != null) {
					sb.append(" by ").append((String)resultsMap.get(MapKeys.USER_KEY));
				}
				if (resultsMap.get(MapKeys.STATUS_KEY) != null) {
					sb.append(" *").append((String)resultsMap.get(MapKeys.STATUS_KEY)).append("*");
				}
				if (resultsMap.get(MapKeys.DESCRIPTION_KEY) != null) {
					sb.append(" '").append((String)resultsMap.get(MapKeys.DESCRIPTION_KEY)).append("'");
				}
				break;

			case CHANGES:
				sb = new StringBuilder();
				if (resultsMap.get(RpcFunctionMapKey.CHANGE) != null) {
					sb.append("Change ").append((String)resultsMap.get(RpcFunctionMapKey.CHANGE));
				}
				if (resultsMap.get(RpcFunctionMapKey.TIME) != null) {
					Date date = new Date(Long.parseLong((String)resultsMap.get(RpcFunctionMapKey.TIME)) * 1000);
					String dateStr = formatDate(date, DATE_PATTERN2);
					sb.append(" on ").append(dateStr);
				}
				if (resultsMap.get(RpcFunctionMapKey.USER) != null) {
					sb.append(" by ").append((String)resultsMap.get(RpcFunctionMapKey.USER));
				}
				if (resultsMap.get(MapKeys.CLIENT_LC_KEY) != null) {
					sb.append("@").append((String)resultsMap.get(MapKeys.CLIENT_LC_KEY));
				}
				if (resultsMap.get(RpcFunctionMapKey.STATUS) != null) {
					if (!((String)resultsMap.get(RpcFunctionMapKey.STATUS)).equalsIgnoreCase("submitted")) {
						sb.append(" *").append((String)resultsMap.get(RpcFunctionMapKey.STATUS)).append("*");
					}
				}
				if (resultsMap.get(MapKeys.DESC_LC_KEY) != null) {
					sb.append(" '").append((String)resultsMap.get(MapKeys.DESC_LC_KEY)).append("'");
				}
				break;

			case USERS:
				sb = new StringBuilder();
				if (resultsMap.get(MapKeys.USER_KEY) != null) {
					sb.append((String)resultsMap.get(MapKeys.USER_KEY));
				}
				if (resultsMap.get(MapKeys.EMAIL_KEY) != null) {
					sb.append(" <").append((String)resultsMap.get(MapKeys.EMAIL_KEY)).append(">");
				}
				if (resultsMap.get(MapKeys.FULLNAME_KEY) != null) {
					sb.append(" (").append((String)resultsMap.get(MapKeys.FULLNAME_KEY)).append(")");
				}
				if (resultsMap.get(MapKeys.ACCESS_KEY) != null) {
					Date date = new Date(Long.parseLong((String)resultsMap.get(MapKeys.ACCESS_KEY)) * 1000);
					String dateStr = formatDate(date, DATE_PATTERN2);
					sb.append(" accessed ").append(dateStr);
				}
				break;

			default:
				break;
			}

			if (sb != null && sb.length() > 0) {
				tickMarker = sb.toString();
			}
		}
		
		return tickMarker;
	}

    /**
     * Parses the date.
     *
     * @param date
     *            the date
     * @param pattern
     *            the pattern
     * @return the date
     * @throws ParseException
     *             the parse exception
     */
    private Date parseDate(String date, String pattern) throws ParseException {
        Date d = null;
        if (date != null) {
            DateFormat df = new SimpleDateFormat(pattern);
            d = df.parse(date);
        }
        return d;
    }

    /**
     * Format date.
     *
     * @param date
     *            the date
     * @param pattern
     *            the pattern
     * @return the string
     */
    private String formatDate(Date date, String pattern) {
        String d = null;
        if (date != null) {
            DateFormat df = new SimpleDateFormat(pattern);
            d = df.format(date);
        }
        return d;
    }
}
