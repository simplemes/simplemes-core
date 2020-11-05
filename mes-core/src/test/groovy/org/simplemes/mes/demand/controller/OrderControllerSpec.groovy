package org.simplemes.mes.demand.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.archive.ArchiverFactoryInterface
import org.simplemes.eframe.archive.FileArchiver
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.MockBean
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
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
 * Tests for the Order Controller and general-purpose tests for Micronaut and Framework logic.
 */
class OrderControllerSpec extends BaseAPISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, Order, User]

  OrderController controller

void setup() {
  controller = Holders.getBean(OrderController)
  setCurrentUser()
  waitForInitialDataLoad()
}

  def "verify that the controller passes the standard controller test - security, task menu, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller OrderController
      role 'SUPERVISOR'
      taskMenu name: 'order', uri: '/order', folder: 'demand:500', displayOrder: 510
    }
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
    HttpResponse res = controller.restPut(order.uuid.toString(), mockRequest([body: src]), new MockPrincipal('joe', 'SUPERVISOR'))
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

  def "verify that release works - via HTTP API"() {
    given: 'an order that can be released with the JSON content'
    def order = null
    def s = null
    Order.withTransaction {
      order = new Order(order: 'ABC', qtyToBuild: 12.0).save()
      def request = new OrderReleaseRequest(order: order, qty: 1.2)
      s = Holders.objectMapper.writeValueAsString(request)
    }

    when: 'the request is made'
    login()
    def res = sendRequest(uri: '/order/release', method: 'post', content: s)

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)
    json.order.order == order.order
    json.qtyReleased == 1.2

    and: 'the database is updated correctly.'
    def order1 = Order.findByOrder('ABC')
    order1.qtyInQueue == 1.2
    order1.qtyReleased == 1.2
  }

  def "verify that releaseUI works - via HTTP API"() {
    given: 'an order that can be released'
    def order = null
    Order.withTransaction {
      order = new Order(order: 'ABC', qtyToBuild: 12.0).save()
    }

    when: 'the request is made'
    login()
    sendRequest(uri: '/order/releaseUI', method: 'post', content: [uuid: order.uuid], status: HttpStatus.FOUND)

    then: 'the order is released'
    def order1 = Order.findByOrder('ABC')
    order1.qtyInQueue == 12.0
    order1.qtyReleased == 12.0
  }

  def "verify that releaseUI redirects to the show page"() {
    given: 'an order that can be released'
    def order = null
    Order.withTransaction {
      order = new Order(order: 'ABC', qtyToBuild: 12.0).save()
    }

    when: 'the request is made'
    login()
    def res = controller.releaseUI(mockRequest(), [uuid: order.uuid.toString()], new MockPrincipal('joe', 'SUPERVISOR'))

    then: 'the order is released'
    def order1 = Order.findByOrder('ABC')
    order1.qtyInQueue == 12.0
    order1.qtyReleased == 12.0

    and: 'the UI is redirected to the right show page'
    res.status == HttpStatus.FOUND
    res.headers.get(HttpHeaders.LOCATION).startsWith("/order/show/${order.uuid}")

    and: 'the info message is in the response'
    def msg = URLEncoder.encode(GlobalUtils.lookup('released.message', null, order.order, 12.0), "UTF-8")
    res.headers.get(HttpHeaders.LOCATION).endsWith("?_info=$msg")
  }

  def "verify that releaseUI gracefully handles BusinessExceptions"() {
    given: 'an order that is already released'
    def order = null
    Order.withTransaction {
      order = new Order(order: 'ABC', qtyToBuild: 12.0, qtyReleased: 12.0).save()
    }

    when: 'the request is made'
    login()
    def res = controller.releaseUI(mockRequest(), [uuid: order.uuid.toString()], new MockPrincipal('joe', 'SUPERVISOR'))

    then: 'the UI is redirected to the right show page'
    res.status == HttpStatus.FOUND
    res.headers.get(HttpHeaders.LOCATION).startsWith("/order/show/${order.uuid}")

    and: 'the info message is in the response'
    //error.3005.message=Can''t release more quantity.  All of the quantity to build ({0}) has been released.
    def msg = URLEncoder.encode(GlobalUtils.lookup('error.3005.message', null, 12.0), "UTF-8")
    res.headers.get(HttpHeaders.LOCATION).contains("?_error=$msg")
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

  def "verify that release fails gracefully with invalid input - via HTTP API"() {
    given: 'a request with an invalid order'
    def request = """
      {"order":"GIBBERISH"}
    """

    and: 'the expected stack trace is not logged'
    disableStackTraceLogging()

    when: 'the request is made'
    login()
    def res = sendRequest(uri: '/order/release', method: 'post', content: request, status: HttpStatus.BAD_REQUEST)

    then: 'the response is a bad request with a valid message'
    def json = new JsonSlurper().parseText(res)
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['gibberish', 'order'])
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify archiveOld with JSON content"() {
    given: 'the controller has a mocked OrderService'
    def originalService = controller.orderService
    def mock = Mock(OrderService)
    controller.orderService = mock

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

    cleanup:
    controller.orderService = originalService
  }

  /**
   * Remember the last order archived so the mock close() method can remove it.
   */
  def orderArchived

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that archiveOld works - via HTTP API"() {
    given: 'a mock archiver factory that returns an object'
    def fileArchiver = Mock(FileArchiver)
    def mockFactory = Mock(ArchiverFactoryInterface)
    2 * mockFactory.getArchiver() >> fileArchiver
    new MockBean(this, ArchiverFactoryInterface, mockFactory).install()

    and: '2 really old orders, with 2 recent ones'
    Date d = new Date() - 1002
    Order.withTransaction {
      new Order(order: 'NEW', qtyToBuild: 1).save()
      new Order(order: 'RECENT1', qtyToBuild: 1, dateCompleted: new Date()).save()
      new Order(order: 'RECENT2', qtyToBuild: 1, dateCompleted: new Date()).save()
      new Order(order: 'REALLY_OLD1', qtyToBuild: 1, dateCompleted: d).save()
      new Order(order: 'REALLY_OLD2', qtyToBuild: 1, dateCompleted: d).save()
    }

    and: 'the JSON request'
    def request = """
      {
        "ageDays": 1000,
        "maxOrdersPerTxn": 50,
        "maxTxns": 1
      }
    """

    when: 'the archive is attempted on the 2 oldest orders'
    login()
    def res = sendRequest(uri: '/order/archiveOld', method: 'post', content: request)

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)
    json.size() == 2
    json[0].startsWith('unit/REALLY_OLD')
    json[1].startsWith('unit/REALLY_OLD')

    and: 'the other newer orders are not deleted'
    Order.list().size() == 3
    Order.findByOrder('RECENT1')
    Order.findByOrder('RECENT2')
    Order.findByOrder('NEW')
    !Order.findByOrder('REALLY_OLD1')
    !Order.findByOrder('REALLY_OLD2')

    and: 'the archiver is called correctly'
    2 * fileArchiver.archive(_) >> { args -> orderArchived = args[0] }
    2 * fileArchiver.close() >> { orderArchived.delete(); "unit/${orderArchived.order}.arc" }
    0 * fileArchiver._
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that archiveOld fails gracefully works - via HTTP API"() {
    given: 'the JSON request'
    def request = """
      {
        "ageDays": gibberish,
        "maxOrdersPerTxn": 50,
        "maxTxns": 1
      }
    """

    and: 'the expected stack trace is not logged'
    disableStackTraceLogging()

    when: 'the archive is attempted'
    login()
    def res = sendRequest(uri: '/order/archiveOld', method: 'post', content: request, status: HttpStatus.BAD_REQUEST)

    then: 'the response is a bad request with a valid message'
    def json = new JsonSlurper().parseText(res)
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['parse'])
  }

  @Rollback
  def "verify delete works"() {
    given: 'an order'
    def order = new Order(order: 'ABC', qtyToBuild: 12.0).save()

    when: 'the delete request is sent'
    def res = controller.delete(mockRequest(), [id: order.uuid.toString()], new MockPrincipal('joe', 'SUPERVISOR'))

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
    def res = controller.delete(mockRequest(), [id: order.uuid.toString()], new MockPrincipal('joe', 'SUPERVISOR'))

    then: 'the response is correct'
    res.status == HttpStatus.FOUND

    and: 'the order was deleted'
    !Order.findByOrder(order.order)
    !ActionLog.list()
  }

  def "verify that delete works - via HTTP API"() {
    given: 'a test user'
    setCurrentUser('admin')

    and: 'an order that can be released with the JSON content'
    def order = null
    Order.withTransaction {
      order = MESUnitTestUtils.releaseOrder([:])
      assert ActionLog.count() != 0
    }

    when: 'the request is made'
    login()
    sendRequest(uri: "/order/delete", method: 'post',
                content: [id: order.uuid.toString()], status: HttpStatus.FOUND)

    then: 'the database is updated correctly.'
    Order.count() == 0
    ActionLog.count() == 0
  }

  @Rollback
  def "test determineQtyStates with JSON output"() {
    given: 'an order'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2)

    when: 'the resolve is attempted in JSON'
    def res = controller.determineQtyStates(order.uuid.toString(), new MockPrincipal('joe', 'SUPERVISOR'))
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

  def "verify that security detects no permission  - via HTTP API"() {
    given: 'a user without the SUPERVISOR Role'
    User.withTransaction {
      new User(userName: 'TEST', password: 'TEST').save()
    }

    when: 'the request is made with a forbidden error'
    login('TEST', 'TEST')
    sendRequest(uri: "/order/release", method: 'post', content: '{}', status: HttpStatus.FORBIDDEN)

    then: 'no exception is thrown'
    notThrown(Throwable)
  }
}
