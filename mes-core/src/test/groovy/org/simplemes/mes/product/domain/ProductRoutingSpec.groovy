package org.simplemes.mes.product.domain

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.test.BaseSpecification

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
  static specNeeds = HIBERNATE

  @Rollback
  def "test duplicate operations fails save"() {
    given: 'a product with a routing with two duplicate operations'
    def product = new Product(product: 'ABC', title: 'description')
    def pr = new ProductRouting()
    def o1 = new RoutingOperation(sequence: 1, title: "Prep")
    def o2 = new RoutingOperation(sequence: 1, title: "Prep")
    pr.addToOperations(o1)
    pr.addToOperations(o2)
    product.productRouting = pr

    when: 'the routing is validated'
    product.validate()

    then: 'the duplicate error is triggered'
    product.errors["productRouting.operations"].codes.contains('duplicate.error')
  }

}
