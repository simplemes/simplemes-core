/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.MockSecurityUtils
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.eframe.web.ui.UIDefaults
import sample.domain.RMA
import sample.domain.SampleParent
import spock.lang.Ignore

/**
 * Tests.
 */
class BaseCrudController2Spec extends BaseAPISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [RMA, FlexType, SampleParent, User]

  BaseCrudController2 buildSampleParentController() {
    def src = """package sample
      import org.simplemes.eframe.controller.BaseCrudController2
      import groovy.util.logging.Slf4j
      import io.micronaut.security.annotation.Secured
      import io.micronaut.http.annotation.Controller

      @Slf4j
      @Secured("isAnonymous()")
      @Controller("/sampleParent")
      class SampleParentController  extends BaseCrudController2 {
        String indexView = 'client/eframe/flexType'
      }
    """
    return CompilerTestUtils.compileSource(src).getConstructor().newInstance() as BaseCrudController2
  }

  @Rollback
  def "verify index checks for controller-level secured annotation and fails when user has wrong permissions"() {
    given: 'a controller for SampleParent'
    def controller = buildSampleParentController()

    and: 'a mocked security utils that will fail'
    new MockSecurityUtils(this, HttpStatus.FORBIDDEN).install()

    when: 'the page method is called on the controller'
    def flowable = controller.index(mockRequest(), null)
    def res = flowable.singleElement().blockingGet()

    then: 'the correct values are returned'
    res.status == HttpStatus.FORBIDDEN
  }

  @Ignore("Ignored test.  Re-enabled when client/eframe is available on github build action.")
  def "verify that index works in a live server"() {
    when: 'the page is read'
    waitForInitialDataLoad()
    login()
    // TODO: Switch to /sample when /client/sample is available
    def res = sendRequest(uri: "/flexType", method: 'get')

    then: 'the page is correct'
    res.contains('<link href="/client/eframe/js/flexType.')
  }

  def "verify that index prevents access without the correct role in a live server"() {
    given: 'a user without the MANAGER Role'
    User.withTransaction {
      new User(userName: 'TEST', password: 'TEST').save()
    }

    when: 'the page is read'
    login('TEST', 'TEST')
    // TODO: Switch to /sample when /client/sample is available
    sendRequest(uri: "/sample", method: 'get', status: HttpStatus.FORBIDDEN)

    then: 'not error is found'
    notThrown(Throwable)
  }


  @Rollback
  def "verify list checks for controller-level secured annotation and fails when user has wrong permissions"() {
    given: 'a controller for SampleParent'
    def controller = buildSampleParentController()

    and: 'a mocked security utils that will fail'
    new MockSecurityUtils(this, HttpStatus.FORBIDDEN).install()

    when: 'the list is called from the controller'
    def res = controller.list(mockRequest(), null)

    then: 'the correct values are returned'
    res.status == HttpStatus.FORBIDDEN
  }

  @Rollback
  def "verify list works for basic case for the SampleParent domain and controller"() {
    given: 'a controller for SampleParent'
    def controller = buildSampleParentController()

    and: 'some test data is created'
    def records = DataGenerator.generate {
      domain SampleParent
      count 20
    } as List<SampleParent>

    when: 'the list is called from the controller'
    def res = controller.list(mockRequest(), null)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body())}"

    then: 'the correct values are returned'
    res.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body())
    json.total_count == 20
    def list = json.data as List<SampleParent>
    list.size() == UIDefaults.PAGE_SIZE
    list[0].name == records[0].name
    list[UIDefaults.PAGE_SIZE - 1].name == records[UIDefaults.PAGE_SIZE - 1].name
  }

  @Rollback
  def "verify list works with simple sorting"() {
    given: 'some test data is created'
    def records = DataGenerator.generate {
      domain SampleParent
      count 20
    } as List<SampleParent>
    def controller = buildSampleParentController()

    when: 'the list is called from the controller'
    def res = controller.list(mockRequest([sort: 'title']), null)

    then: 'the correct values are returned'
    res.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body())
    def list = json.data as List<SampleParent>
    list.size() == UIDefaults.PAGE_SIZE
    list[0].name == records[19].name
  }

  @Rollback
  def "verify list works with case insensitive sorting"() {
    given: 'some test data is created'
    new SampleParent(name: 'ABC4').save()
    new SampleParent(name: 'abc3').save()
    new SampleParent(name: 'abc2').save()
    new SampleParent(name: 'ABC1').save()
    def controller = buildSampleParentController()

    when: 'the list is called from the controller'
    def res = controller.list(mockRequest([sort: 'name']), null)

    then: 'the correct values are returned'
    res.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body())
    def list = json.data as List<SampleParent>
    list[0].name == 'ABC1'
    list[1].name == 'abc2'
    list[2].name == 'abc3'
    list[3].name == 'ABC4'
  }

  @Rollback
  def "verify list works for basic paging case for the SampleParent domain and controller"() {
    given: 'some test data is created'
    def records = DataGenerator.generate {
      domain SampleParent
      count 20
    } as List<SampleParent>
    def controller = buildSampleParentController()

    when: 'the list is called from the controller'
    def res = controller.list(mockRequest([count: '5', start: '10']), null)

    then: 'the correct values are returned'
    res.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body())
    def list = json.data as List<SampleParent>
    list.size() == 5
    list[0].name == records[10].name
    list[4].name == records[14].name
  }

  def "verify list with no domain fails gracefully"() {
    given: 'a controller with invalid domain'
    def src = """package sample
      import org.simplemes.eframe.controller.BaseCrudController2
      import groovy.util.logging.Slf4j
      import io.micronaut.security.annotation.Secured

      @Slf4j
      @Secured("isAnonymous()")
      class _TestController extends BaseCrudController2 {
        String indexView = '/client/eframe/flexType/index.html'
      }
    """
    def controller = CompilerTestUtils.compileSource(src).getConstructor().newInstance() as BaseCrudController2

    when: 'the list is called from the controller'
    controller.list(mockRequest(), null)

    then: 'an exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['_Test'])
  }


}
