/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain

import io.micronaut.data.model.Pageable
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.AllFieldsDomain
import sample.domain.Order
import sample.domain.OrderLine
import spock.lang.Unroll

import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Tests.
 */
class SQLUtilsSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain]

  @Rollback
  def "verify that executeQuery works - simple case"() {
    given: 'a domain record to find'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(sequence: 3)
    order.orderLines << new OrderLine(sequence: 1)
    order.orderLines << new OrderLine(sequence: 2)
    order.save()

    when: 'the query is executed'
    def list = SQLUtils.instance.executeQuery("SELECT * FROM ORDER_LINE order by sequence", OrderLine)

    then: 'the list is correct'
    list.size() == 3
    list[0].sequence == 1
    list[1].sequence == 2
    list[2].sequence == 3
  }

  @Rollback
  def "verify that executeQuery works - with custom fields as JSONB column"() {
    given: 'a custom field for the domain'
    DataGenerator.buildCustomField(fieldName: 'color', domainClass: Order)

    and: 'a domain record to find'
    def order = new Order(order: 'M1001')
    order.color = 'Blue'
    order.save()


    when: 'the query is executed'
    def list = SQLUtils.instance.executeQuery("SELECT * FROM ORDR where uuid=?", Order, order.uuid)

    then: 'the list is correct'
    list.size() == 1
    list[0].color == 'Blue'
  }

  @Rollback
  def "verify that executeQuery works - array of UUIDs using and IN clause"() {
    given: 'a domain record to find'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(sequence: 3)
    order.orderLines << new OrderLine(sequence: 1)
    order.orderLines << new OrderLine(sequence: 2)
    order.save()

    when: 'the query is executed'
    def uuidList = order.orderLines*.uuid
    def list = SQLUtils.instance.executeQuery("SELECT * FROM ORDER_LINE where uuid IN(?) order by uuid limit ? offset ? ",
                                              OrderLine, uuidList, 10, 0)

    then: 'the list is correct'
    list.size() == 3
    def uuidList2 = list*.uuid
    uuidList2.contains(uuidList[0])
    uuidList2.contains(uuidList[1])
    uuidList2.contains(uuidList[2])
  }

  @Rollback
  def "verify that executeQuery works - pageable"() {
    given: 'a domain record to find'
    def order = new Order(order: 'M1001')
    for (i in (1..20)) {
      order.orderLines << new OrderLine(sequence: i)
    }
    order.save()

    when: 'the query is executed'
    def list = SQLUtils.instance.executeQuery("SELECT * FROM ORDER_LINE order by sequence", OrderLine,
                                              Pageable.from(2, 5))

    then: 'the list is correct'
    list.size() == 5
    list[0].sequence == 11
  }

  @Rollback
  def "verify that executeQuery works - mapping defined in Column annotation"() {
    given: 'a domain record to find'
    def order = new Order(order: 'M1001').save()

    when: 'the query is executed'
    def list = SQLUtils.instance.executeQuery("SELECT * FROM ORDR where uuid=?", Order, order.uuid)

    then: 'the list is correct'
    list.size() == 1

    and: 'the record is mapped correctly - column name defined in Column annotation - ordr'
    list[0] == order
  }

  @Unroll
  def "verify that executeQuery works - supported argument scenarios"() {
    when: 'the query is executed'
    def afd = null
    List<AllFieldsDomain> list = null
    AllFieldsDomain.withTransaction {
      afd = new AllFieldsDomain(name: 'M1001', qty: 1.2,
                                dateTime: new Date(UnitTestUtils.SAMPLE_TIME_MS),
                                dueDate: new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)).save()
      list = SQLUtils.instance.executeQuery("SELECT * FROM all_fields_domain where $column=?", AllFieldsDomain, value)
    }

    then: 'the list is correct'
    list.size() == 1

    and: 'the record is mapped correctly'
    list[0] == afd

    where:
    column      | value
    'name'      | 'M1001'
    'qty'       | 1.2
    'date_time' | new Date(UnitTestUtils.SAMPLE_TIME_MS)
    'due_date'  | new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
  }

  @Rollback
  def "verify that executeQuery works - Map option"() {
    given: 'some records to count'
    for (i in (0..9)) {
      new AllFieldsDomain(name: "M100$i", qty: 1.2).save()
    }

    when: 'the query is executed'
    def list = SQLUtils.instance.executeQuery("SELECT COUNT(*) as count FROM all_fields_domain where qty=?", Map, 1.2)

    then: 'the list is correct'
    list.size() == 1

    and: 'the record is mapped correctly'
    list[0].count == 10
  }

  @Rollback
  def "verify that getPreparedStatement and bind work together"() {
    given: 'a domain record to find'
    def order = new Order(order: 'M1001').save()

    when: 'the query is executed'
    def list = []
    String sql = "SELECT * FROM ORDR where uuid=?"
    PreparedStatement ps = null
    ResultSet rs = null
    try {
      ps = SQLUtils.instance.getPreparedStatement(sql)
      ps.setObject(1, order.getUuid())
      ps.execute()
      rs = ps.getResultSet()
      while (rs.next()) {
        list << SQLUtils.instance.bindResultSet(rs, Order)
      }
    } finally {
      try {
        rs?.close()
      } catch (Exception ignored) {
      }
      ps?.close()
    }

    then: 'the list is correct'
    list.size() == 1

    and: 'the record is mapped correctly'
    list[0] == order
  }

  @Rollback
  def "verify that executeQuery gracefully handles SQL exception"() {
    when: 'the query is executed'
    SQLUtils.instance.executeQuery("SELECT * FROM all_X_fields_domain where name=?", AllFieldsDomain, 'ABC')

    then: 'the right exception is thrown'
    thrown(Exception)
  }

  @Rollback
  def "verify that executeQuery detects invalid SQL - single quotes"() {
    when: 'the SQL is checked'
    SQLUtils.instance.getPreparedStatement("SELECT * FROM 'all_X_fields_domain'")

    then: 'the right exception is thrown'
    def ex = thrown(IllegalArgumentException)
    UnitTestUtils.assertExceptionIsValid(ex, ['quote', 'all_X_fields_domain'])
  }

  @Rollback
  def "verify that executeQuery detects invalid SQL - double quotes"() {
    when: 'the SQL is checked'
    SQLUtils.instance.getPreparedStatement('SELECT * FROM "all_X_fields_domain"')

    then: 'the right exception is thrown'
    def ex = thrown(IllegalArgumentException)
    UnitTestUtils.assertExceptionIsValid(ex, ['quote', 'all_X_fields_domain'])
  }

}
