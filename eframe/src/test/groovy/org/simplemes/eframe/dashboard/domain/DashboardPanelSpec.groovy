package org.simplemes.eframe.dashboard.domain


import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DashboardPanelSpec extends BaseSpecification {
  static specNeeds = [HIBERNATE]
  //static dirtyDomains = [DashboardConfig]

  def "verify that domain enforces constraints"() {
    def dashboard = new DashboardConfig(dashboard: 'ABC')

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain DashboardPanel
      requiredValues dashboardConfig: dashboard, panelIndex: 137, panel: 'P1'
      maxSize 'defaultURL', FieldSizes.MAX_URL_LENGTH
      maxSize 'panel', FieldSizes.MAX_KEY_LENGTH
      notNullCheck 'panelIndex'
      notNullCheck 'panel'
      fieldOrderCheck false
    }
  }


}
