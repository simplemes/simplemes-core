package org.simplemes.mes.product.controller


import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.ControllerTester

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests for the Product Controller and general-purpose tests for Micronaut and Framework logic.
 */
class ProductControllerSpec extends BaseSpecification {

  def "verify that the controller passes the standard controller test - security, task menu, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller ProductController
      role 'ENGINEER'
      taskMenu name: 'product', uri: '/product', clientRootActivity: true, folder: 'productDef:600', displayOrder: 610
    }
  }


}
