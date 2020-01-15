package org.simplemes.eframe.domain.validate;

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
 */

import org.simplemes.eframe.i18n.GlobalUtils;

/**
 * A single validation error on a field.  The display value (toString()) uses the messages.properties file
 * to find the matching error string, with replaceable parameters.  The key in the .properties file is
 * "error.$code.message".  The first argument ({0}) is the fieldName for this error.
 */
public class ValidationError implements ValidationErrorInterface {

  /**
   * The message code.
   */
  int code;

  /**
   * The field with the validation error.
   */
  String fieldName;

  /**
   * The args for the formatted message.
   */
  Object[] args;

  /**
   * Basic constructor.
   *
   * @param code      The message code (e.g. 100 will use '100.error' from the messages.properties file(s)).
   * @param fieldName The field with the error.
   */
  public ValidationError(int code, String fieldName, Object... args) {
    this.code = code;
    this.fieldName = fieldName;
    this.args = args;
  }

  /**
   * The name of the field with the validation error.
   *
   * @return The field name.
   */
  @Override
  public String getFieldName() {
    return fieldName;
  }

  /**
   * The message code.
   *
   * @return The message code.
   */
  @Override
  public int getCode() {
    return code;
  }

  @Override
  public String toString() {
    Object[] argsWithFieldName = new Object[args.length + 1];
    argsWithFieldName[0] = fieldName;
    System.arraycopy(args, 0, argsWithFieldName, 1, args.length);
    return GlobalUtils.lookup("error." + code + ".message", null, argsWithFieldName);
  }
}
