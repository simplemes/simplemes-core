/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.javascript


import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.eframe.test.page.GridModule
import spock.lang.IgnoreIf

/**
 * Tests of the eframe_toolkit.js refreshList test.
 */
@IgnoreIf({ !sys['geb.env'] })
class ToolkitJSRefreshListGUISpec extends BaseDashboardSpecification {

  def "verify that refreshList works and preserves the selection"() {
    given: 'a dashboard with the sample work list activity'
    buildDashboard(defaults: ['/order/orderWorkList'])

    when: 'the dashboard is displayed'
    displayDashboard()

    then: 'the list is displayed with the generated data'
    def workList = $("body").module(new GridModule(field: 'theOrderListA'))
    workList.cell(1, 0).click()
    waitFor() {
      messages.text()
    }
    messages.text().contains('M1002')
    def originalInWork = workList.cell(1, 3).text()

    when: 'the list is refreshed'
    driver.executeScript("tk.refreshList('theOrderListA')")
    waitFor {  // Wait for the list to refresh with a new value in the qtyInWork column
      def workList2 = $("body").module(new GridModule(field: 'theOrderListA'))
      workList2.cell(1, 3).text() != originalInWork
    }

    then: 'the list is updated'
    def workList3 = $("body").module(new GridModule(field: 'theOrderListA'))
    workList3.cell(1, 3).text() != originalInWork

    and: 'the row is still selected'
    workList3.isSelected(1)
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that refreshList works and preserves the selection when sorting and paging are used"() {
    given: 'a dashboard with the sample work list activity'
    buildDashboard(defaults: ['/order/orderWorkList'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the list is re-sorted on first column for descending order'
    def workList = $("body").module(new GridModule(field: 'theOrderListA'))
    workList.headers[0].click()
    waitForCompletion()

    and: 'the second page is displayed'
    workList.pagerButtons[1].click()
    waitForCompletion()

    then: 'the list is displayed with the generated data'
    workList.cell(1, 0).click()
    waitFor() {
      messages.text()
    }
    def originalInWork = workList.cell(1, 3).text()

    when: 'the list is refreshed'
    driver.executeScript("tk.refreshList('theOrderListA')")
    waitFor {  // Wait for the list to refresh with a new value in the qtyInWork column
      def workList2 = $("body").module(new GridModule(field: 'theOrderListA'))
      workList2.cell(1, 3).text() != originalInWork
    }

    then: 'the list is updated'
    def workList3 = $("body").module(new GridModule(field: 'theOrderListA'))
    workList3.cell(1, 3).text() != originalInWork

    and: 'the row is still selected'
    workList3.isSelected(1)
  }

  def "verify that refreshList works with no current selection"() {
    given: 'a dashboard with the sample work list activity'
    buildDashboard(defaults: ['/order/orderWorkList'])

    when: 'the dashboard is displayed'
    displayDashboard()

    then: 'the list is displayed with the generated data'
    def workList = $("body").module(new GridModule(field: 'theOrderListA'))
    workList.cell(1, 0).click()
    def originalInWork = workList.cell(1, 3).text()

    when: 'the list is refreshed'
    driver.executeScript("tk.refreshList('theOrderListA')")
    waitFor {  // Wait for the list to refresh with a new value in the qtyInWork column
      def workList2 = $("body").module(new GridModule(field: 'theOrderListA'))
      workList2.cell(1, 3).text() != originalInWork
    }

    then: 'the list is updated'
    def workList3 = $("body").module(new GridModule(field: 'theOrderListA'))
    workList3.cell(1, 3).text() != originalInWork
  }

}
