/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.controller

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.ControllerTester

/**
 * Tests.
 */
class FlexTypeControllerSpec extends BaseSpecification {

  def "verify that the controller passes the standard controller test - security, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller FlexTypeController
      taskMenu name: 'flexType', uri: '/flexType', clientRootActivity: true, folder: 'custom:100', displayOrder: 110
    }
  }

}
