package com.perforce.p4java.env;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.perforce.p4java.common.base.OSUtils;

/**
 * @author Sean Shou
 * @since 24/08/2016
 */
public class ClientTunableSettings {
  private ClientTunableSettings() { /* util */ }

  private static String FILE_SYS_UTF8_BOM = "filesys.utf8bom";
  private static String FILE_SYS_UTF8_BOM_DEFAULT = "1";

  /**
   * Set to 0 to prevent writing utf8 files BOM
   * Set to 1 to write utf8 files with a BOM - default
   * Set to 2 to write utf8 BOM only on Windows
   */
  //TODO: sshou should implement read it from 'p4envrio' with server:port prefix as well
  public static boolean needWriteUtf8Bom() {
    String fileSystemUtf8Bom = System.getProperty(FILE_SYS_UTF8_BOM);
    if (isBlank(fileSystemUtf8Bom)) {
      fileSystemUtf8Bom = System.getenv(FILE_SYS_UTF8_BOM);
    }

    if (isNotBlank(fileSystemUtf8Bom)) {
      return FILE_SYS_UTF8_BOM_DEFAULT.equalsIgnoreCase(fileSystemUtf8Bom)
          || ("2".equalsIgnoreCase(fileSystemUtf8Bom) && OSUtils.isWindows());
    }

    /*
    if (isBlank(fileSystemUtf8Bom)) {
        String p4Enviro = PerforceEnvironment.getP4Enviro();
        Path p4EnviroFile = Paths.get(p4Enviro);
        if (Files.exists(p4EnviroFile)) {

        }
    }
    */

    return true;
  }
}
