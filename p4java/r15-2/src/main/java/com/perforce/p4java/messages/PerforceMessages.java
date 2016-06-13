/**
 * Copyright (c) 2012 Perforce Software. All rights reserved.
 */
package com.perforce.p4java.messages;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Handles formatting Perforce messages. It provides locale (language & country)
 * specific messages. The default locale is set during startup of the JVM based
 * on the host environment. </p>
 * 
 * Additionally, this class provides a convenient way to format messages with
 * parameters.
 */
public class PerforceMessages {

	/** Default name of the Perforce message bundle properties file. */
	public static final String MESSAGE_BUNDLE = PerforceMessages.class
			.getName();

	/** The locale. */
	private Locale locale;

	/** The messages. */
	private ResourceBundle messages;

	/**
	 * Instantiates a new perforce messages using the default message bundle
	 * properties file package path name .
	 */
	public PerforceMessages() {
		this.messages = ResourceBundle.getBundle(MESSAGE_BUNDLE);
	}

	/**
	 * Instantiates a new perforce messages base on the passed-in message bundle
	 * properties file package path name.
	 * 
	 * @param propertiesFile
	 *            the name (without the extension) of the properties file
	 *            including the full package path name (i.e.
	 *            com.perforce.p4java.messages.PerforceMessages)
	 */
	public PerforceMessages(String propertiesFile) {
		this.messages = ResourceBundle.getBundle(propertiesFile);
	}

	/**
	 * Instantiates a new perforce messages.
	 * 
	 * @param locale
	 *            the locale
	 */
	public PerforceMessages(Locale locale) {
		this.locale = locale;
		this.messages = ResourceBundle.getBundle(MESSAGE_BUNDLE, locale);
	}

	/**
	 * Gets the locale.
	 * 
	 * @return the locale
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Sets the locale.
	 * 
	 * @param locale
	 *            the new locale
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * Gets the messages.
	 * 
	 * @return the messages
	 */
	public ResourceBundle getMessages() {
		return messages;
	}

	/**
	 * Sets the messages.
	 * 
	 * @param messages
	 *            the new messages
	 */
	public void setMessages(ResourceBundle messages) {
		this.messages = messages;
	}

	/**
	 * Gets the message.
	 * 
	 * @param key
	 *            the key
	 * @return the message
	 */
	public String getMessage(String key) {
		return messages.getString(key);
	}

	/**
	 * Gets the message.
	 * 
	 * @param key
	 *            the key
	 * @param params
	 *            the params
	 * @return the message
	 */
	public String getMessage(String key, Object[] params) {
		return format(messages.getString(key), params);
	}

	/**
	 * Format a message with parameters.
	 * 
	 * @param message
	 *            the message
	 * @param params
	 *            the params
	 * @return the string
	 * @see MessageFormat
	 */
	public String format(String message, Object[] params) {
		return MessageFormat.format(message, params);
	}
}
