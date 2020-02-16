/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.gui

import org.openqa.selenium.Keys
import sample.domain.CustomOrderComponent
import sample.domain.Order
import sample.page.OrderCreatePage
import sample.page.OrderEditPage
import sample.page.OrderShowPage
import spock.lang.IgnoreIf

/**
 * End to End tests of custom child list field added via an addition.
 */
@IgnoreIf({ !sys['geb.env'] })
class CustomChildListE2ESpec extends BaseDefinitionEditorSpecification {


  @SuppressWarnings("unused")
  static dirtyDomains = [Order, CustomOrderComponent]


  def "verify that the custom child list can be created in a definition GUI"() {
    when: 'the create page is shown'
    login()
    to OrderCreatePage

    then: 'the addition field is correct'
    customComponents.label == 'Components'

    when: 'a rows is added'
    customComponents.addRowButton.click()

    and: 'the sequence field is filled in'
    sendKey('11')
    sendKey(Keys.TAB)

    and: 'the product field is filled in'
    sendKey('PROD1')
    sendKey(Keys.TAB)

    and: 'the record is saved'
    order.input.value('M1002')
    createButton.click()
    waitForNonZeroRecordCount(Order)

    then: 'the record is updated'
    Order.withTransaction {
      def order2 = Order.findByOrder('M1002')
      List<CustomOrderComponent> customComponents = order2.getFieldValue('customComponents') as List<CustomOrderComponent>
      assert customComponents.size() == 1
      assert customComponents[0].product == 'PROD1'
      assert customComponents[0].sequence == 11
      true
    }
  }

  def "verify that the custom child list can be edited in a definition GUI"() {
    given: 'a domain record is available to edit'
    def order = null
    Order.withTransaction {
      order = new Order(order: 'M1001')
      def customComponents = []
      customComponents << new CustomOrderComponent(sequence: 1, product: 'PROD1')
      customComponents << new CustomOrderComponent(sequence: 2, product: 'PROD2')
      customComponents << new CustomOrderComponent(sequence: 3, product: 'PROD3')
      order.setFieldValue('customComponents', customComponents)
      order.save()
    }

    when: 'the edit page is shown'
    login()
    to OrderEditPage, order

    and: 'a rows is changed'
    customComponents.cell(0, 0).click()
    sendKey('9')
    sendKey(Keys.TAB)

    and: 'the record is saved'
    updateButton.click()
    waitForRecordChange(order)

    then: 'the value is shown'
    at OrderShowPage

    and: 'the record is updated'
    Order.withTransaction {
      def order2 = Order.findByOrder('M1001')
      List<CustomOrderComponent> customComponents = order2.getFieldValue('customComponents') as List<CustomOrderComponent>
      assert customComponents.size() == 3
      assert customComponents[0].product == 'PROD1'
      assert customComponents[0].sequence == 9
      true
    }
  }


}
