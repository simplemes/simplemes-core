package org.simplemes.mes.product.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.test.MESUnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ProductSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "test routing update of domain object"() {
    given: 'a product with a routing'
    def product = new Product(product: 'ABC', title: 'description')
    product.operations << new ProductOperation(sequence: 1, title: "Orig Prep")
    product.operations << new ProductOperation(sequence: 3, title: "Orig Pack")
    product.save()

    when: 'the product/routing are updated'
    product = Product.findByUuid(product.uuid)
    product.operations << new ProductOperation(sequence: 2, title: "New Test the assembly")
    product.save()

    then: 'the records are updated'
    def product2 = Product.findByUuid(product.uuid)
    product2.product == 'ABC'

    def ops = product2.operations
    product2.sortOperations()
    ops.size() == 3
    ops[0].sequence == 1
    ops[1].sequence == 2
    ops[2].sequence == 3

    and: 'the domain finders work on the record'
    def list = ProductOperation.list()
    list.size() == 3
    list.find { it.sequence == 1 }
    list.find { it.sequence == 2 }
    list.find { it.sequence == 3 }
  }

  @Rollback
  def "test constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain Product
      requiredValues product: 'ABC'
      maxSize 'product', FieldSizes.MAX_PRODUCT_LENGTH
      maxSize 'title', FieldSizes.MAX_TITLE_LENGTH
      notNullCheck 'product'
      //notInFieldOrder (['operationStates', 'dateReleased', 'dateFirstQueued', 'dateFirstStarted', 'dateQtyQueued', 'dateQtyStarted'])
    }
  }

  @Rollback
  def "determineEffectiveRouting with a built-in product routing"() {
    given: 'a product with a routing'
    def product = new Product(product: 'ABC', title: 'description')
    product.operations << new ProductOperation(sequence: 1, title: "Prep")
    product.operations << new ProductOperation(sequence: 2, title: "Load")
    product.save()

    when: 'the record is read'
    def product2 = Product.findByUuid(product.uuid)

    then: 'product operations are retrieved'
    product2.operations.size() == 2

    and: 'the records are in the DB'
    ProductOperation.list().size() == 2

    and: 'the determineEffectiveRouting method works'
    product.determineEffectiveRouting() == product
  }

  @Rollback
  def "determineEffectiveRouting with a master routing"() {
    given: 'a product with a master routing'
    def product = new Product(product: 'ABC', title: 'description')
    def routing = MESUnitTestUtils.buildMasterRouting(operations: [2, 4, 3, 5, 1])
    product.masterRouting = routing
    product.save()

    expect: 'the right routing is found'
    product.determineEffectiveRouting() == routing
  }
}
