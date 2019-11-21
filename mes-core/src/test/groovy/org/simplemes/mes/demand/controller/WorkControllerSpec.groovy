package org.simplemes.mes.demand.controller

import grails.gorm.transactions.Rollback
import groovy.json.JsonSlurper
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.mes.demand.CompleteRequest
import org.simplemes.mes.demand.CompleteResponse
import org.simplemes.mes.demand.StartRequest
import org.simplemes.mes.demand.StartResponse
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.service.ResolveService
import org.simplemes.mes.demand.service.WorkService
import org.simplemes.mes.test.MESUnitTestUtils

/*
 * Copyright Michael Houston 2016. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the Work Controller.  Concentrates on parsing/formatting values and error handling.
 * Full business logic is tests in the WorkServiceSpec tests.
 */
class WorkControllerSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, HIBERNATE]

  /**
   * Convenience method to start an order.
   * @param order The order.
   * @param qty The quantity.
   */
  def start(Order order, BigDecimal qty) {
    def workService = new WorkService()
    workService.resolveService = new ResolveService()
    workService.start(new StartRequest(order: order, qty: qty))
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify start handles basic scenario"() {
    given: 'a released order'
    setCurrentUser()
    def order = MESUnitTestUtils.releaseOrder(qty: 100.0)

    and: 'a controller with a mocked service'
    def controller = new WorkController()
    def mockService = Mock(WorkService)
    def expectStartRequest = new StartRequest(order: order, qty: 20.2)
    def expectedStartResponse = new StartResponse(order: order, qty: 20.2)
    1 * mockService.start(expectStartRequest) >> [expectedStartResponse]
    0 * mockService._
    controller.workService = mockService

    and: 'a request in JSON format'
    def s = """ {
      "order": "${order.order}",
      "qty": 20.2
    }
    """

    when: 'the request is sent to the controller'
    def res = controller.start(s, new MockPrincipal('jane', 'OPERATOR'))

    then: 'the response is correct'
    //println "s = ${res.body.get()}"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body.get())}"
    res.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body.get())
    def startResponse = json[0]

    startResponse.order == order.order
    startResponse.qty == 20.20
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify complete handles basic scenario"() {
    given: 'a released order that was started'
    setCurrentUser()
    def order = MESUnitTestUtils.releaseOrder(qty: 100.0)
    start(order, 30.2)

    and: 'a controller with a mocked service'
    def controller = new WorkController()
    def mockService = Mock(WorkService)
    def expectedCompleteRequest = new CompleteRequest(order: order, qty: 30.2)
    def expectedCompleteResponse = new CompleteResponse(order: order, qty: 30.2)
    1 * mockService.complete(expectedCompleteRequest) >> [expectedCompleteResponse]
    0 * mockService._
    controller.workService = mockService

    and: 'a request in JSON format'
    def s = """ {
      "order": "${order.order}",
      "qty": 30.2
    }
    """

    when: 'the request is sent to the controller'
    def res = controller.complete(s, new MockPrincipal('jane', 'OPERATOR'))

    then: 'the response is correct'
    //println "s = ${res.body.get()}"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body.get())}"
    res.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body.get())
    def completeResponse = json[0]

    completeResponse.order == order.order
    completeResponse.qty == 30.20
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that reverseStart handles basic scenario"() {
    given: 'a released order is started'
    setCurrentUser()
    def order = MESUnitTestUtils.releaseOrder(qty: 100.0)
    start(order, 30.2)

    and: 'a controller with a mocked service'
    def controller = new WorkController()
    def mockService = Mock(WorkService)
    def expectStartRequest = new StartRequest(order: order, qty: 30.2)
    def expectedStartResponse = new StartResponse(order: order, qty: 30.2)
    1 * mockService.reverseStart(expectStartRequest) >> [expectedStartResponse]
    0 * mockService._
    controller.workService = mockService

    and: 'a request in JSON format'
    def s = """ {
      "order": "${order.order}",
      "qty": 30.2
    }
    """

    when: 'the request is sent to the controller'
    def res = controller.reverseStart(s, new MockPrincipal('jane', 'OPERATOR'))

    then: 'the response is correct'
    //println "s = ${res.body.get()}"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body.get())}"
    res.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body.get())
    def startResponse = json[0]

    startResponse.order == order.order
    startResponse.qty == 30.20
  }

  def "verify that the controller passes the standard controller test - security, task menu, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller WorkController
      role 'OPERATOR'
    }
  }

}
