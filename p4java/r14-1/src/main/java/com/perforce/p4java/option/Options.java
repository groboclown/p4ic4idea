package com.perforce.p4java.option;

import java.util.ArrayList;
import java.util.List;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.server.IServer;

/**
 * Abstract P4Java method options superclass. Supplies the very basic plumbing for
 * the method-specific options classes, including the associated generic options
 * processing gubbins.
 */

public abstract class Options {
	
	/**
	 * The list of options strings associated with this Option. Note that
	 * if this is non-null, the processOptions options processing method
	 * may optionally bypass options construction and
	 * simply return this value; this allows for options reuse, etc.
	 */
	protected List<String> optionList = null;
	
	/**
	 * If true, this Options object is (theoretically) immutable. What this
	 * means in practice is that its processOptions method is only evaluated
	 * once, i.e. the Server implementation class and associated Parameters
	 * (etc.) classes only call the processOptions methods the first time the
	 * object is passed to the Server as an argument (rather than each time
	 * the object is passed to the Server). More precisely, if the object is
	 * immutable and has already been evaluated (i.e. the optionList field is
	 * not null), the optionList is used as-is; otherwise the optionList is
	 * set to the (non-null) value returned by processOptions.<p>
	 * 
	 * This can be useful for Options objects intended for shared and constant
	 * use, and can bypass quite a lot of options evaluation; but note that
	 * in general it should only be used when you're certain that options
	 * don't change or are not reliant on dynamic circumstances.<p>
	 * 
	 * Note that immutable is always set when the string constructor is used,
	 * which can have surprising implications if this fact is forgotten down
	 * the line.
	 * 
	 * Note that subclass implementations are not bound to observe
	 * immutability, in which case the class should ensure (by overriding,
	 * etc.) that isImmutable() always returns false.
	 */
	protected boolean immutable = false;
	
	/**
	 * String used to prefix options for the server. This is pretty fundamental;
	 * don't change this unless you really know what you're doing....
	 */
	protected static final String OPTPFX = "-";
	
	/**
	 * Default constructor. Currently does nothing except set
	 * this.optionList to null.
	 */
	public Options() {
		this.optionList = null;
	}
	
	/**
	 * Construct a new immutable Options object using the passed-in strings as
	 * the options.<p>
	 * 
	 * <b>WARNING: you should not pass more than one option or argument in each
	 * string parameter. Each option or argument should be passed-in as its own
	 * separate string parameter, without any spaces between the option and the
	 * option value (if any).<b><p>
	 * 
	 * The intention here is to provide a way to bypass the various method-specific
	 * options setters with a simple mechanism to allow for constructs like this:
	 * <pre>
	 * new Options("-m10", "-uhreid");
	 * </pre>
	 * where the individual options strings correspond exactly to the Perforce
	 * server arguments and are passed to the server as is (unless a callback
	 * intervenes). Options passed in like this will normally take precedence over
	 * any options set using other mechanisms.<p>
	 * 
	 * <b>NOTE: setting options this way always bypasses the internal options values,
	 * and getter methods against the individual values corresponding to the strings
	 * passed in to this constructor will not normally reflect the string's setting.
	 * Do not use this constructor unless you know what you're doing and / or you do
	 * not also use the field getters and setters.</b>
	 * 
	 * @param options possibly-null option strings.
	 */
	public Options(String ... options) {
		this.optionList = new ArrayList<String>();
		if (options != null) {
			for (String option : options) {
				if ((option != null) && (option.length() > 0)) {
					this.optionList.add(option);
				}
			}
		}
		setImmutable(true);
	}

	/**
	 * Return the options string list associated with this object, if any.
	 * This is a simple getter method for the options field, and is <i>not</i>
	 * the same as the processOptions() method (which does processing).
	 * 
	 * @return possibly null list of options strings.
	 */
	public List<String> getOptions() {
		return this.optionList;
	}
	
