/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.widget


import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.eframe.test.page.GridModule
import sample.controller.OrderController
import spock.lang.IgnoreIf

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
@SuppressWarnings("GroovyAssignabilityCheck")
class ListWidgetGUISpec extends BaseDashboardSpecification {

  def "verify that a basic list widget works"() {
    given: 'a dashboard with the sample work list activity'
    buildDashboard(defaults: ['/order/orderWorkList'])

    when: 'the dashboard is displayed'
    displayDashboard([workCenter: 'WC637'])

    then: 'the list is displayed'
    def workList = $("body").module(new GridModule(field: 'theOrderListA'))
    workList.cell(0, 0).text() == 'M1001'

    and: 'the right number of rows is displayed'
    workList.rows(0).size() == 10

    and: 'the pager is correct'
    def pagerButtons = workList.pagerButtons
    pagerButtons.size() >= 5

    and: 'the default sort order is marked in the column header'
    workList.sortAsc.text() == lookup('order.label')

    and: 'the argument passed to the activity is used in the list'
    workList.cell(0, 4).text() == "WC637"
  }

  def "verify that a basic list widget works - pager works"() {
    given: 'a dashboard with the sample work list activity'
    buildDashboard(defaults: ['/order/orderWorkList'])

    when: 'the dashboard is displayed'
    displayDashboard()
    def workList = $("body").module(new GridModule(field: 'theOrderListA'))

    and: 'the 3rd page is clicked'
    workList.pagerButtons[2].click()

    then: 'the next page of records is displayed'
    waitFor() {
      workList.cell(0, 0).text() == 'M1021'
    }
  }

  def "verify that a basic list widget works - sorting works"() {
    given: 'a dashboard with the sample work list activity'
    buildDashboard(defaults: ['/order/orderWorkList'])

    when: 'the dashboard is displayed'
    displayDashboard()
    def workList = $("body").module(new GridModule(field: 'theOrderListA'))

    and: 'the list is re-sorted on first column for descending order'
    workList.headers[0].click()
    waitForCompletion()

    then: 'the last element is now first'
    workList.cell(0, 0).text() == "M1${sprintf('%03d', OrderController.WORK_RECORD_COUNT)}"
  }

  def "verify that a basic list widget works - column resize is used on next display"() {
    given: 'a dashboard with the sample work list activity'
    buildDashboard(defaults: ['/order/orderWorkList'])

    when: 'the dashboard is displayed'
    displayDashboard()
    def workList = $("body").module(new GridModule(field: 'theOrderListA'))
    def headers = workList.headers
    def origWidth = headers[1].width

    and: 'the column is resized by +50 pixels'
    interact {
      def offset = -5 - (headers[2].width / 2) as int
      moveToElement(headers[2], offset, 10)
      clickAndHold()
      moveByOffset(50, 0)
      release()
    }
    waitFor {
      nonZeroRecordCount(UserPreference)
    }

    headers = workList.headers
    def newWidth = headers[1].width

    then: 'the column is resized by roughly the right amount'
    Math.abs(newWidth - origWidth - 50) < 5

    when: 'the page is re-displayed'
    displayDashboard()
    workList = $("body").module(new GridModule(field: 'theOrderListA'))

    then: 'the new column width is used'
    def headers2 = workList.headers
    def finalWidth = headers2[1].width
    Math.abs(finalWidth - origWidth - 50) < 5
  }

  def "verify that a dataFunction is supported"() {
    given: 'a dashboard with the sample work list activity'
    buildDashboard(defaults: ['/order/orderWorkList'])

    when: 'the dashboard is displayed using data from a JS function'
    displayDashboard([js: 'true'])

    then: 'the list is displayed with the generated data'
    def workList = $("body").module(new GridModule(field: 'theOrderListA'))
    workList.cell(0, 0).text() == 'ABC1'
  }

  def "verify that the onSelect attribute is supported"() {
    given: 'a dashboard with the sample work list activity'
    buildDashboard(defaults: ['/order/orderWorkList'])

    when: 'the dashboard is displayed using data from a JS function'
    displayDashboard([js: 'true'])

    then: 'the list is displayed with the generated data'
    def workList = $("body").module(new GridModule(field: 'theOrderListA'))
    workList.cell(0, 0).click()
    waitFor() {
      messages.text()
    }
    messages.text().contains('ABC1')
  }

}
