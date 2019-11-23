package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.controller.SampleParentController

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class LookupMarkerSpec extends BaseMarkerSpecification {

  def "verify that marker works for the simple case"() {
    expect: 'the lookup works'
    execute(source: '<@efLookup key="addLogger.title"/>', controllerClass: SampleParentController) == lookup('addLogger.title')
  }

  def "verify that marker detects missing key"() {
    when: 'the lookup works'
    execute(source: '<@efLookup/>', controllerClass: SampleParentController)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efLookup', 'key'])
  }
}
