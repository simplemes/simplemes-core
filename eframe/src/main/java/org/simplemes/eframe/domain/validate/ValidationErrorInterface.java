package org.simplemes.eframe.domain.validate;

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
 */

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


}