package org.simplemes.eframe.custom.controller

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.ControllerTester

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class FlexTypeControllerSpec extends BaseSpecification {

  def "verify that the controller passes the standard controller test - security, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller FlexTypeController
      taskMenu name: 'flexType', uri: '/flexType', clientRootActivity: true
    }
  }

}
