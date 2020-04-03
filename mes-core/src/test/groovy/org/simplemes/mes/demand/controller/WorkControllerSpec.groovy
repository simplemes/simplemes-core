package org.simplemes.mes.demand.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.CompleteRequest
import org.simplemes.mes.demand.CompleteResponse
import org.simplemes.mes.demand.StartRequest
import org.simplemes.mes.demand.StartResponse
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.service.ResolveService
import org.simplemes.mes.demand.service.WorkService
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.domain.ProductionLog

/*
 * Copyright Michael Houston 2016. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests the Work Controller.  Concentrates on parsing/formatting values and error handling.
 * Full business logic is tests in the WorkServiceSpec tests.
 */
class WorkControllerSpec extends BaseAPISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, ProductionLog, Order]

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


  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that start works - via HTTP API"() {
    given: 'a released order'
    def order = null
    Order.withTransaction {
      setCurrentUser()
      order = MESUnitTestUtils.releaseOrder([:])
    }

    and: 'the JSON request'
    def request = """
      {
        "order": "$order.order"
      }
    """

    when: 'the starts is called'
    login()
    def res = sendRequest(uri: '/work/start', method: 'post', content: request)

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)
    def startResponse = json[0]
    startResponse.order == order.order
    startResponse.qty == 1.0


    and: 'the order is started'
    Order.withTransaction {
      def order2 = Order.findByOrder(order.order)
      order2 == order
      order2.qtyInWork == 1.0
      order2.qtyInQueue == 0.0
      true
    }
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that start gracefully handles bad order- via HTTP API"() {
    given: 'the JSON request'
    def request = """
      {
        "order": "GIBBERISH"
      }
    """

    and: 'disabled stack trace to reduce console output during tests'
    disableStackTraceLogging()

    when: 'the starts is called'
    login()
    def res = sendRequest(uri: '/work/start', method: 'post', content: request, status: HttpStatus.BAD_REQUEST)

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['GIBBERISH', 'order'])
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that complete works - via HTTP API"() {
    given: 'a released order'
    def order = null
    Order.withTransaction {
      setCurrentUser()
      order = MESUnitTestUtils.releaseOrder([:])
      order.qtyInWork = 1.0
      order.qtyInQueue = 0.0
      order.save()
    }

    and: 'the JSON request'
    def request = """
      {
        "order": "$order.order"
      }
    """

    when: 'the complete is called'
    login()
    def res = sendRequest(uri: '/work/complete', method: 'post', content: request)

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)
    def completeResponse = json[0]
    completeResponse.order == order.order
    completeResponse.qty == 1.0


    and: 'the order is started'
    Order.withTransaction {
      def order2 = Order.findByOrder(order.order)
      order2 == order
      order2.qtyInWork == 0.0
      true
    }
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that reverseStart works - via HTTP API"() {
    given: 'a released order'
    def order = null
    Order.withTransaction {
      setCurrentUser()
      order = MESUnitTestUtils.releaseOrder([:])
      order.qtyInWork = 1.0
      order.qtyInQueue = 0.0
      order.save()
    }

    and: 'the JSON request'
    def request = """
      {
        "order": "$order.order"
      }
    """

    when: 'the reverse is called'
    login()
    def res = sendRequest(uri: '/work/reverseStart', method: 'post', content: request)

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)
    def startResponse = json[0]
    startResponse.order == order.order
    startResponse.qty == 1.0

    and: 'the order is started'
    Order.withTransaction {
      def order2 = Order.findByOrder(order.order)
      order2 == order
      order2.qtyInWork == 0.0
      order2.qtyInQueue == 1.0
      true
    }
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that reverseComplete handles basic scenario"() {
    given: 'a released order is in done state'
    setCurrentUser()
    def order = MESUnitTestUtils.releaseOrder(qty: 100.0, qtyDone: 100.0)

    and: 'a controller with a mocked service'
    def controller = new WorkController()
    def mockService = Mock(WorkService)
    def expectCompleteRequest = new CompleteRequest(order: order, qty: 30.2)
    def expectedCompleteResponse = new CompleteResponse(order: order, qty: 30.2)
    1 * mockService.reverseComplete(expectCompleteRequest) >> [expectedCompleteResponse]
    0 * mockService._
    controller.workService = mockService

    and: 'a request in JSON format'
    def s = """ {
      "order": "${order.order}",
      "qty": 30.2
    }
    """

    when: 'the request is sent to the controller'
    def res = controller.reverseComplete(s, new MockPrincipal('jane', 'OPERATOR'))

    then: 'the response is correct'
    //println "s = ${res.body.get()}"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body.get())}"
    res.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body.get())
    def completeResponse = json[0]

    completeResponse.order == order.order
    completeResponse.qty == 30.20
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that reverseComplete works - via HTTP API"() {
    given: 'a released order is in done state'
    setCurrentUser()
    def order = MESUnitTestUtils.releaseOrder(qty: 1.0, qtyDone: 1.0)

    and: 'the JSON request'
    def request = """
      {
        "order": "$order.order"
      }
    """

    when: 'the reverse is called'
    login()
    def res = sendRequest(uri: '/work/reverseComplete', method: 'post', content: request)

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)
    def completeResponse = json[0]
    completeResponse.order == order.order
    completeResponse.qty == 1.0

    and: 'the order is completed'
    Order.withTransaction {
      def order2 = Order.findByOrder(order.order)
      order2 == order
      order2.qtyInWork == 0.0
      order2.qtyInQueue == 1.0
      true
    }
  }

}
