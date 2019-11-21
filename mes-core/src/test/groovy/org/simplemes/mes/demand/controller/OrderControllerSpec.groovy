package org.simplemes.mes.demand.controller

import grails.gorm.transactions.Rollback
import groovy.json.JsonSlurper
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.mes.demand.LSNHoldStatus
import org.simplemes.mes.demand.LSNStatus
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.OrderReleaseRequest
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.service.OrderService
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OrderControllerSpec extends BaseSpecification {

  static specNeeds = [JSON, HIBERNATE]

  OrderController controller

  void setup() {
    //loadInitialData(LSNSequence)

    // release() needs the OrderService.
    controller = new OrderController()
    controller.setOrderService(new OrderService())

    setCurrentUser()
  }

  def "verify that the controller passes the standard controller test - security, task menu, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller OrderController
      role 'SUPERVISOR'
      taskMenu name: 'order', uri: '/order'
    }
  }

  def "verify that taskMenuItems are defined"() {
    when: 'the task menu items are checked'
    def taskMenuItems = controller.taskMenuItems

    then: 'the correct item is in the menu'
    def item = taskMenuItems.find { it.name == 'order' }
    item.folder == 'demand:500'
    item.uri == '/order'
    item.displayOrder == 510
  }

  @Rollback
  def "verify that updates of child LSN statuses work via JSON crud action - ensures Order/LSN are wired to framework logic"() {
    given: 'an order with LSNs'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY, qty: 2)
    def originalStatus = order.lsns[0].status
    LSNStatus newStatus1 = LSNHoldStatus.instance
    String lsn1 = order.lsns[0].lsn
    String lsn2 = order.lsns[1].lsn

    and: 'the source JSON'
    def src = """
      {
        "lsns" : [
          {"lsn": "${lsn1}", "status": "${newStatus1.id}"},
          {"lsn": "${lsn2}", "status": "${originalStatus.id}"}
        ]
      }
    """

    when: 'the status is updated'
    HttpResponse res = controller.restPut(order.id.toString(), mockRequest([body: src]), new MockPrincipal('joe', 'SUPERVISOR'))
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint((String) res.getBody().get())}"

    then: 'the response is valid'
    res.status() == HttpStatus.OK
    new JsonSlurper().parseText((String) res.getBody().get())

    and: 'the database is updated with the new status'
    def order1 = Order.findByOrder(order.order)
    order1.lsns.size() == 2
    order1.lsns[0].lsn == lsn1
    order1.lsns[0].status == newStatus1
    order1.lsns[1].lsn == lsn2
    order1.lsns[1].status == originalStatus
  }

  @Rollback
  def "verify release works"() {
    given: 'an order to be released'
    def order = new Order(order: 'ABC', qtyToBuild: 12.0).save()

    and: 'a release request'
    def request = Holders.objectMapper.writeValueAsString(new OrderReleaseRequest(order: order, qty: 1.2))

    when: 'the request is sent'
    HttpResponse res = controller.release(mockRequest([body: request]), null)

    then: 'the response is valid'
    res.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.getBody().get())
    json.order.order == order.order
    json.order.qtyReleased == 1.2

    and: 'the database is updated correctly.'
    def order1 = Order.findByOrder('ABC')
    order1.qtyInQueue == 1.2
    order1.qtyReleased == 1.2
  }

  @Rollback
  def "verify release works fails with wrong order"() {
    given: 'a release request'
    def request = """
      {"order":"GIBBERISH"}
    """

    when: 'the request is sent'
    controller.release(mockRequest([body: request]), null)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['not', 'find', 'GIBBERISH'])
  }

  @Rollback
  def "verify release works fails with no body"() {
    when: 'the request is sent'
    HttpResponse res = controller.release(mockRequest(), null)

    then: 'the response identifies the problem'
    res.status == HttpStatus.BAD_REQUEST
    def json = new JsonSlurper().parseText((String) res.getBody().get())
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['Empty', 'body'])
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify archiveOld with JSON content"() {
    given: 'the controller has a mocked OrderService'
    def mock = Mock(OrderService)
    controller.setOrderService(mock)

    and: 'a JSON request'
    def request = """
      {
        "ageDays": 100,
        "maxOrdersPerTxn": 50,
        "maxTxns": 1
      }
    """

    when: 'the request is sent'
    HttpResponse res = controller.archiveOld(mockRequest([body: request]), null)

    then: 'the service method is called properly'
    1 * mock.archiveOld(100, 50, 1) >> ['ABC']
    0 * _

    then: 'the response is valid'
    res.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.getBody().get())

    and: 'the archive references are returned properly'
    json[0] == 'ABC'
  }

  @Rollback
  def "verify delete works"() {
    given: 'an order'
    def order = new Order(order: 'ABC', qtyToBuild: 12.0).save()

    when: 'the delete request is sent'
    def res = controller.delete(mockRequest(), [id: order.id.toString()], new MockPrincipal('joe', 'SUPERVISOR'))

    then: 'the response is correct'
    res.status == HttpStatus.FOUND

    and: 'the order was deleted'
    !Order.findByOrder('ABC')
  }

  @Rollback
  def "verify delete works with related records"() {
    given: 'an order with ActionLog records'
    def order = MESUnitTestUtils.releaseOrder()

    when: 'the delete request is sent'
    def res = controller.delete(mockRequest(), [id: order.id.toString()], new MockPrincipal('joe', 'SUPERVISOR'))

    then: 'the response is correct'
    res.status == HttpStatus.FOUND

    and: 'the order was deleted'
    !Order.findByOrder(order.order)
    !ActionLog.list()
  }

  @Rollback
  def "test determineQtyStates with JSON output"() {
    given: 'an order'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2)

    when: 'the resolve is attempted in JSON'
    def res = controller.determineQtyStates(order.id.toString(), new MockPrincipal('joe', 'SUPERVISOR'))
    //println "s = ${res.body.get()}"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res.body.get())}"

    then: 'the response is correct'
    res.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) res.body.get())

    json.qtyInQueue == 1.2
    json.qtyInWork == 0.0
    json.object.order == order.order
  }

  @Rollback
  def "test determineQtyStates with not found error"() {
    when: 'the resolve is attempted in JSON'
    def res = controller.determineQtyStates('gibberish', new MockPrincipal('joe', 'SUPERVISOR'))

    then: 'the response is correct'
    res.status == HttpStatus.NOT_FOUND
  }


  // TODO: Test release() in embedded server
}