	/**
	 * Set the options string list associated with this options object.<p>
	 * 
	 * <b>WARNING: you should not pass more than one option or argument in each
	 * string parameter. Each option or argument should be passed-in as its own
	 * separate string parameter, without any spaces between the option and the
	 * option value (if any).<b><p>
	 * 
	 * The intention here is to provide a way to bypass the various method-specific
	 * options setters with a simple mechanism to allow for constructs like this:
	 * <pre>
	 * opts = new Options();
	 * opts.setOptions("-m10", "-uhreid");
	 * </pre>
	 * where the individual options strings correspond exactly to the Perforce
	 * server arguments and are passed to the server as is (unless a callback
	 * intervenes). Options passed in like this may take precedence over any
	 * options set using other mechanisms.<p>
	 * 
	 * @param options possibly-null option strings list
	 * @return this object
	 */
	public Options setOptions(String ... options) {
		if (options != null) {
			this.optionList = new ArrayList<String>();
			for (String option : options) {
				if (option != null) {
					this.optionList.add(option);
				}
			}
		} else {
			this.optionList = null;
		}
		return this;
	}
	
	/**
	 * Turn this (specific) options object into a list
	 * of strings to be sent to the Perforce server as options for
	 * a specific command. As a side effect, set the option list
	 * associated with this Option to the result.<p>
	 * 
	 * The method is used by the server object to generate the string-based
	 * arguments expected by the Perforce server corresponding to the state of this
	 * method-specific options object. Will return an empty list if
	 * there are no "interesting" options set or available. May simply
	 * return the superclass options string list if is non-null,
	 * but that behaviour is neither guaranteed nor required.<p>
	 * 
	 * <b>Note that this method is not intended to be called directly by users
	 * but by the underlying P4Java plumbing; odd results may occur if this
	 * method is called in other contexts.</b>
	 * 
	 * @param server possibly-null IServer representing the Perforce server
	 * 			the options are to be used against. If this parameter is
	 * 			null, it is acceptable to throw an OptionsException, but
	 * 			it is also possible to ignore it and do the best you can
	 * 			with what you've got...
	 * @return non-null (but possibly empty) string list representing the
	 * 			normalized Perforce server arguments corresponding to
	 * 			the state of this specific options object. 
	 * @throws OptionsException if an error occurs in options processing that is
	 * 			not some species of ConnectionException, RequestException,
	 * 			AccessException, etc.
	 */
		
	public abstract List<String> processOptions(IServer server) throws OptionsException;

	
	/**
	 * Process command method options according to a simple getopts-like options
	 * specifier string. The intention here is to provide a very simple way for methods
	 * with common options to turn those options values into a list of strings
	 * suitable for sending to the perforce server. Usage is typically something
	 * like this:
	 * <pre>
	 * optsList = processFields("i:c:cl s:j b:i i:m:gtz",
	 						opts.getChangelistId(), opts.getJobId(),
	 						opts.isIncludeIntegrations(), opts.getMaxFixes())
	 * </pre>
	 * 
	 * The format of the optsSpecs string parameter is:
	 * <pre>
	  typespec:server-flag[:rulename]
	 * </pre>
	 * where typespec is currently one of:
	 * <pre>
	  i -- integer; assumes the corresponding argument is an int; will generally
	  			just concatenate the flag and the value.
	  b -- boolean; assumes the corresponding argument is a boolean; will normally
	  			only return the corresponding flag if true.
	  s -- string; assumes the corresponding argument is a string; will normally
	  			just concatenate the flag and the value if the value is non-null.
	 * </pre>
	 * and server-flag is the flag string associated with this option
	 * when sent to the Perforce server, and where the optional rulename is passed
	 * to the relevant applyRule method. See the individual applyRule documentation
	 * below for rule processing details.
	 *
	 * Note that use of this method is entirely voluntary, and that it does not always
	 * work for non-simple cases. Note also that both the general implementation
	 * and the rules section will probably expand a bit over time.
	 * 
	 * @param optsSpecs non-null options specifier string as specified above
	 * @param opts non-null options to be processed
	 * @return a non-null but possibly-empty list of options strings
	 * @throws OptionsException if any errors occurred during options
	 * 			processing.
	 */
	
