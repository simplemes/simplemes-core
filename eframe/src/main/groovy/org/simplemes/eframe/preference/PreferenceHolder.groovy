/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.preference

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.preference.domain.UserPreference

/**
 * Defines a user preference object for given user, page and HTML element.  This includes
 * a DSL for accessing these preference settings and for persisting them.  This object hides most of the
 * mechanics of saving and finding the preferences. 
 * See <a href="http://docs.simplemes.org/latest/guide.html#gui-state-persistence">GUI State Persistence</a>
 * in the framework guidelines.
 * <h3>Example</h3>
 * <pre>
 * PreferenceHolder preference = PreferenceHolder.find &#123;
 *   page '/app/testPage'
 *   user 'admin'
 *   element 'OrderList'
 * &#125;
 *
 * def columnPref = preference[column] ?: new ColumnPreference(column: column)
 * columnPref.width = newSize
 * preference.setPreference(columnPref).save() 
 </pre>
 *
 * <b>Note:</b> You can switch to a different element if needed using: <code>preference.element = 'OrderListB'</code>
 * <p>
 * <p>
 * <p>
 * <b>Note:</b> Due the quirks in Groovy DSL processing, do not use values for the values (page, user, element) such
 *              as <b>'id', 'page', 'user' or 'element'</b>.  These get set to null if used.  Use another variable
 *              name such as 'idParam', 'pageParam', 'userParam' or 'elementParam'.</code>
 */
@Slf4j
@SuppressWarnings("ConfusingMethodName")
@ToString(includeNames = true, includePackage = false)
class PreferenceHolder {
  /**
   * The HTML page this preference is defined for.
   */
  String _page

  /**
   * The username who this preference is defined for.
   */
  String _user

  /**
   * The HTML element ID/name for this preference.
   */
  String _element

  /**
   * The named definition for this preference.  Used to pre-define settings  (<b>Optional</b>).
   */
  String _name = ''

  /**
   * The persisted domain object to save the value in.
   */
  UserPreference userPreference

  /**
   * The current preference we are processing.  This corresponds to the element passed in (or the first one in
   * the list).
   */
  Preference currentPreference

  /**
   * Sets the HTML page this preference is defined for.
   */
  void page(final String page) { this._page = page }

  /**
   * Sets the username who this preference is defined for.
   */
  void user(final String user) { this._user = user }

  /**
   * Sets the HTML element ID/name for this preference.
   */
  void element(final String element) { this._element = element }

  /**
   * Sets the named definition for this preference.  Used to pre-define settings (<b>Optional</b>).
   */
  void name(final String name) { this._name = name }

  /**
   * A preference holder to return when the find() method is called.
   */
  static PreferenceHolder mockPreferenceHolder

  /**
   * Finds the preferences for the given page/user/element.  This is the main entry point for the DSL.
   * @param config The configuration DSL closure.
   * @return The holder.
   */
  static PreferenceHolder find(@DelegatesTo(PreferenceHolder) final Closure config) {
    if (mockPreferenceHolder) {
      log.trace("find() Returning mock holder {}", mockPreferenceHolder)
      return mockPreferenceHolder
    }
    PreferenceHolder preferenceHolder = new PreferenceHolder()
    preferenceHolder.with config

    if (!preferenceHolder._element) {
      log.warn('find() element is null. User: {}, Page: {}  Check for un-qualified reference to a constant in the find DSL.',
               preferenceHolder._user, preferenceHolder._page)
    }
    if (preferenceHolder._page) {
      preferenceHolder.load()
    } else {
      def msg = "find() page is null. User: ${preferenceHolder._user}, element: ${preferenceHolder._element}. for un-qualified reference to a constant in the find DSL."
      throw new IllegalArgumentException(msg)
    }

    return preferenceHolder
  }

  /**
   * Load or create new UserPreference record.  Uses the cache, when possible.
   */
  @SuppressWarnings(["GroovyAssignabilityCheck", "GroovyMissingReturnStatement"])
  void load() {
    // Try to find the preference in the DB, with cache support.
    def basePage = ControllerUtils.instance.determineBaseURI(_page)
    if (_user && basePage) {
      userPreference = UserPreference.findByUserNameAndPage((String) _user, basePage)
    }
    if (!userPreference) {
      // No record found, so create it.
      userPreference = new UserPreference(page: basePage, userName: _user)
    }
    if (_element) {
      currentPreference = userPreference.preferences?.find { it.element == _element }
      if (currentPreference == null) {
        // Make sure the element has a preference entry in the map
        currentPreference = new Preference(element: _element)
        userPreference.preferences << currentPreference
      }
    } else {
      // No element name, so use the first one found in the list
      currentPreference = userPreference?.preferences?.getAt(0)
      _element = currentPreference?.element
    }
  }

  /**
   * Gets the preference from the stored setting.
   * @param key The preference key.
   * @return This preference setting.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  PreferenceSettingInterface get(String key) {
    def settings = currentPreference?.settings
    return settings?.find() { it.key == key }
  }

  /**
   * Sets the preference in the stored setting.
   * This simulates a map-style set to mirror the map-style get above.
   * @param key The preference key.
   * @param value This preference setting.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  void set(String key, PreferenceSettingInterface value) {
    def settings = currentPreference?.settings
    if (settings != null) {
      def index = settings?.findIndexOf() { it.key == key }
      if (index >= 0) {
        // Update the setting
        settings[index] = value
      } else {
        settings << value
      }
    }
  }

  /**
   * Save current setting to database.
   * @return This holder.
   */
  PreferenceHolder save() {
    userPreference.save()
    return this
  }

  /**
   * Set the given preference in the current settings.  Will replace if the setting already exists.
   * @return This holder.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  PreferenceHolder setPreference(PreferenceSettingInterface setting) {
    currentPreference = userPreference.preferences?.find { it.element == _element }
    if (currentPreference == null) {
      // Element might have been switched to a new one that does not exist in the Map yet, so create it.
      currentPreference = new Preference(element: _element)
      userPreference.preferences << currentPreference
    }
    def settings = currentPreference.settings
    def index = settings.findIndexOf() { it.key == setting.key }
    if (index >= 0) {
      settings[index] = setting
    } else {
      settings << setting
    }

    return this
  }

  /**
   * Returns the list of element names.
   * @return The names (can be empty list).
   */
  List<String> getElementNames() {
    if (userPreference?.preferences) {
      return userPreference.preferences*.element
    }

    return []
  }

  /**
   * Returns the list of element names.
   * @return The names (can be empty list).
   */
  void setElement(String s) {
    this._element = s
    currentPreference = userPreference.preferences?.find { it.element == _element }
    if (currentPreference == null) {
      // Make sure the element has a preference entry in the map
      currentPreference = new Preference(element: _element)
      userPreference.preferences << currentPreference
    }
  }

  /**
   * Returns the list of settings.
   * @return The settings.
   */
  List getSettings() {
    return currentPreference?.settings
  }

  /**
   * Sets the list of settings.
   * @param settings The settings.
   */
  void setSettings(List settings) {
    currentPreference?.settings = settings
  }

}
