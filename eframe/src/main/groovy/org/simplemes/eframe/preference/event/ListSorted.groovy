package org.simplemes.eframe.preference.event

import groovy.util.logging.Slf4j
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.preference.ColumnPreference
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.security.SecurityUtils

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Detects when a list's sorting has changed from the default and stores this in the users preferences.
 * Supports client events (non-server sorting) and explicit calls from the server-side queries when the
 * sorting is available.
 * The server-side sorting is a little different form the normal preferences events since there is no explicit 'sort order changed' event.
 * Just a call to the controller's list() event with a different sort order.
 *
 */
@Slf4j
class ListSorted implements GUIEventInterface {

  /**
   * Handles the list sorted event when called from the client.
   * Inputs are:
   * <ul>
   *   <li><code>pageURI</code> - The URI for the page these settings changed on.</li>
   *   <li><code>element</code> - The HTML ID of the element that changed.</li>
   *   <li><code>event</code> - The change event (e.g. 'ListSorted', etc).</li>
   * </ul>
   * The request parameters follow the standard sorting protocol as defined in the
   * method {@link org.simplemes.eframe.controller.ControllerUtils#calculateSortingForList(java.util.Map)}.
   *
   * @param params The HTTP request parameters.
   */
  void handleEvent(Map params) {
    checkForUserSortOrder(params)
  }

  /**
   * Handles the list sorted event (called explicitly from the server-side sorting).
   * Inputs are:
   * <ul>
   *   <li><code>pageURI</code> - The URI for the page these settings changed on.</li>
   *   <li><code>element</code> - The HTML ID of the element that changed.</li>
   * </ul>
   * The request parameters follow the standard sorting protocol as defined in the
   * method {@link org.simplemes.eframe.controller.ControllerUtils#calculateSortingForList(java.util.Map)}.
   *
   * @param params The HTTP parameters.
   */
  static void checkForUserSortOrder(Map params) {
    log.debug('checkForUserSortOrder() params = {}', params)
    def (sortField, sortOrder) = ControllerUtils.instance.calculateSortingForList(params)

    if (!params.get('pageURI')) {
      // No page info, so just do nothing.
      return
    }
    String pageParam = ControllerUtils.instance.determineBaseURI(params.pageURI)
    String elementParam = params.get('element')
    //println "page = $page, element = $element"
    String userParam = SecurityUtils.currentUserName
    if (!userParam) {
      // Can't remember the settings if not logged in.
      return
    }
    String defaultSortField = params.get('defaultSortField')

    PreferenceHolder holder = PreferenceHolder.find {
      page pageParam
      user userParam
      element elementParam
    }

    // Clear any previous sorting criteria, removing the empty ones from the list.
    def newSettings = []
    for (columnPref in holder.settings) {
      columnPref.sortAscending = null
      columnPref.sortLevel = null
      if (!columnPref.determineIfEmpty()) {
        // Still need this one in the list.
        newSettings << columnPref
      }
    }
    holder.settings = newSettings

    // Determine if this is the default sort order for the list.
    boolean isDefaultSort = false
    if (sortField == defaultSortField && sortOrder == 'asc') {
      isDefaultSort = true
    }

    if (!isDefaultSort && sortField) {
      // Only save the sort criteria if it is not the default sort order.
      ColumnPreference columnPref = (ColumnPreference) holder[sortField]
      if (columnPref == null) {
        columnPref = new ColumnPreference(column: sortField)
        holder[sortField] = columnPref
      }

      columnPref.sortLevel = 1
      columnPref.sortAscending = sortOrder == 'asc'
    }

    log.trace("checkForUserSortOrder() holder = {}", holder)
    holder.save()
  }

}
