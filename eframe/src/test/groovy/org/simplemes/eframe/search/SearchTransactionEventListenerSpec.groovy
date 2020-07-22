/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import org.simplemes.eframe.test.BaseSpecification
import sample.domain.SampleParent

/**
 * Tests.
 */
class SearchTransactionEventListenerSpec extends BaseSpecification {

  @SuppressWarnings('unused')
  static dirtyDomains = [SampleParent]

  def setup() {
    SearchHelper.instance = new SearchHelper()
  }

  def "verify that the commit event is handled correctly"() {
    given: 'a mock helper'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    when: 'a record is saved'
    waitForInitialDataLoad()
    SampleParent.withTransaction {
      new SampleParent(name: 'ABC').save()
    }

    then: 'the handle method is called'
    1 * searchHelper.handlePersistenceChange({ it.name == 'ABC' })
  }

  def "verify that the event is not triggered on rollback"() {
    given: 'a mock helper'
    def searchHelper = Mock(SearchHelper)
    SearchHelper.instance = searchHelper

    when: 'a record is saved, but the txn is rolled back - due to exception'
    waitForInitialDataLoad()
    try {
      SampleParent.withTransaction {
        new SampleParent(name: 'ABC').save()
        throw new IllegalArgumentException()
      }
    } catch (Exception ignored) {
    }

    then: 'the handle method is called'
    0 * searchHelper.handlePersistenceChange(_)
  }

  // not triggered on rollback
}
