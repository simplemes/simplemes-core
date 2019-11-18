package org.simplemes.eframe.preference

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This holds the specific preferences for a single column in a list.
 * The column's width, display order and sorting options are stored here.
 *
 * <h3>User Preference Usage</h3>
 * This preference is stored in the user preference table under the keys:
 * <ul>
 *   <li><b>page</b> - The URI of the page the column is in. Stripped of arguments. </li>
 *   <li><b>user</b> - The user name. </li>
 *   <li><b>element</b> - The HTML ID of the grid this column belongs to. </li>
 * </ul>
 *
 */
@ToString(includePackage = false, includeNames = true, includeSuper = true)
@EqualsAndHashCode(includes = ['column'])
class ColumnPreference extends BasePreferenceSetting {

  ColumnPreference() {
  }

  /**
   * The column (name) for this preference.
   */
  String column

  /**
   * The sequence (order) this column is displayed in.
   */
  Integer sequence

  /**
   * The width of the column, in percentage of screen width.  If null, then use the default width.
   */
  BigDecimal width

  /**
   * If non-null, then this is the sorting order for this column.   sortLevel=1 is the primary sort key, etc.
   */
  Integer sortLevel

  /**
   * The sort direction (of sortLevel is non-null).  Defaults to ascending.
   */
  Boolean sortAscending

  /**
   * Standard map constructor.
   * @param options The options.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  ColumnPreference(Map options) {
    options?.each { k, v ->
      this[k] = v
    }
  }

  /**
   * The key (column name) for this preference.  This is unique within a single {@link Preference} object's settings list.
   */
  @Override
  String getKey() {
    return column
  }

  /**
   * Determines if this column preference is empty (has not settings).  This is used to keep the list of column preferences
   * as small as possible.<p>
   * <b>Note:</b> This is not called isEmpty() to avoid XML parsing issues.
   * @return True if the preference is empty of any settings.
   */
  boolean determineIfEmpty() {
    return sequence == null && width == null && sortLevel == null && sortAscending == null
  }

}
