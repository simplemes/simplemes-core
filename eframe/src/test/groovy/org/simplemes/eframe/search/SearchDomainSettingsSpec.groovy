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
  def "verify that the map constructor works"() {
    given: 'a map'
    def map = [
      exclude   : ['title', 'releaseDate'],
      parent    : Order,
      searchable: false
    ]

    when: 'the constructor is called'
    def settings = new SearchDomainSettings(map)

    then: 'the settings are correct'
    settings.getExclude() == ['title', 'releaseDate']
    settings.parent == Order
    !settings.searchable
  }

  def "verify that if parent is not null then the searchable is false"() {
    given: 'a map'
    def map = [parent: Order]

    when: 'the constructor is called'
    def settings = new SearchDomainSettings(map)

    then: 'the domain is not searchable'
    !settings.searchable
  }

}
