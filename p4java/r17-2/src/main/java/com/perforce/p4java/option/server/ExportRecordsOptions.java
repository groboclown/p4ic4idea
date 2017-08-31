/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.option.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer.getExportRecords method. Please see the
 * relevant Perforce admin help for details of the meaning and usage of the
 * options defined here -- this method is not intended for general use. Note
 * also some special-casing in the options processing for this class.<p>
 * 
 * The 'skip*' options are specific to P4Java only; they are not Perforce
 * command options. These options are for field handling rules in the lower
 * layers of P4Java. The field rules are for identifying the fields that should
 * skip charset translation of their values; leaving their values as bytes
 * instead of converting them to strings.<p>
 * 
 * These 'skip*' options (if any) are placed into the command 'input map' and
 * carried downstream to the lower layer of P4Java for field rule processing.<p>
 * 
 * If you choose to use the IOptionsServer.execStreamingMapCommand method, you
 * would place those 'skip*' options manually into an 'input map' and pass the
 * map to the execStreamingMapCommand method. Use 'startField' and 'stopField'
 * as map keys for the 'field range' rule; and use 'fieldPattern' as map key for
 * the 'field pattern' rule. See examples (code snippets) below.<p>
 * 
 * <pre>
 * HashMap<String, Object> inMap = new HashMap<String, Object>();
 * Map<String, Object> skipParams = new HashMap<String, Object>();
 * skipParams.put("startField", "op");
 * skipParams.put("stopField", "func");
 * inMap.put(CmdSpec.EXPORT.toString(), skipParams);
 * server.execStreamingMapCommand("export", new String[] { "-l100000",
 *               "-j155", "-Ftable=db.traits" }, inMap, handler, key);
 * 
 * <pre>
 * HashMap<String, Object> inMap = new HashMap<String, Object>();
 * Map<String, Object> skipParams = new HashMap<String, Object>();
 * skipParams.put("fieldPattern", "^[A-Z]{2}\\w+");
 * inMap.put(CmdSpec.EXPORT.toString(), skipParams);
 * server.execStreamingMapCommand("export", new String[] { "-l100000",
 *               "-j155", "-Ftable=db.traits" }, inMap, handler, key);
 * </pre>
 * 
 * Currently, there are two implemented field rules for skipping charset
 * translations. Only one rule can be activated at once. To turn on the rules
 * you would set the 'skipDataConversion' option to true. Note that the rule
 * creation will be processed in the order listed below.<p>
 * 
 * The 'field pattern' rule defines a regex pattern matching the fields to be
 * skipped. To use this rule you would need to set the 'skipFieldPattern'
 * option.<p>
 * 
 * The 'field range' rule defines a range of fields to be skipped, with a start
 * field (inclusive) and a stop field (non-inclusive). To use this rule you
 * would set the 'skipStartField' and 'skipStopField' options.
 */
public class ExportRecordsOptions extends Options {

	/**
	 * Options:
	 */
	public static final String OPTIONS_SPECS = "";

	/**
	 * If true, specifies a journal number and optional offset position (journal
	 * number/offset) from which to start exporting. Corresponds to the '-j
	 * token' flag.<p>
	 * 
	 * If false, specifies a checkpoint number and optional offset position
	 * (checkpoint number#offset) from which to start exporting. Corresponds to
	 * the '-c token' flag.
	 */
	protected boolean useJournal = false;

	/**
	 * If greater than zero, limits the number of lines (records) exported.
	 * Corresponds to the '-l lines' flag.
	 */
	protected long maxRecs = 0;

	/**
	 * If positive, specifies a journal or checkpoint number. Corresponds to the
	 * 'token' part of the '-j token' and '-c token' flags.<p>
	 * 
	 * The '-j token' flag specifies a journal number and optional position (in
	 * the form: journal number/offset) from which to start exporting. The -c
	 * token flag specifies a checkpoint number and optional position (in the
	 * form: checkpoint number#offset) from which to start exporting.
	 */
	protected int sourceNum = 0;

	/**
	 * If positive, specifies a journal or checkpoint optional offset position
	 * (journal number/offset or checkpoint number#offset) from which to start
	 * exporting.
	 */
	protected long offset = 0;

	/**
	 * If true, formats non-textual datatypes appropriately. Corresponds to the
	 * '-f' flag.
	 */
	protected boolean format = false;

	/**
	 * If non-null, specifies a file name prefix to match the one used with 'p4d
	 * -jc <prefix>'. Corresponds to the '-J' flag.
	 */
	protected String journalPrefix = null;

	/**
	 * If non-null, limits output to records that match the filter pattern.
	 * Corresponds to the '-F' flag.
	 */
	protected String filter = null;

