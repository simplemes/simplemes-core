/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.system.controller.DemoDataController
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockPrincipal
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/**
 * Tests.
 */
class DashboardDemoDataLoaderSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [SampleParent, AllFieldsDomain, DashboardConfig]

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
    DashboardConfig.findByDashboard('SUPERVISOR_DEFAULT')
    DashboardConfig.findByDashboard('OPERATOR_DEFAULT')
    DashboardConfig.findByDashboard('MANAGER_DEFAULT')

    and: 'the model is correct'
    def model = res.model.get()
    def map1 = model.list.find { it.name == DashboardConfig.simpleName }
    map1.uri == '/dashboard'
    map1.count == 3
    map1.possible == 3
  }

}
