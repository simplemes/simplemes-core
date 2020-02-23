package org.simplemes.mes.product.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ProductRoutingSpec extends BaseSpecification {
  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "test duplicate operations fails save"() {
    given: 'a product with a routing with two duplicate operations'
    def product = new Product(product: 'ABC', title: 'description')
    def pr = new ProductRouting()
    def o1 = new MasterOperation(sequence: 1, title: "Prep")
    def o2 = new MasterOperation(sequence: 1, title: "Prep")
    pr.addToOperations(o1)
    pr.addToOperations(o2)
    product.productRouting = pr

    when: 'the routing is validated'
    product.validate()

    then: 'the duplicate error is triggered'
    product.errors["productRouting.operations"].codes.contains('duplicate.error')
  }

}
