/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security

import org.simplemes.eframe.test.BaseGUISpecification
import spock.lang.IgnoreIf

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class CookieClearingLogoutHandlerGUISpec extends BaseGUISpecification {

  def "verify that the cookie clearing logout handler removes the JWT refresh token from the client"() {
    when: 'the user is logged in'
    login()

    and: 'logged out'
    logout()

    and: 'the home page is refreshed'
    go "/test/dashboard/cookies"

    then: 'no cookies are visible'
    def content = $('div#cookies').text()
    !content.contains("JWT")
  }
}
