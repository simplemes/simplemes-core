package org.simplemes.eframe.application

import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseAPISpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the loader in a running environment - for the slow embedded server startup case.
 * This is really on a problem with single tests that use hibernate and the embedded server.
 * This has little effect when run in all tests case.
 */
class InitialDataLoader2APISpec extends BaseAPISpecification {

  @Override
  def setupSpec() {
    // force delay for the server startup
    serverStartDelay = 5000
  }

  def "verify that the initial data is loaded for a test environment when hibernate starts before the embedded server starts"() {

    when: 'the user can be used in a login'
    login()

    then: 'the user records were loaded correctly'
    User.withTransaction {
      assert User.count() > 0
      true
    }

  }
}
