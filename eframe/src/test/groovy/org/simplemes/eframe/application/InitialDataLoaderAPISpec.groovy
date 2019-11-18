package org.simplemes.eframe.application

import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseAPISpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the loader in a running environment - for the slow hibernate startup case.
 * This is really on a problem with single tests that use hibernate and the embedded server.
 * This has little effect when run in all tests case.
 */
class InitialDataLoaderAPISpec extends BaseAPISpecification {

  @Override
  def setupSpec() {
    // a force delay for the hibernate startup
    hibernateStartDelay = 5000
  }

  def "verify that the initial data is loaded for a test environment when the server starts before hibernate finishes"() {

    when: 'the user is used in a login'
    login()

    then: 'the user records were loaded correctly'
    User.withTransaction {
      assert User.count() > 0
      true
    }

  }
}
