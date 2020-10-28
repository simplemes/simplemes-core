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
class DashboardButtonSpec extends BaseSpecification {
  static specNeeds = [SERVER]
  //static dirtyDomains = [DashboardConfig]

  def "verify that domain enforces constraints"() {
    def dashboard = new DashboardConfig(dashboard: 'ABC')

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain DashboardButton
      requiredValues dashboardConfig: dashboard, sequence: 237, buttonID: 'A', url: 'url1', panel: 'P1', label: 'L1'
      maxSize 'label', FieldSizes.MAX_TITLE_LENGTH
      maxSize 'title', FieldSizes.MAX_TITLE_LENGTH
      maxSize 'url', FieldSizes.MAX_URL_LENGTH
      maxSize 'panel', FieldSizes.MAX_CODE_LENGTH
      maxSize 'css', FieldSizes.MAX_SINGLE_LINE_LENGTH
      notNullCheck 'buttonID'
      notNullCheck 'label'
      notNullCheck 'sequence'
      notNullCheck 'url'
      notNullCheck 'panel'
      notInFieldOrder(['dashboardConfig'])
    }
  }


}
