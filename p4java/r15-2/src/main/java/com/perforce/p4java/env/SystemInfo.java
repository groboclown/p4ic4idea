/**
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.env;

/**
 * Provides system information about the Java runtime environment
 */
public class SystemInfo {

	public static boolean isWindows() {
		return (getOsName().toLowerCase().startsWith("windows"));
	}

	public static boolean isMac() {
		return (getOsName().toLowerCase().startsWith("mac") || getOsName()
				.toLowerCase().startsWith("darwin"));
	}

	public static boolean isLinux() {
		return (getOsName().toLowerCase().startsWith("linux"));
	}

	public static boolean isUnix() {
		// Linux
		if (isLinux()) {
			return true;
		}
		// Solaris or SUN OS
		if (getOsName().toLowerCase().startsWith("solaris")
				|| getOsName().toLowerCase().startsWith("sunos")) {
			return true;
		}
		// Mac OS X
		if (getOsName().toLowerCase().indexOf("mac os x") != -1) {
			return true;
		}
		// FreeBSD, NetBSD or OpenBSD
		if (getOsName().toLowerCase().startsWith("freebsd")
				|| getOsName().toLowerCase().startsWith("netbsd")
				|| getOsName().toLowerCase().startsWith("openbsd")) {
			return true;
		}
		// AIX
		if (getOsName().toLowerCase().startsWith("aix")) {
			return true;
		}
		// HP-UX
		if (getOsName().toLowerCase().startsWith("hp-ux")) {
			return true;
		}
		// IRIX
		if (getOsName().toLowerCase().startsWith("irix")) {
			return true;
		}

		return false;
	}

	public static String getOsName() {
		return System.getProperty("os.name");
	}

	public static String getOsVersion() {
		return System.getProperty("os.version");
	}

	public static String getOsArch() {
		return System.getProperty("os.arch");
	}

	public static String getFileSeparator() {
		return System.getProperty("file.separator");
	}

	public static String getUserHome() {
		return System.getProperty("user.home");
	}
}
