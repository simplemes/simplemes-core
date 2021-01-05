/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.misc.TypeUtils

/**
 * Defines common logic used for the generic fields (e.g. FlexField and FieldExtension).
 * Only valid in a domain that implements FieldInterface.
 */
trait FieldTrait {
  def validate() {
    if (fieldName && !NameUtils.isLegalIdentifier(fieldName)) {
      //error.201.message="{1}" is not a legal custom field name.  Must be a legal Java variable name.
      return new ValidationError(201, 'fieldName', fieldName)
    }
    if (guiHints) {
      def ex = null
      try {
        TextUtils.parseNameValuePairs(guiHints)
      } catch (Exception e) {
        ex = e
      }
      if (ex) {
        //error.209.message="{1}" is not a valid GUI Hint for field {2}.  The hint must be in the format ''name1="value" name2="value"'' format.  Error: {3}
        return new ValidationError(209, 'guiHints', guiHints, fieldName, ex.toString())
      }
    }
    return validateValueClassName()
  }


  /**
   * Validates that the class name is correct for the current FieldFormat and is legal.
   */
  def validateValueClassName() {
    //noinspection GrEqualsBetweenInconvertibleTypes
    if (fieldFormat == EnumFieldFormat.instance) {
      if (!valueClassName) {
        //error.1.message=Required value is missing "{0}" ({1}).
        return new ValidationError(1, 'valueClassName', this.getClass().simpleName)
      }
      try {
        def clazz = TypeUtils.loadClass(valueClassName)
        if (!clazz.isEnum()) {
          //error.202.message={0} ({1}) is not an enumeration.
          return new ValidationError(202, 'valueClassName', valueClassName)
        }
      } catch (ClassNotFoundException ignored) {
        //error.4.message=Invalid "{0}" class.  Class "{1}" not found.
        return new ValidationError(4, 'valueClassName', valueClassName)
      }
    }
    return null
  }

}