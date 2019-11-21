package org.simplemes.eframe.dashboard.domain

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DashboardConfigSpec extends BaseSpecification {
  static specNeeds = [HIBERNATE]
  static dirtyDomains = [DashboardConfig]

  def "verify that domain enforces constraints"() {
    def dashboardPanel = new DashboardPanel(panel: 'A')
    //def requiredValues = ['dashboard': 'ABC', panels: [dashboardPanel]]
    //def dashboard = new DashboardConfig(dashboard: 'XYZ')

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain DashboardConfig
      requiredValues dashboard: 'ABC', category: 'OPERATOR', panels: [dashboardPanel]
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

  @Rollback
  def "verify that unique panel ID constraint error is detected"() {
    when: 'a dashboard with invalid setting is validated'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.addToPanels(new DashboardPanel(panel: 'ABC'))
    dashboard.addToPanels(new DashboardPanel(panel: 'ABC'))

    then: 'there are errors'
    !dashboard.validate()

    and: 'the error is clear'
    def errors = GlobalUtils.lookupValidationErrors(dashboard)
    UnitTestUtils.assertContainsAllIgnoreCase(errors.panels[0], ['panel', 'abc', 'unique'])
  }

  @Rollback
  def "verify that invalid panel for a button is detected"() {
    when: 'a dashboard with invalid setting is validated'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.addToPanels(new DashboardPanel(panel: 'ABC'))
    dashboard.addToButtons(new DashboardButton(label: '123', panel: 'PDQ'))

    then: 'there are errors'
    !dashboard.validate()

    and: 'the error is clear'
    def errors = GlobalUtils.lookupValidationErrors(dashboard)
    UnitTestUtils.assertContainsAllIgnoreCase(errors.buttons[0], ['button', '123', 'invalid', 'PDQ'])
  }

  @Rollback
  def "verify that too many splitter children error is detected"() {
    when: 'a dashboard with invalid setting is validated'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.addToPanels(new DashboardPanelSplitter(panelIndex: 47))
    dashboard.addToPanels(new DashboardPanel(panelIndex: 1, parentPanelIndex: 47, panel: 'A'))
    dashboard.addToPanels(new DashboardPanel(panelIndex: 2, parentPanelIndex: 47, panel: 'B'))
    dashboard.addToPanels(new DashboardPanel(panelIndex: 3, parentPanelIndex: 47, panel: 'C'))

    then: 'there are errors'
    !dashboard.validate()

    and: 'the error is clear'
    def errors = GlobalUtils.lookupValidationErrors(dashboard)
    //dashboardConfig.panels.wrongNumberOfPanels=The splitter panel {3} must have exactly 2 child panels. It has {4} panels
    UnitTestUtils.assertContainsAllIgnoreCase(errors.panels[0], ['splitter', '47', 'child', '2', '3'])
  }

  @Rollback
  def "verify that too few splitter children error is detected"() {
    when: 'a dashboard with invalid setting is validated'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.addToPanels(new DashboardPanelSplitter(panelIndex: 47))
    dashboard.addToPanels(new DashboardPanel(panelIndex: 1, parentPanelIndex: 47, panel: 'A'))

    then: 'there are errors'
    !dashboard.validate()

    and: 'the error is clear'
    def errors = GlobalUtils.lookupValidationErrors(dashboard)
    //dashboardConfig.panels.wrongNumberOfPanels=The splitter panel {3} must have exactly 2 child panels. It has {4} panels
    UnitTestUtils.assertContainsAllIgnoreCase(errors.panels[0], ['splitter', '47', 'child', '2', '1'])
  }

  def "verify that only one default dashboard is allowed in each category - all others marked as non-default"() {
    given: 'an existing dashboard'
    DashboardConfig.withTransaction {
      def dashboardPanel1 = new DashboardPanel()
      def dashboard1 = new DashboardConfig(dashboard: 'XYZ', category: 'CAT1')
      dashboard1.addToPanels(dashboardPanel1)
      dashboard1.save()
    }

    when: 'a second dashboard is saved with the same category'
    DashboardConfig.withTransaction {
      def dashboardPanel2 = new DashboardPanel()
      def dashboard2 = new DashboardConfig(dashboard: 'PDQ', category: 'CAT1')
      dashboard2.addToPanels(dashboardPanel2)
      dashboard2.save()
    }

    then: 'there is only one in the DB marked as default'
    DashboardConfig.withTransaction {
      assert DashboardConfig.findAllByCategoryAndDefaultConfig('CAT1', true).size() == 1
      assert DashboardConfig.findByCategoryAndDefaultConfig('CAT1', true).dashboard == 'PDQ'
      true
    }
  }

  def "verify that update does not clear the default flag"() {
    given: 'an existing dashboard'
    DashboardConfig.withTransaction {
      def dashboardPanel1 = new DashboardPanel()
      def dashboard1 = new DashboardConfig(dashboard: 'XYZ', category: 'CAT1')
      dashboard1.addToPanels(dashboardPanel1)
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
    dashboard.addToPanels(new DashboardPanel())
    dashboard.addToPanels(new DashboardPanel())
    dashboard.save()

    then: 'the panel IDs are correct'
    dashboard.panels[0].panelIndex == 0
    dashboard.panels[0].panel == 'A'
    dashboard.panels[1].panelIndex == 1
    dashboard.panels[1].panel == 'B'
  }

  @Rollback
  def "verify that the default panel IDs are assigned to new panels - update"() {
    given: 'a saved dashboard'
    def dashboardConfig = new DashboardConfig(dashboard: 'XYZ')
    dashboardConfig.addToPanels(new DashboardPanel())
    dashboardConfig.save()

    when: 'a new panel is added'
    dashboardConfig.addToPanels(new DashboardPanel())
    dashboardConfig.validate(); assert !dashboardConfig.errors.allErrors
    dashboardConfig.save()

    then: 'the panel IDs are correct'
    dashboardConfig.panels[0].panel == 'A'
    dashboardConfig.panels[1].panel == 'B'
  }

  def "verify that the a panel ID can be set for a new dashboard"() {
    when: 'a saved dashboard with panel IDs'
    DashboardConfig.withTransaction {
      def dashboardConfig = new DashboardConfig(dashboard: 'XYZ')
      dashboardConfig.addToPanels(new DashboardPanel(panel: "X1"))
      dashboardConfig.addToPanels(new DashboardPanel(panel: "X2"))
      dashboardConfig.save()
    }

    then: 'the panel IDs are correct'
    DashboardConfig.withTransaction {
      def dashboardConfig = DashboardConfig.findByDashboard('XYZ')
      dashboardConfig.panels[0].panel == 'X1'
      dashboardConfig.panels[1].panel == 'X2'
    }
  }

  @Rollback
  def "verify that the default button sequences are assigned to new button"() {
    when: 'a dashboard is saved'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.addToPanels(new DashboardPanel(panel: 'A'))
    dashboard.addToButtons(new DashboardButton(label: 'b1', url: 'page1', panel: 'A'))
    dashboard.addToButtons(new DashboardButton(label: 'b2', url: 'page1', panel: 'A'))
    dashboard.save()

    then: 'the button sequences are correct'
    dashboard.buttons[0].sequence == 10
    dashboard.buttons[0].label == 'b1'
    dashboard.buttons[1].sequence == 20
    dashboard.buttons[1].label == 'b2'
  }

  void testHierarchyToString() {

  }

  @Rollback
  def "verify that hierarchyToString works"() {
    when: 'a dashboard with a complex hierarchy is written to a string'
    def dashboard = new DashboardConfig(dashboard: 'XYZ')
    dashboard.addToPanels(new DashboardPanelSplitter(panelIndex: 0))
    dashboard.addToPanels(new DashboardPanel(panelIndex: 1, parentPanelIndex: 0, panel: 'A'))
    dashboard.addToPanels(new DashboardPanel(panelIndex: 2, parentPanelIndex: 0, panel: 'B'))
    dashboard.addToPanels(new DashboardPanelSplitter(panelIndex: 3, parentPanelIndex: 0))
    dashboard.addToPanels(new DashboardPanel(panelIndex: 4, parentPanelIndex: 3, panel: 'C'))
    dashboard.addToPanels(new DashboardPanel(panelIndex: 5, parentPanelIndex: 3, panel: 'D'))
    def s = dashboard.hierarchyToString()

    then: 'the hierarchy string is correct'
    s.startsWith('Splitter[0]')
    s.contains('  PanelA[1]')
    s.contains('  PanelB[2]')
    s.contains('  Splitter[3]')
    s.contains('    PanelC[4]')
    s.contains('    PanelD[5]')
    s.count('PanelD[5]') == 1
  }

}
