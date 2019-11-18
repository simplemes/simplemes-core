package org.simplemes.eframe.preference

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ColumnPreferenceSpec extends BaseSpecification {

  def "verify that determineIfEmpty detects empty preferences"() {
    expect: 'the empty state is detected correctly'
    new ColumnPreference(options).determineIfEmpty() == result

    where:
    options                | result
    null                   | true
    [width: null]          | true
    [sequence: 10]         | false
    [width: 102]           | false
    [sortLevel: 1]         | false
    [sortAscending: true]  | false
    [sortAscending: false] | false
  }
}
