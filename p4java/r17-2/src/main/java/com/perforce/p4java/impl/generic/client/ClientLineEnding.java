/**
 *
 */
package com.perforce.p4java.impl.generic.client;

import java.util.Arrays;
import java.util.Map;

import com.perforce.p4java.impl.mapbased.rpc.func.helper.StringHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcLineEndFilterOutputStream;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType;

/**
 * Defines the various line ending mappings needed
 * for text files in the transfer between client and server.<p>
 *
 * Crucially important for Windows / Mac / Linux / Nintendo
 * (etc.) text files; explanations are given elsewhere...
 */

public enum ClientLineEnding {

  FST_L_LOCAL,    // LineTypeLocal
  FST_L_LF,        // LineTypeRaw
  FST_L_CR,        // LineTypeCr
  FST_L_CRLF,        // LineTypeCrLf
  FST_L_LFCRLF,    // LineTypeLfcrlf
  FST_L_LF_UTF_16BE,        // LineTypeRaw
  FST_L_CR_UTF_16BE,        // LineTypeCr
  FST_L_CRLF_UTF_16BE,        // LineTypeCrLf
  FST_L_LFCRLF_UTF_16BE,    // LineTypeLfcrlf
  FST_L_LF_UTF_16LE,        // LineTypeRaw
  FST_L_CR_UTF_16LE,        // LineTypeCr
  FST_L_CRLF_UTF_16LE,        // LineTypeCrLf
  FST_L_LFCRLF_UTF_16LE;    // LineTypeLfcrlf

  /**
   * The key for the system-wide line separator.
   */
  public static final String LINESEP_PROPS_KEY = "line.separator";

  /**
   * The local line end string, as retrieved from the system
   * properties. The JVM apparently guarantees that this is
   * accurate....
   */
  public static final String localLineEndStr =
      System.getProperty(LINESEP_PROPS_KEY, "\n");

  /**
   * What the Perforce server uses internally to signal line end.
   * Not coincidentally, the same as the Unix, Linux, and Mac OS X
   * line end byte.
   */
  public static final byte PERFORCE_SERVER_LINE_END_BYTE = '\n';

  public static final byte[] FST_L_LOCAL_BYTES = localLineEndStr.getBytes();
  public static final char FST_L_LF_CHAR = '\n';
  public static final char FST_L_CR_CHAR = '\r';
  public static final byte[] FST_L_LF_BYTES = new byte[]{'\n'};
  public static final byte[] FST_L_CR_BYTES = new byte[]{'\r'};
  public static final byte[] FST_L_CRLF_BYTES = new byte[]{'\r', '\n'};
  public static final byte[] FST_L_LFCRLF_BYTES = new byte[]{'\n', '\r', '\n'}; // ?? - HR

  public static final byte[] FST_L_LF_UTF_16BE_BYTES = new byte[]{(byte) 0x00, '\n'};
  public static final byte[] FST_L_CR_UTF_16BE_BYTES = new byte[]{(byte) 0x00, '\r'};
  public static final byte[] FST_L_CRLF_UTF_16BE_BYTES = new byte[]{(byte) 0x00, '\r', (byte) 0x00, '\n'};
  public static final byte[] FST_L_LFCRLF_UTF_16BE_BYTES = new byte[]{(byte) 0x00, '\n', (byte) 0x00, '\r', (byte) 0x00, '\n'};

  public static final byte[] FST_L_LF_UTF_16LE_BYTES = new byte[]{'\n', (byte) 0x00};
  public static final byte[] FST_L_CR_UTF_16LE_BYTES = new byte[]{'\r', (byte) 0x00};
  public static final byte[] FST_L_CRLF_UTF_16LE_BYTES = new byte[]{'\r', (byte) 0x00, '\n', (byte) 0x00};
  public static final byte[] FST_L_LFCRLF_UTF_16LE_BYTES = new byte[]{'\n', (byte) 0x00, '\r', (byte) 0x00, '\n', (byte) 0x00};

  public static final boolean CONVERT_TEXT = !Arrays.equals(FST_L_LOCAL_BYTES, FST_L_LF_BYTES);

  /**
   * Decode the file's actual or intended line ending type from the passed-in
   * string. The line ending stuff is usually the second char (if it exists),
   * but that may be overridden if the associated file type is a raw text
   * type (usually something to do with merges or diff / resolve, apparently),
   * in which case we return FST_L_LF.<p>
   *
   * Hence the anomalous-looking second parameter here....
   */

