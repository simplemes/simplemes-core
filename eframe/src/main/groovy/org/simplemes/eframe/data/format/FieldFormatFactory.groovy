/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format

import org.simplemes.eframe.custom.ExtensibleFieldHelper
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
   * @param property The property definition for this field.  Can be null.
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
   * @param property The property definition for this field.  Can be null.
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
      // Only flag as ConfigurableTypeDomainFormat if the domain class has an @ExtensibleFieldHolder annotation.
      if (hasExtensibleFields(property)) {
        return ConfigurableTypeDomainFormat.instance
      }
      // No fields, so try as simple domain reference
      if (DomainUtils.instance.isDomainEntity(clazz)) {
        return DomainReferenceFieldFormat.instance
      }
    } else if (EncodedTypeInterface.isAssignableFrom(clazz)) {
      return EncodedTypeFieldFormat.instance
    } else if (DomainUtils.instance.isDomainEntity(clazz)) {
      return DomainReferenceFieldFormat.instance
    }
    return null
  }

  /**
   * Checks for extensible field holder in the property's domain class.  Null safe checks.
   * @param property The property definition for this field.  Can be null.
   * @return True if the domain class (declaring class for the property) has extensible fields.
   */
  static boolean hasExtensibleFields(PersistentProperty property) {
    def domainClass = property?.field?.declaringClass
    if (domainClass) {
      return ExtensibleFieldHelper.instance.hasExtensibleFields(domainClass)
    }
    return false
  }


}
