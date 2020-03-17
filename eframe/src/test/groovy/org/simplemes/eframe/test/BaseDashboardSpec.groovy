/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import sample.domain.Order
import spock.lang.IgnoreIf

/**
 * Tests for the BaseDashboardSpecification logic.
 */
@IgnoreIf({ !sys['geb.env'] })
class BaseDashboardSpec extends BaseDashboardSpecification {


  @SuppressWarnings("unused")
  static dirtyDomains = [Order]

  def "verify that the dirty domains work from the parent BaseDashboardSpecification class"() {
    given: 'some records to cleanup'
    Order.withTransaction {
      new Order(order: 'M1001').save()
    }

    and: 'a dashboard that should be cleaned up by the base-class'
    buildDashboard(defaults: ['/controller/dummy'])

    expect: 'a dummy test'
    Order.list().size() > 0
  }

}
