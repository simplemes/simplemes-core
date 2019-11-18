package org.simplemes.eframe.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.MockRenderer
import sample.pogo.SamplePOGO

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class BaseControllerSpec extends BaseSpecification {

  static specNeeds = JSON

  Class buildSampleParentController() {
    def src = """package sample
      import org.simplemes.eframe.controller.BaseController
      import groovy.util.logging.Slf4j
      @Slf4j
      class SampleParentController extends BaseController {
      }
    """
    return CompilerTestUtils.compileSource(src)

  }

  def "verify parseBody parses the body as JSON correctly"() {
    given: 'a controller for the base class for SampleParent'
    Class clazz = buildSampleParentController()

    and: 'the JSON for an object'
    def src = """{
      "name": "ABC",
      "title": "abc"
    }
    """

    when: 'the body is parsed correctly'
    def controller = clazz.newInstance()
    def res = controller.parseBody(mockRequest([body: src]), SamplePOGO)

    then: 'the correct value is returned'
    res instanceof SamplePOGO
    res.name == 'ABC'
    res.title == 'abc'
  }

  def "verify parseBody returns null if no body given"() {
    given: 'a controller for the base class for SampleParent'
    Class clazz = buildSampleParentController()

    when: 'the body is parsed correctly'
    def controller = clazz.newInstance()
    def res = controller.parseBody(mockRequest(), SamplePOGO)

    then: 'null is returned'
    res == null
  }

  def "verify buildErrorResponse works correctly"() {
    given: 'a controller for the base class for SampleParent'
    Class clazz = buildSampleParentController()

    when: 'the body is parsed correctly'
    def controller = clazz.newInstance()
    def res = (HttpResponse) controller.buildErrorResponse('an error message')

    then: 'the correct value is returned'
    res.status == HttpStatus.BAD_REQUEST
    def json = new JsonSlurper().parseText((String) res.body.get())
    json.message.text == 'an error message'
  }

  def "verify error works correctly for HTML content requests"() {
    given: 'a controller for the base class for SampleParent'
    Class clazz = buildSampleParentController()

    and: 'a mock renderer'
    def mock = new MockRenderer(this).install()

    when: 'the body is parsed correctly'
    def controller = clazz.newInstance()
    def ex = new IllegalArgumentException('Bad Dates')
    def res = (HttpResponse) controller.error(mockRequest(accept: MediaType.TEXT_HTML), ex)

    then: 'the correct value is returned'
    res.status == HttpStatus.OK

    and: 'the right view and content is rendered'
    mock.view == 'home/error'
    mock.model[StandardModelAndView.FLASH].contains('Bad Dates')
  }

  def "verify buildOkResponse works correctly - with content"() {
    given: 'a controller for the base class for SampleParent'
    Class clazz = buildSampleParentController()

    when: 'the body is parsed correctly'
    def controller = clazz.newInstance()
    def res = (HttpResponse) controller.buildOkResponse(new SamplePOGO(name: 'ABC'))

    then: 'the correct value is returned'
    res.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body.get())
    json.name == 'ABC'
  }

  def "verify buildOkResponse works correctly - with no content"() {
    given: 'a controller for the base class for SampleParent'
    Class clazz = buildSampleParentController()

    when: 'the body is parsed correctly'
    def controller = clazz.newInstance()
    def res = (HttpResponse) controller.buildOkResponse()

    then: 'the correct value is returned'
    res.status == HttpStatus.OK
    !res.body
  }

  def "verify buildDeniedResponse detects HTML requests"() {
    given: 'a controller for the base class for SampleParent'
    Class clazz = buildSampleParentController()

    and: 'a mock renderer'
    def mockRenderer = new MockRenderer(this).install()

    when: 'the body is parsed correctly'
    def controller = (BaseController) clazz.newInstance()
    def res = (HttpResponse) controller.buildDeniedResponse(mockRequest(accept: 'other,text/html'), 'ABC', new MockPrincipal())

    then: 'the correct value is returned'
    res.status == HttpStatus.OK

    and: 'the renderer has the right view and msg'
    mockRenderer.view == 'home/denied'
    mockRenderer.model[StandardModelAndView.FLASH].contains('ABC')
  }

  def "verify buildDeniedResponse detects JSON requests"() {
    given: 'a controller for the base class for SampleParent'
    Class clazz = buildSampleParentController()

    when: 'the body is parsed correctly'
    def controller = (BaseController) clazz.newInstance()
    def res = (HttpResponse) controller.buildDeniedResponse(mockRequest(accept: "other,$accept".toString()),
                                                            'ABC', new MockPrincipal())

    then: 'the correct value is returned'
    res.status == HttpStatus.FORBIDDEN

    where:
    accept                     | _
    MediaType.APPLICATION_JSON | _
    MediaType.TEXT_JSON        | _
  }

}
