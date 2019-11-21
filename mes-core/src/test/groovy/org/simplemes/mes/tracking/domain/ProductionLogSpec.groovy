package org.simplemes.mes.tracking.domain

import grails.gorm.transactions.Rollback
import groovy.json.JsonSlurper
import org.simplemes.eframe.archive.ArchiveLog
import org.simplemes.eframe.archive.FileArchiver
import org.simplemes.eframe.misc.FileFactory
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.MockFileFactory
import org.simplemes.eframe.test.MockObjectMapper
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.mes.misc.FieldSizes

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ProductionLogSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ArchiveLog, ProductionLog]

  @Rollback
  def "test constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain ProductionLog
      requiredValues 'action': 'ABC', userName: SecurityUtils.TEST_USER
      maxSize 'action', FieldSizes.MAX_CODE_LENGTH
      maxSize 'userName', FieldSizes.MAX_CODE_LENGTH
      maxSize 'order', FieldSizes.MAX_CODE_LENGTH
      maxSize 'lsn', FieldSizes.MAX_LSN_LENGTH
      maxSize 'product', FieldSizes.MAX_PRODUCT_LENGTH
      maxSize 'masterRouting', FieldSizes.MAX_PRODUCT_LENGTH
      maxSize 'workCenter', FieldSizes.MAX_CODE_LENGTH
      notNullCheck 'action'
      notNullCheck 'userName'
      notInFieldOrder(['masterRouting', 'operationSequence', 'elapsedTime', 'startDateTime'])
    }

  }

  @Rollback
  def "verify that the default dateTime is set to now"() {
    given: 'a simulated current user for the request'
    setCurrentUser()

    when: 'a record is created'
    def pl = new ProductionLog()

    then: 'the dateTime is correct'
    UnitTestUtils.dateIsCloseToNow(pl.dateTime)
    UnitTestUtils.dateIsCloseToNow(pl.startDateTime)
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that production log records can be archived to JSON correctly"() {
    given: 'a production log record'
    def record = null
    ProductionLog.withTransaction {
      record = new ProductionLog(action: 'ABC', userName: 'DEF').save()
    }

    and: 'a mock file archiver to avoid file operations'
    def stringWriter = new StringWriter()
    FileFactory.instance = new MockFileFactory(stringWriter)

    and: 'The Jackson mapper is setup in the mocked application context'
    new MockObjectMapper(this).install()

    when: 'the record is archived'
    def reference = null
    ProductionLog.withTransaction {
      def archiver = new FileArchiver()
      archiver.archive(record)
      reference = archiver.close()
    }

    then: 'the old records are removed'
    ProductionLog.withTransaction {
      assert ProductionLog.findAllByAction('OLD').size() == 0
      true
    }

    and: 'the records are written to the JSON file correctly'
    def s = stringWriter.toString()
    s.size() > 0
    def json = new JsonSlurper().parseText(s)

    and: 'the JSON is correct'
    json[1].action == 'ABC'

    and: 'the file reference does not use the full toString result'
    reference.size() < 20
  }

  // TODO: Add toShortString() for archive file name purposes.
}
