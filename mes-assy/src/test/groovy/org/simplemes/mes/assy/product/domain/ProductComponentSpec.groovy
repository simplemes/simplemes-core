package org.simplemes.mes.assy.product.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.product.domain.Product

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ProductComponentSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "test constraints"() {
    given: 'a product'
    def product = new Product(product: 'ABC').save()

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain ProductComponent
      requiredValues product: product, sequence: 10
      notNullCheck 'product'
      notInFieldOrder(['product'])
    }
  }

  @Rollback
  def "verify that toShortString works"() {
    given: 'a product'
    def product = new Product(product: 'ABC', title: 'xyz').save() as Product

    expect: 'the method works'
    def productComponent = new ProductComponent(component: product, sequence: 237)
    productComponent.toShortString().contains('ABC')
    productComponent.toShortString().contains('xyz')
    productComponent.toShortString().contains('237')
  }

  @Rollback
  def "verify that toLocalizedString works"() {
    given: 'a product'
    def product = new Product(product: 'ABC', title: 'xyz').save() as Product

    expect: 'the method works'
    def productComponent = new ProductComponent(component: product, sequence: 237)
    productComponent.toStringLocalized() == productComponent.toShortString()
  }


}
