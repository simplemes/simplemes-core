package org.simplemes.eframe.misc

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ShortTermCacheMapSpec extends BaseSpecification {

  def "verify that cache works within the lifetime"() {
    when: 'a value is added'
    def cache = new ShortTermCacheMap(10000)
    cache.example = 'ABC'

    then: 'the value can be retrieved'
    cache.example == 'ABC'
  }

  def "verify that cache returns null if nothing is in the cache"() {
    when: 'the empty cache is created'
    def cache = new ShortTermCacheMap(10000)

    then: 'no value is found'
    cache.example == null
  }

  def "verify that cache will not find the value if it has expired"() {
    when: 'a value is added'
    def cache = new ShortTermCacheMap(100)
    cache.example = 'ABC'

    and: 'we wait until it has expired'
    sleep(150)

    then: 'the value is not retrieved'
    cache.example == null
  }
}
