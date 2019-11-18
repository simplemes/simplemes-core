package org.simplemes.eframe.preference

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class SimplePreferenceFactorySpec extends BaseSpecification {

  def "verify that buildPreference handles string values"() {
    when: 'the preference is built'
    def pref = SimplePreferenceFactory.buildPreference('WC237')

    then: 'the right preference and value are returned'
    pref instanceof SimpleStringPreference
    pref.value == 'WC237'
  }

}
