package org.simplemes.eframe.test

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.reactivex.Flowable
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.security.SecurityUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The base class for Spock tests that need to make API calls (over HTTP) to a running (embedded) server.
 * Supports a current login for API access.
 * This test always starts the embedded server and GORM.  This also creates an HTTP client that sets
 * followRedirects to false.
 * <p>
 * The test should login() for most tests.  All later requests will be sent with the login authorization cookie (until
 * logout() is called).
 * <p>
 * <b>Note:</b> Most test specs don't need to call logout().  If the user does not change, then the login authorization cookie
 * will be re-used by later tests.
 *
 */
@Slf4j
class BaseAPISpecification extends BaseSpecification {

  /**
   * Makes sure the API tests have the embedded server (with GORM/Hibernate).
   */
  @SuppressWarnings("unused")
  static specNeeds = [SERVER]

  /**
   * The client to send requests to the embedded server.
   */
  static HttpClient _client

  /**
   * The JWT cookie string to send with all non-login requests.
   */
  static String jwtCookie


  /**
   * Tracks the current user logged in.
   */
  static String loggedInUser

  /**
   * Gets the current (static) client for requests sent to the embedded server.
   * @return
   */
  HttpClient getClient() {
    if (_client == null) {
      _client = HttpClient.create(embeddedServer.URL)
      _client.configuration.followRedirects = false
      log.debug('getClient() forcing followRedirects=false for client calls')
    }
    return _client
  }


  /**
   * Logs the user in as the given user, if not already logged in.
   * If the user is different from the previous login, then the user will be logged out before the new user is logged in.
   * @param _userName The user to login as (<b>Default:</b> DEFAULT_USER='admin').
   * @param passwordInput The password to login with (<b>Default</b>: userName).
   */
  void login(String _userName = null, String passwordInput = null) {
    _userName = _userName ?: SecurityUtils.API_TEST_USER
    log.debug("Checking userName = {} loggedInUser = {}", _userName, loggedInUser)
    //println "userName = $userName, loggedInUser = $loggedInUser"
    if (loggedInUser) {
      // Already logged in, so make sure it is the right user
      if (_userName == loggedInUser) {
        // No change in user, so ignore the login() request.
        return
      }
      // Force a logout, then we can log in as the new user.
      log.debug("Logging out {} to switch to user {}", loggedInUser, _userName)
      logout()
    }
    if (passwordInput == null) {
      passwordInput = _userName
    }

    def client = getClient()
    Flowable<HttpResponse<Map>> flowable = Flowable.fromPublisher(client.exchange(
      HttpRequest.POST("/login", [username: _userName, password: _userName])
        .accept(MediaType.TEXT_HTML_TYPE)
        .header("X-My-Header", "Foo"),
      Map
    ))
    HttpResponse<Map> response = flowable.blockingFirst()

    def headers = response.headers
    def cookies = headers.getAll(HttpHeaders.SET_COOKIE)
    if (cookies.size() < 1) {
      def msg = """Login failed: No JWT cookie in response from server.
      Location Returned = ${headers.get(HttpHeaders.LOCATION)}
      Initial Data load may not have finished."""
      throw new IllegalStateException(msg)
    }
    for (cookie in cookies) {
      //println "cookie (${cookie.getClass()}) = $cookie, "
      if (cookie.startsWith('JWT=')) {
        def end = cookie.indexOf(';')
        if (end > 0) {
          jwtCookie = cookie[0..(end - 1)]
        }
      }
    }


    //def cookie = headers.get(HttpHeaders.COOKIE)
    //println "cookie = $cookie, ${cookie?.dump()}"
    //Optional<Map> body = response.getBody()
    //if (body) {
    //println "body = ${body?.get()}"
    //}

    loggedInUser = _userName
  }

  /**
   * Logs out the current test user.
   */
  void logout() {
    if (loggedInUser) {
      sendRequest(uri: "/logout", method: 'post', content: '{}', status: HttpStatus.SEE_OTHER)
      loggedInUser = null
    }
  }

  /**
   * Sends an HTTP request to the embedded server using the internal client.
   * <h3>options</h3>
   * The input options for this method are:
   * <ul>
   *   <li><b>uri</b> - The URI to send the request to (<b>Required</b>). </li>
   *   <li><b>content</b> - The request's entire content (typically JSON or a Map for form submission)(<b>Required for post/put</b>). </li>
   *   <li><b>method</b> - The request's method (<b>Default:</b> 'post'). Allowed values ('post', 'put', 'delete').</li>
   *   <li><b>status</b> - The expected status code from the request (<b>Default:</b> HttpStatus.OK (200) ). </li>
   *   <li><b>locale</b> - The language locale for the request. </li>
   * </ul>
   *
   * If the expected status code is given, then response must have that status code.
   * @param options The options.  <b>Required.</b>
   * @return The response as a string
   */
  String sendRequest(Map options) {
    // TODO: Support PUT/DELETE and maybe expected type support.
    def uri = (String) options.uri
    ArgumentUtils.checkMissing(uri, 'uri')
    def method = options.method ?: 'get'
    HttpRequest request
    def content = options.content
    switch (method.toLowerCase()) {
      case 'get':
        request = HttpRequest.GET(uri)
        break
      case 'post':
        request = HttpRequest.POST(uri, content)
        break
      case 'put':
        request = HttpRequest.PUT(uri, content)
        break
      case 'delete':
        request = HttpRequest.DELETE(uri)
        break
      default:
        throw new IllegalArgumentException("Invalid HTTP method ${method}.")
    }

    if (jwtCookie) {
      request.header(HttpHeaders.COOKIE, jwtCookie)
    }
    if (options.locale) {
      request.header(HttpHeaders.ACCEPT_LANGUAGE, (String) options.locale.toLanguageTag())
    }
    if (options.content instanceof Map) {
      request.header(HttpHeaders.CONTENT_TYPE, (String) MediaType.APPLICATION_FORM_URLENCODED)
    }
    //println "uri = $uri"
    log.debug('sendRequest() {}', request)
    //String resp = client.toBlocking().retrieve(request)
    def response
    try {
      response = client.toBlocking().exchange(request, String)
    } catch (HttpClientResponseException e) {
      if (options.status) {
        response = e.response
      } else {
        // No error was expected, so just re-throw the original exception out to the test.
        throw (e)
      }
    }

    def expectedStatus = options.status ?: HttpStatus.OK

    // Make sure the response status is expected.
    assert response.status == expectedStatus

    if (response.getBody()) {
      return response.getBody().get()
    } else {
      return null
    }
  }

  /*
  sendRequest(uri: "user/update/${user.id}", method: 'post',
              content: params,
              status: ControllerUtils.HTTP_REDIRECT_FOUND)

  sendRequest("user/update/${user.id}", [method: 'post',
                                         content: params,
                                         status: ControllerUtils.HTTP_REDIRECT_FOUND])


  APITester.sendRequest(uri '/abc'
    parameters [:]
    status 200
    method 'GET'
  )
   */

}
