package org.simplemes.eframe.data

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Contains all of the fields defined for domain/POGO object.  This has map and list access methods.  The list is
 * not sorted in any specific order.
 */
class FieldDefinitions extends HashMap<String, FieldDefinitionInterface> {

  /**
   * The list of fields covered by this grouping.
   */
  //Map<String, FieldDefinitionInterface> fieldDefinitions = [:]

  void add(FieldDefinitionInterface fieldDef) {
    this[fieldDef.name] = fieldDef
  }

  /**
   * Gets the given field (by name).
   * @param key The field name.
   * @return The Field Definition
   */
/*
  FieldDefinitionInterface getAt(Object key) {
    return fieldDefinitions[key as String]
  }
*/
  /**
   * Returns an iterator for all fields in the this definition.
   * @return
   */
  Iterator iterator() {
    return values().iterator()
  }

  /**
   * Add a new entry to the the field definition.
   * @param fieldDef
   */
  void leftShift(FieldDefinitionInterface fieldDef) {
    put(fieldDef.name, fieldDef)
  }

  /**
   * Returns a shallow copy of this instance.  The field list is cloned.
   *
   * @return a shallow copy of this map
   */
  @Override
  Object clone() {
    return super.clone()
  }


  /**
   * Build human-readable form.
   * @return
   */
  @Override
  String toString() {
    return "FieldDefinitions{" + super.toString() + '}'
  }
}
