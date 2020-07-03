/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import groovy.transform.ToString
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.i18n.GlobalUtils

/**
 * This class contains the current search engine status.  This includes basic statistics on the background
 * tasks currently queued.
 */
@ToString(includeNames = true, includePackage = false)
class SearchStatus {

  /**
   * The external engine's status.
   */
  String status = 'unknown'

  /**
   * If true, then the search engine is configured (has 'hosts' defined).
   */
  boolean configured = true

  /**
   * The number of engine requests of all types that are waiting in the search queue.
   */
  int pendingRequests = 0

  /**
   * The number of engine requests of all types that have been finished.
   */
  int finishedRequestCount = 0

  /**
   * The number of engine requests of all types that failed for some reason.
   */
  int failedRequests = 0

  /**
   * The number of bulk index request that are waiting in the search queue.
   */
  int pendingBulkRequests = 0

  /**
   * The number of bulk index request that are finished.
   */
  int finishedBulkRequests = 0

  /**
   * The total number of bulk requests in the current/last bulk index request.
   */
  int totalBulkRequests = 0

  /**
   * The number of errors found during the bulk index processing.
   */
  int bulkIndexErrorCount = 0

  /**
   * The time the current/last bulk index request was started.
   */
  long bulkIndexStart

  /**
   * The time the current/last bulk index request was finished.
   */
  long bulkIndexEnd

  /**
   * The current status of the current/last bulk index request.
   */
  String bulkIndexStatus = ''

  /**
   * Used to flag test mode for display. T for test, D for dev modes.
   */
  String testMode = ''

  /**
   * Empty constructor.
   */
  SearchStatus() {
  }

  /**
   * Convenience constructor that pulls the status from the search engine.
   * @param jsonMap The parsed JSON (as map) from the search engine.
   */
  SearchStatus(Map jsonMap) {
    status = jsonMap.status
  }

  /**
   * Builds the display CSS class that corresponds to the status color.
   * @return Returns the CSS class to use for this status.
   */
  String getStatusCSSClass() {
    switch (status) {
      case 'green':
      case 'yellow':
      case 'red':
        return "status-$status"
    }
    return "status-unknown"
  }

  /**
   * Localizes the overall status.
   * @return The localized status.
   */
  String getLocalizedStatus() {
    return GlobalUtils.lookup("searchStatus.${status}.label")
  }

  /**
   * Localizes the bulk indexing status.
   * @return The localized status.
   */
  String getLocalizedBulkIndexStatus() {
    if (bulkIndexEnd) {
      // If finished, then tack on the elapsed time.
      def elapsed = DateUtils.formatElapsedTime(bulkIndexEnd - bulkIndexStart, null)
      def dateTime = DateUtils.formatDate(new Date(bulkIndexEnd))
      return GlobalUtils.lookup("searchStatus.completed.label", dateTime, elapsed)
    } else {
      return GlobalUtils.lookup("searchStatus.${bulkIndexStatus}.label", bulkPercentComplete)
    }
  }

  /**
   * Calculate the percent complete on the bulk requests.
   * @return The percent complete.
   */
  @SuppressWarnings("UnnecessaryGetter")
  int getBulkPercentComplete() {
    if (totalBulkRequests == 0) {
      return 0
    }
    return (100 * finishedBulkRequests / totalBulkRequests).toInteger()
  }

}
