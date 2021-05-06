package org.simplemes.eframe.data.format

import org.simplemes.eframe.data.ChoiceListItemInterface
import org.simplemes.eframe.data.ConfigurableTypeInterface
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.misc.ArgumentUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the format for a field that is Configurable Type used with a domain such as Flex Type.
 * This is a open-ended list of dynamic custom fields that can be configured by the user.
 */
class ConfigurableTypeDomainFormat extends DomainReferenceFieldFormat implements ConfigurableFormatInterface {
  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static BasicFieldFormat instance = new ConfigurableTypeDomainFormat()

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   */
  public static final String ID = 'G'

  /**
   * The internal ID for the format.  Used as a short name in internal (string) representations of this field.
   */
  @Override
  String getId() {
    return ID
  }

  /**
   * Gets list of fields currently selected for the Configurable Type value.
   * These are typically treated as normal fields in the GUI and API, but are stored in the
   * custom field holder.
   *
   * @param object The domain object the field is stored in.
   * @param fieldName The name of the configurable field.  Used to find the current value and the fields.
   * @return The list.
   */
  @Override
  List<FieldDefinitionInterface> getCurrentFields(Object object, String fieldName) {
    ArgumentUtils.checkMissing(fieldName, fieldName)
    if (!object || !object[fieldName]) {
      return []
    }
    ConfigurableTypeInterface configurableType = (ConfigurableTypeInterface) object[fieldName]
    def fields = configurableType.determineInputFields()
    for (field in fields) {
      field.configTypeFieldName = fieldName
    }
    return fields
  }

  /**
   * Returns the list of valid values for those formats that use a combobox or similar widget.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The list of valid values.
   */
  @Override
  List<ChoiceListItemInterface> getValidValues(FieldDefinitionInterface fieldDefinition) {
    def list = super.getValidValues(fieldDefinition)

    // Mark the default value, if any provided.
    for (choice in list) {
      if (choice.value instanceof ChoiceListItemInterface) {
        choice.defaultChoice = choice.value.isDefaultChoice()
      }
    }


    return list
  }

  /**
   * Returns the client format type code.  Used by the Vue client logic only.
   * @return The client code.
   */
  @Override
  String getClientFormatType() {
    // This is treated as list of valid choices on the client.
    return EnumFieldFormat.instance.id
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  String toString() {
    return 'Configurable Type'
  }
}
