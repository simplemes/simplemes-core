package org.simplemes.mes.demand.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.product.domain.ProductRouting
import org.simplemes.mes.product.domain.RoutingOperation

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OrderRoutingSpec extends BaseSpecification {


  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "test copy constructor"() {
    given: 'a product with some routing operations'
    def product = new Product(product: 'ABC', title: 'description')
    def pr = new ProductRouting()
    pr.addToOperations(new RoutingOperation(sequence: 1, title: "Prep"))
    pr.addToOperations(new RoutingOperation(sequence: 2, title: "Test"))
    pr.addToOperations(new RoutingOperation(sequence: 3, title: "Pack"))
    product.productRouting = pr
    product.save()

    when: 'a new OrderRouting is created from the product routing'
    def orderRouting = new OrderRouting(pr)

    then: 'the operations are copied'
    def operations = orderRouting.operations
    operations.size() == 3
    operations[0].sequence == 1
    operations[0].title == "Prep"
    operations[1].sequence == 2
    operations[1].title == "Test"
    operations[2].sequence == 3
    operations[2].title == "Pack"
  }

/*
  def "test copy constructor with custom fields"() {
    given: 'custom fields on the product and order routing objects'
    new FieldExtension(domainClassName: ProductRouting.name, fieldName: 'color').save()
    new FieldExtension(domainClassName: OrderRouting.name, fieldName: 'color').save()
    FieldExtensionHelper.setupFieldExtensionsInDomains(????.application)

    and: 'a product with some routing operations'
    def product = new Product(product: 'ABC', title: 'description')
    def pr = new ProductRouting()
    pr.addToOperations(new RoutingOperation(sequence: 1, title: "Prep"))
    pr.color = 'yellow'
    product.productRouting = pr
    product.save()

    when: 'a new OrderRouting is created from the product routing'
    def orderRouting = new OrderRouting(pr)

    then: 'the custom field is copied'
    orderRouting.color == 'yellow'
  }
*/
}
