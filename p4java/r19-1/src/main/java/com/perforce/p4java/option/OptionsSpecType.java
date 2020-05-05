package com.perforce.p4java.option;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwOptionsExceptionIfConditionFails;
import static com.perforce.p4java.option.server.OptionsHelper.applyRule;
import static com.perforce.p4java.option.server.OptionsHelper.objectToBoolean;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.perforce.p4java.exception.OptionsException;

/**
 * @author Sean Shou
 * @since 8/09/2016
 */
public enum OptionsSpecType {
  IntOption("i") {
    @Override
    String getP4CommandOptionField(String ruleName, String serverOptionStr, Object optValue) throws OptionsException {
      try {
        String securityOptValue = String.valueOf(optValue);
        return applyRule(ruleName, serverOptionStr, Integer.parseInt(securityOptValue));
      } catch (Exception e) {
        throw new OptionsException(e);
      }
    }
  },

  LongOption("l") {
    @Override
    String getP4CommandOptionField(String ruleName, String serverOptionStr, Object optValue) throws OptionsException {
      try {
        String securityOptValue = String.valueOf(optValue);
        return applyRule(ruleName, serverOptionStr, Long.parseLong(securityOptValue));
      } catch (Exception e) {
        throw new OptionsException(e);
      }
    }
  },

  BoolOption("b") {
    @Override
    String getP4CommandOptionField(String ruleName, String serverOptionStr, Object optValue) throws OptionsException {
      try {
        return applyRule(ruleName, serverOptionStr, objectToBoolean(optValue));
      } catch (Exception e) {
        throw new OptionsException(e);
      }
    }
  },

  StringOption("s"),

  StringArrayOption("s[]") {
    @Override
    List<String> getP4CommandOptionFields(String ruleName, String serverOptionStr, Object optValue) throws OptionsException {
      List<String> p4CommandOptionFields = new ArrayList<>();
      if (nonNull(optValue)) {
    	  throwOptionsExceptionIfConditionFails(
    			  (optValue instanceof String[]),
    			  "Invalid opt value, expected '%s' is a string array object", optValue);

        for (String opt : (String[]) optValue) {
          String p4CommandOptionField = applyRule(ruleName, serverOptionStr, opt);
          if (isNotBlank(p4CommandOptionField)) {
            p4CommandOptionFields.add(p4CommandOptionField);
          }
        }
      }

      return p4CommandOptionFields;
    }
  };

  private static final Map<String, OptionsSpecType> p4CommandOptionsMap = new HashMap<>();

  static {
    for (OptionsSpecType optionsSpecType : OptionsSpecType.values()) {
      p4CommandOptionsMap.put(optionsSpecType.optSpecType, optionsSpecType);
    }
  }

  private String optSpecType;

  OptionsSpecType(String optSpecType) {
    this.optSpecType = optSpecType;
  }

  static OptionsSpecType of(String optSpecType) throws OptionsException {
    OptionsSpecType p4CommandOptions = p4CommandOptionsMap.get(optSpecType);
    if (nonNull(p4CommandOptions)) {
      return p4CommandOptions;
    }

    throw new OptionsException("Unknown option spec type");
  }

  List<String> getP4CommandOptionFields(String ruleName, String serverOptionStr, Object optValue) throws OptionsException {
    List<String> p4CommandOptionFields = new ArrayList<>();
    String p4CommandOptionFiled = getP4CommandOptionField(ruleName, serverOptionStr, optValue);
    if (isNotBlank(p4CommandOptionFiled)) {
      p4CommandOptionFields.add(p4CommandOptionFiled);
    }
    return p4CommandOptionFields;
  }

  String getP4CommandOptionField(String ruleName, String serverOptionStr, Object optValue) throws OptionsException {
    String optionField = EMPTY;
    if (nonNull(optValue)) {
      throwOptionsExceptionIfConditionFails(
    		  (optValue instanceof String),
              "Invalid opt value, expected '%s' to be a string", optValue);
      optionField = applyRule(ruleName, serverOptionStr, Objects.toString(optValue));
    }

    return optionField;
  }
}
