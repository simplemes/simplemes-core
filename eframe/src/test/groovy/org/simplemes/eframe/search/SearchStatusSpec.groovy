/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/**
 * Tests.
 */
class SearchStatusSpec extends BaseSpecification {

  def "verify that constructor handles search results fields"() {
    when: 'the result is built'
    def status = new SearchStatus([status: "green"])

    then: 'the values are copied correctly'
    status.status == 'green'
  }

  def "verify that getStatusCSSClass works"() {
    when: 'the status is built'
    def searchStatus = new SearchStatus([status: status])

    then: 'the values are copied correctly'
    searchStatus.statusCSSClass == result

    where:
    status      | result
    'green'     | 'status-green'
    'yellow'    | 'status-yellow'
    'red'       | 'status-red'
    'unknown'   | 'status-unknown'
    'gibberish' | 'status-unknown'
  }

  def "verify that getBulkPercentComplete works for supported cases"() {
    when: 'the status is built'
    def searchStatus = new SearchStatus()
    searchStatus.totalBulkRequests = total
    searchStatus.finishedBulkRequests = finished

    then: 'the values are copied correctly'
    searchStatus.bulkPercentComplete == result

    where:
    finished | total | result
    50       | 0     | 0
    25       | 100   | 25
    50       | 100   | 50
    75       | 100   | 75
    100      | 100   | 100
    0        | 100   | 0
  }

  def "verify that localized status values works"() {
    given: 'a specific locale for the current request'
    GlobalUtils.defaultLocale = locale

    when: 'the status is built'
    def searchStatus = new SearchStatus()
    searchStatus.status = 'red'

    then: 'the localizes statuses are correct'
    searchStatus.localizedStatus == GlobalUtils.lookup("searchStatus.red.label", null, locale)

    cleanup:
    GlobalUtils.defaultLocale = Locale.default

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that localized bulk index status values work while in progress"() {
    given: 'a specific locale for the current request'
    GlobalUtils.defaultLocale = locale

    when: 'the status is built'
    def searchStatus = new SearchStatus()
    searchStatus.bulkIndexStatus = 'inProgress'

    then: 'the localizes statuses are correct'
    searchStatus.localizedBulkIndexStatus == GlobalUtils.lookup("searchStatus.inProgress.label", searchStatus.bulkPercentComplete, locale)

    cleanup:
    GlobalUtils.defaultLocale = Locale.default

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that localized bulk index status values work when status is completed"() {
    given: 'a specific locale for the current request'
    GlobalUtils.defaultLocale = locale

    when: 'the status is built'
    def searchStatus = new SearchStatus()
    searchStatus.bulkIndexStatus = 'completed'
    searchStatus.bulkIndexStart = UnitTestUtils.SAMPLE_TIME_MS
    searchStatus.bulkIndexEnd = UnitTestUtils.SAMPLE_TIME_MS + 10000

    then: 'the localizes statuses are correct'
    def dateTime = DateUtils.formatDate(new Date(searchStatus.bulkIndexEnd))
    def elapsed = DateUtils.formatElapsedTime(searchStatus.bulkIndexEnd - searchStatus.bulkIndexStart, null)
    searchStatus.localizedBulkIndexStatus == GlobalUtils.lookup("searchStatus.completed.label", dateTime, elapsed, locale)

    cleanup:
    GlobalUtils.defaultLocale = Locale.default

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

}
