package org.simplemes.eframe.custom.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import org.simplemes.eframe.custom.ConfigurableTypeFieldDefinition
import org.simplemes.eframe.data.ChoiceListItemInterface
import org.simplemes.eframe.data.ConfigurableTypeInterface
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.data.annotation.ExtensibleFields
import org.simplemes.eframe.misc.FieldSizes

/**
 * Defines the basic user-defined data types.
 * The category can be extended in sub-classes at the application level.
 * <p/>
 * The actual data values are typically stored in text fields.
 */
@Entity
@ExtensibleFields()
@EqualsAndHashCode(includes = ["flexType"])
@JsonIgnoreProperties(['value'])
//@ToString(includePackage = false, includeNames = true)
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
  String category = CATEGORY_BASIC

  /**
   * The flexible type name.
   * <b>Required.</b>
   */
  String flexType

  /**
   * The title (short) of this flex type.  Used for many displays/lists.
   */
  String title

  /**
   * A list of data entry fields for this flex type.<p/>
   * <b>Required.</b>
   */
  List<FlexField> fields = []

  /**
   * A transient list of the fields defined for this flex type.
   */
  String fieldSummary

  @SuppressWarnings("unused")
  static hasMany = [fields: FlexField]

  /**
   * If true, then this is the default FlexType to use if one is not specified.
   */
  boolean defaultFlexType = false

  /**
   * The date this record was last updated.
   */
  Date lastUpdated

  /**
   * The date this record was created
   */
  @SuppressWarnings("unused")
  Date dateCreated

  @SuppressWarnings("unused")
  static mapping = {
    sort "flexType"
    fields lazy: 'join'
    cache true
  }

  @SuppressWarnings("unused")
  static constraints = {
    flexType(maxSize: FieldSizes.MAX_CODE_LENGTH, blank: false, unique: true)
    category(maxSize: FieldSizes.MAX_CODE_LENGTH, blank: false)
    title(maxSize: FieldSizes.MAX_TITLE_LENGTH, nullable: true, blank: true)
    fields(minSize: 1)
  }

  /**
   * The primary keys for this object.
   */
  //static keys = ['flexType', 'category']

  /**
   * Defines the order the fields are shown in the edit/show/etc GUIs.
   */
  @SuppressWarnings("unused")
  static fieldOrder = ['flexType', 'category', 'title', 'defaultFlexType', 'fields']

  /**
   * Internal transient field list.
   */
  static transients = ['fieldSummary']

  /**
   * Called before validate happens.
   */
  @SuppressWarnings("unused")
  def beforeValidate() {
    // Now, auto-assign Sequences to the fields if needed.
    for (int i = 0; i < fields.size(); i++) {
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
   * Returns the value for the choice list (e.g. a domain record of enum element).
   * @return The value.
   */
  @Override
  Object getValue() {
    return this
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
    for (field in fields) {
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
