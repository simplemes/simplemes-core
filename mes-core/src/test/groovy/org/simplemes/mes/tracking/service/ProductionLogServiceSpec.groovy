package org.simplemes.mes.tracking.service

import ch.qos.logback.classic.Level
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.archive.ArchiverFactory
import org.simplemes.eframe.archive.ArchiverFactoryInterface
import org.simplemes.eframe.archive.FileArchiver
import org.simplemes.eframe.archive.domain.ArchiveLog
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.misc.FileFactory
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.MockBean
import org.simplemes.eframe.test.MockFileFactory
import org.simplemes.eframe.test.MockObjectMapper
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.domain.LSNSequence
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.service.WorkService
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.ProductionLogArchiveRequest
import org.simplemes.mes.tracking.ProductionLogRequest
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.domain.ProductionLog

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ProductionLogServiceSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ArchiveLog, ActionLog, ProductionLog, Order, Product, LSNSequence]

  static ProductionLogService productionLogService

  def setup() {
    setCurrentUser()
    productionLogService = Holders.getBean(ProductionLogService)
  }

  @Rollback
  def "verify that log method converts Domain objects to primitives for save"() {
    given: 'an order with LSNs, workCenter and product routing'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.0, lsnTrackingOption: LSNTrackingOption.LSN_ONLY,
                                              operations: [10, 20], masterRouting: 'MR')

    and: 'a work center'
    def workCenter = new WorkCenter(workCenter: 'WC1').save()

    and: 'a production log request for the order'
    def now = new Date()
    def startDateTime = new Date(now.time - 10000)
    def request = new ProductionLogRequest(action: WorkService.ACTION_COMPLETE,
                                           order: order,
                                           lsn: order.lsns[0],
                                           operationSequence: 20,
                                           workCenter: workCenter,
                                           dateTime: now,
                                           startDateTime: startDateTime,
                                           qty: 1.2, qtyStarted: 2.2, qtyCompleted: 3.2)

    when: 'the log method is called'
    productionLogService.log(request)

    then: 'the domain record is created and saved'
    ProductionLog.list().size() == 1
    def pl = ProductionLog.list().find { it.action == WorkService.ACTION_COMPLETE }
    pl.order == order.order
    pl.lsn == order.lsns[0].lsn
    pl.product == order.product.product
    pl.masterRouting == order?.product?.masterRouting?.routing
    pl.operationSequence == request.operationSequence
    pl.workCenter == workCenter.workCenter
    pl.qty == request.qty
    pl.qtyStarted == request.qtyStarted
    pl.qtyCompleted == request.qtyCompleted
    UnitTestUtils.dateIsCloseToNow(pl.dateTime)
    UnitTestUtils.compareDates(pl.startDateTime, startDateTime)
    pl.elapsedTime == 10000

    and: 'the default user is used'
    pl.userName == SecurityUtils.TEST_USER
  }

  @Rollback
  def "verify that log method uses the passed in user"() {
    given: 'an order with LSNs and product routing'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.0)

    and: 'a user'
    User user = (User) new User(userName: 'ABC', password: 'xyz').save()

    and: 'a production log request for the order'
    def request = new ProductionLogRequest(action: WorkService.ACTION_COMPLETE,
                                           order: order, user: user)

    when: 'the log method is called'
    productionLogService.log(request)

    then: 'the domain record is created with the user'
    ProductionLog.list().size() == 1
    def pl = ProductionLog.list().find { it.action == WorkService.ACTION_COMPLETE }
    pl.userName == user.userName
  }

  @Rollback
  def "verify that log method detects missing request"() {
    when: 'the log method is called'
    productionLogService.log(null)

    then: 'an exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['request'])
  }

  @Rollback
  def "verify that log method detects missing action"() {
    when: 'the log method is called'
    productionLogService.log(new ProductionLogRequest())

    then: 'an exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['action'])
  }

  @Rollback
  def "verify that log method uses the passed in product instead of the order values"() {
    given: 'a product'
    def product = MESUnitTestUtils.buildSimpleProductWithRouting([product: 'OTHER_PC'])

    and: 'an order that uses another product'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.0, operations: [10, 20])

    and: 'a production log request for the order'
    def request = new ProductionLogRequest(action: WorkService.ACTION_COMPLETE,
                                           order: order, product: product)

    when: 'the log method is called'
    productionLogService.log(request)

    then: 'the domain record is created and saved'
    ProductionLog.list().size() == 1
    def pl = ProductionLog.list().find { it.action == WorkService.ACTION_COMPLETE }
    pl.product == product.product
  }

  @Rollback
  def "verify that log method can save records with minimal request inputs"() {
    given: 'a production log request'
    def request = new ProductionLogRequest(action: WorkService.ACTION_COMPLETE)

    when: 'the log method is called'
    productionLogService.log(request)

    then: 'the domain record is created and saved'
    ProductionLog.list().size() == 1
    ProductionLog.list().find { it.action == WorkService.ACTION_COMPLETE }
  }

  @Rollback
  def "verify that log method defaults the dates/elapsed time correctly"() {
    given: 'a production log request'
    def request = new ProductionLogRequest(action: WorkService.ACTION_COMPLETE)

    when: 'the log method is called'
    productionLogService.log(request)

    then: 'the domain record is created and saved'
    ProductionLog.list().size() == 1
    def pl = ProductionLog.list().find { it.action == WorkService.ACTION_COMPLETE }
    pl.startDateTime == pl.dateTime
    pl.elapsedTime == 0
  }

  /**
   * Builds some test records for the production log.  The date/times are incremented 1 day from the beginDateTime
   * @param options The options used to build the records.  Values include: action, count=1, beginDateTime, dateIncrement
   */
  void buildProductionLogRecords(Map options) {
    def count = options.count ?: 1
    def dateIncrement = options.dateIncrement ?: 1.0
    long dateIncrementMillis = (long) (dateIncrement * DateUtils.MILLIS_PER_DAY)
    def dateTime = options.beginDateTime ?: new Date()
    for (i in 1..count) {
      //println "dateTime = $dateTime.time $dateTime"
      productionLogService.log(new ProductionLogRequest(action: options.action ?: 'ACTION', dateTime: (Date) dateTime))
      dateTime = new Date(dateTime.time + dateIncrementMillis)
    }
  }

  def "verify that archiveOld deletes records correctly - delete mode"() {
    given: 'some old and recent production log records'
    def now = new Date()
    buildProductionLogRecords(action: 'OLD', count: 10, beginDateTime: now - 100)
    buildProductionLogRecords(action: 'NEW', count: 10, beginDateTime: now - 10)

    when: 'the archive method is called'
    def request = new ProductionLogArchiveRequest(ageDays: 15, delete: true, batchSize: 5)
    productionLogService.archiveOld(request)

    then: 'the old records are removed and the new ones are left in the DB'
    ProductionLog.withTransaction {
      assert !ProductionLog.list().find { it.action == 'OLD' }
      assert ProductionLog.list().findAll { it.action == 'NEW' }.size() == 10
      true
    }
  }

  def "verify that archiveOld supports fractional ageDays and batch size"() {
    given: 'some old production log records'
    buildProductionLogRecords(action: 'OLD', count: 10, beginDateTime: beginDateTime, dateIncrement: dateIncrement)

/*
    long offset = (long) (DateUtils.MILLIS_PER_DAY * ageDays)
    def now = new Date()
    def ageTime = now.time - offset
    def list1 = ProductionLog.list()*.dateTime
    //println "orig"
    list1.each {
      //println "  $it.time ${it.time < ageTime} $it ${it.class}"
    }
*/

    when: 'the archive method is called to delete 50% of the records'
    productionLogService.archiveOld(new ProductionLogArchiveRequest(ageDays: ageDays, delete: true, batchSize: 51.7))
/*
    def list2 = ProductionLog.list()*.dateTime
    //println "after"
    list2.each {
      //println "  $it.time ${it.time < ageTime} $it  ${it.class}"
    }
*/

    then: 'only half of the old records are removed'
    ProductionLog.withTransaction {
      assert ProductionLog.list().findAll { it.action == 'OLD' }.size() == nRecordsArchived
      true
    }

    where:
    beginDateTime  | dateIncrement | ageDays | nRecordsArchived
    new Date() - 5 | 0.5           | 2.6     | 5
    new Date() - 1 | 0.1           | 0.55    | 5
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that archiveOld archives records in batches correctly"() {
    given: 'a mock archiver factory that returns an object'
    def fileArchiver = Mock(FileArchiver)
    def mockFactory = Mock(ArchiverFactoryInterface)
    mockFactory.getArchiver() >> fileArchiver
    new MockBean(this, ArchiverFactoryInterface, mockFactory).install()

    and: 'some old and recent production log records'
    def now = new Date()
    buildProductionLogRecords(action: 'OLD', count: 10, beginDateTime: now - 100)
    buildProductionLogRecords(action: 'NEW', count: 10, beginDateTime: now - 10)

    and: 'an archive request'
    def request = new ProductionLogArchiveRequest(ageDays: 15, delete: false, batchSize: 5)

    when: 'the archive method is called'
    def fileRefs = productionLogService.archiveOld(request)

    then: 'the records are written as two files'
    fileRefs.size() == 2
    fileRefs[0] == "dummy1.arc"
    fileRefs[1] == "dummy2.arc"

    and: 'the archiver is called correctly'
    10 * fileArchiver.archive(_) >> { args -> args[0].delete() }
    2 * fileArchiver.close() >>> ["dummy1.arc", "dummy2.arc"]
    0 * fileArchiver._
  }

  @Rollback
  def "verify that archiveOld fails with missing request"() {
    when: 'the archive method is called with null'
    productionLogService.archiveOld(null)

    then: 'an exception is triggered correctly'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['request'])
  }

  @Rollback
  def "verify that archiveOld fails with missing ageDays"() {
    when: 'the archive method is called with null ageDays'
    productionLogService.archiveOld(new ProductionLogArchiveRequest())

    then: 'an exception is triggered correctly'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['ageDays'])
  }

  def "verify that log method writes debugging log messages"() {
    given: 'some old production log records'
    buildProductionLogRecords(action: 'OLD', count: 10, beginDateTime: new Date() - 100)

    and: 'mock logging is setup'
    def mockAppender = MockAppender.mock(ProductionLogService, Level.DEBUG)

    when: 'the archive method is called'
    productionLogService.archiveOld(new ProductionLogArchiveRequest(ageDays: 15, delete: true, batchSize: 5))

    then: 'the correct message is logged'
    mockAppender.assertMessageIsValid(['request', 'ageDays', 'batchSize', 'ageDate'])

    cleanup:
    MockAppender.cleanup()
  }

  def "verify that archiveOld writes trace log messages - delete scenario"() {
    given: 'some old production log records'
    buildProductionLogRecords(action: 'OLD', count: 10, beginDateTime: new Date() - 100)

    and: 'mock logging is setup'
    def mockAppender = MockAppender.mock(ProductionLogService, Level.TRACE)

    when: 'the archive method is called'
    productionLogService.archiveOld(new ProductionLogArchiveRequest(ageDays: 15, delete: true, batchSize: 5))

    then: 'the correct message is logged'
    mockAppender.assertMessageIsValid(['deleted', 'batch'])

    cleanup:
    MockAppender.cleanup()
  }

  def "verify that archiveOld writes trace log messages - archive scenario"() {
    given: 'some old production log records'
    buildProductionLogRecords(action: 'OLD', count: 10, beginDateTime: new Date() - 100)

    and: 'mock logging is setup'
    def mockAppender = MockAppender.mock(ProductionLogService, Level.TRACE)

    and: 'a mock file archiver to avoid file operations'
    def stringWriter = new StringWriter()
    FileFactory.instance = new MockFileFactory(stringWriter)

    and: 'The Jackson mapper is setup in the mocked application context'
    new MockObjectMapper(this).install()

    and: 'the archiver is mocked'
    new MockBean(this, ArchiverFactoryInterface, new ArchiverFactory()).install()  // Auto cleaned up

    when: 'the archive method is called'
    productionLogService.archiveOld(new ProductionLogArchiveRequest(ageDays: 15, delete: false, batchSize: 5))

    then: 'the correct message is logged'
    mockAppender.assertMessageIsValid(['archived', 'batch'])

    cleanup:
    FileFactory.instance = new FileFactory()
  }

  def "verify that archiveOld writes info log messages - delete scenario"() {
    given: 'some old production log records'
    buildProductionLogRecords(action: 'OLD', count: 10, beginDateTime: new Date() - 100)

    and: 'mock logging is setup'
    def mockAppender = MockAppender.mock(ProductionLogService, Level.INFO)

    when: 'the archive method is called'
    productionLogService.archiveOld(new ProductionLogArchiveRequest(ageDays: 15, delete: true, batchSize: 5))

    then: 'the correct message is logged'
    mockAppender.assertMessageIsValid(['archived', '10 records', '2 batches'])

    cleanup:
    MockAppender.cleanup()
  }

  def "verify that archiveOld handles exceptions correctly"() {
    given: 'some old production log records'
    buildProductionLogRecords(action: 'OLD', count: 10, beginDateTime: new Date() - 100)

    and: 'a mock archiver is used that will trigger exceptions on the second call'
    Holders.configuration.archive.archiver = FailingArchiver.name

    and: 'a mock file archiver to avoid file operations'
    def stringWriter = new StringWriter()
    FileFactory.instance = new MockFileFactory(stringWriter)

    and: 'The Jackson mapper is setup in the mocked application context'
    new MockObjectMapper(this).install()

    and: 'the archiver is mocked'
    new MockBean(this, ArchiverFactoryInterface, new ArchiverFactory()).install()  // Auto cleaned up

    and: 'mock logging is setup'
    def mockAppender = MockAppender.mock(ProductionLogService, Level.ERROR)

    when: 'the archive method is called and triggers an exception'
    productionLogService.archiveOld(new ProductionLogArchiveRequest(ageDays: 15, delete: false, batchSize: 5))

    then: 'the exception is handled and not thrown by the service'
    notThrown(Exception)

    and: 'the failed records are left in the database'
    ProductionLog.withTransaction {
      ProductionLog.list().size() == 5
    }

    then: 'the correct message is logged'
    mockAppender.assertMessageIsValid(['exception', 'archive'])

    cleanup:
    Holders.configuration.archive.archiver = null
    FileFactory.instance = new FileFactory()
  }

  /**
   * A dummy archiver that does nothing.
   */
  private class FailingArchiver extends FileArchiver {
    static count = 0

    /**
     * Closes the XML file and ends the archive process.
     * This test class fails on second use.
     */
    @Override
    String close() {
      count++
      if (count == 2) {
        throw new IllegalArgumentException('Failed for Testing')
      } else {
        super.close()
      }
    }
  }

}