	public List<String> processFields(String optsSpecs, Object ... opts)
											throws OptionsException {
		List<String> optsList = new ArrayList<String>();
		
		if (optsSpecs == null) {
			throw new OptionsException("null options spec in options processor");
		}
		
		if (opts != null) {
			String[] specStrs = optsSpecs.split(" ");
			if (specStrs.length != opts.length) {
				throw new OptionsException(
							"specs vs opts size mismatch in options processor");
			}
			
			for (int i = 0; i < opts.length; i++) {
				if (specStrs[i] == null) {
					throw new OptionsException(
							"null options spec in options processor: " + optsSpecs);
				}
				String[] optSpec = specStrs[i].split(":");
				if ((optSpec.length < 2 || optSpec.length > 3)) {
					throw new OptionsException(
							"bad options spec in options processor: " + specStrs[i]);
				}
				
				try {
					String optVal = null;
					if (optSpec[0].equals("i")) {
						optVal = applyRule(optSpec.length >= 3 ? optSpec[2] : null,
								optSpec[1], (Integer) opts[i]);
					} else if (optSpec[0].equals("l")) {
						optVal = applyRule(optSpec.length >= 3 ? optSpec[2] : null,
								optSpec[1], (Long) opts[i]);
					} else if (optSpec[0].equals("s")) {
						optVal = applyRule(optSpec.length >= 3 ? optSpec[2] : null,
								optSpec[1], (String) opts[i]);
					} else if (optSpec[0].equals("s[]")) {
						String[] args = (String[]) opts[i];
						if (args != null) {
							for (String arg : args) {
								String option = applyRule(optSpec.length >= 3 ? optSpec[2] : null,
									optSpec[1], arg);
								if (option != null) {
									optsList.add(option);
								}
							}
						}
					} else if (optSpec[0].equals("b")) {
						optVal = applyRule(optSpec.length >= 3 ? optSpec[2] : null,
								optSpec[1], (Boolean) opts[i]);
					}
					if (optVal != null) {
						optsList.add(optVal);
					}
				} catch (Exception exc) {
					throw new OptionsException(
							"bad conversion encountered in options processor with option string '"
							+ specStrs[i] + "': " + exc.getLocalizedMessage()
						);
				}
			}
		}

		return optsList;
	}
	
	/**
	 * Apply an optional rule to an integer option value. This method is always
	 * called by the default implementation of Options.processOptions to process
	 * integer values; you should override this if you need your own rules processing.<p>
	 * 
	 * This version of applyRules implements the rules specified below:
	 * <pre>
	 * "gtz": don't return anything unless the value is > 0; typically used for
	 * 		things like maxUsers or maxRows.
	 * "cl": ignore negative values; convert 0 to the string "default". Typically
	 * 		used for changelists.
	 * "clz": ignore non-positive values; typically used for changelists where we
	 * 		let the server infer "default" for IChangelist.DEFAULT rather than
	 * 		spelling it out.
	 * "dcn": implements the -dc[n] rule for diff contexts, i.e. if the int value is
	 * 		zero, emit the flag alone; if it's positive, emit the flag with the int
	 * 		value attached; if it's negative, don't emit anything.
	 * </pre>
	 * If the passed-in ruleName is non-null and not recognized, the behaviour
	 * is the same as if a null rule name was passed in.
	 * 
	 * @param ruleName rule name string from the options spec string. If null, no
	 * 				rule was specified.
	 * @param serverOptStr the flag string to be sent to the Perforce server prefixing
	 * 				this value
	 * @param value the integer value itself.
	 * @return processed value or null if the rules resulted in nothing to be sent
	 * 				to the Perforce server.
	 * @throws OptionsException if any errors occurred during options
	 * 			processing.
	 */
	protected String applyRule(String ruleName, String serverOptStr, int value)
													throws OptionsException {
		if (serverOptStr == null) {
			throw new OptionsException("Null server options spec");
		}
		
		if (ruleName == null) {
			return OPTPFX + serverOptStr + value;
		} else {
			if (ruleName.equals("gtz")) {
				if (value > 0) {
					return OPTPFX + serverOptStr + value;
				}
			} else if (ruleName.equals("cl")) {
				if (value >= 0) {
					return OPTPFX + serverOptStr + 
						(value == IChangelist.DEFAULT ? "default" : value);
				}
			} else if (ruleName.equals("clz")) { 
				if (value > 0) {
					return OPTPFX + serverOptStr + value;
				}
			} else if (ruleName.equals("dcn")) {
				if (value > 0) {
					return OPTPFX + serverOptStr + value;
				} else if (value == 0) {
					return OPTPFX + serverOptStr;
				}
			} else {				
				throw new OptionsException("Unrecognized option rule name in options parser: '"
											+ ruleName + "'");
			}
		}
		
		return null;
	}
	
