/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.server;

/**
 * Provides an interface onto information about Perforce server
 * implementations available through the server factory. Useful
 * for presenting choices to end users, debugging, etc.
 */

public interface IServerImplMetadata {
	
	/**
	 * Describes the various known implementation types. There is currently
	 * only one implementation type, but this may change in the long term.
	 */
	enum ImplType {
		/**
		 * Type is unknown. Don't use this implementation....
		 */
		UNKNOWN,
		
		/**
		 * Java-native RPC protocol implementation.
		 */
		NATIVE_RPC
	};
	
	/**
	 * Return a short name for this implementation, intended for use in menu
	 * pulldowns, etc. May contain spaces, but won't usually be more than 32
	 * characters long.
	 */
	String getScreenName();
	
	/**
	 * Return the canonical name of the implementation class
	 * associated with this implementation.
	 */
	String getImplClassName();

	/**
	 * Returns true IFF this implementation will be used if the non-specific
	 * implementation scheme "p4j" is provided in the URI passed in to the
	 * server factory. There will be only one such default implementation
	 * for each factory.
	 */
	boolean isDefault();
	
	/**
	 * Get the implementation type associated with this implementation.
	 */
	ImplType getImplType();
	
	/**
	 * Get the URI scheme part to be used to specify this implementation
	 * to the server factory.
	 */
	String getUriScheme();
	
	/**
	 * Get the earliest Perforce server version that this implementation
	 * will work reliably against. Typically in the form 20073 or 20091, etc.
	 */
	int getMinimumServerLevel();
	
	/**
	 * Return any comments associated with the implementation. These
	 * should note any special restrictions or dependencies associated with
	 * the implementation that the user might need to know; this string
	 * may be null.
	 */
	String getComments();
}
