package org.simplemes.mes.demand.service

import ch.qos.logback.classic.Level
import org.simplemes.eframe.archive.ArchiverFactoryInterface
import org.simplemes.eframe.archive.FileArchiver
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.MockBean
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.OrderHoldStatus
import org.simplemes.mes.demand.OrderReleaseRequest
import org.simplemes.mes.demand.WorkableInterface
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.LSNOperState
import org.simplemes.mes.demand.domain.LSNSequence
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.domain.OrderOperState
import org.simplemes.mes.demand.domain.OrderRouting
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.product.domain.ProductRouting
import org.simplemes.mes.product.domain.RoutingOperation
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OrderServiceSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, Order, Product, LSNSequence]

  def setup() {
    setCurrentUser()
  }

  @Rollback
  def "test release with no LSNs"() {
    given: 'an order ready for release'
    def order1 = new Order(order: 'M001', qtyToBuild: 10).save()

    when: 'the order is released'
    new OrderService().release(new OrderReleaseRequest(order1))

    then: 'the order was released with the right quantities/state'
    def order = Order.findByOrder('M001')
    order.qtyInQueue == 10
    order.qtyReleased == 10
    UnitTestUtils.dateIsCloseToNow(order.dateReleased)

    and: 'the right ActionLog record was created'
    def l = ActionLog.findAllByAction(OrderService.ACTION_RELEASE_ORDER)
    assert l.size() == 1
    assert l[0].order == order
    assert l[0].qty == 10
  }

  @Rollback
  def "test release with LSN creation"() {
    given: 'a product that will force creation of LSNs'
    Product product = null
    LSNSequence.withNewSession { session ->
      LSNSequence.withNewTransaction {
        // Needs a new session to avoid the dreaded DuplicateKeyException that happens with @Rollback and existing Sequence records.
        loadInitialData(LSNSequence)
        def lsnSequence = LSNSequence.findDefaultSequence()
        lsnSequence.currentSequence = 1000
        lsnSequence.save()
        product = new Product(product: 'PC', lsnSequence: LSNSequence.findDefaultSequence(),
                              lsnTrackingOption: LSNTrackingOption.LSN_ONLY).save()
      }
    }
    def order1 = new Order(order: 'M001', qtyToBuild: 5.0, product: product).save()

    when: 'the order is released'
    new OrderService().release(new OrderReleaseRequest(order1))

    then: 'the order was released with the right qty/state'
    def order = Order.findByOrder('M001')
    order.qtyInQueue == 0
    order.qtyReleased == 5
    UnitTestUtils.dateIsCloseToNow(order.dateReleased)

    and: 'the LSNs are created with the right qty/state'
    List<LSN> lsns = order.lsns
    lsns.size() == 5
    lsns[0].lsn == 'SN1000'
    lsns[4].lsn == 'SN1004'
    lsns[0].qtyInQueue == 1
    lsns[4].qtyInQueue == 1
    order.qtyInQueue == 0

    and: 'the right ActionLog record was created for the order'
    def l = ActionLog.findAllByAction(OrderService.ACTION_RELEASE_ORDER)
    assert l.size() == 1
    assert l[0].order == order
    assert l[0].qty == 5

    and: 'the right ActionLog record was created for each LSN'
    for (lsn in lsns) {
      def al = ActionLog.findByLsn(lsn)
      al.order == order
      al.lsn == lsn
      al.qty == 1.0
    }
  }

  @Rollback
  def "test release with date-time passed in "() {
    given: 'an order that will force creation of LSNs'
    def product = null
    Product.withNewSession {
      Product.withNewTransaction {
        // Needs a new session to avoid the dreaded DuplicateKeyException that happens with @Rollback and existing Sequence records.
        product = new Product(product: 'PC', lsnSequence: LSNSequence.get(1),
                              lsnTrackingOption: LSNTrackingOption.LSN_ONLY).save()
        LSNSequence.initialDataLoad()
      }
    }
    def order1 = new Order(order: 'M001', qtyToBuild: 1.0, product: product).save()

    and: 'a date/time to log the creation on'
    def dateTime = new Date(UnitTestUtils.SAMPLE_TIME_MS)

    when: 'the order is released'
    new OrderService().release(new OrderReleaseRequest(order: order1, dateTime: dateTime))

    then: 'the order was released with the right qty/state'
    def order = Order.findByOrder('M001')
    order.dateReleased == dateTime

    and: 'the LSNs are created with the right qty/state'
    List<LSN> lsns = order.lsns
    lsns[0].dateFirstQueued == dateTime

    and: 'the right ActionLog record was created for the order'
    def l = ActionLog.findAllByAction(OrderService.ACTION_RELEASE_ORDER)
    l[0].dateTime == dateTime

    and: 'the right ActionLog record was created for each LSN'
    for (lsn in lsns) {
      def al = ActionLog.findByLsn(lsn)
      al.dateTime == dateTime
    }
  }

  def "test release with LSN creation using parameters in the LSNSequence"() {
    given: 'a product that will use parameters to create the LSNs'
    LSNSequence lsnSequence = null
    LSNSequence.withTransaction {
      lsnSequence = new LSNSequence(sequence: 'ABC',
                                    formatString: 'X${order.order}_$product.product-$currentSequence').save()
    }

    when: 'the order is released'
    def order = null
    Order.withTransaction {
      // Must re-read the sequence because we can't use it from the other session above
      lsnSequence = LSNSequence.load(lsnSequence.id)
      order = MESUnitTestUtils.releaseOrder(qty: 5.0,
                                            lsnSequence: lsnSequence,
                                            lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
      DomainUtils.instance.resolveProxies(order)
    }

    then: 'the LSNs use the correct sequence with the right parameter values'
    List<LSN> lsns = order.lsns
    lsns.size() == 5
    lsns[0].lsn == "X${order.order}_${order.product.product}-1"
    lsns[4].lsn == "X${order.order}_${order.product.product}-5"
  }

  @Rollback
  def "test release with LSNs passed in with the order creation"() {
    given: 'an order with LSN provided on creation'
    LSN sn1 = new LSN(lsn: '1234-001')
    LSN sn2 = new LSN(lsn: '1234-002')
    Order order1 = new Order(order: '1234', qtyToBuild: 2.0)
    order1.addToLsns(sn1)
    order1.addToLsns(sn2)
    order1.save()

    when: 'the order is released'
    new OrderService().release(new OrderReleaseRequest(order1))

    then: 'the order was released with the LSNs in the right qty/state'
    def order = Order.findByOrder('1234')
    List<LSN> lsns = order.lsns
    lsns.size() == 2
    lsns[0].lsn == '1234-001'
    lsns[1].lsn == '1234-002'
    lsns[0].qtyInQueue == 1
    lsns[1].qtyInQueue == 1
  }

  @Rollback
  def "test release with routing and LSNs"() {
    given: 'the LSN sequence and an order with a product routing ready for release'
    def product = null
    LSNSequence.withNewSession { session ->
      LSNSequence.withNewTransaction {
        // Needs a new session to avoid the dreaded DuplicateKeyException that happens with @Rollback and existing Sequence records.
        loadInitialData(LSNSequence)
        product = MESUnitTestUtils.buildSimpleProductWithRouting(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
      }
    }
    def order1 = new Order(order: 'M001', qtyToBuild: 5, product: product).save()

    when: 'the order is released'
    new OrderService().release(new OrderReleaseRequest(order1))

    then: 'the order has a copy of the routing'
    def order = Order.findByOrder('M001')
    ProductRouting.findAll().size() == 1
    OrderRouting.findAll().size() == 1
    RoutingOperation.findAllBySequence(1).size() == 2
    order.orderRouting.operations.size() == 3

    and: 'the lsn operation state records match the routing'
    def lsn = order.lsns[0]
    lsn.operationStates.size() == 3
    for (int i = 0; i < lsn.operationStates.size(); i++) {
      lsn.operationStates[i].sequence == order.orderRouting.operations[i].sequence
    }
    lsn.operationStates[0].qtyInQueue == 1.0
    lsn.operationStates[1].qtyInQueue == 0.0
    lsn.operationStates[2].qtyInQueue == 0.0
  }

  @Rollback
  def "test release with routing and no LSNs"() {
    given: 'an order with a product routing ready for release'
    def order1 = new Order(order: 'M001', qtyToBuild: 5,
                           product: MESUnitTestUtils.buildSimpleProductWithRouting()).save()

    when: 'the order is released'
    new OrderService().release(new OrderReleaseRequest(order1))

    then: 'the order has a copy of the routing'
    def order = Order.findByOrder('M001')
    ProductRouting.findAll().size() == 1
    OrderRouting.findAll().size() == 1
    RoutingOperation.findAllBySequence(1).size() == 2
    order.orderRouting.operations.size() == 3

    and: 'the order operation state records match the routing'
    order.operationStates.size() == 3
    for (int i = 0; i < order.operationStates.size(); i++) {
      order.operationStates[i].sequence == order.orderRouting.operations[i].sequence
    }
    order.operationStates[0].qtyInQueue == 5.0
    order.operationStates[1].qtyInQueue == 0.0
    order.operationStates[2].qtyInQueue == 0.0

    order.qtyInQueue == 0

    and: 'there is one Action Log record.'
    ActionLog.list().size() == 1
    def al = ActionLog.findByAction(OrderService.ACTION_RELEASE_ORDER)
    al.order == order
    al.product == order1.product
    al.qty == 5.0
  }

  @Rollback
  def "test release with null order"() {
    when: 'a null order is released'
    new OrderService().release(new OrderReleaseRequest(null as Order))

    then: 'should fail with proper message'
    def e = thrown(IllegalArgumentException)
    assert UnitTestUtils.allParamsHaveValues(e)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['order', 'null', 'allowed'])
  }

  @Rollback
  def "test release with negative qty"() {
    given: 'an order ready for release'
    def order = new Order(order: 'M001', qtyToBuild: 10).save()

    when: 'a negative qty is released'
    new OrderService().release(new OrderReleaseRequest(order: order, qty: -1.2))

    then: 'should fail with proper message'
    def e = thrown(BusinessException)
    UnitTestUtils.allParamsHaveValues(e)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['1.2', 'greater'])
    e.code == 3002
  }

  @Rollback
  def "test release with qty too large"() {
    given: 'an order ready for release'
    def order = new Order(order: 'M001', qtyToBuild: 10).save()

    when: 'a bad qty is released'
    new OrderService().release(new OrderReleaseRequest(order: order, qty: 11.1))

    then: 'should fail with proper message'
    def e = thrown(BusinessException)
    UnitTestUtils.allParamsHaveValues(e)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['11.1', '10', 'available'])
    e.code == 3004
  }

  @Rollback
  def "test release with all qty already released"() {
    given: 'an order that is already released'
    def order = new Order(order: 'M001', qtyToBuild: 10, qtyReleased: 10).save()

    when: 'a bad release is requested'
    new OrderService().release(new OrderReleaseRequest(order))

    then: 'should fail with proper message'
    def e = thrown(BusinessException)
    UnitTestUtils.allParamsHaveValues(e)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['10', 'more'])
    e.code == 3005
  }

  @Rollback
  def "test release() a bad order status"() {
    given: 'an order with a status that prevents release'
    def order = new Order(order: 'M001', qtyToBuild: 10, overallStatus: OrderHoldStatus.instance).save()

    when: 'a bad release is requested'
    new OrderService().release(new OrderReleaseRequest(order))

    then: 'should fail with proper message'
    def e = thrown(BusinessException)
    UnitTestUtils.allParamsHaveValues(e)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['M001', 'hold'])
    e.code == 3001
  }

  /**
   * Remember the last order archived so the mock close() method can remove it.
   */
  def orderArchived

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "test archiveOld with multiple transactions"() {
    given: 'a mock archiver factory that returns an object'
    def fileArchiver = Mock(FileArchiver)
    def mockFactory = Mock(ArchiverFactoryInterface)
    mockFactory.getArchiver() >> fileArchiver
    new MockBean(this, ArchiverFactoryInterface, mockFactory).install()

    and: 'enough orders to test 2 batches of 2 orders.  '
    Order.withTransaction {
      Date d = new Date() - 1002
      new Order(order: '1', dateCompleted: d).save()
      new Order(order: '2', dateCompleted: d).save()
      new Order(order: '3', dateCompleted: d).save()
      new Order(order: '4', dateCompleted: d).save()
      new Order(order: '5', dateCompleted: d).save()
    }

    when: 'the archive is attempted with small txn size'
    def refs = null
    Order.withTransaction {
      refs = new OrderService().archiveOld(0, 2, 2)
    }

    then: '4 orders are archived'
    refs.size() == 4

    and: 'one order is not deleted (random)'
    Order.withTransaction {
      assert Order.findAll().size() == 1
      true
    }

    and: 'the archiver is called correctly'
    4 * fileArchiver.archive(_) >> { args -> orderArchived = args[0] }
    4 * fileArchiver.close() >> { orderArchived.delete(flush: true); "unit/${orderArchived.order}.arc" }
    0 * fileArchiver._
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "test archiveOld with system default settings"() {
    given: 'a mock archiver factory that returns an object'
    def fileArchiver = Mock(FileArchiver)
    def mockFactory = Mock(ArchiverFactoryInterface)
    1 * mockFactory.getArchiver() >> fileArchiver
    new MockBean(this, ArchiverFactoryInterface, mockFactory).install()

    and: 'three orders - one order is done more than 100 days ago, one is recently done and one is not done'
    Date d = new Date() - 1002
    new Order(order: 'REALLY_OLD', qtyToBuild: 1, dateCompleted: d).save()
    new Order(order: 'RECENT', qtyToBuild: 1, dateCompleted: new Date()).save()
    new Order(order: 'NEW', qtyToBuild: 1).save()

    when: 'the archive is attempted'
    def refs = new OrderService().archiveOld()

    then: 'the right order is archived'
    refs[0].startsWith('unit/REALLY_OLD')
    !Order.findByOrder('REALLY_OLD')

    and: 'other orders were not deleted'
    Order.findAll().size() == 2
    Order.findByOrder('RECENT')
    Order.findByOrder('NEW')

    and: 'the archiver is called correctly'
    1 * fileArchiver.archive(_) >> { args -> orderArchived = args[0] }
    1 * fileArchiver.close() >> { orderArchived.delete(flush: true); "unit/${orderArchived.order}.arc" }
    0 * fileArchiver._
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "test archiveOld with specific age passed in"() {
    given: 'a mock archiver factory that returns an object'
    def fileArchiver = Mock(FileArchiver)
    def mockFactory = Mock(ArchiverFactoryInterface)
    2 * mockFactory.getArchiver() >> fileArchiver
    new MockBean(this, ArchiverFactoryInterface, mockFactory).install()

    and: '2 really old orders, with 2 recent ones'
    Date d = new Date() - 1002
    new Order(order: 'NEW', qtyToBuild: 1).save()
    new Order(order: 'RECENT1', qtyToBuild: 1, dateCompleted: new Date()).save()
    new Order(order: 'RECENT2', qtyToBuild: 1, dateCompleted: new Date()).save()
    new Order(order: 'REALLY_OLD1', qtyToBuild: 1, dateCompleted: d).save()
    new Order(order: 'REALLY_OLD2', qtyToBuild: 1, dateCompleted: d).save()

    when: 'the archive is attempted on the 2 oldest orders'
    def refs = new OrderService().archiveOld(1000)

    then: 'the 2 orders are archived'
    refs.size() == 2
    refs[0].startsWith('unit/REALLY_OLD')
    refs[1].startsWith('unit/REALLY_OLD')

    and: 'the other newer orders are not deleted'
    Order.findAll().size() == 3
    Order.findByOrder('RECENT1')
    Order.findByOrder('RECENT2')
    Order.findByOrder('NEW')
    !Order.findByOrder('REALLY_OLD1')
    !Order.findByOrder('REALLY_OLD2')

    and: 'the archiver is called correctly'
    2 * fileArchiver.archive(_) >> { args -> orderArchived = args[0] }
    2 * fileArchiver.close() >> { orderArchived.delete(flush: true); "unit/${orderArchived.order}.arc" }
    0 * fileArchiver._
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "test archiveOld in stable-mode"() {
    given: 'a mock archiver factory that returns an object'
    def fileArchiver = Mock(FileArchiver)
    def mockFactory = Mock(ArchiverFactoryInterface)
    2 * mockFactory.getArchiver() >> fileArchiver
    new MockBean(this, ArchiverFactoryInterface, mockFactory).install()

    and: 'a mock logging appender to capture the log message'
    def mockAppender = MockAppender.mock(OrderService, Level.ERROR)

    and: 'one really old order'
    Date d = new Date() - 1002
    new Order(order: '1', qtyToBuild: 1, dateCompleted: d).save()
    new Order(order: '2', qtyToBuild: 1, dateCompleted: new Date()).save()
    new Order(order: '3', qtyToBuild: 1).save()

    and: 'the stable mode archive is setup with 3 orders total in the DB'
    def refs1 = new OrderService().archiveOld(-1.0, -1, -1)
    assert Order.count() == 3
    assert refs1.size() == 0

    and: 'some new orders are added'
    new Order(order: '4', qtyToBuild: 1, dateCompleted: d).save()
    new Order(order: '5', dateCompleted: d).save()

    when: 'an archive in stable mode is attempted'
    def refs = new OrderService().archiveOld(-1.0, -1, -1)

    then: 'the 2 orders are archived'
    refs.size() == 2
    Order.count() == 3

    and: 'the archiver is called correctly'
    2 * fileArchiver.archive(_) >> { args -> orderArchived = args[0] }
    2 * fileArchiver.close() >> { orderArchived.delete(flush: true); "unit/${orderArchived.order}.arc" }
    0 * fileArchiver._

    and: 'a message is logged'
    mockAppender.assertMessageIsValid(['stableRowCount'])

    cleanup:
    MockAppender.cleanup()
  }

  @Rollback
  def "test archiveOld with invalid ageDays"() {
    when: 'invalid value'
    new OrderService().archiveOld(-2.0)

    then: 'an exception is triggered'
    def e = thrown(IllegalArgumentException)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['ageDays'])
  }

  def "test delete related records - ActionLog"() {
    given: 'a released order'
    def order = null
    Order.withTransaction {
      order = MESUnitTestUtils.releaseOrder()
    }

    when: 'the order is deleted'
    Order.withTransaction {
      new OrderService().delete(order)
    }

    then: 'the record is deleted'
    Order.withTransaction {
      assert !Order.findByOrder((String) order.order)
      true
    }

    and: 'all ActionLog records are deleted'
    Order.withTransaction {
      assert !ActionLog.findAllByOrder(order)
      true
    }
  }

  @Rollback
  def "test determineQtyStates with order - in queue"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder()

    when: 'the qty state is determined'
    def workables = new OrderService().determineQtyStates(order)

    then: 'the right workable is found'
    workables.size() == 1
    workables[0] instanceof Order
    workables[0] == order
    workables[0].qtyInQueue == 1.0
  }

  @Rollback
  @SuppressWarnings("GroovyAccessibility")
  def "test determineQtyStates with order - in work"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder()

    and: 'a qty in work only'
    order.qtyInWork = order.qtyInQueue
    order.qtyInQueue = 0
    order.save()

    when: 'the qty state is determined'
    List<WorkableInterface> workables = new OrderService().determineQtyStates(order)

    then: 'the right workable is found'
    workables.size() == 1
    workables[0] instanceof Order
    workables[0] == order
    workables[0].qtyInQueue == 0.0
    workables[0].qtyInWork == 1.0
  }

  @Rollback
  def "test determineQtyStates with order on a routing - multiple in queue and in queue"() {
    given: 'a released order on a routing'
    def order = MESUnitTestUtils.releaseOrder(operations: [10, 20, 30])

    and: 'a qty in work at another operation'
    order.operationStates[2].qtyInWork = 2.0
    order.save()

    when: 'the qty state is determined'
    def workables = new OrderService().determineQtyStates(order)

    then: 'the right workable is found'
    workables.size() == 2
    workables[0] instanceof OrderOperState
    workables[0].sequence == 10
    workables[0].qtyInQueue == 1.0
    workables[0].qtyInWork == 0.0
    workables[1] instanceof OrderOperState
    workables[1].sequence == 30
    workables[1].qtyInQueue == 0.0
    workables[1].qtyInWork == 2.0
  }

  @Rollback
  def "test determineQtyStates with order and no routing - none found"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder()

    and: 'no qty in work/queue anywhere'
    order.qtyInQueue = 0.0
    order.save()

    when: 'the qty state is determined'
    def workables = new OrderService().determineQtyStates(order)

    then: 'no workable is found'
    workables.size() == 0
  }

  @Rollback
  def "test determineQtyStates with order on a routing - none found"() {
    given: 'a released order on a routing'
    def order = MESUnitTestUtils.releaseOrder(operations: [10, 20, 30])

    and: 'no qty in queue anywhere'
    order.operationStates[0].qtyInQueue = 0.0
    order.save()

    when: 'the qty state is determined'
    def workables = new OrderService().determineQtyStates(order)

    then: 'no workable is found'
    workables.size() == 0
  }

  @Rollback
  def "test determineQtyStates with LSN - in queue"() {
    given: 'a released order with LSNs'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)

    when: 'the qty state is determined'
    def workables = new OrderService().determineQtyStates(order.lsns[0])

    then: 'the right workable is found'
    workables.size() == 1
    workables[0] instanceof LSN
    workables[0] == order.lsns[0]
    workables[0].qtyInQueue == 1.0
  }

  @Rollback
  @SuppressWarnings("GroovyAccessibility")
  def "test determineQtyStates with LSN - in work"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)

    and: 'a qty in work only'
    order.lsns[0].qtyInWork = order.lsns[0].qtyInQueue
    order.lsns[0].qtyInQueue = 0
    order.lsns[0].save()

    when: 'the qty state is determined'
    List<WorkableInterface> workables = new OrderService().determineQtyStates(order.lsns[0])

    then: 'the right workable is found'
    workables.size() == 1
    workables[0] instanceof LSN
    workables[0] == order.lsns[0]
    workables[0].qtyInQueue == 0.0
    workables[0].qtyInWork == 1.0
  }

  @Rollback
  def "test determineQtyStates with LSN and no routing - none found"() {
    given: 'a released order'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY)

    and: 'no qty in work/queue anywhere'
    order.lsns[0].qtyInQueue = 0.0
    order.lsns[0].save()

    when: 'the qty state is determined'
    def workables = new OrderService().determineQtyStates(order.lsns[0])

    then: 'no workable is found'
    workables.size() == 0
  }

  @Rollback
  def "test determineQtyStates with LSN on a routing - multiple in queue and in queue"() {
    given: 'a released order on a routing'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY, operations: [10, 20, 30])

    and: 'a qty in work at another operation'
    order.lsns[0].operationStates[2].qtyInWork = 2.0
    order.lsns[0].save()

    when: 'the qty state is determined'
    def workables = new OrderService().determineQtyStates(order.lsns[0])

    then: 'the right workable is found'
    workables.size() == 2
    workables[0] instanceof LSNOperState
    workables[0].sequence == 10
    workables[0].qtyInQueue == 1.0
    workables[0].qtyInWork == 0.0
    workables[1] instanceof LSNOperState
    workables[1].sequence == 30
    workables[1].qtyInQueue == 0.0
    workables[1].qtyInWork == 2.0
  }

  @Rollback
  def "test determineQtyStates with LSN on a routing - none found"() {
    given: 'a released order on a routing'
    def order = MESUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY, operations: [10, 20, 30])

    and: 'no qty in queue anywhere'
    order.lsns[0].operationStates[0].qtyInQueue = 0.0
    order.lsns[0].save()

    when: 'the qty state is determined'
    def workables = new OrderService().determineQtyStates(order.lsns[0])

    then: 'no workable is found'
    workables.size() == 0
  }

  // test archive LSNs
  // test archiveOld() with config settings.
}
