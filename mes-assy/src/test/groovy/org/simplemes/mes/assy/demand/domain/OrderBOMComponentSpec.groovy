package org.simplemes.mes.assy.demand.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.assy.product.domain.ProductComponent
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.product.domain.Product

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OrderBOMComponentSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "test constraints"() {
    given: 'a product and an order'
    def product = new Product(product: 'ABC').save()
    def order = new Order(order: 'M1001').save()

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain OrderBOMComponent
      requiredValues order: order, component: product, sequence: 10, qty: 1.2
      notNullCheck 'order'
      notNullCheck 'component'
      notNullCheck 'sequence'
      notNullCheck 'qty'
      notInFieldOrder(['order'])
    }
  }

  @Rollback
  def "verify that ProductComponent constructor works"() {
    given: 'a ProductComponent with values'
    Product component = new Product(product: 'CPU').save() as Product
    def productComponent = new ProductComponent(component: component, sequence: 247, qty: 32.2)

    when: 'the OrderBOMComponent constructor is used'
    def orderComponent = new OrderBOMComponent(productComponent)

    then: 'the values are copied'
    orderComponent.sequence == 247
    orderComponent.component == component
    orderComponent.qty == 32.2

    and: 'the special fields are not copied'
    !orderComponent.uuid
  }
}
