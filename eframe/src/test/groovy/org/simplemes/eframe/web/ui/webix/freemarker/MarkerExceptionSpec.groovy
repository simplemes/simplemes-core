package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 *
 */
class MarkerExceptionSpec extends BaseSpecification {

  def "verify that toString provides the right information"() {
    given: 'a marker exception'
    def marker = Mock(MarkerInterface)
    marker.toStringForException() >> '<@displayValue/>'

    when: 'the exception is created'
    def ex = new MarkerException('bad result', marker)

    then: 'the toString is valid'
    UnitTestUtils.assertExceptionIsValid(ex, ['bad result', '<@displayValue/>'])
  }
}
