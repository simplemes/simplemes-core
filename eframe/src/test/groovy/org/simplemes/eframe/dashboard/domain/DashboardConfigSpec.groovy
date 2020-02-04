/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.domain

import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback

/**
 * Tests.
 */
class DashboardConfigSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [DashboardConfig]

  def "verify that domain enforces constraints"() {
    def dashboardPanel = new DashboardPanel(panel: 'A')
    //def requiredValues = ['dashboard': 'ABC', panels: [dashboardPanel]]
    //def dashboard = new DashboardConfig(dashboard: 'XYZ')

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain DashboardConfig
      requiredValues dashboard: 'ABC', category: 'OPERATOR', dashboardPanels: [dashboardPanel]
      maxSize 'dashboard', FieldSizes.MAX_CODE_LENGTH
      maxSize 'category', FieldSizes.MAX_CODE_LENGTH
      maxSize 'title', FieldSizes.MAX_TITLE_LENGTH
      notNullCheck 'dashboard'
      notNullCheck 'category'
      fieldOrderCheck false
    }
  }

  def "verify that domain has the right primary key"() {
    expect: 'the correct key'
    DomainUtils.instance.getPrimaryKeyField(DashboardConfig) == 'dashboard'
  }

  def "verify that unique panel ID constraint error is detected"() {
    when: 'a dashboard with invalid setting is validated'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.dashboardPanels << new DashboardPanel(panel: 'ABC')
    dashboard.dashboardPanels << new DashboardPanel(panel: 'ABC')

    then: 'there are errors'
    def errors = DomainUtils.instance.validate(dashboard)
    //error.203.message=The panel must be unique for each panel.  Panel {3} is used on {4} panels
    UnitTestUtils.assertContainsError(errors, 203, 'dashboardPanels', ['panel', 'abc', 'unique'])
  }

  def "verify that invalid panel for a button is detected"() {
    when: 'a dashboard with invalid setting is validated'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.dashboardPanels << new DashboardPanel(panel: 'ABC')
    dashboard.buttons << new DashboardButton(label: '123', panel: 'PDQ')

    then: 'the errors are correct'
    def errors = DomainUtils.instance.validate(dashboard)
    //error.204.message=The button {1} references an invalid panel {2}
    UnitTestUtils.assertContainsError(errors, 204, 'buttons', ['button', '123', 'invalid', 'PDQ'])
  }

  def "verify that too many splitter children error is detected"() {
    when: 'a dashboard with invalid setting is validated'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.splitterPanels << new DashboardPanelSplitter(panelIndex: 47)
    dashboard.dashboardPanels << new DashboardPanel(panelIndex: 1, parentPanelIndex: 47, panel: 'A')
    dashboard.dashboardPanels << new DashboardPanel(panelIndex: 2, parentPanelIndex: 47, panel: 'B')
    dashboard.dashboardPanels << new DashboardPanel(panelIndex: 3, parentPanelIndex: 47, panel: 'C')

    then: 'the errors are correct'
    def errors = DomainUtils.instance.validate(dashboard)
    //error.205.message=The splitter panel {1} must have exactly 2 child panels. It has {2} panels.
    UnitTestUtils.assertContainsError(errors, 205, 'dashboardPanels', ['splitter', '47', 'child', '2', '3'])
  }

  def "verify that too few splitter children error is detected"() {
    when: 'a dashboard with invalid setting is validated'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.splitterPanels << new DashboardPanelSplitter(panelIndex: 47)
    dashboard.dashboardPanels << new DashboardPanel(panelIndex: 1, parentPanelIndex: 47, panel: 'A')

    then: 'the errors are correct'
    def errors = DomainUtils.instance.validate(dashboard)
    //error.205.message=The splitter panel {1} must have exactly 2 child panels. It has {2} panels.
    UnitTestUtils.assertContainsError(errors, 205, 'dashboardPanels', ['splitter', '47', 'child', '1 panel(s)'])
  }

  def "verify that only one default dashboard is allowed in each category - all others marked as non-default"() {
    given: 'an existing dashboard'
    DashboardConfig.withTransaction {
      def dashboardPanel1 = new DashboardPanel()
      def dashboard1 = new DashboardConfig(dashboard: 'XYZ', category: 'CAT1')
      dashboard1.dashboardPanels << dashboardPanel1
      dashboard1.save()
    }

    when: 'a second dashboard is saved with the same category'
    //enableSQLTrace()
    DashboardConfig.withTransaction {
      def dashboardPanel2 = new DashboardPanel()
      def dashboard2 = new DashboardConfig(dashboard: 'PDQ', category: 'CAT1')
      dashboard2.dashboardPanels << dashboardPanel2
      dashboard2.save()
    }
    //disableSQLTrace()

    then: 'there is only one in the DB marked as default'
    DashboardConfig.withTransaction {
      assert DashboardConfig.list().findAll { it.category == 'CAT1' && it.defaultConfig }.size() == 1
      assert DashboardConfig.findByCategoryAndDefaultConfig('CAT1', true).dashboard == 'PDQ'
      true
    }
  }

  def "verify that update does not clear the default flag"() {
    given: 'an existing dashboard'
    DashboardConfig.withTransaction {
      def dashboardPanel1 = new DashboardPanel()
      def dashboard1 = new DashboardConfig(dashboard: 'XYZ', category: 'CAT1')
      dashboard1.dashboardPanels << dashboardPanel1
      dashboard1.save()
    }

    when: 'the dashboard is updated'
    DashboardConfig.withTransaction {
      def dashboard = DashboardConfig.findByDashboard('XYZ')
      dashboard.title = 'New Title'
      dashboard.save()
    }

    then: 'there is only one in the DB marked as default'
    DashboardConfig.withTransaction {
      assert DashboardConfig.findAllByCategoryAndDefaultConfig('CAT1', true).size() == 1
      assert DashboardConfig.findByCategoryAndDefaultConfig('CAT1', true).dashboard == 'XYZ'
      true
    }
  }


  @Rollback
  def "verify that the default panel IDs are assigned to new panels - create"() {
    when: 'a dashboard is saved'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.dashboardPanels << new DashboardPanel()
    dashboard.dashboardPanels << new DashboardPanel()
    dashboard.save()

    then: 'the panel IDs are correct'
    dashboard.dashboardPanels[0].panelIndex == 0
    dashboard.dashboardPanels[0].panel == 'A'
    dashboard.dashboardPanels[1].panelIndex == 1
    dashboard.dashboardPanels[1].panel == 'B'
  }

  @Rollback
  def "verify that the default panel IDs are assigned to new panels - update"() {
    given: 'a saved dashboard'
    def dashboardConfig = new DashboardConfig(dashboard: 'XYZ')
    dashboardConfig.dashboardPanels << new DashboardPanel()
    dashboardConfig.save()

    when: 'a new panel is added'
    dashboardConfig.dashboardPanels << new DashboardPanel()
    dashboardConfig.save()

    then: 'the panel IDs are correct'
    dashboardConfig.dashboardPanels[0].panel == 'A'
    dashboardConfig.dashboardPanels[1].panel == 'B'
  }

  def "verify that the a panel ID can be set for a new dashboard"() {
    when: 'a saved dashboard with panel IDs'
    DashboardConfig.withTransaction {
      def dashboardConfig = new DashboardConfig(dashboard: 'XYZ')
      dashboardConfig.dashboardPanels << new DashboardPanel(panel: "X1")
      dashboardConfig.dashboardPanels << new DashboardPanel(panel: "X2")
      dashboardConfig.save()
    }

    then: 'the panel IDs are correct'
    DashboardConfig.withTransaction {
      def dashboardConfig = DashboardConfig.findByDashboard('XYZ')
      dashboardConfig.dashboardPanels[0].panel == 'X1'
      dashboardConfig.dashboardPanels[1].panel == 'X2'
    }
  }

  @Rollback
  def "verify that the default button sequences are assigned to new button"() {
    when: 'a dashboard is saved'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.dashboardPanels << new DashboardPanel(panel: 'A')
    dashboard.buttons << new DashboardButton(label: 'b1', url: 'page1', panel: 'A')
    dashboard.buttons << new DashboardButton(label: 'b2', url: 'page1', panel: 'A')
    dashboard.save()

    then: 'the button sequences are correct'
    dashboard.buttons[0].sequence == 10
    dashboard.buttons[0].label == 'b1'
    dashboard.buttons[1].sequence == 20
    dashboard.buttons[1].label == 'b2'
  }

  @Rollback
  def "verify that hierarchyToString works"() {
    when: 'a dashboard with a complex hierarchy is written to a string'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.splitterPanels << new DashboardPanelSplitter(panelIndex: 0)
    dashboard.dashboardPanels << new DashboardPanel(panelIndex: 1, parentPanelIndex: 0, panel: 'A')
    dashboard.dashboardPanels << new DashboardPanel(panelIndex: 2, parentPanelIndex: 0, panel: 'B')
    dashboard.splitterPanels << new DashboardPanelSplitter(panelIndex: 1, parentPanelIndex: 0, vertical: true)
    dashboard.dashboardPanels << new DashboardPanel(panelIndex: 4, parentPanelIndex: 1, panel: 'C')
    dashboard.dashboardPanels << new DashboardPanel(panelIndex: 5, parentPanelIndex: 1, panel: 'D')
    def s = dashboard.hierarchyToString()

    then: 'the hierarchy string is correct'
    s.startsWith('Splitter[0] Horizontal')
    s.contains('  PanelA[1]')
    s.contains('  PanelB[2]')
    s.contains('  Splitter[1] Vertical')
    s.contains('    PanelC[4]')
    s.contains('    PanelD[5]')
    s.count('PanelD[5]') == 1
  }

}
