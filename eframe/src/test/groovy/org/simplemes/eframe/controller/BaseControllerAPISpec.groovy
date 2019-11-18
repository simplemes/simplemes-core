package org.simplemes.eframe.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.controller.SampleController
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the base class behavior in an embedded server.
 */
class BaseControllerAPISpec extends BaseAPISpecification {

  static dirtyDomains = [SampleParent, AllFieldsDomain, User]

  def "verify that error works in a live server"() {
    when: 'the controller throws an error'
    disableStackTraceLogging()
    def res = sendRequest(uri: "/sample/throwsException", method: 'post', content: '{}', status: HttpStatus.BAD_REQUEST)

    then: 'the response is a bad request with a valid message'
    def json = new JsonSlurper().parseText(res)
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['a bad argument'])
  }

  def "verify that error returns the correct values when called directly"() {
    when: 'the method is called'
    disableStackTraceLogging()
    def ex = new IllegalArgumentException('a bad exception')
    def res = new SampleController().error(mockRequest(), ex)

    then: 'the response is a standard message holder in JSON'
    res.status() == HttpStatus.BAD_REQUEST
    def json = new JsonSlurper().parseText((String) res.body())
    json.message.text == ex.toString() //.contains('a bad exception')
  }

}
