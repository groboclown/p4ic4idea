package com.perforce.p4java.option;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwOptionsException;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwOptionsExceptionIfConditionFails;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.server.IServer;

/**
 * Abstract P4Java method options superclass. Supplies the very basic plumbing
 * for the method-specific options classes, including the associated generic
 * options processing gubbins.
 */

public abstract class Options {
	/**
	 * The list of options strings associated with this Option. Note that if
	 * this is non-null, the processOptions options processing method may
	 * optionally bypass options construction and simply return this value; this
	 * allows for options reuse, etc.
	 */
	protected List<String> optionList;

	/**
	 * If true, this Options object is (theoretically) immutable. What this
	 * means in practice is that its processOptions method is only evaluated
	 * once, i.e. the Server implementation class and associated Parameters
	 * (etc.) classes only call the processOptions methods the first time the
	 * object is passed to the Server as an argument (rather than each time the
	 * object is passed to the Server). More precisely, if the object is
	 * immutable and has already been evaluated (i.e. the optionList field is
	 * not null), the optionList is used as-is; otherwise the optionList is set
	 * to the (non-null) value returned by processOptions.
	 * <p>
	 *
	 * This can be useful for Options objects intended for shared and constant
	 * use, and can bypass quite a lot of options evaluation; but note that in
	 * general it should only be used when you're certain that options don't
	 * change or are not reliant on dynamic circumstances.
	 * <p>
	 *
	 * Note that immutable is always set when the string constructor is used,
	 * which can have surprising implications if this fact is forgotten down the
	 * line.
	 *
	 * Note that subclass implementations are not bound to observe immutability,
	 * in which case the class should ensure (by overriding, etc.) that
	 * isImmutable() always returns false.
	 */
	protected boolean immutable = false;

	/**
	 * Default constructor. Currently does nothing except set this.optionList to
	 * null.
	 */
	public Options() {
	}

	/**
	 * Construct a new immutable Options object using the passed-in strings as
	 * the options.
	 * <p>
	 *
	 * <b>WARNING: you should not pass more than one option or argument in each
	 * string parameter. Each option or argument should be passed-in as its own
	 * separate string parameter, without any spaces between the option and the
	 * option value (if any).<b>
	 * <p>
	 *
	 * The intention here is to provide a way to bypass the various
	 * method-specific options setters with a simple mechanism to allow for
	 * constructs like this:
	 * 
	 * <pre>
	 * new Options("-m10", "-uhreid");
	 * </pre>
	 * 
	 * where the individual options strings correspond exactly to the Perforce
	 * server arguments and are passed to the server as is (unless a callback
	 * intervenes). Options passed in like this will normally take precedence
	 * over any options set using other mechanisms.
	 * <p>
	 *
	 * <b>NOTE: setting options this way always bypasses the internal options
	 * values, and getter methods against the individual values corresponding to
	 * the strings passed in to this constructor will not normally reflect the
	 * string's setting. Do not use this constructor unless you know what you're
	 * doing and / or you do not also use the field getters and setters.</b>
	 *
	 * @param options
	 *            possibly-null option strings.
	 */
	public Options(String... options) {
		setOptions(options);
	}
	
	/**
	 * Set the options string list associated with this options object.
	 * <p>
	 *
	 * <b>WARNING: you should not pass more than one option or argument in each
	 * string parameter. Each option or argument should be passed-in as its own
	 * separate string parameter, without any spaces between the option and the
	 * option value (if any).<b>
	 * <p>
	 *
	 * The intention here is to provide a way to bypass the various
	 * method-specific options setters with a simple mechanism to allow for
	 * constructs like this:
	 * 
	 * <pre>
	 * opts = new Options();
	 * opts.setOptions("-m10", "-uhreid");
	 * </pre>
	 * 
	 * where the individual options strings correspond exactly to the Perforce
	 * server arguments and are passed to the server as is (unless a callback
	 * intervenes). Options passed in like this may take precedence over any
	 * options set using other mechanisms.
	 * <p>
	 *
	 * @param options
	 *            possibly-null option strings list
	 */
	public void setOptions(String... options) {
		if (nonNull(options)) {
			optionList = new ArrayList<>();
			for (String option : options) {
				if (isNotBlank(option)) {
					optionList.add(option);
				}
			}

			if (!optionList.isEmpty()) {
				setImmutable(true);
			}
		} else {
			optionList = null;
		}
	}
	
	public void setImmutable(boolean immutable) {
		this.immutable = immutable;
	}

	public boolean isImmutable() {
		return immutable;
	}

	/**
	 * Return the options string list associated with this object, if any. This
	 * is a simple getter method for the options field, and is <i>not</i> the
	 * same as the processOptions() method (which does processing).
	 *
	 * @return possibly null list of options strings.
	 */
	public List<String> getOptions() {
		return optionList;
	}

