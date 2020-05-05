package com.perforce.p4java.common.base;

// p4ic4idea: add security checks and extra code for looking up system information.
import java.security.AccessController;
import javax.annotation.Nonnull;
import java.security.PrivilegedAction;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * @author Sean Shou
 * @since 20/07/2016
 */
public final class OSUtils {
    // p4ic4idea: replace the original functionality, which relied upon Oracle-specific extensions, to instead
    // be usable by any compliant JRE.  Anyway, it was just attempting a property fetch without triggering a
    // security error.
    @Nonnull
    private static String getLowerCaseProperty(final String name) {
        try {
            String val = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(name));
            if (val == null) {
                return EMPTY;
            }
            return val.toLowerCase(Locale.ENGLISH);
        } catch (SecurityException e) {
            return EMPTY;
        }
    }
    //private static final String OS_NAME = AccessController.doPrivileged(new GetPropertyAction("os.name")).toLowerCase();
    //private static final String OS_ARCH = AccessController.doPrivileged(new GetPropertyAction("os.arch")).toLowerCase();

    // This code was taken from the Ant 1.10.2 source code for org.apache.tools.ant.taskdefs.condition.Os,
    // which is under the Apache 2.0 license.
    private static final String OS_NAME = getLowerCaseProperty("os.name");
    private static final String OS_ARCH = getLowerCaseProperty("os.arch");


  private OSUtils() { /* util */ }

  public static boolean isWindows() {
      // p4ic4idea: Ant windows probing logic relies on the word 'windows' in the OS.
    //return (OS_NAME.contains("win"));
      return OS_NAME.contains("windows");
  }

  public static boolean isClassicMac() {
    return (OS_NAME.contains("mac") && OS_ARCH.contains("ppc"));
  }

  public static boolean isOSX() {
    return (OS_NAME.contains("mac") && OS_ARCH.contains("x86_64"));
  }

  public static boolean isUnix() {

    return (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix"));

  }

  public static boolean isSolaris() {
    return OS_NAME.contains("sunos");
  }

  public static boolean isFreebsd() {
    return OS_NAME.contains("freebsd");
  }
}
