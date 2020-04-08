package org.simplemes.mes.assy.demand.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.misc.FieldSizes
import org.simplemes.mes.product.domain.Product

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OrderAssembledComponentSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "test constraints"() {
    given: 'a product and an order'
    def product = new Product(product: 'ABC').save()
    def order = new Order(order: 'M1001').save()

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain OrderAssembledComponent
      requiredValues order: order, sequence: 10, component: product, userName: 'JOE'
      maxSize 'location', FieldSizes.MAX_CODE_LENGTH
      maxSize 'userName', FieldSizes.MAX_CODE_LENGTH
      maxSize 'removedByUserName', FieldSizes.MAX_CODE_LENGTH
      notNullCheck 'order'
      notNullCheck 'component'
      notNullCheck 'userName'
      notInFieldOrder(['order', 'bomSequence', 'assemblyData', 'workCenter', 'removedByUserName', 'removedDate'])
    }

    and: 'toString works'
    new OrderAssembledComponent().toString()
  }

}
