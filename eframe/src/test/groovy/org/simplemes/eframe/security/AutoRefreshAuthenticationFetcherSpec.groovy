/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security

import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.application.EFrameConfiguration
import org.simplemes.eframe.security.domain.RefreshToken
import org.simplemes.eframe.test.BaseAPISpecification

/**
 * Tests. Also tests AutoRefreshTokenFilter in live server scenario(s).
 */
class AutoRefreshAuthenticationFetcherSpec extends BaseAPISpecification {

  /**
   * The prefix for the JWT cookie.
   */
  private static final String JWT_PREFIX = 'JWT='

  /**
   * The prefix for the JWT_REFRESH_PREFIX cookie.
   */
  private static final String JWT_REFRESH_PREFIX = 'JWT_REFRESH_TOKEN='

  void cleanup() {
    jwtCookie = null
    jwtRefreshCookie = null
    logout()
  }

  def "verify that auto refresh is triggered when no JWT is passed"() {
    given: 'a logged out user'
    waitForInitialDataLoad()
    logout()

    when: 'the user is logged in'
    login()

    and: 'the JWT cookie is removed'
    jwtCookie = null
    //jwtCookie = 'JWT=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsIm5iZiI6MTYwNDQ5NjMwMCwicm9sZXMiOlsiQURNSU4iLCJDVVNUT01JWkVSIiwiTUFOQUdFUiIsIkRFU0lHTkVSIl0sImlzcyI6InNlZnJhbWUiLCJleHAiOjE2MDQ0OTcyMDEsImlhdCI6MTYwNDQ5NjMwMH0.v0ixPD5zn3zKVpest3TGWccoaOZ7izALd3DZK68GITw'

    def originalRefreshCookie = jwtRefreshCookie

    and: 'a simple request is made'
    sendRequest(uri: '/logging?abc=1')

    then: 'the response has a new JWT and JWT_REFRESH_TOKEN in it'
    def cookies = lastHttpResponse.headers.getAll(HttpHeaders.SET_COOKIE)
    def newJwtRefreshCookie = extractCookieValueFromHeader(cookies.find { it.startsWith(JWT_REFRESH_PREFIX) } as String)
    newJwtRefreshCookie != originalRefreshCookie

    def newJwtCookie = extractCookieValueFromHeader(cookies.find { it.startsWith(JWT_PREFIX) } as String)
    newJwtCookie

    when: 'another request is made with the new JWT and refresh tokens'
    jwtCookie = newJwtCookie
    jwtRefreshCookie = newJwtRefreshCookie
    sendRequest(uri: '/logging?abc=2') // Force a GET with different params to avoid caching.

    then: 'the response does not replace the JWT and JWT_REFRESH_TOKENs cookies'
    def cookies2 = lastHttpResponse.headers.getAll(HttpHeaders.SET_COOKIE)
    !cookies2.find { it.startsWith(JWT_REFRESH_PREFIX) }
    !cookies2.find { it.startsWith(JWT_PREFIX) }
  }

  def "verify that auto refresh is not triggered for various scenarios"() {
    given: 'a logged out user'
    logout()

    when: 'the user is logged in'
    login()
    def originalJwtCookie = jwtCookie
    def originalRefreshCookie = jwtRefreshCookie

    and: 'the cookies are removed, if the scenario is defined to remove them'
    if (sendJwtCookie == 'no') {
      jwtCookie = null
    } else if (sendJwtCookie == 'expired') {
      // an expired JWT
      jwtCookie = 'JWT=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsIm5iZiI6MTYwNDQ5NjMwMCwicm9sZXMiOlsiQURNSU4iLCJDVVNUT01JWkVSIiwiTUFOQUdFUiIsIkRFU0lHTkVSIl0sImlzcyI6InNlZnJhbWUiLCJleHAiOjE2MDQ0OTcyMDEsImlhdCI6MTYwNDQ5NjMwMH0.v0ixPD5zn3zKVpest3TGWccoaOZ7izALd3DZK68GITw'
    }
    if (sendRefreshCookie == 'no') {
      jwtRefreshCookie = null
    } else if (sendRefreshCookie == 'expired') {
      // an expired refresh token, so fix the value in the DB
      def tokens = RefreshToken.list()
      assert tokens.size() == 1  // No extra tokens in the DB
      RefreshToken.withTransaction {
        tokens[0].expirationDate = new Date() - 100
        tokens[0].save()
      }
    }

    if (countOver) {
      // Adjust the use count to force a new refresh token
      def tokens = RefreshToken.list()
      assert tokens.size() == 1  // No extra tokens in the DB
      RefreshToken.withTransaction {
        tokens[0].useAttemptCount = new EFrameConfiguration().security.jwtRefreshUseMax - 1
        tokens[0].save()
      }
    }

    and: 'a simple request is made'
    def method = (uri == '/logout') ? 'post' : 'get'
    def content = (uri == '/logout') ? '{}' : null
    sendRequest(uri: uri, method: method, status: status, content: content)

    then: 'the response has the expected JWT and JWT_REFRESH_TOKENs in it'
    def cookies = lastHttpResponse.headers.getAll(HttpHeaders.SET_COOKIE)
    def newJwtRefreshCookie = extractCookieValueFromHeader(cookies.find { it.startsWith(JWT_REFRESH_PREFIX) } as String)
    def newJwtCookie = extractCookieValueFromHeader(cookies.find { it.startsWith(JWT_PREFIX) } as String)

    if (newJwt) {
      newJwtCookie
      newJwtCookie != originalJwtCookie
    }
    if (newRefresh) {
      newJwtRefreshCookie
      newJwtRefreshCookie != originalRefreshCookie
    }

    where:
    sendJwtCookie | sendRefreshCookie | uri                  | countOver | newJwt | newRefresh | status
    'no'          | 'yes'             | '/logging'           | false     | true   | false      | HttpStatus.OK
    'no'          | 'yes'             | '/logging'           | true      | true   | true       | HttpStatus.OK
    'expired'     | 'yes'             | '/logging'           | false     | true   | false      | HttpStatus.OK
    'yes'         | 'yes'             | '/logging'           | false     | false  | false      | HttpStatus.OK
    'yes'         | 'no'              | '/logging'           | false     | false  | false      | HttpStatus.OK
    'no'          | 'yes'             | '/logout'            | false     | false  | true       | HttpStatus.SEE_OTHER
    'no'          | 'yes'             | '/assets/eframe.css' | false     | false  | false      | HttpStatus.OK
    'no'          | 'no'              | '/logging'           | false     | false  | false      | HttpStatus.SEE_OTHER
    'no'          | 'expired'         | '/logging'           | false     | false  | false      | HttpStatus.SEE_OTHER
  }


}
