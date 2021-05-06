package org.simplemes.eframe.system


import groovy.transform.ToString
import org.simplemes.eframe.data.ChoiceListInterface
import org.simplemes.eframe.data.ChoiceListItemInterface
import org.simplemes.eframe.data.EncodedTypeInterface
import org.simplemes.eframe.data.EncodedTypeListUtils
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.NameUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines a basic enabled/disabled status that can be applied to domain objects.
 * Also provides choice list support for core statuses and possible add-on statuses from other modules.
 */
@ToString(includeNames = true, includePackage = false)
abstract class BasicStatus implements EncodedTypeInterface, ChoiceListInterface, ChoiceListItemInterface {

  /**
   * A list of classes that are valid Basic Statuses.
   */
  static List<Class> coreValues = [EnabledStatus, DisabledStatus]

  /**
   * Returns true if this status means that the object is enabled.
   * @return True if enabled.
   */
  abstract boolean isEnabled()

  /**
   * Returns true if this choice is the default choice in the list.
   * @return True if it is the default.
   */
  @Override
  boolean isDefaultChoice() {
    return this.defaultChoice
  }

  /**
   * True if this choice is the default choice in the list.
   */
  Boolean defaultChoice = false

  /**
   * Returns the instance for the given DB ID.
   * @param id The ID (e.g. 'ENABLED')
   * @return The corresponding status (can be null if ID is not valid or null).
   */
  static BasicStatus valueOf(String id) {
    def entry = EncodedTypeListUtils.instance.getAllValues(this).find { it.instance.id == id }
    return entry?.instance
  }

  /**
   * Returns the list of valid values for those formats that use a combobox or similar widget.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The list of valid values.
   */
  static List<ChoiceListItemInterface> getValidValues(FieldDefinitionInterface fieldDefinition) {
    return EncodedTypeListUtils.instance.getAllValues(BasicStatus)
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
   * Returns the display value for the choice list.
   * @return The display value.
   */
  String getDisplayValue() {
    return "label.${NameUtils.lowercaseFirstLetter(this.getClass().simpleName)}"
  }

  /**
   * Returns the localized display value for the choice.
   * @param locale The locale to use for the localized string (<b>Default:</b> default locale).
   * @return The display value.
   */
  @Override
  String toStringLocalized(Locale locale = null) {
    return GlobalUtils.lookup("basicStatus.${id}.label", locale)
  }
}

