package org.simplemes.mes.system

import groovy.json.JsonSlurper
import org.openqa.selenium.Keys
import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.eframe.test.page.TextFieldModule
import org.simplemes.mes.dashboard.DashboardSpecSupport
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
@IgnoreIf({ !sys['geb.env'] })
class WorkCenterSelectionGUISpec extends BaseDashboardSpecification {


  def "verify that work center passed on URL is used by activity and the page is localized"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'])

    when: 'the dashboard is displayed'
    displayDashboard([workCenter: 'WC137'])

    then: 'the passed in work center is displayed'
    $('#workCenter').text() == 'WC137'

    and: 'the work center field is labeled correctly'
    $('#workCenterLabel').text() == lookup('workCenter.label')

    and: 'the input field is display'
    def field = (TextFieldModule) $("body").module(new TextFieldModule(field: 'order'))
    field.label == lookup('orderLsn.label')
  }

  def "verify that work center change dialog allows change and value is re-used on refresh"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the dialog is opened'
    $('#workCenter').click()
    waitFor {
      dialog0.exists
    }

    then: 'the work center field is labeled correctly'
    def field = textField("wcdChangeWorkCenter")
    field.label == lookupRequired('workCenter.label')

    when: 'the value is change and saved'
    field.input.value('XYZ')
    button('dialog0-ok').click()
    waitFor {
      !(dialog0.exists)
    }

    and: 'the page is re-displayed'
    displayDashboard()

    then: 'the previous work center is used'
    $('#workCenter').text() == 'XYZ'
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that the selection activity sends the ORDER_LSN_CHANGED event"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection', DashboardSpecSupport.DISPLAY_EVENT_ACTIVITY])

    when: 'the dashboard is displayed'
    displayDashboard([workCenter: 'WC137'])

    then: 'the order is changed'
    textField('order').input.value('ORDER1')
    sendKey(Keys.TAB)
    waitFor {
      $('#events').text().contains('ORDER_LSN_CHANGED')
    }

    and: 'the event is triggered'
    def s = $('#events').text()
    def json = new JsonSlurper().parseText(s)
    json.type == 'ORDER_LSN_CHANGED'
    json.source == '/selection/workCenterSelection'
    json.list.size() == 1
    json.list[0].order == 'ORDER1'
  }

  def "verify that the selection activity sends the WORK_CENTER_CHANGED event"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection', DashboardSpecSupport.DISPLAY_EVENT_ACTIVITY])

    when: 'the dashboard is displayed'
    displayDashboard([workCenter: 'WC137'])

    and: 'the dialog is opened'
    $('#workCenter').click()
    waitFor {
      dialog0.exists
    }

    and: 'the value is change and saved'
    def field = textField("wcdChangeWorkCenter")
    field.input.value('XYZ')
    button('dialog0-ok').click()
    waitFor {
      $('#events').text().contains('WORK_CENTER_CHANGED')
    }

    then: 'the event is triggered and contains the correct values'
    def s = $('#events').text()
    def json = new JsonSlurper().parseText(s)
    json.type == 'WORK_CENTER_CHANGED'
    json.source == '/selection/workCenterSelection'
    json.workCenter == 'XYZ'
  }


  // handles work list selection changed event
  // provide parameters
}