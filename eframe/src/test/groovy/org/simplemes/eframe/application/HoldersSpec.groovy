package org.simplemes.eframe.application

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class HoldersSpec extends BaseSpecification {

  def "verify that failure path for getCurrentRequest works "() {
    expect: 'no request is handled gracefully'
    Holders.currentRequest == null

  }
}
