/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.exception


import org.simplemes.eframe.domain.validate.ValidationErrorInterface
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils

/**
 * Defines a validation exception that is triggered when a domain object  save() fails the validation.
 */
class ValidationException extends BusinessException {

  List<ValidationErrorInterface> errors

  Object object

  /**
   * The basic constructor.
   *
   * @param errors The errors.
   * @param object The object the error is on.
   */
  ValidationException(List<ValidationErrorInterface> errors, Object object) {
    //error.3.message=Validation Failed: {0}
    setCode(3)
    setParams([errors])
    this.errors = errors
    this.object = object
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
    def className = object?.getClass()?.simpleName ?: "Unknown"
    def objectString = TypeUtils.toShortString(object) ?: ''
    return GlobalUtils.lookup("error.${code}.message", locale, s, className, objectString) + " (${code})"
  }

}
