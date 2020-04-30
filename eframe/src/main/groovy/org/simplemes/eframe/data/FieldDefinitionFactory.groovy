/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data

import org.simplemes.eframe.domain.PersistentProperty
import org.simplemes.eframe.misc.ArgumentUtils

import java.lang.reflect.Field

/**
 * Builds the correct field definition for a given property of class field.
 */
class FieldDefinitionFactory {

  /**
   * Builds the correct field definition for a persistent entity property.
   * @param property The presisted property.
   */
  static FieldDefinitionInterface buildFieldDefinition(PersistentProperty property) {
    ArgumentUtils.checkMissing(property, 'property')
    return new SimpleFieldDefinition(property)
  }

  /**
   * Builds the correct field definition for a normal Java field.
   * @param field The field.
   */
  static FieldDefinitionInterface buildFieldDefinition(Field field) {
    ArgumentUtils.checkMissing(field, 'field')
    return new SimpleFieldDefinition(field)
  }


  static List<String> ignoreOnCopyProperties = ['class']

  /**
   * Create a new instance of this field definition and adjust the copy's fields
   * with the given options.
   * @param original The original field definition.
   * @param options The options to apply to the copy.
   * @return The copy.
   */
  static FieldDefinitionInterface copy(FieldDefinitionInterface original, Map options = null) {
    def res = new SimpleFieldDefinition()
    for (property in original.properties) {
      def key = (String) property.key
      if (!ignoreOnCopyProperties.contains(key)) {
        res[key] = original[key]
      }
    }
    options?.each { k, v ->
      res[(String) k] = options[(String) k]
    }
    return res
  }

}
