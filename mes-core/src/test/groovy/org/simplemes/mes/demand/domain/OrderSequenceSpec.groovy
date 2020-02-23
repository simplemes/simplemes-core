package org.simplemes.mes.demand.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OrderSequenceSpec extends BaseSpecification {
  @SuppressWarnings("unused")
  static dirtyDomains = [OrderSequence]

  @Rollback
  def "test initial data load"() {
    when: 'the initial data is loaded'
    OrderSequence.initialDataLoad()

    then: 'should have 1 record'
    List<OrderSequence> l = OrderSequence.list()
    l.size() == 1

    l[0].sequence == 'ORDER'
    l[0].title == 'Order Sequence'
    l[0].defaultSequence

    and: 'it is the default'
    OrderSequence seq = OrderSequence.findDefaultSequence()
    seq.sequence == 'ORDER'
  }

  @Rollback
  def "test multiple sequence generation"() {
    given: 'a simple sequence'
    def orderSequence = new OrderSequence(sequence: 'ABC', formatString: 'SX$currentSequence').save()

    when: 'multiple sequences are generated'
    String[] numbers = orderSequence.formatValues(3)

    then: 'the right sequences are generated'
    numbers.size() == 3
    numbers[0] == 'SX1'
    numbers[1] == 'SX2'
    numbers[2] == 'SX3'
  }

}