	/**
	 * Process command method options according to a simple getopts-like options
	 * specifier string. The intention here is to provide a very simple way for
	 * methods with common options to turn those options values into a list of
	 * strings suitable for sending to the perforce server. Usage is typically
	 * something like this:
	 * 
	 * <pre>
	 * optsList = processFields("i:c:cl s:j b:i i:m:gtz", opts.getChangelistId(), opts.getJobId(),
	 * 		opts.isIncludeIntegrations(), opts.getMaxFixes())
	 * </pre>
	 *
	 * The format of the optionSpecsString string parameter is:
	 * 
	 * <pre>
	 * typespec:server-flag[:rulename]
	 * </pre>
	 * 
	 * where typespec is currently one of:
	 * 
	 * <pre>
	 * i -- integer; assumes the corresponding argument is an int; will generally
	 * just concatenate the flag and the value.
	 * b -- boolean; assumes the corresponding argument is a boolean; will normally
	 * only return the corresponding flag if true.
	 * s -- string; assumes the corresponding argument is a string; will normally
	 * just concatenate the flag and the value if the value is non-null.
	 * </pre>
	 * 
	 * and server-flag is the flag string associated with this option when sent
	 * to the Perforce server, and where the optional rulename is passed to the
	 * relevant applyRule method. See the individual applyRule documentation
	 * below for rule processing details.
	 *
	 * Note that use of this method is entirely voluntary, and that it does not
	 * always work for non-simple cases. Note also that both the general
	 * implementation and the rules section will probably expand a bit over
	 * time.
	 *
	 * @param optionSpecsString
	 *            non-null options specifier string as specified above
	 * @param opts
	 *            non-null options to be processed
	 * @return a non-null but possibly-empty list of options strings
	 * @throws OptionsException
	 *             if any errors occurred during options processing.
	 */
	public List<String> processFields(@Nonnull String optionSpecsString, @Nullable Object... opts)
			throws OptionsException {
		throwOptionsExceptionIfConditionFails(nonNull(optionSpecsString), "options specs are required");

		List<String> optsList = new ArrayList<>();
		if (nonNull(opts)) {
			String[] optionSpecExpressions = optionSpecsString.split(SPACE);
			throwOptionsExceptionIfConditionFails(optionSpecExpressions.length == opts.length,
					"specs vs opts size mismatch in options processor");

			String currentOptionSpecExpression = EMPTY;
			try {
				for (int i = 0; i < opts.length; i++) {
					currentOptionSpecExpression = optionSpecExpressions[i];
					throwOptionsExceptionIfConditionFails(isNotBlank(currentOptionSpecExpression),
							"null options spec in options processor: %s", optionSpecsString);
					String[] optSpecParts = currentOptionSpecExpression.split(":");

					throwOptionsExceptionIfConditionFails(optSpecParts.length == 2 || optSpecParts.length == 3,
							"null options spec in options processor: %s", optionSpecsString);

					String ruleName = EMPTY;
					if (optSpecParts.length >= 3) {
						ruleName = optSpecParts[2];
					}
					String serverOptionStr = optSpecParts[1];

					String optSpecType = optSpecParts[0];
					OptionsSpecType p4CommandOptions = OptionsSpecType.of(optSpecType);
					optsList.addAll(p4CommandOptions.getP4CommandOptionFields(ruleName, serverOptionStr, opts[i]));
				}
			} catch (Exception exc) {
				throwOptionsException(exc,
						"bad conversion encountered in options processor with option string '%s': %s",
						currentOptionSpecExpression, exc.getLocalizedMessage());
			}
		}

		return optsList;
	}

	/**
	 * Turn this (specific) options object into a list of strings to be sent to
	 * the Perforce server as options for a specific command. As a side effect,
	 * set the option list associated with this Option to the result.
	 * <p>
	 *
	 * The method is used by the server object to generate the string-based
	 * arguments expected by the Perforce server corresponding to the state of
	 * this method-specific options object. Will return an empty list if there
	 * are no "interesting" options set or available. May simply return the
	 * superclass options string list if is non-null, but that behaviour is
	 * neither guaranteed nor required.
	 * <p>
	 *
	 * <b>Note that this method is not intended to be called directly by users
	 * but by the underlying P4Java plumbing; odd results may occur if this
	 * method is called in other contexts.</b>
	 *
	 * @param server
	 *            possibly-null IServer representing the Perforce server the
	 *            options are to be used against. If this parameter is null, it
	 *            is acceptable to throw an OptionsException, but it is also
	 *            possible to ignore it and do the best you can with what you've
	 *            got...
	 * @return non-null (but possibly empty) string list representing the
	 *         normalized Perforce server arguments corresponding to the state
	 *         of this specific options object.
	 * @throws OptionsException
	 *             if an error occurs in options processing that is not some
	 *             species of ConnectionException, RequestException,
	 *             AccessException, etc.
	 */

	public abstract List<String> processOptions(IServer server) throws OptionsException;
}
