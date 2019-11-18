package org.simplemes.eframe.data.format

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.data.CustomFieldDefinition
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import sample.domain.Order
import sample.domain.OrderLine

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class CustomCustomChildListFieldFormatSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [HIBERNATE]

  @SuppressWarnings("unused")
  static dirtyDomains = [OrderLine, Order]


  def "verify that id and toString work and the format is registered in the BasicFieldFormat class"() {
    expect:
    CustomChildListFieldFormat.instance.id == CustomChildListFieldFormat.ID
    CustomChildListFieldFormat.instance.toString() == 'CustomChildList'
    BasicFieldFormat.coreValues.contains(CustomChildListFieldFormat)
  }

  def "verify that the format methods fail"() {
    when: 'the method is called'
    CustomChildListFieldFormat.instance.format('', null, null)

    then: 'the right exception is thrown'
    thrown(UnsupportedOperationException)
  }

  def "verify that the parse methods fail"() {
    when: 'the method is called'
    CustomChildListFieldFormat.instance.parse('', null, null)

    then: 'the right exception is thrown'
    thrown(UnsupportedOperationException)
  }

  def "verify that the encode methods fail"() {
    when: 'the method is called'
    CustomChildListFieldFormat.instance.encode('', null)

    then: 'the right exception is thrown'
    thrown(UnsupportedOperationException)
  }

  def "verify that the decode methods fail"() {
    when: 'the method is called'
    CustomChildListFieldFormat.instance.decode('', null)

    then: 'the right exception is thrown'
    thrown(UnsupportedOperationException)
  }

  @Rollback
  def "verify that the readList method finds child records"() {
    given: 'a field definition'
    def fieldDefinition = new CustomFieldDefinition(name: 'orderLine', format: CustomChildListFieldFormat.instance,
                                                    type: List, referenceType: OrderLine)

    and: 'a parent record'
    def (Order order) = DataGenerator.generate {
      domain Order
    }

    and: 'the child custom records'
    def orderLines = DataGenerator.generate {
      domain OrderLine
      values orderId: order.id, sequence: 0, product: 'PRODUCT-$i', qty: 1.2
    }

    when: 'the values are read via the format'
    def list = (List) CustomChildListFieldFormat.instance.readList(order, fieldDefinition)

    then: 'the list contains the value'
    list.size() == 1
    list[0].id == orderLines[0].id
  }

  def "verify that the saveList method saves changed child records - in a proper transaction"() {
    given: 'a field definition'
    def fieldDefinition = new CustomFieldDefinition(name: 'orderLine', format: CustomChildListFieldFormat.instance,
                                                    type: List, referenceType: OrderLine)

    and: 'the records for the parent and child list'
    def order = null
    Order.withTransaction {
      def orders = DataGenerator.generate {
        domain Order
      }
      order = orders[0]
      DataGenerator.generate {
        domain OrderLine
        values orderId: order.id, sequence: 0, product: 'PRODUCT-$i', qty: 1.2
      }
    }

    when: 'the values are read via the format and changed in a different transaction'
    Order.withTransaction {
      def list = (List) CustomChildListFieldFormat.instance.readList(order, fieldDefinition)

      list[0].product = 'NEW PRODUCT'
      CustomChildListFieldFormat.instance.saveList(order, list, fieldDefinition)
    }

    then: 'the DB list contains the value'
    Order.withTransaction {
      def list2 = OrderLine.list()
      assert list2.size() == 1
      assert list2[0].product == 'NEW PRODUCT'
      true
    }
  }

}