	/**
	 * This is not a Perforce command option. If true, it will signal the deeper
	 * level logic to skip the charset conversion of data fields; leave the
	 * values as bytes.
	 * <p>
	 * 
	 * Note: by default the field values affected will be between the start
	 * field "op" (non inclusive) and the end field "func" (non inclusive).
	 */
	protected boolean skipDataConversion = false;

	/**
	 * This is not a Perforce command option. If non-null, it is the start field
	 * (inclusive) marking the beginning of a series of fields which the field
	 * values (bytes) will not be converted to strings.
	 * <p>
	 * 
	 * Note: don't change this unless you know what you're doing.
	 */
	protected String skipStartField = "op";

	/**
	 * This is not a Perforce command option. If non-null, it is the stop field
	 * (non-inclusive) marking the end of a series of fields which the field
	 * values (bytes) will not be converted to strings.
	 * <p>
	 * 
	 * Note: don't change this unless you know what you're doing.
	 */
	protected String skipStopField = "func";

	/**
	 * The regex pattern for matching fields which the field values (bytes) will
	 * not be converted to strings.
	 * <p>
	 * 
	 * Note: don't set this value unless you know what you're doing.
	 */
	protected String skipFieldPattern = null;

	/**
	 * Default constructor.
	 */
	public ExportRecordsOptions() {
		super();
	}

	/**
	 * Strings-based constructor; see 'p4 help [command]' for possible options.
	 * <p>
	 * 
	 * <b>WARNING: you should not pass more than one option or argument in each
	 * string parameter. Each option or argument should be passed-in as its own
	 * separate string parameter, without any spaces between the option and the
	 * option value (if any).<b>
	 * <p>
	 * 
	 * <b>NOTE: setting options this way always bypasses the internal options
	 * values, and getter methods against the individual values corresponding to
	 * the strings passed in to this constructor will not normally reflect the
	 * string's setting. Do not use this constructor unless you know what you're
	 * doing and / or you do not also use the field getters and setters.</b>
	 * 
	 * @see com.perforce.p4java.option.Options#Options(java.lang.String...)
	 */
	public ExportRecordsOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 * 
	 * @param useJournal
	 *            the use journal
	 * @param maxRecs
	 *            the max recs
	 * @param sourceNum
	 *            the source num
	 * @param offset
	 *            the offset
	 * @param format
	 *            the format
	 * @param journalPrefix
	 *            the journal prefix
	 * @param filter
	 *            the filter
	 */
	public ExportRecordsOptions(boolean useJournal, long maxRecs,
			int sourceNum, long offset, boolean format, String journalPrefix,
			String filter) {
		super();
		this.useJournal = useJournal;
		this.maxRecs = maxRecs;
		this.sourceNum = sourceNum;
		this.offset = offset;
		this.format = format;
		this.journalPrefix = journalPrefix;
		this.filter = filter;
	}

	/**
	 * Process options.
	 * 
	 * @param server
	 *            the server
	 * @return the list
	 * @throws OptionsException
	 *             the options exception
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		List<String> args = new ArrayList<String>();

		if (maxRecs > 0)
			args.add("-l" + maxRecs);
		if (sourceNum >= 0)
			args.add((useJournal ? "-j" : "-c") + sourceNum
					+ (offset >= 0 ? (useJournal ? "/" : "#") + offset : ""));
		if (journalPrefix != null)
			args.add("-J" + journalPrefix);
		if (format)
			args.add("-f");
		if (filter != null)
			args.add("-F" + filter);

		return args;
	}

	/**
	 * Process the field rules and return an input map for field handling rules
	 * in the deeper layers of P4Java.
	 * 
	 * @return map of field handling rules
	 */
	public Map<String, Object> processFieldRules() {

		HashMap<String, Object> inMap = new HashMap<String, Object>();
		
		if (isSkipDataConversion()) {
			Map<String, Object> skipParams = new HashMap<String, Object>();
			if (getSkipFieldPattern() != null) {
				skipParams.put("fieldPattern", getSkipFieldPattern());
			} else if (getSkipStartField() != null
					&& getSkipStopField() != null) {
				skipParams.put("startField", getSkipStartField());
				skipParams.put("stopField", getSkipStopField());
			}
			inMap.put(CmdSpec.EXPORT.toString(), skipParams);
		}

		return inMap;
	}
	
	/**
	 * Checks if is use journal.
	 * 
	 * @return true, if is use journal
	 */
	public boolean isUseJournal() {
		return useJournal;
	}

