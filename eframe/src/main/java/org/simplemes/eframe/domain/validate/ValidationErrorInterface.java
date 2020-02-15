/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain.validate;

/**
 * A single validation error on a field.
 */
public interface ValidationErrorInterface {
  /**
   * The name of the field with the validation error.
   *
   * @return The field name.
   */
  String getFieldName();

  /**
   * The message code.
   *
   * @return The message code.
   */
  int getCode();

  /**
   * Returns the other args for the error message.
   *
   * @return The args.
   */
  Object[] getArgs();


}