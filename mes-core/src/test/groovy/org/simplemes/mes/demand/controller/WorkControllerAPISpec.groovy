package org.simplemes.mes.demand.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.domain.ProductionLog

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class WorkControllerAPISpec extends BaseAPISpecification {

  static dirtyDomains = [ActionLog, ProductionLog, Order]


  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that start works with a live server and JSON"() {
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
  def "verify that start gracefully handles bad order"() {
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
  def "verify that complete works with a live server and JSON"() {
    given: 'a released order'
    def order = null
    Order.withTransaction {
      setCurrentUser()
      order = MESUnitTestUtils.releaseOrder([:])
      order.qtyInWork = 1.0
      order.qtyInQueue = 0.0
      order.save(flush: true)
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
  def "verify that reverseStart works with a live server and JSON"() {
    given: 'a released order'
    def order = null
    Order.withTransaction {
      setCurrentUser()
      order = MESUnitTestUtils.releaseOrder([:])
      order.qtyInWork = 1.0
      order.qtyInQueue = 0.0
      order.save(flush: true)
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


}