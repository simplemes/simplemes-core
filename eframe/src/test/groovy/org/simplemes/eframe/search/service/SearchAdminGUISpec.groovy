/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search.service


import org.simplemes.eframe.search.MockSearchEngineClient
import org.simplemes.eframe.search.SearchHelper
import org.simplemes.eframe.search.SearchStatus
import org.simplemes.eframe.search.page.SearchAdminPage
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.domain.RMA
import sample.domain.SampleParent
import spock.lang.IgnoreIf

import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class SearchAdminGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [SampleParent, RMA]

/**
 * Disables the status updates.  Used only for tests to prevent login issues.
 */
  def disableStatusUpdates() {
    js.exec('disableStatusUpdates()')
  }

  void cleanup() {
    disableStatusUpdates()
    SearchHelper.instance = new SearchHelper()
  }

  def "verify that admin page displays the current status correctly"() {
    given: 'a mock client with a simulated status'
    def searchStatus = new SearchStatus(status: 'green')
    searchStatus.pendingRequests = 437
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(searchStatus: searchStatus)
    SearchHelper.instance.finishedRequestCount = new AtomicInteger(137)
    SearchHelper.instance.failureCount = new AtomicInteger(3)

    when: 'the admin page is displayed'
    login()
    to SearchAdminPage

    then: 'the fields display the correct values'
    status.label == lookup('status.label')
    status.value == searchStatus.localizedStatus
    pendingRequests.label == lookup('searchStatus.pendingRequests.label')
    pendingRequests.value == searchStatus.pendingRequests.toString()
    finishedRequests.label == lookup('searchStatus.finishedRequests.label')
    finishedRequests.value == '137'
    failedRequests.label == lookup('searchStatus.failedRequests.label')
    failedRequests.value == '3'

    and: 'the buttons have the right labels/tooltips'
    resetButton.button.text() == lookup('searchResetCounters.label')
    resetButton.title == lookup('searchResetCounters.tooltip')
    rebuildAllButton.button.text() == lookup('search.rebuild.label')
    rebuildAllButton.title == lookup('search.rebuild.tooltip')

    and: 'the bulk status is not displayed'
    !bulkIndexSection.displayed
  }


  def "verify that admin page displays the current status correctly when the bulk rebuild status is available"() {
    given: 'a mock client with a simulated status'
    def searchStatus = new SearchStatus(status: 'green')
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient(searchStatus: searchStatus)
    SearchHelper.instance.bulkIndexStatus = 'completed'
    SearchHelper.instance.bulkIndexStart = UnitTestUtils.SAMPLE_TIME_MS
    SearchHelper.instance.bulkIndexEnd = UnitTestUtils.SAMPLE_TIME_MS + 10000
    SearchHelper.instance.bulkIndexRequestCount = 297
    SearchHelper.instance.bulkIndexFinishedCount = 137

    when: 'the admin page is displayed'
    login()
    to SearchAdminPage
    waitFor {
      bulkIndexSection.displayed
    }

    then: 'the fields display the correct values'
    bulkIndexStatus.label == lookup('searchStatus.bulkIndexStatus.label')
    bulkIndexStatus.value == searchStatus.localizedBulkIndexStatus
    totalBulkRequests.label == lookup('searchStatus.totalBulkRequests.label')
    totalBulkRequests.value == searchStatus.totalBulkRequests.toString()
    pendingBulkRequests.label == lookup('searchStatus.pendingBulkRequests.label')
    pendingBulkRequests.value == searchStatus.pendingBulkRequests.toString()
    finishedBulkRequests.label == lookup('searchStatus.finishedBulkRequests.label')
    finishedBulkRequests.value == searchStatus.finishedBulkRequests.toString()
    bulkIndexErrorCount.label == lookup('searchStatus.bulkIndexErrorCount.label')
    bulkIndexErrorCount.value == searchStatus.bulkIndexErrorCount.toString()
  }

  def "verify that admin page rebuild indices button works and status is updated"() {
    given: 'a mock client with a simulated status'
    SearchHelper.instance.searchEngineClient = new MockSearchEngineClient()

    and: 'the stack trace logging is disabled'
    disableStackTraceLogging()

    when: 'the admin page is displayed'
    login()
    to SearchAdminPage

    then: 'the bulk status is not displayed'
    !bulkIndexSection.displayed

    when: 'the rebuild button is pushed'
    rebuildAllButton.button.click()
    waitFor { dialog0.exists }

    then: 'the dialog is localized'
    dialog0.title.contains(lookup('search.rebuild.dialog.title'))
    dialog0.templateContent.text().contains(lookup('search.rebuild.dialog.content'))

    when: 'the build is confirmed'
    dialog0.okButton.click()
    waitFor {
      bulkIndexSection.displayed
    }

    then: 'the request counts are at the initial value'
    bulkIndexStatus.value == SearchHelper.instance.status.localizedBulkIndexStatus
    totalBulkRequests.value == SearchHelper.instance.status.totalBulkRequests.toString()
    pendingBulkRequests.value == SearchHelper.instance.status.pendingBulkRequests.toString()
    finishedBulkRequests.value == SearchHelper.instance.status.finishedBulkRequests.toString()
    bulkIndexErrorCount.value == SearchHelper.instance.status.bulkIndexErrorCount.toString()

    when: 'the status is marked as finished on the server'
    SearchHelper.instance.bulkIndexRequestCount = 137
    SearchHelper.instance.bulkIndexFinishedCount = 237
    SearchHelper.instance.bulkIndexErrorCount = 437
    SearchHelper.instance.bulkIndexStatus = 'completed'

    and: 'the status is updated'
    waitFor {
      !bulkIndexStatus.value.contains('%')
    }

    then: 'the request counts are at the final value'
    bulkIndexStatus.value == SearchHelper.instance.status.localizedBulkIndexStatus
    totalBulkRequests.value == SearchHelper.instance.status.totalBulkRequests.toString()
    pendingBulkRequests.value == SearchHelper.instance.status.pendingBulkRequests.toString()
    finishedBulkRequests.value == SearchHelper.instance.status.finishedBulkRequests.toString()
    bulkIndexErrorCount.value == SearchHelper.instance.status.bulkIndexErrorCount.toString()
  }
}

