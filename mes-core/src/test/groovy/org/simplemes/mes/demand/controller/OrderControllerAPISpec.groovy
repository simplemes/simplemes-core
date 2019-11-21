package org.simplemes.mes.demand.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.archive.ArchiverFactoryInterface
import org.simplemes.eframe.archive.FileArchiver
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.MockBean
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.mes.demand.OrderReleaseRequest
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests. Verifies at least one-pass through all methods to verify correct behavior with JSON/Hibernate methods.
 * All other method testing (e.g. error path) is done in the non-API method.
 */
class OrderControllerAPISpec extends BaseAPISpecification {

  static dirtyDomains = [ActionLog, Order, User]

  def "verify that release works with a live server and JSON"() {
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
    json.order.qtyReleased == 1.2

    and: 'the database is updated correctly.'
    Order.withTransaction {
      def order1 = Order.findByOrder('ABC')
      assert order1.qtyInQueue == 1.2
      assert order1.qtyReleased == 1.2
      true
    }
  }

  def "verify that release fails gracefully with invalid input"() {
    given: 'a request with an invalid order'
    def s = " { } "

    and: 'the expected stack trace is not logged'
    disableStackTraceLogging()

    when: 'the request is made'
    login()
    def res = sendRequest(uri: '/order/release', method: 'post', content: s, status: HttpStatus.BAD_REQUEST)

    then: 'the response is a bad request with a valid message'
    def json = new JsonSlurper().parseText(res)
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['null', 'order'])
  }

  /**
   * Remember the last order archived so the mock close() method can remove it.
   */
  def orderArchived

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that archiveOld works with a live server and JSON"() {
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
    Order.withTransaction {
      assert Order.findAll().size() == 3
      assert Order.findByOrder('RECENT1')
      assert Order.findByOrder('RECENT2')
      assert Order.findByOrder('NEW')
      assert !Order.findByOrder('REALLY_OLD1')
      assert !Order.findByOrder('REALLY_OLD2')
      true
    }

    and: 'the archiver is called correctly'
    2 * fileArchiver.archive(_) >> { args -> orderArchived = args[0] }
    2 * fileArchiver.close() >> { orderArchived.delete(flush: true); "unit/${orderArchived.order}.arc" }
    0 * fileArchiver._
  }

  // test old archive with non-numeric inputs
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that archiveOld fails gracefully works with a live server and JSON"() {
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

  def "verify that delete works on related records with a live server and JSON"() {
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
                content: [id: order.id.toString()], status: HttpStatus.FOUND)

    then: 'the database is updated correctly.'
    Order.withTransaction {
      assert Order.count() == 0
      assert ActionLog.count() == 0
      true
    }
  }

  def "verify that security works with a live server - failed access to controller"() {
    given: 'a user without the SUPERVISOR Role'
    User.withTransaction {
      new User(userName: 'TEST', password: 'TEST').save()
    }

    when: 'the request is made with a forbidden error'
    login('TEST', 'TEST')
    sendRequest(uri: "/order/x", method: 'get', status: HttpStatus.FORBIDDEN)

    then: 'no exception is thrown'
    notThrown(Throwable)
  }

}
