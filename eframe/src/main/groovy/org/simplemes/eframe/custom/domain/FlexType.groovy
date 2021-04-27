/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Transient
import io.micronaut.data.model.DataType
import org.simplemes.eframe.custom.ConfigurableTypeFieldDefinition
import org.simplemes.eframe.data.ChoiceListItemInterface
import org.simplemes.eframe.data.ConfigurableTypeInterface
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.misc.FieldSizes

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.OneToMany

/**
 * Defines the basic user-defined data types.
 * The category can be extended in sub-classes at the application level.
 * <p/>
 * The actual data values are typically stored in text fields.
 */
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ["flexType"])
@JsonIgnoreProperties(['value', 'defaultChoice'])
@ToString(includePackage = false, includeNames = true, excludes = ['flexType'])
class FlexType implements ConfigurableTypeInterface, ChoiceListItemInterface {

  /**
   * The BASIC category type.
   */
  public static final String CATEGORY_BASIC = 'BASIC'

  /**
   * The flexible category name.  This is used to restrict use of the type to specific areas of the application.
   * Each domain object that uses a FlexType should specify the category.<p/>
   * A default Category of 'BASIC' is provided by the framework, but it should not be used frequently.
   * (<b>Default:</b>'BASIC').  <b>Required.</b>
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String category = CATEGORY_BASIC

  /**
   * The flexible type name.
   * <b>Required.</b>
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  @MappedProperty(type = DataType.STRING, definition = 'VARCHAR(30) UNIQUE')
  String flexType

  /**
   * The title (short) of this flex type.  Used for many displays/lists.
   */
  @Column(length = FieldSizes.MAX_TITLE_LENGTH)
  @Nullable String title

  /**
   * A list of data entry fields for this flex type.<p/>
   * <b>Required.</b>
   */
  @OneToMany(mappedBy = "flexType")
  List<FlexField> fields

  /**
   * If true, then this is the default FlexType to use if one is not specified.
   */
  boolean defaultFlexType = false

  /**
   * A transient list of the fields defined for this flex type.
   */
  @Transient String fieldSummary

  @SuppressWarnings('unused')
  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @SuppressWarnings('unused')
  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  Integer version = 0

  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

  /**
   * Defines the order the fields are shown in the edit/show/etc GUIs.
   */
  @SuppressWarnings("unused")
  static fieldOrder = ['flexType', 'category', 'title', 'defaultFlexType', 'fields']

  def validate() {
    if (fields?.size() <= 0) {
      //error.200.message=The list value ({0}) must have at least one entry in it.
      return new ValidationError(200, 'fields')
    }
    return null
  }

  /**
   * Called before validate happens.
   */
  @SuppressWarnings("unused")
  def beforeSave() {
    // Now, auto-assign Sequences to the fields if needed.
    for (int i = 0; i < fields?.size(); i++) {
      if (fields[i].sequence == 0 || fields[i].sequence == null) {
        fields[i].sequence = i + 1
      }
    }
  }

  /**
   * Build human readable version of this object.
   * @return
   */
  @Override
  String toString() {
    return flexType
  }

  /**
   * Gets the localized display name for this type of configurable object.  This is typically displayed in drop-down lists
   * and other display locations.
   * @param locale The locale to display the name from (if null, defaults to the request locale).
   * @return The display name.
   */
  @Override
  String toStringLocalized(Locale locale) {
    return flexType
  }

  /**
   * Returns the localized display value for the choice (uses the default locale).
   * @return The display value.
   */
  @Override
  String toStringLocalized() {
    return flexType
  }

  /**
   * Returns the value for the choice list (e.g. a domain record or enum element).
   * @return The value.
   */
  @Override
  @Transient
  Object getValue() {
    return this
  }

  /**
   * Returns internal ID for this choice.
   * @return The ID.
   */
  @Override
  @Transient
  Object getId() {
    return uuid
  }

  /**
   * Returns true if this choice is the default choice in the list.
   * @return True if it is the default.
   */
  @Override
  boolean isDefaultChoice() {
    return defaultFlexType
  }

  /**
   * Builds the fields needed to configure this type.  This will be used to build the GUI field HTML elements.
   * @param configurableTypeFieldName The name of the Configurable Type field that the caller wants the fields for.
   *                                  This is used to specify the prefix for the values in the custom field holder.
   * @return The list of fields needed to configure this type.  These fields should match the names and types of the
   *         related flex type.
   */
  @Override
  List<FieldDefinitionInterface> determineInputFields(String configurableTypeFieldName) {
    def list = []
    for (field in getFields()) {
      list << new ConfigurableTypeFieldDefinition(field, configurableTypeFieldName)
    }
    return list
  }

  /**
   *
   * Builds the transient list of the fields defined for this flex type.
   * This is a comma-delimited list of fields.
   * @return The list of fields as a string.
   */
  String getFieldSummary() {
    if (fieldSummary) {
      return fieldSummary
    }
    StringBuilder sb = new StringBuilder()
    for (field in fields) {
      if (sb) {
        sb << ', '
      }
      sb << field.fieldName
    }

    fieldSummary = sb.toString()
    return fieldSummary
  }

}
