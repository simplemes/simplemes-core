/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.format


import org.simplemes.eframe.data.CustomFieldDefinition
import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.domain.annotation.DomainEntityInterface
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.Order
import sample.domain.OrderLine
import sample.domain.SampleParent

/**
 * Tests.
 */
class CustomCustomChildListFieldFormatSpec extends BaseSpecification {

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
      values order: order, sequence: 0, product: 'PRODUCT-$i', qty: 1.2
    }

    when: 'the values are read via the format'
    def list = (List) CustomChildListFieldFormat.instance.readList(order as DomainEntityInterface, fieldDefinition)

    then: 'the list contains the value'
    list.size() == 1
    list[0].uuid == orderLines[0].uuid
  }

  def "verify that the saveList method saves changed child records - in a proper transaction"() {
    given: 'a field definition'
    def fieldDefinition = new CustomFieldDefinition(name: 'orderLine', format: CustomChildListFieldFormat.instance,
                                                    type: List, referenceType: OrderLine)

    and: 'the records for the parent and child list'
    Order order = null
    Order.withTransaction {
      def orders = DataGenerator.generate {
        domain Order
      }
      order = orders[0] as Order
      DataGenerator.generate {
        domain OrderLine
        values order: order, sequence: 0, product: 'PRODUCT-$i', qty: 1.2
      }
    }

    when: 'the values are read via the format and changed in a different transaction'
    Order.withTransaction {
      def list = (List) CustomChildListFieldFormat.instance.readList(order as DomainEntityInterface, fieldDefinition)

      list[0].product = 'NEW PRODUCT'
      CustomChildListFieldFormat.instance.saveList(order as DomainEntityInterface, list, fieldDefinition)
    }

    then: 'the DB list contains the value'
    Order.withTransaction {
      def list2 = OrderLine.list()
      assert list2.size() == 1
      assert list2[0].product == 'NEW PRODUCT'
      true
    }
  }

  @Rollback
  def "verify that the formatCustomListForJSON method creates the proper JSON"() {
    given: 'a domain object reference and field definition'
    def sampleParent1 = new SampleParent(name: 'ABC1').save()
    def sampleParent2 = new SampleParent(name: 'ABC2').save()
    def fieldDefinition = new SimpleFieldDefinition(format: CustomChildListFieldFormat.instance, type: SampleParent)

    when: 'the values are formatted for JSON'
    def list = (List) CustomChildListFieldFormat.instance.formatCustomListForJSON([sampleParent1, sampleParent2], fieldDefinition)

    then: 'the list contains the values'
    list.size() == 2
    list[0] == sampleParent1.uuid.toString()
    list[1] == sampleParent2.uuid.toString()
  }

  @Rollback
  def "verify that the parseJSONForCustomList method creates the proper records"() {
    given: 'a domain object reference and field definition'
    def sampleParent1 = new SampleParent(name: 'ABC1').save()
    def sampleParent2 = new SampleParent(name: 'ABC2').save()
    def fieldDefinition = new SimpleFieldDefinition(format: CustomChildListFieldFormat.instance,
                                                    type: SampleParent, referenceType: SampleParent)

    when: 'the values are formatted for JSON'
    def list = (List) CustomChildListFieldFormat.instance.parseJSONForCustomList([sampleParent1.uuid.toString(), sampleParent2.uuid.toString()],
                                                                                 fieldDefinition)

    then: 'the list contains the values'
    list.size() == 2
    list[0] == sampleParent1
    list[1] == sampleParent2
  }

  def "verify that the client ID is correct - mapped to enum list format on client"() {
    expect: 'the value is correct'
    CustomChildListFieldFormat.instance.clientFormatType == EnumFieldFormat.instance.clientFormatType
  }

}