  public static ClientLineEnding decodeFromServerString(String str,
                                                        RpcPerforceFileType fileType) {

    if ((fileType != null) && (fileType == RpcPerforceFileType.FST_RTEXT)) {
      return FST_L_LF;    // See comments above... this overrides everything
    }

    if (str == null) {
      return FST_L_LOCAL;
    }

    // Copied wholesale from the C++ API...

    // fileType [ lineType [ uncompress ] ]

    int tl = 0;

    if (str.length() >= 2) {
      tl = StringHelper.hexcharToInt(str.charAt(1));
    }

    switch (tl) {
      case 0x1:
        return FST_L_LF;
      case 0x2:
        return FST_L_CR;
      case 0x3:
        return FST_L_CRLF;
      case 0x4:
        return FST_L_LFCRLF;

      default:
        return FST_L_LOCAL;
    }
  }


  /**
   * Determine whether we really need to translate newlines. Since the
   * server stores them internally as "\n" characters, we only need to
   * translate on systems where that's not the default -- currently
   * win and mac, but we use the system prop just to be sure...
   */
  // TODO: should use charset
  public static boolean needsLineEndFiltering(ClientLineEnding lineEndSpec) {
    if (lineEndSpec != null) {
      switch (lineEndSpec) {
        case FST_L_LOCAL:
          if (!localLineEndStr.equals(
              RpcLineEndFilterOutputStream.P4SERVER_LINSEP_STR)) {
            return true;
          }
          break;

        case FST_L_LF:
          break;

        case FST_L_CR:
        case FST_L_CRLF:
        case FST_L_LFCRLF:

        case FST_L_LF_UTF_16BE:
        case FST_L_CR_UTF_16BE:
        case FST_L_CRLF_UTF_16BE:
        case FST_L_LFCRLF_UTF_16BE:

        case FST_L_LF_UTF_16LE:
        case FST_L_CR_UTF_16LE:
        case FST_L_CRLF_UTF_16LE:
        case FST_L_LFCRLF_UTF_16LE:

          return true;
      }
    }
    return false;
  }

  /**
   * Return the byte array corresponding to the client line ending.
   * The default is {@link #FST_L_LOCAL_BYTES}.
   */

  public static byte[] getLineEndBytes(ClientLineEnding lineEndSpec) {
    if (lineEndSpec != null) {
      switch (lineEndSpec) {
        case FST_L_LOCAL:
          return FST_L_LOCAL_BYTES;

        case FST_L_LF:
          return FST_L_LF_BYTES;

        case FST_L_CR:
          return FST_L_CR_BYTES;

        case FST_L_CRLF:
          return FST_L_CRLF_BYTES;

        case FST_L_LFCRLF:
          return FST_L_LFCRLF_BYTES;

        case FST_L_LF_UTF_16BE:
          return FST_L_LF_UTF_16BE_BYTES;

        case FST_L_CR_UTF_16BE:
          return FST_L_CR_UTF_16BE_BYTES;

        case FST_L_CRLF_UTF_16BE:
          return FST_L_CRLF_UTF_16BE_BYTES;

        case FST_L_LFCRLF_UTF_16BE:
          return FST_L_LFCRLF_UTF_16BE_BYTES;

        case FST_L_LF_UTF_16LE:
          return FST_L_LF_UTF_16LE_BYTES;

        case FST_L_CR_UTF_16LE:
          return FST_L_CR_UTF_16LE_BYTES;

        case FST_L_CRLF_UTF_16LE:
          return FST_L_CRLF_UTF_16LE_BYTES;

        case FST_L_LFCRLF_UTF_16LE:
          return FST_L_LFCRLF_UTF_16LE_BYTES;
      }
    }
    return FST_L_LOCAL_BYTES;
  }

  /**
   * Convert the line endings for any string found in the map to the
   * {@link #FST_L_LF_BYTES} values from {@link #localLineEndStr}. The
   * conversion will be done in place.
   */
  public static void convertMap(Map<String, Object> map) {
    if (map != null) {
      String convertTo = new String(FST_L_LF_BYTES);
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        if (entry.getValue() instanceof String) {
          String value = ((String) entry.getValue()).replace(localLineEndStr, convertTo);
          map.put(entry.getKey(), value);
        }
      }
    }
  }
}
