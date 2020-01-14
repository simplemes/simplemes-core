package org.simplemes.eframe.domain.validate;/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
 */

/**
 * A single validation error on a field.
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
   * Basic constructor.
   *
   * @param code      The message code (e.g. 100 will use '100.error' from the messages.properties file(s)).
   * @param fieldName The field with the error.
   */
  public ValidationError(int code, String fieldName) {
    this.code = code;
    this.fieldName = fieldName;
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
}
