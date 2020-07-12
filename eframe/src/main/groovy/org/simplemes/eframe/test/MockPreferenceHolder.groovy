/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import groovy.transform.ToString
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.PreferenceSettingInterface

/**
 * Builds a very simple mock/stub for the PreferenceHolder that returns the given preference.
 * This class restores the original PreferenceHolder during BaseSpecification.cleanup().
 * <pre>
 *   def mock = new MockPreferenceHolder(this,columnPreference).install()
 * </pre>
 * <p>
 * You can access any preferences stored here by name:
 * <pre>
 *   def mock = new MockPreferenceHolder(this,columnPreference).install()
 *   mock['aParamName'] = 'aParamValue'
 * </pre>
 * <p>
 *
 */
@ToString(includePackage = false, includeNames = true)
class MockPreferenceHolder extends PreferenceHolder implements AutoCleanupMockInterface {

  /**
   * The preference to return when requested, by the correct key.
   */
  List<PreferenceSettingInterface> preferences = []


  /**
   * The holder mock.
   */
  PreferenceHolder holder

  /**
   * Basic constructor - supports single preference.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param preferences A list of preference to return. Optional.  These might be updated by new preferences
   *        added to this mock holder.
   */
  MockPreferenceHolder(BaseSpecification baseSpec, List<PreferenceSettingInterface> preferences) {
    holder = this
    this.preferences = preferences
    baseSpec.registerAutoCleanup(this)
  }

  /**
   * Installs the mock.
   */
  MockPreferenceHolder install() {
    PreferenceHolder.mockPreferenceHolder = holder
    return this
  }


  /**
   * Performs the cleanup action.
   * @param testSpec The test that requests the cleanup.
   */
  @Override
  void doCleanup(BaseSpecification testSpec) {
    PreferenceHolder.mockPreferenceHolder = null
  }

  /**
   * Returns the appropriate bean implementation, if needed.
   * @return The bean implementation for the getBean() response.
   */
  @Override
  PreferenceSettingInterface get(String name) {
    return preferences?.find() { it?.key == name }
  }

  /**
   * Internal method to set/update the preference.
   * @param setting The setting to store.
   */
  @Override
  PreferenceHolder setPreference(PreferenceSettingInterface setting) {
    def settings = preferences
    def index = settings.findIndexOf() { it.key == setting.key }
    if (index >= 0) {
      settings[index] = setting
    } else {
      settings << setting
    }
    return this
  }

  /**
   * Returns the list of settings.
   * @return The settings.
   */
  @Override
  List getSettings() {
    return preferences
  }

  /**
   * Save current setting to database.
   * @return This holder.
   */
  @Override
  PreferenceHolder save() {
    return this
  }
}
