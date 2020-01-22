/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format

import org.simplemes.eframe.data.ConfigurableTypeInterface
import org.simplemes.eframe.data.EncodedTypeInterface
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.domain.PersistentProperty
import org.simplemes.eframe.misc.ArgumentUtils

/**
 * Builds the correct field format for the given object class.
 */
class FieldFormatFactory {

  /**
   * Builds the correct field format for the given class.
   * @param clazz The clazz to build the format for.  Can be null.
   * @param property The property definition for this field.  Cane be null.
   */
  static FieldFormatInterface build(Class clazz, PersistentProperty property = null) {
    ArgumentUtils.checkMissing(clazz, 'clazz')
    switch (clazz) {
      case String:
        return StringFieldFormat.instance
      case Integer:
      case int:
        return IntegerFieldFormat.instance
      case Long:
      case long:
        return LongFieldFormat.instance
      case BigDecimal:
        return BigDecimalFieldFormat.instance
      case Boolean:
      case boolean:
        return BooleanFieldFormat.instance
      case DateOnly:
        return DateOnlyFieldFormat.instance
      case Date:
        return DateFieldFormat.instance
    }
    return checkOtherTypes(clazz, property)
  }

  /**
   * Check for other, more complex types.
   * @param clazz The class.
   * @param property The property definition for this field.  Cane be null.
   * @return The format for the field.
   */
  static FieldFormatInterface checkOtherTypes(Class clazz, PersistentProperty property = null) {
    if (clazz.isEnum()) {
      return EnumFieldFormat.instance
    } else if (Collection.isAssignableFrom(clazz)) {
      if (property?.referenceType) {
        if (!(property.child)) {
          return DomainRefListFieldFormat.instance
        }
      }
      return ChildListFieldFormat.instance
    } else if (ConfigurableTypeInterface.isAssignableFrom(clazz)) {
      return ConfigurableTypeDomainFormat.instance
    } else if (EncodedTypeInterface.isAssignableFrom(clazz)) {
      return EncodedTypeFieldFormat.instance
    } else if (DomainUtils.instance.isDomainEntity(clazz)) {
      return DomainReferenceFieldFormat.instance
    }
    return null
  }

}
