package org.simplemes.mes.demand

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ResolveQuantityPreferenceSpec extends BaseSpecification {

  def "verify that areQuantitiesPreferred works for the supported cases"() {
    expect: 'the method handles the case correctly'
    preference.areQuantitiesPreferred(qtyInQueue, qtyInWork, qtyDone) == results

    where:
    preference                              | qtyInQueue | qtyInWork | qtyDone | results
    ResolveQuantityPreference.QUEUE         | 1.0        | 0.0       | 0.0     | true
    ResolveQuantityPreference.QUEUE         | 1.0        | 1.0       | 0.0     | true
    ResolveQuantityPreference.QUEUE         | 0.0        | 1.0       | 0.0     | false
    ResolveQuantityPreference.QUEUE         | 0.0        | 0.0       | 1.0     | false

    ResolveQuantityPreference.WORK          | 0.0        | 1.0       | 0.0     | true
    ResolveQuantityPreference.WORK          | 1.0        | 1.0       | 0.0     | true
    ResolveQuantityPreference.WORK          | 1.0        | 0.0       | 0.0     | false
    ResolveQuantityPreference.WORK          | 0.0        | 0.0       | 1.0     | false

    ResolveQuantityPreference.QUEUE_OR_WORK | 1.0        | 1.0       | 0.0     | true
    ResolveQuantityPreference.QUEUE_OR_WORK | 0.0        | 1.0       | 0.0     | true
    ResolveQuantityPreference.QUEUE_OR_WORK | 1.0        | 0.0       | 0.0     | true
    ResolveQuantityPreference.QUEUE_OR_WORK | 0.0        | 0.0       | 1.0     | false

    ResolveQuantityPreference.DONE          | 0.0        | 0.0       | 1.0     | true
    ResolveQuantityPreference.DONE          | 0.0        | 1.0       | 1.0     | true
    ResolveQuantityPreference.DONE          | 1.0        | 0.0       | 1.0     | true
    ResolveQuantityPreference.DONE          | 1.0        | 1.0       | 0.0     | false
    ResolveQuantityPreference.DONE          | 0.0        | 1.0       | 0.0     | false
  }
}
