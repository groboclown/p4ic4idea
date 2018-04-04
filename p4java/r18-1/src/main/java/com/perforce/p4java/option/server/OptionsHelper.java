package com.perforce.p4java.option.server;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwOptionsException;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwOptionsExceptionIfConditionFails;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Objects;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;

/**
 * @author Sean Shou
 * @since 8/09/2016
 */
public class OptionsHelper {
  private OptionsHelper() { /* util */ }

  /**
   * String used to prefix options for the server. This is pretty fundamental;
   * don't change this unless you really know what you're doing....
   */
  public static final String OPTPFX = "-";

  /**
   * Apply an optional rule to a boolean option value. This method is always
   * called by the default implementation of Options.processOptions to process
   * boolean values; you should override this if you need your own rules processing.<p>
   *
   * There are currently no rules recognised or implemented in this method.
   *
   * @param ruleName     rule name string from the options spec string. If null, no rule was
   *                     specified.
   * @param serverOptStr the flag string to be sent to the Perforce server prefixing this value
   * @param value        the boolean value itself.
   * @return processed value or null if the rules resulted in nothing to be sent to the Perforce
   * server.
   * @throws OptionsException if any errors occurred during options processing.
   */
  public static String applyRule(String ruleName, String serverOptStr, boolean value) throws OptionsException {
    throwOptionsExceptionIfConditionFails(isNotBlank(serverOptStr), "Null or Empty server options spec");

    if (isBlank(ruleName)) {
      if (value) {
        return OPTPFX + serverOptStr;
      }
    } else {
      throwOptionsException("Unrecognized option rule name in options parser: '%s'", ruleName);
    }

    return EMPTY;
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
   * @param ruleName     rule name string from the options spec string. If null, no rule was
   *                     specified.
   * @param serverOptStr the flag string to be sent to the Perforce server prefixing this value
   * @param value        the integer value itself.
   * @return processed value or null if the rules resulted in nothing to be sent to the Perforce
   * server.
   * @throws OptionsException if any errors occurred during options processing.
   */
  public static String applyRule(String ruleName, String serverOptStr, int value) throws OptionsException {
    throwOptionsExceptionIfConditionFails(isNotBlank(serverOptStr), "Null or Empty server options spec");

    if (isBlank(ruleName)) {
      return OPTPFX + serverOptStr + value;
    } else {
      if ("gtz".equals(ruleName) || "clz".equals(ruleName)) {
        if (value > 0) {
          return OPTPFX + serverOptStr + value;
        }
      } else if ("cl".equals(ruleName)) {
        if (value >= 0) {
          return OPTPFX + serverOptStr + (value == IChangelist.DEFAULT ? "default" : value);
        }
      } else if ("dcn".equals(ruleName)) {
        if (value > 0) {
          return OPTPFX + serverOptStr + value;
        } else if (value == 0) {
          return OPTPFX + serverOptStr;
        }
      } else {
        throwOptionsException("Unrecognized option rule name in options parser: '%s'", ruleName);
      }
    }

    return EMPTY;
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
   * @param ruleName     rule name string from the options spec string. If null, no rule was
   *                     specified.
   * @param serverOptStr the flag string to be sent to the Perforce server prefixing this value
   * @param value        the long value itself.
   * @return processed value or null if the rules resulted in nothing to be sent to the Perforce
   * server.
   * @throws OptionsException if any errors occurred during options processing.
   */
  public static String applyRule(String ruleName, String serverOptStr, long value) throws OptionsException {
    throwOptionsExceptionIfConditionFails(isNotBlank(serverOptStr), "Null or Empty server options spec");

    if (isBlank(ruleName)) {
      return OPTPFX + serverOptStr + value;
    } else {
      if ("gtz".equals(ruleName)) {
        if (value > 0) {
          return OPTPFX + serverOptStr + value;
        }
      } else if ("gez".equals(ruleName)) {
        if (value >= 0) {
          return OPTPFX + serverOptStr + value;
        }
      } else {
        throwOptionsException("Unrecognized option rule name in options parser: '%s'", ruleName);
      }
    }

    return EMPTY;
  }

  /**
   * Apply an optional rule to a string option value. This method is always
   * called by the default implementation of Options.processOptions to process
   * string values; you should override this if you need your own rules processing.<p>
   *
   * There are currently no rules recognised or implemented in this method.
   *
   * @param ruleName     rule name string from the options spec string. If null, no rule was
   *                     specified.
   * @param serverOptStr the flag string to be sent to the Perforce server prefixing this value
   * @param value        the string value itself; may be null.
   * @return processed value or null if the rules resulted in nothing to be sent to the Perforce
   * server.
   * @throws OptionsException if any errors occurred during options processing.
   */
  public static String applyRule(String ruleName, String serverOptStr, String value) throws OptionsException {
    throwOptionsExceptionIfConditionFails(isNotBlank(serverOptStr), "Null or Empty server options spec");

    if (isBlank(ruleName)) {
      if (isNotBlank(value)) {
        return OPTPFX + serverOptStr + value;
      }
    } else {
      throwOptionsException("Unrecognized option rule name in options parser: '%s'", ruleName);
    }

    return EMPTY;
  }

  public static boolean objectToBoolean(Object optValue) throws OptionsException {
    throwOptionsExceptionIfConditionFails(nonNull(optValue), ".option value can not be NULL");
    if (optValue instanceof String) {
      String value = String.valueOf(optValue);
      if ("true".equalsIgnoreCase(value)) {
        return true;
      } else if ("false".equalsIgnoreCase(value)) {
        return false;
      } else {
        throwOptionsException("Invalid boolean type options value: %s. \nBoolean type options value is only 'true' or 'false' (case insensitive).", Objects.toString(optValue));
      }
    }

    if (optValue instanceof Boolean) {
      return (boolean) optValue;
    }
    throw new OptionsException("Invalid boolean type options value: " + Objects.toString(optValue));
  }
}
