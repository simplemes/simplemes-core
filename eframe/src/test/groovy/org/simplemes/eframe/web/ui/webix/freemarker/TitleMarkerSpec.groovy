/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.dashboard.domain.DashboardPanel
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.controller.SampleParentController
import sample.domain.SampleParent

/**
 * Tests.
 */
class TitleMarkerSpec extends BaseMarkerSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [DashboardConfig]

  def "verify that marker works for the main type"() {
    when: 'the text is generated'
    def title = execute(source: '<@efTitle />', controllerClass: SampleParentController)

    then: 'the generated text is correct'
    title == Holders.configuration.appName
  }

  def "verify that marker works for the main type with label option"() {
    when: 'the text is generated'
    def title = execute(source: '<@efTitle label="home.label"/>', controllerClass: SampleParentController)

    then: 'the generated text is correct'
    title == lookup('main.app.title', null, [lookup('home.label'), Holders.configuration.appName] as Object[])
  }

  def "verify that marker works for the list type"() {
    when: 'the text is generated'
    def title = execute(source: '<@efTitle type="list"/>', controllerClass: SampleParentController)

    then: 'the generated text is correct'
    title == lookup('list.title', [lookup('sampleParent.label'), Holders.configuration.appName] as Object[])
  }

  def "verify that marker works for the create type"() {
    when: 'the text is generated'
    def title = execute(source: '<@efTitle type="create"/>', controllerClass: SampleParentController)

    then: 'the generated text is correct'
    title == lookup('create.title', [lookup('sampleParent.label'), Holders.configuration.appName] as Object[])
  }

  def "verify that marker works for the edit and show types with model specified"() {
    when: 'the text is generated'
    def title = execute(source: "<@efTitle type='${type}'/>", controllerClass: SampleParentController,
                        dataModel: [sampleParent: new SampleParent(name: 'ABC', title: 'xyz')])

    then: 'the generated text is correct'
    title == lookup("${type}.title", ['ABC', lookup('sampleParent.label'), Holders.configuration.appName] as Object[])

    where:
    type   | _
    'edit' | _
    'show' | _
  }

  def "verify that apply gracefully handles it when no domain object is found"() {
    when: 'the text is generated'
    def title = execute(source: "<@efTitle type='${type}'/>", controllerClass: SampleParentController)

    then: 'the generated text is correct'
    title == lookup("${type}.title", [domainString, lookup('sampleParent.label'), Holders.configuration.appName] as Object[])

    where:
    type   | domainString
    'edit' | '-unknown-'
    'show' | '-unknown-'
  }

  def "verify that apply escapes HTML elements for the edit and show types"() {
    when: 'the text is generated'
    def title = execute(source: "<@efTitle type='${type}'/>", controllerClass: SampleParentController,
                        dataModel: [sampleParent: new SampleParent(name: '<script>', title: 'xyz')])

    then: 'the generated text is escaped'
    title.contains('&gt;')
    !title.contains('<script>')

    where:
    type   | _
    'edit' | _
    'show' | _
  }

  def "verify that bad type throws exception"() {
    when: 'the text is generated'
    execute(source: "<@efTitle type='gibberish'/>")

    then: 'the exception details are correct'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['gibberish', 'efTitle'])
  }

  def "verify that no label or controller throws exception"() {
    when: 'the text is generated'
    execute(source: "<@efTitle type='list'/>")

    then: 'the exception details are correct'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['No controller', 'label='])
  }

  @Rollback
  def "verify that marker works for the dashboard type - category scenario"() {
    given: 'a dashboard'
    DashboardConfig dashboardConfig = new DashboardConfig(category: 'MANAGER', dashboard: 'MANAGER', title: 'Manager')
    dashboardConfig.dashboardPanels << new DashboardPanel(panelIndex: 0)
    dashboardConfig.save()

    when: 'the text is generated'
    def title = execute(source: '<@efTitle type="dashboard" dashboardCategory="MANAGER" dashboard=""/>')

    then: 'the generated text is correct'
    title.contains('EFrame')
    title.contains('Manager')
    title.contains(lookup('dashboard.label'))
  }

  def "verify that marker works for the dashboard type - dashboard scenario"() {
    given: 'a dashboard'
    DashboardConfig.withTransaction {
      DashboardConfig dashboardConfig = new DashboardConfig(category: 'OTHER', dashboard: 'MANAGER', title: 'Manager')
      dashboardConfig.dashboardPanels << new DashboardPanel(panelIndex: 0)
      dashboardConfig.save()
    }

    when: 'the text is generated'
    def title = execute(source: '<@efTitle type="dashboard"  dashboardCategory="" dashboard="MANAGER"/>')

    then: 'the generated text is correct'
    title.contains('EFrame')
    title.contains('Manager')
  }

  def "verify that missing dashboard and category throws exception"() {
    when: 'the text is generated'
    execute(source: "<@efTitle type='dashboard'/>")

    then: 'the exception details are correct'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['efTitle', 'dashboard', 'requires', 'dashboardCategory'])
  }


}
