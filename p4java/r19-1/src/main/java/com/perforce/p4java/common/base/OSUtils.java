package com.perforce.p4java.common.base;

/**
 * @author Sean Shou
 * @since 20/07/2016
 */
public final class OSUtils {
  private static final String OS_NAME = System.getProperty("os.name") != null ? System.getProperty("os.name").toLowerCase() : "";
  private static final String OS_ARCH = System.getProperty("os.arch") != null ? System.getProperty("os.arch").toLowerCase() : "";

  private OSUtils() { /* util */ }

  public static boolean isWindows() {
    return (OS_NAME.contains("win"));
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
