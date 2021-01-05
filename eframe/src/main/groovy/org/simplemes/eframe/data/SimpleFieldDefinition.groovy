/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data

import groovy.transform.ToString
import org.simplemes.eframe.data.format.FieldFormatFactory
import org.simplemes.eframe.data.format.FieldFormatInterface
import org.simplemes.eframe.domain.PersistentProperty
import org.simplemes.eframe.misc.ArgumentUtils

import java.lang.reflect.Field

/**
 * A simple field definition.  Typically corresponds to String, Integer, etc fields in a domain/POGO.
 */
@ToString(includePackage = false, includeNames = true)
class SimpleFieldDefinition implements FieldDefinitionInterface {
  /**
   * The field's internal name.  This must be a legal Groovy field name.
   */
  String name

  /**
   * The field's Database column name.
   */
  String columnName

  /**
   * The display label.  If not provided, then the field name is used.
   */
  String label

  /**
   * The format of this field (e.g. String, integer, etc).
   * <b>This should never change on existing records without database migration!</b>
   * It is possible to change to a String format for most cases, but most other changes will require a migration.
   */
  FieldFormatInterface format

  /**
   * The maximum length of the custom value (Strings only).
   * 0 or null means no limit (except for column limitations).
   */
  Integer maxLength

  /**
   * The display sequence.
   */
  int sequence

  /**
   * The basic field type.
   */
  Class type

  /**
   * If true, then this field is a reference to another persistent entity.
   * Only valid for persistent properties cases.
   */
  boolean reference = false

  /**
   * The basic field type.
   */
  Class referenceType

  /**
   * If true, then this field is a reference to the parent persistent entity.
   * Only valid for persistent properties cases.
   */
  boolean parentReference = false

  /**
   * If true, then this field is a wholly-owned child of the entity.
   * Only valid for persistent properties cases.
   */
  boolean child = false

  /**
   * If true, then this field is the primary key UUID (for the DB).
   * Only valid for persistent properties cases.
   */
  boolean primaryUuid = false

  /**
   * If true, then this field marked as required.
   */
  boolean required = false

  /**
   * If true, then this field is a custom field.
   */
  boolean custom = false

  /**
   * The GUI hints (field-specific parameters) to pass to a marker.
   */
  Map guiHints

  /**
   * The persistent property for the definition.  May be null.
   */
  PersistentProperty property


  /**
   * Empty constructor.
   */
  SimpleFieldDefinition() {
  }

  /**
   * Basic constructor.
   * @param options The options.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  SimpleFieldDefinition(Map options) {
    options.each { key, value ->
      this[key] = value
    }
    setDefaultValues()
  }

  /**
   * Basic constructor for a Java reflection Field.
   * @param field The field.
   */
  SimpleFieldDefinition(Field field) {
    this(new PersistentProperty(field))
  }

  /**
   * Basic constructor for persistent property.
   * @param options The options.
   */
  SimpleFieldDefinition(PersistentProperty property) {
    ArgumentUtils.checkMissing(property, 'property')
    name = property.name
    columnName = property.columnName
    type = property.type
    this.property = property
    referenceType = property.referenceType
    reference = (property.referenceType != null)
    if (property.referenceType) {
      reference = true
      child = property.child
      if (!child) {
        parentReference = property.parentReference
      }
    }
    if (type == String) {
      maxLength = property.maxLength
    }
    required = !property.nullable
    primaryUuid = (name == 'uuid' && type == UUID)
    setDefaultValues()
  }

  /**
   * Sets the default values (e.g. for the label), if none are provided.
   */
  void setDefaultValues() {
    label = label ?: "${name}.label"
  }

  /**
   * Gets the field format.
   * @return The format (can be null).
   */
  FieldFormatInterface getFormat() {
    if (!format) {
      if (type) {
        format = FieldFormatFactory.build(type, property)
      }
    }
    return format
  }

  /**
   * Gets the given field value from the given object (domain or POGO depending on sub-class).
   *
   * @param domainOrPogoObject The domain or POGO object the field is stored in.
   * @return The value of the field.
   */
  @Override
  Object getFieldValue(Object domainOrPogoObject) {
    if (!domainOrPogoObject) {
      return null
    }
    return domainOrPogoObject[name]
  }

  /**
   * Sets the given field value in the given object (domain or POGO depending on sub-class).
   *
   * @param domainOrPogoObject The domain or POGO object the field is to be stored in.
   * @param value The field value.
   */
  @Override
  void setFieldValue(Object domainOrPogoObject, Object value) {
    domainOrPogoObject[name] = value
  }

}
