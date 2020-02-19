package org.simplemes.mes.numbering.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.demand.domain.OrderSequence
import org.simplemes.mes.misc.FieldSizes

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Test for basic CodeSequenceTrait logic.  Uses OrderSequence to test the super-class features.
 */
class CodeSequenceTraitSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [OrderSequence]

  @Override
  void checkForLeftoverRecords() {
    // TODO: Remove when all repos are defined.
    println "checkForLeftoverRecords DISABLED"
  }

  def "test standard constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain OrderSequence
      requiredValues sequence: 'ABC'
      maxSize 'sequence', FieldSizes.MAX_CODE_LENGTH
      maxSize 'formatString', FieldSizes.MAX_LONG_STRING_LENGTH
      notNullCheck 'sequence'
      notNullCheck 'formatString'
      fieldOrderCheck false
    }
  }

  def "basic sequence generation"() {
    given: 'a simple sequence'
    def n = new OrderSequence(currentSequence: 10, formatString: 'MPH$currentSequence')

    expect: 'the sequence to be valid'
    n.formatTest() == 'MPH10'
  }

  def "test multiple sequence generation"() {
    given: 'a simple sequence'
    def n = null
    OrderSequence.withTransaction {
      n = new OrderSequence(sequence: 'ABC', currentSequence: 10, formatString: 'MPH$currentSequence').save()
    }

    when: 'multiple sequences are generated'
    String[] numbers = null
    OrderSequence.withTransaction {
      numbers = n.formatValues(3)
    }

    then: 'the right sequences are generated'
    numbers.size() == 3
    numbers[0] == 'MPH10'
    numbers[1] == 'MPH11'
    numbers[2] == 'MPH12'

    and: 'the current sequence is correct'
    OrderSequence.withTransaction {
      def n1 = OrderSequence.findBySequence('ABC')
      n1.currentSequence == 13
    }
  }

  def "zero count passed to formatValues() should fail"() {
    given: 'a simple sequence'
    def n = new OrderSequence(currentSequence: 10, formatString: 'MPH$currentSequence')

    when: 'the format is attempted'
    n.formatValues(0) == ['MPH10']

    then: 'should fail with correct info'
    def e = thrown(IllegalArgumentException)
    UnitTestUtils.allParamsHaveValues(e)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['nValues', '>', '0'])
  }

  def "negative count passed to formatValues() should fail"() {
    given: 'a simple sequence'
    def n = new OrderSequence(currentSequence: 10, formatString: 'MPH$currentSequence')

    when: 'the format is attempted'
    n.formatValues(-23) == ['MPH10']

    then: 'should fail with correct info'
    def e = thrown(IllegalArgumentException)
    UnitTestUtils.allParamsHaveValues(e)
    UnitTestUtils.assertContainsAllIgnoreCase(e.toString(), ['nValues', '>', '0'])
  }

  def "formatting with dates"() {
    given: 'a simple sequence with a date'
    def now = new Date().format('yyMMdd')
    def n = new OrderSequence(formatString: '${date.format(\'yyMMdd\')}')

    when: 'a value is formatted'
    def s = n.formatTest()

    then: 'the current date appears'
    s == now
  }

  def "complex formatting with dates"() {
    given: 'a complex sequence with a date and an expression'
    def now = (new Date() + 1).format('yyMMdd')
    def n = new OrderSequence(formatString: '${def d=date+1;d.format(\'yyMMdd\')}')

    when: 'a value is formatted'
    def s = n.formatTest()

    then: 'the current date appears'
    s == now
  }

  @Rollback
  def "formatting with passed in parameters"() {
    given: 'a simple sequence with a date'
    def pogo = new DummyPOGO(field: 'FIELD')
    OrderSequence n = new OrderSequence(sequence: 'ABC', formatString: '${pogo.field}').save()

    when: 'a value is formatted'
    def list = n.formatValues(1, [pogo: pogo])

    then: 'the passed in value is used'
    list[0] == pogo.field
  }

  @Rollback
  def "formatting with passed in null parameter"() {
    given: 'a simple sequence with a date'
    def pogo = new DummyPOGO()
    OrderSequence n = new OrderSequence(sequence: 'ABC', formatString: 'X${pogo.field?.length() ?: 1}').save()

    when: 'a value is formatted'
    def values = n.formatValues(1, [pogo: pogo])

    then: 'the passed in value is used in a complex expression'
    values[0] == 'X1'
  }

  def "verify that formatValues works with multi-threaded access"() {
    given: 'a code sequence to be shared'
    def n = null
    OrderSequence.withTransaction {
      n = new OrderSequence(sequence: 'ABC', currentSequence: 1, formatString: 'MPH$currentSequence').save()
    }

    when: 'a lot of values are generated'
    def nThreads = 20
    def nIterations = 20
    def exceptionFound = null

    def t = []
    def logic = {
      for (int j = 0; j < nIterations; j++) {
        // Just generate a value
        try {
          OrderSequence.withTransaction {
            //println "txn = ${txn.transaction.sessionHolder.dump()}"
            def n3 = OrderSequence.findBySequence('ABC')
            n3.formatValues(1)
            //println "s = $s"
          }
        } catch (Exception e) {
          exceptionFound = e
        }
        //sleep(10)
      }
    }

    for (int i = 0; i < nThreads; i++) {
      t[i] = new Thread(logic as Runnable)
      t[i].start()
    }

    // Wait till all threads finish
    boolean stillRunning = true
    while (stillRunning) {
      stillRunning = false
      for (int i = 0; i < nThreads; i++) {
        if (t[i].isAlive()) {
          stillRunning = true
        }
        //println "  t = ${t[i].isAlive()}"
      }
      //println "stillRunning = $stillRunning"
    }

    then: 'no exception was found'
    !exceptionFound

    and: 'the right number of values were formatted and committed correctly'
    OrderSequence.withTransaction {
      def n2 = OrderSequence.findBySequence('ABC')
      assert n2.currentSequence == (nIterations * nThreads) + 1
      true
    }
  }

}

/**
 * A simple POGO to test passing objects to the NameSequence format() method.
 */
class DummyPOGO {
  String field
}

