package org.simplemes.mes.assy.product.domain

import org.openqa.selenium.Keys
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.test.BaseGUISpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.mes.assy.product.page.ProductEditPage
import org.simplemes.mes.assy.product.page.ProductShowPage
import org.simplemes.mes.product.domain.Product
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests for assembly elements added to the Product domain/GUI.
 */
@IgnoreIf({ !sys['geb.env'] })
class ProductAssemblyAdditionGUISpec extends BaseGUISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [Product, FlexType]

  def "verify that the product components can be displayed and changed - edit mode"() {
    given: 'a product with components'
    def product = null
    Product.withTransaction {
      def comp1 = new Product(product: 'COMP1').save() as Product
      def comp2 = new Product(product: 'COMP2').save() as Product
      new Product(product: 'COMP3').save()
      product = new Product(product: 'ABC')
      def components = [new ProductComponent(component: comp1, sequence: 10, qty: 1.2),
                        new ProductComponent(component: comp2, sequence: 20, qty: 2.2)]
      product.setFieldValue('components', components)
      product.save()
    }

    when: 'the edit page is displayed'
    login()
    to ProductEditPage, product

    and: 'the components panel is displayed'
    componentsPanel.click()

    then: 'the values in the grid are correct'
    components.cell(0, getColumnIndex(ProductComponent, 'sequence')).text() == '10'
    components.cell(0, getColumnIndex(ProductComponent, 'component')).text() == 'COMP1'
    components.cell(0, getColumnIndex(ProductComponent, 'qty')).text().contains(NumberUtils.formatNumber(1.2))

    when: 'a component is added'
    components.addRowButton.click()

    and: 'values are entered'
    sendKey('99')   // Sequence field
    sendKey(Keys.TAB)

    sendKey('COMP3')   // Component field
    sendKey(Keys.TAB)

    sendKey(NumberUtils.formatNumber(4.2))   // Qty field
    sendKey(Keys.TAB)

    and: 'the record is saved'
    updateButton.click()
    waitForRecordChange(product)

    then: 'the value is shown'
    at ProductShowPage

    and: 'the records are updated'
    def record = Product.findByUuid(product.uuid)
    def list = record.getFieldValue('components') as List<ProductComponent>
    list.size() == 3
    def comp = list.find { it.sequence == 99 }
    comp.component.product == 'COMP3'
    comp.qty == 4.2
  }

  def "verify that the product components can be deleted - edit mode"() {
    given: 'a product with components'
    def product = null
    Product.withTransaction {
      def comp1 = new Product(product: 'COMP1').save() as Product
      def comp2 = new Product(product: 'COMP2').save() as Product
      product = new Product(product: 'ABC')
      def components = [new ProductComponent(component: comp1, sequence: 10, qty: 1.2),
                        new ProductComponent(component: comp2, sequence: 20, qty: 2.2)]
      product.setFieldValue('components', components)
      product.save()
    }

    when: 'the edit page is displayed'
    login()
    to ProductEditPage, product

    and: 'the components panel is displayed'
    componentsPanel.click()

    and: 'a row is removed'
    components.cell(1, 0).click()
    components.removeRowButton.click()

    and: 'the record is saved'
    updateButton.click()
    waitForRecordChange(product)

    then: 'the value is shown'
    at ProductShowPage

    and: 'the records are updated'
    def record = Product.findByUuid(product.uuid)
    def list = record.getFieldValue('components') as List<ProductComponent>
    list.size() == 1
    list[0].component.product == 'COMP1'
  }

  def "verify that the assembly data type can be displayed and changed - edit mode"() {
    given: 'a product with an assembly data type'
    def product = null
    def flexType1 = DataGenerator.buildFlexType(flexType: 'ABC')
    def flexType2 = DataGenerator.buildFlexType(flexType: 'XYZ')
    Product.withTransaction {
      product = new Product(product: 'ABC')
      product.setFieldValue('assemblyDataType', flexType1)
      product.save()
    }

    when: 'the edit page is displayed'
    login()
    to ProductEditPage, product

    then: 'the value in the custom field is correct'
    assemblyDataType.input.value() == TypeUtils.toShortString(flexType1, true)

    when: 'the drop-down is changed'
    setCombobox(assemblyDataType, flexType2.uuid.toString())

    and: 'the record is saved'
    updateButton.click()
    waitForRecordChange(product)

    then: 'the value is shown'
    at ProductShowPage

    and: 'the records are updated'
    def record = Product.findByUuid(product.uuid)
    def flexType = record.getFieldValue('assemblyDataType') as FlexType
    flexType == flexType2
  }
}
