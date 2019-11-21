package org.simplemes.eframe.data

import org.simplemes.eframe.data.format.FieldFormatInterface

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines fields used by the framework.
 * This used to handling parsing/formatting and defining features used for
 * The sub-classes provide support for XML-String storage and POGO storage of the custom values.
 * Most of the details are hidden by the sub-classes.
 */
interface FieldDefinitionInterface {
  /**
   * The field's internal name.  This must be a legal Groovy field name.
   */
  String getName()

  /**
   * The display label.  If not provided, then the field name +".label" is used.  This value can be looked up
   * in the messages.properties file.
   */
  String getLabel()

  /**
   * The format of this field (e.g. String, integer, etc).
   * <b>This should never change on existing records without database migration!</b>
   * It is possible to change to a String format for most cases, but most other changes will require a migration.
   */
  FieldFormatInterface getFormat()

  /**
   * The maximum length of the custom value (Strings only).
   * 0 or null means no limit (except for column limitations).
   */
  Integer getMaxLength()

  /**
   * The display sequence.
   */
  int getSequence()

  /**
   * The basic field type.
   */
  Class getType()

  /**
   * If true, then this field is a reference to another persistent entity.
   */
  boolean isReference()

  /**
   * The class defined for the referenced element (mainly used for List, enumerations and encoded types).
   */
  Class getReferenceType()

  /**
   * If true, then this field is a wholly-owned child of the entity.
   */
  boolean isChild()

  /**
   * If true, then this field is a reference the parent entity.
   */
  boolean isParentReference()

  /**
   * If true, then this field is flagged as required.
   */
  boolean isRequired()

  /**
   * If true, then this field is a custom field.
   */
  boolean isCustom()

  /**
   * The GUI hints (field-specific parameters) to pass to a marker.
   */
  Map getGuiHints()

  /**
   * The database ID for the custom field.
   */
  //Long getId()

  /**
   * A static list of valid values for the input field (used to text/string fields only).
   */
  //List getValidValues()

  /**
   * The context this field is defined for (typically a domain class name string).
   */
  //Object getContext()

  /**
   * The source of this field definition (usually the class name of the source object (e.g. FieldExtension).
   */
  //Object getSource()

  /**
   * True, if this is a user-extension.  False if it comes from an addition module.
   */
  //boolean isUserExtension()

  /**
   * The class that provides the field values.  Currently, this only supports Enumerations.
   */
  //String getValueClassName()

  /**
   * Some GUI hints, stored as name/value paris in a standard map.
   */
  //Map<String, String> getGuiHints()

  /**
   * Gets the given field value from the given object (domain or POGO depending on sub-class).
   *
   * @param domainOrPogoObject The domain or POGO object the field is stored in.
   * @return The value of the field.
   */
  Object getFieldValue(Object domainOrPogoObject)

  /**
   * Sets the given field value in the given object (domain or POGO depending on sub-class).
   *
   * @param domainOrPogoObject The domain or POGO object the field is to be stored in.
   * @param value The field value.
   */
  void setFieldValue(Object domainOrPogoObject, Object value)

  /**
   * Create a new instance of this field definition and adjust the copy's fields
   * with the given options.
   * @param original The original field definition.
   * @param options The options to apply to the copy.
   * @return The copy.
   */
  //FieldDefinitionInterface copy(FieldDefinitionInterface original,Map options)
}