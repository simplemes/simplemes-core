/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.controller


import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.dashboard.domain.DashboardPanel
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.eframe.web.ui.webix.freemarker.DashboardMarker

/**
 * Tests.
 */
class DashboardControllerSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  def "verify that controller follows standards - security etc"() {
    expect: 'the tester is run'
    ControllerTester.test {
      controller DashboardController
      role 'DESIGNER'
      secured 'index', SecurityRule.IS_AUTHENTICATED
    }
  }

  def "verify that request parameters are available in the model for the dashboard activities"() {
    when: 'a request with params is made'
    def params = [workCenter: 'WC247', category: 'CAT237']
    def modelAndView = new DashboardController().index(mockRequest(params), new MockPrincipal('joe', 'ADMIN'))

    then: 'the model returned contains the params'
    def otherParams = modelAndView.model.get().get(DashboardMarker.ACTIVITY_PARAMETERS_NAME)
    otherParams.workCenter == 'WC247'

    and: 'the special parameters are dropped from the params'
    otherParams.category == null
  }

  @Rollback
  def "verify that taskMenuItems uses the default dashboards for the menu items"() {
    given: 'some dashboards in non-sorted order, some are not the default dashboard for their category'
    def dash1 = new DashboardConfig(dashboard: 'XYZ', title: 'xyz', defaultConfig: true, category: 'CAT_X')
    dash1.dashboardPanels << new DashboardPanel()
    dash1.save()

    def dash2 = new DashboardConfig(dashboard: 'ABC', title: 'abc', defaultConfig: true, category: 'CAT_A')
    dash2.dashboardPanels << new DashboardPanel()
    dash2.save()

    def dash3 = new DashboardConfig(dashboard: 'AB1', title: 'ab1', defaultConfig: false, category: 'CAT_A')
    dash3.dashboardPanels << new DashboardPanel()
    dash3.save()

    def dash4 = new DashboardConfig(dashboard: 'XY2', title: 'xy2', defaultConfig: false, category: 'CAT_X')
    dash4.dashboardPanels << new DashboardPanel()
    dash4.save()

    when: 'the task menu items are checked'
    def taskMenuItems = new DashboardController().taskMenuItems

    then: 'the correct default dashboards are found'
    taskMenuItems.size() == 2

    and: 'the first one is correct'
    def item1 = taskMenuItems.find { it.name == 'abc' }
    item1.folder == 'dashboard:200'
    item1.uri == '/dashboard?dashboard=ABC'
    item1.displayOrder == 210

    and: 'the second one is correct'
    def item2 = taskMenuItems.find { it.name == 'xyz' }
    item2.folder == 'dashboard:200'
    item2.uri == '/dashboard?dashboard=XYZ'
    item2.displayOrder == 220
  }

  def "verify that taskMenuItems works outside of a caller transaction"() {
    when: 'the task menu items are checked'
    new DashboardController().taskMenuItems

    then: 'no error is thrown'
    notThrown(Throwable)
  }

  @Rollback
  def "verify that getTaskMenuItems works cleanly with dashboards with no titles"() {
    given: 'a dashboard with no title'
    def dash = new DashboardConfig(dashboard: 'XYZ', defaultConfig: true, category: 'CAT_X')
    dash.dashboardPanels << new DashboardPanel()
    dash.save()

    when: 'the task menu items are checked'
    def taskMenuItems = new DashboardController().taskMenuItems

    then: 'the dashboard is found and is correct'
    taskMenuItems.size() == 1
    def item1 = taskMenuItems.find { it.name == 'XYZ' }
    item1.folder == 'dashboard:200'
    item1.uri == '/dashboard?dashboard=XYZ'
    item1.displayOrder == 210
  }


}
