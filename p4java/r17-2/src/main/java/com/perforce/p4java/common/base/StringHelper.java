package com.perforce.p4java.common.base;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.annotation.Nullable;

/**
 * @author Sean Shou
 * @since 30/08/2016
 */
public class StringHelper {
  private StringHelper() { /* util */ }

  public static String format(String template, @Nullable Object... args) {
    template = String.valueOf(template); // null -> "null"

    // start substituting the arguments into the '%s' placeholders
    StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
    int templateStart = 0;
    int i = 0;
    while (i < args.length) {
      int placeholderStart = template.indexOf("%s", templateStart);
      if (placeholderStart == -1) {
        break;
      }
      builder.append(template.substring(templateStart, placeholderStart));
      builder.append(args[i++]);
      templateStart = placeholderStart + 2;
    }
    builder.append(template.substring(templateStart));

    // if we run out of placeholders, append the extra args in square braces
    if (i < args.length) {
      builder.append(" [");
      builder.append(args[i++]);
      while (i < args.length) {
        builder.append(", ");
        builder.append(args[i++]);
      }
      builder.append(']');
    }

    return builder.toString();
  }

  public static String firstNonBlank(String first, String second) {
    return isNotBlank(first) ? first : second;
  }

  public static String firstConditionIsTrue(boolean expression, String first, String second) {
    return expression ? first : second;
  }
}
