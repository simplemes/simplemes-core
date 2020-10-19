/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.controller

import groovy.json.JsonSlurper
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.dashboard.domain.DashboardPanel
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.MockPrincipal

/**
 * Tests.
 */
class DashboardConfigControllerSpec extends BaseAPISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [DashboardConfig]

  def "verify that controller follows standards - security etc"() {
    expect: 'the tester is run  '
    ControllerTester.test {
      controller DashboardConfigController
      role 'DESIGNER'
      secured 'index', SecurityRule.IS_AUTHENTICATED
    }
  }

  def "verify that CRUD works on dashboards with a live server"() {
    given: 'a test record'
    def record = null
    DashboardConfig.withTransaction {
      record = new DashboardConfig(dashboard: 'ABC')
      record.dashboardPanels << new DashboardPanel()
      record.save()
    }

    when: 'the get is triggered'
    login()
    def s = sendRequest(uri: "/dashboardConfig/crud/${record.dashboard}")

    then: 'the JSON is valid'
    def json = new JsonSlurper().parseText(s)
    json.dashboard == 'ABC'
  }

  def "verify that the details dialog method works"() {
    when: 'the dialog method is called'
    def modelAndView = new DashboardConfigController().detailsDialog(new MockPrincipal('joe'))

    then: 'the model returned contains the params'
    modelAndView.view.get() == 'dashboard/detailsDialog'
  }


}
