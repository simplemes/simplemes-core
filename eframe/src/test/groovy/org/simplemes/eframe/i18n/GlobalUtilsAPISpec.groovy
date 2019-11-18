package org.simplemes.eframe.i18n

import org.simplemes.eframe.test.BaseAPISpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Test in running server.
 */
class GlobalUtilsAPISpec extends BaseAPISpecification {

  def "verify that getRequestLocale works in a live server"() {
    when:
    def rsp = sendRequest(uri: '/sample/locale', locale: locale)

    then:
    rsp == response

    where:
    locale         | response
    Locale.US      | "Home"
    Locale.GERMANY | "Startseite"
  }
}
