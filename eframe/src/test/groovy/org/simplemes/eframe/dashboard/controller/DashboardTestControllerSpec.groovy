package org.simplemes.eframe.dashboard.controller

import io.micronaut.http.HttpStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.MockRenderer
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *  Tests.
 */
class DashboardTestControllerSpec extends BaseSpecification {

  def "verify that page works"() {
    given: 'a mock renderer'
    def mockRenderer = new MockRenderer(this).install()

    when: 'the page is rendered'
    def res = new DashboardTestController().page(mockRequest([view: 'sample/dashboard/wcSelection']),
                                                 new MockPrincipal('jane', 'OPERATOR'))

    then: 'the renderer has the right view and msg'
    mockRenderer.view == 'sample/dashboard/wcSelection'

    and: 'the response is Ok'
    res.status == HttpStatus.OK
  }

  def "verify that page fails with missing view"() {
    when: 'the page is rendered'
    new DashboardTestController().page(mockRequest(), new MockPrincipal('jane', 'OPERATOR'))

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['missing', 'view'])
  }

}