	/**
	 * Apply an optional rule to a long option value. This method is always
	 * called by the default implementation of Options.processOptions to process
	 * long values; you should override this if you need your own rules processing.<p>
	 * 
	 * This version of applyRules implements the rules specified below:
	 * <pre>
	 * "gtz": don't return anything unless the value is > 0.
	 * "gez": don't return anything unless the value is >= 0.
	 * </pre>
	 * If the passed-in ruleName is non-null and not recognized, the behaviour
	 * is the same as if a null rule name was passed in.
	 * 
	 * @param ruleName rule name string from the options spec string. If null, no
	 * 				rule was specified.
	 * @param serverOptStr the flag string to be sent to the Perforce server prefixing
	 * 				this value
	 * @param value the long value itself.
	 * @return processed value or null if the rules resulted in nothing to be sent
	 * 				to the Perforce server.
	 * @throws OptionsException if any errors occurred during options
	 * 			processing.
	 */
	protected String applyRule(String ruleName, String serverOptStr, long value)
													throws OptionsException {
		if (serverOptStr == null) {
			throw new OptionsException("Null server options spec");
		}
		
		if (ruleName == null) {
			return OPTPFX + serverOptStr + value;
		} else {
			if (ruleName.equals("gtz")) {
				if (value > 0) {
					return OPTPFX + serverOptStr + value;
				}
			} else if (ruleName.equals("gez")) {
				if (value >= 0) {
					return OPTPFX + serverOptStr + value;
				}
			} else {				
				throw new OptionsException("Unrecognized option rule name in options parser: '"
											+ ruleName + "'");
			}
		}
		
		return null;
	}

	/**
	 * Apply an optional rule to a string option value. This method is always
	 * called by the default implementation of Options.processOptions to process
	 * string values; you should override this if you need your own rules processing.<p>
	 *
	 * There are currently no rules recognised or implemented in this method.
	 * 
	 * @param ruleName rule name string from the options spec string. If null, no
	 * 				rule was specified.
	 * @param serverOptStr the flag string to be sent to the Perforce server prefixing
	 * 				this value
	 * @param value the string value itself; may be null.
	 * @return processed value or null if the rules resulted in nothing to be sent
	 * 				to the Perforce server.
	 * @throws OptionsException if any errors occurred during options
	 * 			processing.
	 */
	protected String applyRule(String ruleName, String serverOptStr, String value)
		throws OptionsException {
		if (serverOptStr == null) {
			throw new OptionsException("Null server options spec");
		}
		
		if (ruleName == null) {
			if (value != null) {
				return OPTPFX + serverOptStr + value;
			}
		} else {
			throw new OptionsException("Unrecognized option rule name in options parser: '"
					+ ruleName + "'");
		}
		
		return null;
	}
	
	/**
	 * Apply an optional rule to a boolean option value. This method is always
	 * called by the default implementation of Options.processOptions to process
	 * boolean values; you should override this if you need your own rules processing.<p>
	 * 
	 * There are currently no rules recognised or implemented in this method.
	 * 
	 * @param ruleName rule name string from the options spec string. If null, no
	 * 				rule was specified.
	 * @param serverOptStr the flag string to be sent to the Perforce server prefixing
	 * 				this value
	 * @param value the boolean value itself.
	 * @return processed value or null if the rules resulted in nothing to be sent
	 * 				to the Perforce server.
	 * @throws OptionsException if any errors occurred during options
	 * 			processing.
	 */
	protected String applyRule(String ruleName, String serverOptStr, boolean value)
									throws OptionsException {
		if (serverOptStr == null) {
			throw new OptionsException("Null server options spec");
		}
		
		if (ruleName == null) {
			if (value) {
				return OPTPFX + serverOptStr;
			}
		} else {
			throw new OptionsException("Unrecognized option rule name in options parser: '"
					+ ruleName + "'");
		}
		
		return null;
	}

	public boolean isImmutable() {
		return immutable;
	}

	public Options setImmutable(boolean immutable) {
		this.immutable = immutable;
		return this;
	}
}
