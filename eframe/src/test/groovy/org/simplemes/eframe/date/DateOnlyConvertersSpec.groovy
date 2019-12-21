package org.simplemes.eframe.date

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.Order

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DateOnlyConvertersSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [Order]

  @Rollback
  def "verify that converters work on round-trip to and from the DB"() {
    when: 'a record with a dateOnly is saved'
    def order = new Order('M1001')
    order.dueDate = new DateOnly()
    order.save()

    and: 'the record is re-read'
    def order2 = Order.findById(order.uuid)

    then: 'the read value has the correct date'
    order2.dueDate == new DateOnly()
  }

}
