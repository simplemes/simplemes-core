/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import org.simplemes.eframe.test.BaseSpecification
import sample.domain.Order

/**
 * Tests.
 */
class SearchDomainSettingsSpec extends BaseSpecification {
  def "verify that the closure constructor works"() {
    given: 'a closure'
    def closure = {
      exclude = ['title', 'releaseDate']
      parent = Order
      searchable = false
    }

    when: 'the constructor is called'
    def settings = new SearchDomainSettings(closure)

    then: 'the settings are read'
    settings.getExclude() == ['title', 'releaseDate']
    settings.parent == Order
    !settings.searchable
  }

}
