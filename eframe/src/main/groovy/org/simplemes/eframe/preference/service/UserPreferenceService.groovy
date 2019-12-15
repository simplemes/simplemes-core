package org.simplemes.eframe.preference.service

import groovy.util.logging.Slf4j
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SimplePreferenceFactory
import org.simplemes.eframe.preference.SimpleStringPreference
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.preference.event.GUIEventInterface
import org.simplemes.eframe.security.SecurityUtils

import javax.inject.Singleton
import javax.transaction.Transactional

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides access to the user preferences that are used to save GUI settings and related objects.
 * This is the main business logic for the preference handling.
 * All logged in users can store their preferences.
 */
@Slf4j
@Singleton
class UserPreferenceService {

  /**
   * Handles the gui state change event.  This processes the event and delegates to an event handler to
   * store the preferences for the state change.
   * Contents of the <code>params</code> Map are:
   * <ul>
   *   <li><code>event</code> - The change event (e.g. 'ColumnResized', etc).  This is used to construct the handler
   *                            for the event by adding the package 'org.simplemes.eframe.user.event.' to the event name.</li>
   * </ul>
   *
   * @param params The HTTP request values .
   */
  @Transactional
  void guiStateChanged(Map params) {
    ArgumentUtils.checkMissing(params, 'params')
    ArgumentUtils.checkMissing(params.get('event'), 'params.event')
    def className = "org.simplemes.eframe.preference.event.${params.get('event')}"
    def c = TypeUtils.loadClass(className)
    GUIEventInterface handler = (GUIEventInterface) c.newInstance()
    handler.handleEvent(params)
  }

  /**
   * Finds the given preference(s).  This is designed for use in the browser for dynamic preferences such as dialog
   * sizes.<p>
   * <b>Note:</b> Reads via the second level cache.
   * @param pageURI The URI for the page these settings are to be found for (<b>Required</b>).
   * @param desiredElement The ID of the element for the preference (<b>Optional</b>).
   * @param preferenceType The preference type to return.  For example, 'DialogPreference' (<b>Optional</b>).
   * @return The list of matching preference(s).
   */
  @Transactional
//(readOnly = true)
  Map findPreferences(String pageURI, String desiredElement = null, String preferenceType = null) {
    def res = [:]

    log.debug('findPreferences(): uri {}, element {}, type {}, user {}', pageURI, desiredElement, preferenceType, SecurityUtils.currentUserName)
    def userPreference = UserPreference.findByUserNameAndPage(SecurityUtils.currentUserName, pageURI, [cache: true])
    for (preference in userPreference?.preferences) {
      for (detail in preference.settings) {
        // Filter out any that don't match the optional criteria
        def addToList = true
        if (desiredElement && preference.element != desiredElement) {
          addToList = false
        }
        if (preferenceType && !detail.class.name.endsWith(preferenceType)) {
          addToList = false
        }
        if (addToList) {
          if (detail instanceof SimpleStringPreference) {
            res[preference.element] = detail.value
          } else {
            res[preference.element] = detail
          }
        }
      }
    }

    return res
  }

  /**
   * Saves the given preference value into the user's preferences for the give element.
   * This is a simplified view of preferences that just supports a simple name/value setting.
   * @param pageParam The page (URI) the preference applies to.
   * @param elementParam The page element (or preference name).
   * @param value The value.  Can be a POGO or a simple element.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  @Transactional
  void saveSimplePreference(String pageParam, String elementParam, Object value) {
    // Grab the current preference (in case it is stored in the session).
    String userParam = SecurityUtils.currentUserName
    PreferenceHolder holder = PreferenceHolder.find {
      page pageParam
      user userParam
      element elementParam
    }

    // See if this preference is already in the list.
    def simplePreference = SimplePreferenceFactory.buildPreference(value) ?: value
    if (holder?.settings[0]) {
      holder.settings[0] = simplePreference
    } else {
      holder.settings << simplePreference
    }
    holder.save()

    log.trace("Stored Preference {}", holder)

  }

  /**
   * Saves the given preference value into the user's preferences for the give element.
   * This is a simplified view of preferences that just supports a simple name/value setting.<p>
   * <b>Note:</b> Reads via the second level cache.
   * @param pageParam The page (URI) the preference applies to.
   * @param elementParam The page element (or preference name).
   * @return value The value.  Can be a POGO or a simple element.
   */
  // TODO: Replace with non-hibernate alternative
  @Transactional(/*readOnly = true*/)
  Object findSimplePreference(String pageParam, String elementParam) {
    String userParam = SecurityUtils.currentUserName
    PreferenceHolder holder = PreferenceHolder.find {
      page pageParam
      user userParam
      element elementParam
    }
    log.trace("findSimplePreference() holder {}, page {}, element {}, user {}", holder, pageParam, elementParam, userParam)

    def setting = holder.settings[0]
    if (setting instanceof SimpleStringPreference) {
      return setting.value
    } else {
      return setting
    }
  }

}
