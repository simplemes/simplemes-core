/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.custom

import org.simplemes.eframe.test.BaseSpecification

/**
 * Tests.
 */
class HistoryTrackingSpec extends BaseSpecification {

  def "verify that relative tracking level is correct to prevent reduction in tracking"() {
    expect: 'the relative levels are correct'
    HistoryTracking.NONE.detailLevel < HistoryTracking.VALUES.detailLevel
    HistoryTracking.VALUES.detailLevel < HistoryTracking.ALL.detailLevel
    HistoryTracking.ALL.detailLevel > HistoryTracking.NONE.detailLevel

  }
}
