package org.simplemes.mes.floor


import org.simplemes.eframe.data.ChoiceListInterface
import org.simplemes.eframe.data.ChoiceListItemInterface
import org.simplemes.eframe.data.EncodedTypeInterface
import org.simplemes.eframe.data.EncodedTypeListUtils
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the basic status codes needed for a work center.  This Status controls the overall status of the work center.
 * Sub-elements of the work center may have other states or statuses.
 *
 */
//@ToString(includeNames = true, includePackage = false)
abstract class WorkCenterStatus implements EncodedTypeInterface, ChoiceListInterface, ChoiceListItemInterface {

  /**
   * The max length of the database representation of this status 8.
   */
  public static final int ID_LENGTH = 8

  /**
   * A list of classes that are valid Basic Statuses.
   */
  @SuppressWarnings("unused")
  static List<Class> coreValues = [WorkCenterEnabledStatus, WorkCenterDisabledStatus]

  /**
   * True if this choice is the default choice in the list.
   */
  Boolean defaultChoice = false

  /**
   * Returns true if this status means that the object is workable.
   * @return True if workable.
   */
  abstract boolean isEnabled()

  /**
   * Returns the instance for the given DB ID.
   * @param id The ID (e.g. 'ENABLED').
   * @return The corresponding status (can be null if ID is not valid or null).
   */
  static WorkCenterStatus valueOf(String id) {
    def entry = EncodedTypeListUtils.instance.getAllValues(this).find { it.instance.id == id }
    return entry?.instance
  }

  /**
   * Returns the list of valid values for those formats that use a combobox or similar widget.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The list of valid values.
   */
  @SuppressWarnings("unused")
  static List<ChoiceListItemInterface> getValidValues(FieldDefinitionInterface fieldDefinition) {
    return EncodedTypeListUtils.instance.getAllValues(WorkCenterStatus)
  }

  /**
   * Returns the localized display value for the choice.
   * @param locale The locale to use for the localized string (<b>Default:</b> default locale).
   * @return The display value.
   */
  @Override
  String toStringLocalized(Locale locale = null) {
    return GlobalUtils.lookup("workCenterStatus.${id}.label", locale)
  }

  /**
   * Returns the default status.
   * @return The default status.
   */
  static WorkCenterStatus getDefault() {
    // TODO: Delete if not needed by new Choice list approach
    return WorkCenterEnabledStatus.instance
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
    return this.defaultChoice
  }


}

