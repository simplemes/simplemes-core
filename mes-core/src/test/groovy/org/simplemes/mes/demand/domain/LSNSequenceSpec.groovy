package org.simplemes.mes.demand.domain

import grails.gorm.transactions.Rollback
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class LSNSequenceSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [LSNSequence]

  @Rollback
  def "test initial data load"() {
    when: 'the initial data is loaded'
    LSNSequence.initialDataLoad()

    then: 'should have 1 record'
    List<LSNSequence> l = LSNSequence.list()
    l.size() == 1

    l[0].sequence == 'SERIAL'
    l[0].title == 'Serial Number'
    l[0].defaultSequence

    and: 'it is the default'
    LSNSequence seq = LSNSequence.findDefaultSequence()
    seq.sequence == 'SERIAL'
  }

  @Rollback
  def "test multiple sequence generation"() {
    given: 'a simple sequence'
    def lsnSequence = new LSNSequence(sequence: 'ABC', formatString: 'SX$currentSequence').save()

    when: 'multiple sequences are generated'
    String[] numbers = lsnSequence.formatValues(3)

    then: 'the right sequences are generated'
    numbers.size() == 3
    numbers[0] == 'SX1'
    numbers[1] == 'SX2'
    numbers[2] == 'SX3'
  }

  @Rollback
  def "verify that toShortString works for the records"() {
    given: 'a simple sequence'
    def lsnSequence = new LSNSequence(sequence: 'ABC', title: 'abc', formatString: 'SX$currentSequence').save()

    expect: 'the string is correct'
    TypeUtils.toShortString(lsnSequence, true) == "ABC (abc)"
  }


}