	/**
	 * Sets the use journal (true/false).
	 * 
	 * @param useJournal
	 *            the use journal (true/false)
	 * @return the export records options
	 */
	public ExportRecordsOptions setUseJournal(boolean useJournal) {
		this.useJournal = useJournal;
		return this;
	}

	/**
	 * Gets the maximum lines to be exported.
	 * 
	 * @return the maximum lines
	 */
	public long getMaxRecs() {
		return maxRecs;
	}

	/**
	 * Sets the maximum lines to be exported.
	 * 
	 * @param maxRecs
	 *            the maximum lines
	 * @return the export records options
	 */
	public ExportRecordsOptions setMaxRecs(long maxRecs) {
		this.maxRecs = maxRecs;
		return this;
	}

	/**
	 * Gets the journal or checkpoint number.
	 * 
	 * @return the journal or checkpoint number
	 */
	public int getSourceNum() {
		return sourceNum;
	}

	/**
	 * Sets the journal or checkpoint number.
	 * 
	 * @param sourceNum
	 *            the journal or checkpoint number
	 * @return the export records options
	 */
	public ExportRecordsOptions setSourceNum(int sourceNum) {
		this.sourceNum = sourceNum;
		return this;
	}

	/**
	 * Gets the journal or checkpoint offset.
	 * 
	 * @return the journal or checkpoint offset
	 */
	public long getOffset() {
		return offset;
	}

	/**
	 * Sets the journal or checkpoint offset.
	 * 
	 * @param offset
	 *            the journal or checkpoint offset
	 * @return the export records options
	 */
	public ExportRecordsOptions setOffset(long offset) {
		this.offset = offset;
		return this;
	}

	/**
	 * Checks if is format.
	 * 
	 * @return true, if is format
	 */
	public boolean isFormat() {
		return format;
	}

	/**
	 * Sets the format (true/false).
	 * 
	 * @param format
	 *            the format (true/false)
	 * @return the export records options
	 */
	public ExportRecordsOptions setFormat(boolean format) {
		this.format = format;
		return this;
	}

	/**
	 * Gets the journal prefix.
	 * 
	 * @return the journal prefix
	 */
	public String getJournalPrefix() {
		return journalPrefix;
	}

	/**
	 * Sets the journal prefix.
	 * 
	 * @param journalPrefix
	 *            the journal prefix
	 * @return the export records options
	 */
	public ExportRecordsOptions setJournalPrefix(String journalPrefix) {
		this.journalPrefix = journalPrefix;
		return this;
	}

	/**
	 * Gets the filter.
	 * 
	 * @return the filter
	 */
	public String getFilter() {
		return filter;
	}

	/**
	 * Sets the filter.
	 * 
	 * @param filter
	 *            the filter
	 * @return the export records options
	 */
	public ExportRecordsOptions setFilter(String filter) {
		this.filter = filter;
		return this;
	}

	/**
	 * Checks if is skip data conversion.
	 * 
	 * @return true, if is skip data conversion
	 */
	public boolean isSkipDataConversion() {
		return skipDataConversion;
	}

	/**
	 * Sets the skip data conversion.
	 * 
	 * @param skipDataConversion
	 *            the skip data conversion (true/false)
	 * @return the export records options
	 */
	public ExportRecordsOptions setSkipDataConversion(boolean skipDataConversion) {
		this.skipDataConversion = skipDataConversion;
		return this;
	}

	/**
	 * Gets the skip start field.
	 * 
	 * @return the skip start field
	 */
	public String getSkipStartField() {
		return skipStartField;
	}

	/**
	 * Sets the skip start field.
	 * 
	 * @param skipStartField
	 *            the skip start field
	 * @return the export records options
	 */
	public ExportRecordsOptions setSkipStartField(String skipStartField) {
		this.skipStartField = skipStartField;
		return this;
	}

	/**
	 * Gets the skip stop field.
	 * 
	 * @return the skip stop field
	 */
	public String getSkipStopField() {
		return skipStopField;
	}

	/**
	 * Sets the skip stop field.
	 * 
	 * @param skipStopField
	 *            the skip stop field
	 * @return the export records options
	 */
	public ExportRecordsOptions setSkipStopField(String skipStopField) {
		this.skipStopField = skipStopField;
		return this;
	}

	/**
	 * Gets the skip field pattern.
	 * 
	 * @return the skip field pattern
	 */
	public String getSkipFieldPattern() {
		return skipFieldPattern;
	}

	/**
	 * Sets the skip field pattern.
	 * 
	 * @param skipFieldPattern
	 *            the skip field pattern
	 * @return the export records options
	 */
	public ExportRecordsOptions setSkipFieldPattern(String skipFieldPattern) {
		this.skipFieldPattern = skipFieldPattern;
		return this;
	}
}
