package org.simplemes.eframe.custom.gui


import sample.domain.Order
import sample.page.OrderCreatePage
import sample.page.OrderEditPage
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * End to End tests of addition features. Tests definition GUIs.
 */
@IgnoreIf({ !sys['geb.env'] })
class AdditionE2ESpec extends BaseDefinitionEditorSpecification {


  @SuppressWarnings("unused")
  static dirtyDomains = [Order]

  def "verify that the edit page can update a custom field value"() {
    given: 'a domain record is available to edit'
    def order = null
    Order.withTransaction {
      order = new Order(order: 'M1001')
      order.setFieldValue('priority', 437)
      order.save()
    }

    when: 'the edit page is shown'
    login()
    to OrderEditPage, order

    then: 'the addition field is correct'
    priority.label == '*Delivery Priority'
    priority.input.value() == '437'

    when: 'the addition field value is changed'
    priority.input.value('237')

    and: 'the record is saved'
    updateButton.click()
    waitForRecordChange(order)

    then: 'the record is updated'
    Order.withTransaction {
      def order2 = Order.findByOrder('M1001')
      assert order2.getFieldValue('priority') == 237
      true
    }
  }

  def "verify that the create page can set an addition field value"() {
    when: 'the create page is shown'
    login()
    to OrderCreatePage

    then: 'the addition field is correct'
    priority.label == '*Delivery Priority'

    when: 'the addition field value is changed'
    priority.input.value('237')

    and: 'the record is saved'
    order.input.value('M1002')
    createButton.click()
    waitForNonZeroRecordCount(Order)

    then: 'the record is updated'
    Order.withTransaction {
      def order2 = Order.findByOrder('M1002')
      assert order2.getFieldValue('priority') == 237
      true
    }
  }


}
