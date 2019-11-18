package org.simplemes.eframe.data

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.domain.AllFieldsDomain

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DateOnlyTypeSpec extends BaseSpecification {

  static dirtyDomains = [AllFieldsDomain]

  @Rollback
  def "verify that DateOnly survives round trip to db without changes"() {
    given: 'a domain'
    def dueDate = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def o = new AllFieldsDomain(name: 'ABC', dueDate: dueDate).save()

    when: 'the record is re-read'
    def object = AllFieldsDomain.get(o.id)

    then: 'the date only is correct'
    object.dueDate == dueDate
  }

  @Rollback
  def "verify that DateOnly null values work"() {
    given: 'a domain'
    def o = new AllFieldsDomain(name: 'ABC').save()

    when: 'the record is re-read'
    def object = AllFieldsDomain.get(o.id)

    then: 'the date only is correct'
    object.dueDate == null
  }

  def "test simple methods for DateOnlyType"() {
    given: 'an instance'
    def type = new DateOnlyType()
    def dueDate1 = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def dueDate1a = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
    def dueDate2 = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS + DateUtils.MILLIS_PER_DAY)

    expect: 'the simple methods work'
    type.returnedClass() == DateOnly

    and: 'equals works'
    type.equals(dueDate1, dueDate1a)
    !type.equals(dueDate1, dueDate2)
    type.hashCode(dueDate1) == dueDate1.hashCode()

    and: 'the methods that call deepCopy work'
    type.assemble(dueDate1, '?') == dueDate1
    type.disassemble(dueDate1) == dueDate1
    type.replace(dueDate1, '?', '?') == dueDate1
  }

}
