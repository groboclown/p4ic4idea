/*
 * Copyright 2008 Perforce Software Inc., All Rights Reserved.
 */

package com.perforce.p4java;

import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

/**
 * Defines public version, date, etc., metadata about the P4Java API.<p>
 * 
 * The version strings are mostly intended for support, debugging, and logging purposes,
 * meaning the format and semantics of much of the data here is intentionally
 * opaque.
 */

public class Metadata {
	
	/**
	 * The Java properties key prefix use to pick up P4Java properties
	 * from the passed-in properties, etc.
	 */
	public static final String P4JAVA_PROPS_KEY_PREFIX = "com.perforce.p4java.";
	
	/**
	 * The default minimum Perforce server level supported by the entire P4Java
	 * API. Specific implementations may override this.
	 */
	public static final int DEFAULT_MINIMUM_SUPPORTED_SERVER_VERSION = 20052;
	
	/**
	 * The value returned by the getP4JVersionString method if it
	 * can not determine the correct version from the associated JAR
	 * manifest.
	 */
	public static final String DEFAULT_VERSION_STRING = "Internal 2010.1 Perforce Development";
	
	/**
	 * The value returned by the getP4JDateString method if it
	 * can not determine the correct date from the associated JAR
	 * manifest.
	 */
	public static final String DEFAULT_DATE_STRING = "unknown";
	
	private static String p4jVersionString = null;
	private static String p4jDateString = null;
	
	private static Manifest manifest = null;
	
	/**
	 * Return the P4Java version string associated with this instance
	 * as contained in the enclosing JAR file's manifest IMPLEMENTATION_TITLE
	 * attribute. If this information is not available, DEFAULT_VERSION_STRING
	 * is returned.<p>
	 * 
	 * The format and semantics of this string are not specified here.
	 * 
	 * @return non-null version string
	 */
	
	public static String getP4JVersionString() {
		if (p4jVersionString != null) {
			return p4jVersionString;
		} else {
			p4jVersionString = DEFAULT_VERSION_STRING;
			
			// try to get this information from the JAR manifest
			try {
				if (manifest == null) {
						manifest = getManifest();
				}
				// manifest is non-null or we got an exception...
				Attributes attr = manifest.getMainAttributes();
				String version = attr.getValue(Name.IMPLEMENTATION_VERSION);
				if (version != null) {
					p4jVersionString = version;
				}
			} catch (Exception exc) {
				Log.warn("unable to get manifest version attribute: "
									+ exc.getLocalizedMessage());
			}
		}
		
		return p4jVersionString;
	}
	
	/**
	 * Return the P4Java date string associated with this instance as contained
	 * in the enclosing JAR file's manifest Build-Date attribute. If this information
	 * is not available, DEFAULT_DATE_STRING is returned.<p>
	 * 
	 * The format and semantics of this string are not specified here.
	 * 
	 * @return non-null date string
	 */
	
	public static String getP4JDateString() {
		if (p4jDateString != null) {
			return p4jVersionString;
		} else {
			p4jDateString = DEFAULT_DATE_STRING;
			
			// try to get this information from the JAR manifest
			try {
				if (manifest == null) {
						manifest = getManifest();
				}
				// manifest is non-null or we got an exception...
				Attributes attr = manifest.getMainAttributes();
				String date = attr.getValue("Build-Date");
				if (date != null) {
					p4jDateString = date;
				}
			} catch (Exception exc) {
				Log.warn("unable to get manifest date attribute: "
									+ exc.getLocalizedMessage());
			}
		}
		
		return p4jDateString;
	}
	
	/**
     * Public main method, used solely for allowing customers to
     * print version and other metadata information from the enclosing
     * JAR file's manifest. This information is printed to stdout;
     * errors are printed to stderr.
     * 
     * @since 2011.1
     * @param args not used.
     */
	public static void main(String[] args) {
		try {
			Manifest manifest = getManifest();
			Attributes attr = manifest.getMainAttributes();
			System.out.println(attr.getValue(Name.IMPLEMENTATION_TITLE));
			StringBuilder version = new StringBuilder(
					attr.getValue(Name.IMPLEMENTATION_VERSION));
			String changelist = attr.getValue("Build-Changelist");
			if (changelist != null) {
				version.append('/').append(changelist);
			}
			String type = attr.getValue("Build-Type");
			if (type != null) {
				version.append('/').append(type);
			}
			System.out.println(version);
		} catch (Exception exception) {
			System.err.println(exception.getLocalizedMessage());
		}
	}
    
	/**
	 * Get the JAR Manifest associated with this P4Java instance,
	 * if it exists. The interpretation of the attributes in this
	 * manifest is not defined here and will depend on how the
	 * jar was built; for a full list of normal Perforce release build
	 * attributes, contact support, but in general, most of the normal
	 * Maven build attributes will probably exist, as will
	 * Name.IMPLEMENTATION_TITLE and Name.IMPLEMENTATION_VERSION (but
	 * this is not guaranteed).
	 * 
	 * @since 2011.1
	 * @return non-null Manifest object if it exists and is retrievable.
	 * @throws Exception if the manifest can't be found or retrieved
	 * 				for any reason.
	 */
	
	public static Manifest getManifest() throws Exception {
		String className = Metadata.class.getSimpleName() + ".class";
		URL classURL = Metadata.class.getResource(className);
		if (classURL == null) {
			throw new Exception("Error retrieving class as resource: "
					+ className);
		}

		String classPath = classURL.toString();
		if (!classPath.startsWith("jar")) {
			throw new Exception("Not running from jar file: " + classPath);
		}

		int separator = classPath.lastIndexOf('!');
		if (separator != -1 && separator + 1 >= classPath.length()) {
			throw new Exception("Invalid jar file class path: " + classPath);
		}

		String manifestPath = classPath.substring(0, separator + 1)
				+ "/META-INF/MANIFEST.MF";
		
		InputStream inStream = null;
		try {
			inStream = new URL(manifestPath).openStream();
			return new Manifest(inStream);
		} finally {
			if (inStream != null) {
				inStream.close();
			}
		}
	}
}
