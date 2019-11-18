package org.simplemes.eframe.test

import org.simplemes.eframe.data.EncodedTypeInterface
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.data.FieldDefinitions
import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A mock of the FieldDefinitions, designed to easily create the field definitions needed for marker and related testing.
 */
class MockFieldDefinitions extends FieldDefinitions {

  /**
   * Convenience constructor where each entry in the map defines a single field.
   * @param fields The map of fields.  The key is the field name, the value is the type (a Class).
   */
  MockFieldDefinitions(Map<String, Object> fields) {
    fields.each { k, v ->
      if (isSimple((Class) v)) {
        put(k, new SimpleFieldDefinition(name: k, maxLength: 40, type: (Class) v))
      } else if (EncodedTypeInterface.isAssignableFrom((Class) v)) {
        put(k, new SimpleFieldDefinition(name: k, type: (Class) v, format: EncodedTypeFieldFormat.instance))
      } else {
        // Assume a simple domain reference
        put(k, new SimpleFieldDefinition(name: k, reference: true, type: (Class) v, referenceType: (Class) v))
      }
    }
  }


  /**
   * Creates a FieldDefinitions holder for multiple fields of simple type (String/40 or a specific field def).
   * @param fieldDefs The list of field names or field definitions.  If a field starts with a '*', then it is marked as required.
   */
  MockFieldDefinitions(List fieldDefs) {
    for (fieldDef in fieldDefs) {
      if (fieldDef instanceof String) {
        def required = fieldDef.startsWith('*')
        fieldDef = fieldDef - '*'
        put(fieldDef, new SimpleFieldDefinition(name: fieldDef, maxLength: 40, type: String, required: required))
      } else if (fieldDef instanceof FieldDefinitionInterface) {
        put(fieldDef.name, fieldDef)
      }
    }
  }

  /**
   * Determines if the class is a simple class for field definition purposes.
   * @param clazz
   * @return
   */
  boolean isSimple(Class clazz) {
    return (clazz == String) || (Number.isAssignableFrom(clazz))
  }
}
