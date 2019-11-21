package org.simplemes.mes.product

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class RoutingUtilsSpec extends BaseSpecification {

  def "test combineKeyAndSequence"() {
    expect: 'the values are combined correctly'
    RoutingUtils.combineKeyAndSequence(key, sequence) == expected

    where:
    key   | sequence | expected
    null  | 13       | "?/13"
    'abc' | 14       | "abc/14"
    ''    | 11       | "?/11"
  }
}
