package org.simplemes.mes.system

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.system.controller.DemoDataController
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.mes.product.domain.Product

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DemoDataLoaderSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [DashboardConfig]

  DemoDataController controller

  @SuppressWarnings('unused')
  def setup() {
    controller = Holders.getBean(DemoDataController)
  }

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "verify that the loader loads the demo records"() {
    when: 'the index is triggered'
    def res = controller.index(new MockPrincipal())

    then: 'the records are loaded'
    DashboardConfig.findByDashboard('TRADITIONAL')
    DashboardConfig.findByDashboard('SCAN')

    and: 'the model is correct'
    def model = res.model.get()
    def map1 = model.list.find { it.name.contains('Traditional') }
    map1.uri == '/dashboard'
    map1.count == 1
    map1.possible == 1

    def map2 = model.list.find { it.name.contains('Scan') }
    map2.uri == '/dashboard'
    map2.count == 1
    map2.possible == 1
  }

}
