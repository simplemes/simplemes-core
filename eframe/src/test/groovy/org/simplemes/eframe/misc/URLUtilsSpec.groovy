/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import org.simplemes.eframe.test.BaseSpecification

/**
 * Tests.
 */
class URLUtilsSpec extends BaseSpecification {

  def "verify that addParametersToURI works with supported cases"() {
    expect:
    URLUtils.addParametersToURI(uri, params) == result

    where:
    uri                     | params             | result
    "http://xyz/page"       | null               | "http://xyz/page"
    "http://xyz/page"       | [:]                | "http://xyz/page"
    "http://xyz/page"       | [p1: 'p1']         | "http://xyz/page?p1=p1"
    "http://xyz/page?p0=Ok" | [p1: 'p1']         | "http://xyz/page?p0=Ok&p1=p1"
    "http://xyz/page?p0=Ok" | [p1: 'p 1']        | "http://xyz/page?p0=Ok&p1=p+1"
    "http://xyz/page?p0=Ok" | ['p/1': 'p/@$?=1'] | "http://xyz/page?p0=Ok&p%2F1=p%2F%40%24%3F%3D1"

  }
}
