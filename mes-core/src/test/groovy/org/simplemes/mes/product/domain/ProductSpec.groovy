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

  @Override
  void checkForLeftoverRecords() {
    println "checkForLeftoverRecords DISABLED"
  }

  @Rollback
  def "test routing update of domain object"() {
    given: 'a product with a routing'
    def p = new Product(product: 'ABC', title: 'description')
    def pr = new ProductRouting()
    def o1 = new MasterOperation(sequence: 1, title: "Orig Prep")
    def o3 = new MasterOperation(sequence: 3, title: "Orig Pack")
    pr.addToOperations(o1)
    pr.addToOperations(o3)
    p.productRouting = pr
    p.validate()
    assert !p.errors.allErrors
    assert p.save()

    when: 'the product/routing are updated'
    p = Product.get(p.id)
    assert p.product == 'ABC'
    def o2 = new MasterOperation()
    o2.sequence = 2
    o2.title = "New Test the assembly"
    pr.addToOperations(o2)
    assert p.save(flush: true)

    then: 'the records are updated'
    def product2 = Product.get(p.id)
    product2.product == 'ABC'

    def ops = product2.productRouting.operations
    ops.size() == 3
    ops[0].sequence == 1
    ops[1].sequence == 2
    ops[2].sequence == 3

    and: 'the domain finders work on the record'
    ProductRouting.findAll().size() == 1
    MasterOperation.findAllBySequence(1).size() == 1
    MasterOperation.findAllBySequence(2).size() == 1
    MasterOperation.findAllBySequence(3).size() == 1
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
      fieldOrderCheck false     // TODO: Re-enable when state is ported
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
