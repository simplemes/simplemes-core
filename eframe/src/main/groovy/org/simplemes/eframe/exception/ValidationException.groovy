package org.simplemes.eframe.exception

import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines a validation exception that is triggered when a domain object  save() fails the validation.
 */
class ValidationException extends BusinessException {

  List<ValidationError> errors

  /**
   * The basic constructor.
   */
  ValidationException(List<ValidationError> errors) {
    //error.3.message=Validation Failed: {0}
    setCode(3)
    setParams([errors])
    this.errors = errors
  }

  /**
   * Build a human-readable version of this exception.
   * @param locale The locale to display the enum display text.
   * @return The human-readable string.
   */
  @Override
  String toStringLocalized(Locale locale = null) {
    def sb = new StringBuilder()
    for (error in errors) {
      if (sb) {
        sb << ','
      }
      sb << error.toString(locale)
    }
    def s = "[$sb]"
    return GlobalUtils.lookup("error.${code}.message", locale, s) + " (${code})"
  }

}
