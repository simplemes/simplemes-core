package org.simplemes.mes.demand.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.product.domain.ProductOperation

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class LSNOperStateSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [LSN, LSNOperState, Order]

  def "test constraints"() {
    given: 'an LSN'
    def order = new Order(order: 'M1001')
    def lsn = new LSN()
    order.lsns << lsn

    expect: 'the constraints are enforced'
    DomainTester.test {
      domain LSNOperState
      requiredValues lsn: lsn, sequence: 1
      fieldOrderCheck false
    }
  }

  @Rollback
  def "verify that the copy constructor works"() {
    given: 'a routing operation'
    def op1 = new ProductOperation(sequence: 100, title: 'XYZ')

    when: 'the copy constructor is used'
    def opState = new LSNOperState(op1)

    then: 'the fields are copied'
    opState.sequence == op1.sequence
    opState.dateFirstQueued == null
    opState.dateFirstStarted == null
  }

  def "verify that determineNextWorkable is delegated to LSN"() {
    given: 'a mocked LSN'
    def lsn = Mock(LSN)
    def opState = new LSNOperState(lsn: lsn, sequence: 237)

    when: 'the method is called'
    def res = opState.determineNextWorkable()

    then: 'the LSN method is used'
    1 * lsn.determineNextWorkable(opState) >> lsn

    and: 'it returned the value from the LSN method'
    lsn == res
  }

  @Rollback
  def "verify that saveChanges saves this oper state"() {
    given: 'an LSN'
    def order = new Order(order: 'M1001')
    def lsn = new LSN()
    order.lsns << lsn

    when: 'the method is called'
    def opState = new LSNOperState(lsn: lsn, sequence: 237)
    assert opState.uuid == null
    opState.saveChanges()

    then: 'the record was saved'
    LSNOperState.findByUuid(opState.uuid)
  }

  @Rollback
  def "verify that toString works"() {
    given: 'an LSN'
    def lsn = new LSN(lsn: 'ABC')

    when: 'the method is called'
    def opState = new LSNOperState(lsn: lsn, sequence: 237)

    then: 'the string is valid'
    opState.toString().contains('ABC')
    opState.toString().contains('237')
  }

}
