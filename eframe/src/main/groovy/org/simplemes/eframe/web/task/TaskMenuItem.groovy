package org.simplemes.eframe.web.task

import groovy.transform.EqualsAndHashCode
import groovy.transform.Sortable
import groovy.transform.ToString
import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Defines a single task menu entry that the user can execute.
 */
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode()
@Sortable(includes = ['displayOrder', 'name'])
class TaskMenuItem {

  /**
   * The parent folder(s) this task belongs to in the task menu (<b>Optional</b>).  This is a folder definition string that may be
   * more than a simple folder name. The folder string can contain
   * multiple levels with a display order (e.g. 'core:50.system:101').
   * This is used by the TaskMenuHelper to build the folders needed and to order them.
   * <p>
   * If not given, then this menu item is displayed at the root level.
   */
  String folder

  /**
   * The internal name for this task item (<b>Required</b>).  Will use the messages.properties lookup of the name by adding '.label'
   * and '.title' to the end of this name.
   */
  String name

  /**
   * The path to the page that starts this task(<b>Required</b>).  Typically the absolute URI such as '/flexType/index'.
   */
  String uri

  /**
   * The URI defines the client logger root element for client-level logging (e.g. the URI is '/user', means the
   * logger is 'client.user'). (*Default:* true).
   */
  boolean clientRootActivity = true

  /**
   * The order this should be displayed in (<b>Optional</b>).  If not provided, then one will assigned based on the order this item
   * was discovered.
   * <p>
   * <b>Optional.</b> Default assigned alphabetically.
   */
  Integer displayOrder

  /**
   * The controller class this item came from (<b>Optional</b>).
   */
  Class controllerClass

  /**
   * Returns the localized name.  Uses the name with 'taskMenu.${name}.label' to find the localized name.
   * @param locale The optional locale.
   * @return The localized name.  If not found, then returns the raw 'name'.  Single and double quotes are removed.
   */
  String getLocalizedName(Locale locale = null) {
    def key = "taskMenu.${name}.label"
    def s = GlobalUtils.lookup(key, null, locale)
    if (s == key) {
      // No lookup found, so just use the name.
      s = name
    }
    return s.replaceAll(/["']/, '')
  }

  /**
   * Returns the localized tooltip.  Uses the name with 'taskMenu.${name}.tooltip' to find the localized tooltip.
   * @param locale The optional locale.
   * @return The localized tooltip.  If not found, then returns the raw 'name'.  Single and double quotes are removed.
   */
  String getLocalizedTooltip(Locale locale = null) {
    def key = "taskMenu.${name}.tooltip"
    def s = GlobalUtils.lookup(key, null, locale)
    if (s == key) {
      // No lookup found, so just use the name.
      s = name
    }
    return s.replaceAll(/["']/, '')
  }

  /**
   * Returns the sanitized name.
   * @return The name. Single and double quotes are removed.
   */
  String getName() {
    return name?.replaceAll(/["']/, '')
  }

  /**
   * Verifies that this menu item is valid.  Returns a reason message explaining the problem.
   * @return Null if Ok.  Reason message if not Ok.
   */
  String check() {
    if (!name) {
      return "Missing name value.  Source=${controllerClass?.name}"
    }
    if (!uri) {
      return "Missing uri value. Name=${name},  Source=${controllerClass?.name}"
    }
    return null
  }


}
