/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.domain


import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester

/**
 * Tests.
 */
class DashboardPanelSpec extends BaseSpecification {
  static specNeeds = [SERVER]
  //static dirtyDomains = [DashboardConfig]

  def "verify that domain enforces constraints"() {
    def dashboard = new DashboardConfig(dashboard: 'ABC')

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain DashboardPanel
      requiredValues dashboardConfig: dashboard, panelIndex: 137, panel: 'P1'
      maxSize 'defaultURL', FieldSizes.MAX_URL_LENGTH
      maxSize 'panel', FieldSizes.MAX_CODE_LENGTH
      notNullCheck 'panelIndex'
      notNullCheck 'panel'
      fieldOrderCheck false
    }
  }


}
