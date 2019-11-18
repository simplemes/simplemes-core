package org.simplemes.eframe.preference

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A generic holder for a single user Preference.  This is a set of values for a given HTML element
 * with one or more settings held in a map.
 *
 */
@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode
@JsonSerialize(using = PreferenceSerializer)
@JsonDeserialize(using = PreferenceDeserializer.class)
class Preference {

  /**
   * The HTML Element ID.  Must be unique on a page for preferences.  For example,
   * a list might have an ID <code>OrderList</code>.
   */
  String element

  /**
   * The name of this preference.  Allows for pre-defined (named) preference settings to pre-configure lists and such
   * for all users.  (<b>Optional</b>).
   */
  String name = ''

  /**
   * This List holds specific details used by the element.  For example, an <code>OrderList</code> element
   * will have a series of ColumnPreference entries in the list.
   */
  List<PreferenceSettingInterface> settings = []

}
