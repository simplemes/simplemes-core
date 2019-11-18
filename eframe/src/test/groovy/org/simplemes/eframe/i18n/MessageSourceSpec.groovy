package org.simplemes.eframe.i18n

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class MessageSourceSpec extends BaseSpecification {

  //static specNeeds = [EMBEDDED]

  def "verify that getMessage works for default locale"() {
    when: 'the lookup is made'
    def s = new MessageSource().getMessage('home.label', null, null)

    then: 'it is correct'
    s == GlobalUtils.lookup('home.label')
  }

  def "verify that getMessage works for a specific locale"() {
    when: 'the lookup is made'
    def s = new MessageSource().getMessage('searchStatus.green.label', null, Locale.GERMAN)

    then: 'it is correct'
    s == GlobalUtils.lookup('searchStatus.green.label', Locale.GERMAN)
  }

  def "verify that lookup works with UTF-8 encoding for German"() {
    expect: 'the correct string encoding is returned'
    def s = new MessageSource().getMessage('searchStatus.green.label', null, Locale.GERMAN)
    s.contains('Grün')
    s == 'Grün'
  }


}
