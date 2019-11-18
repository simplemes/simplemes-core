package org.simplemes.eframe.web.ui.webix.freemarker

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class MarkerCoordinatorSpec extends BaseSpecification {

  def "verify that coordinator works with properties and toString"() {
    when: 'a marker coordinator is set'
    def mc = new MarkerCoordinator()

    and: 'the various adders and setters are used'
    mc.addPostscript('post1')
    mc.addPostscript('post2')
    mc.addPrescript('pre1')
    mc.addPrescript('pre2')
    mc.formID = 'form1'
    mc.formURL = '/index'

    and: 'the other values are set'
    mc.others['ABC'] = 'PDQ'
    mc.others['XYZ'] = 'DEF'

    then: 'the toString works'
    UnitTestUtils.assertContainsAllIgnoreCase(mc, ['post1', 'post2', 'pre1', 'pre2', 'form1', '/index'])

    and: 'the others map is correct'
    mc.others == [ABC: 'PDQ', XYZ: 'DEF']
  }
}
