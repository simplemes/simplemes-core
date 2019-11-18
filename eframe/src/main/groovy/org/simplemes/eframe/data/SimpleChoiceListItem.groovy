package org.simplemes.eframe.data

import groovy.transform.ToString

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A generic entry of the ChoiceListItemInterface element.  This uses a static display value for the current locale.
 * Not localization is performed by this class.
 * <p>
 * <b>Note: </b>This class should not be cache between HTTP requests.  It should be generated for the request and
 * discarded when the request is done.
 */
@ToString(includePackage = false, includeNames = true)
class SimpleChoiceListItem implements ChoiceListItemInterface {

  /**
   * The internal ID.
   */
  Object id

  /**
   * The display value for the current locale.
   */
  String displayValue

  /**
   * The value for the choice list (e.g. a domain record of enum element).
   */
  Object value

  /**
   * True if this choice is the default choice in the list.
   */
  Boolean defaultChoice = false

  /**
   * Returns true if this choice is the default choice in the list.
   * @return True if it is the default.
   */
  @Override
  boolean isDefaultChoice() {
    return this.defaultChoice
  }


  /**
   * Returns the localized display value for the choice.
   * @param locale The locale to use for the localized string.
   * @return The display value.
   */
  @Override
  String toStringLocalized(Locale locale = null) {
    return displayValue
  }

}
