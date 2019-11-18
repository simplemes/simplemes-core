package org.simplemes.eframe.misc

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class PerformanceUtilsSpec extends BaseSpecification {

  def setup() {
    PerformanceUtils.lastTime = 0
  }

  def "verify that basic elapsed time works"() {
    given: 'a starting elapsed call'
    PerformanceUtils.elapsed("")

    when: 'the elapsed time is calculated'
    sleep(100)
    def s = PerformanceUtils.elapsed('PointA')

    then: 'the result is Ok'
    s.contains('PointA')
  }

  def "verify that basic elapsedPrint works"() {
    given: 'a buffer to capture print output'
    def originalOut = System.out
    def outStream = new ByteArrayOutputStream()
    System.out = new PrintStream(outStream)

    and: 'a starting elapsed call'
    PerformanceUtils.elapsedPrint()

    when: 'the elapsed time is calculated'
    sleep(100)
    PerformanceUtils.elapsedPrint('PointB')

    then: 'the output contains the information'
    outStream.toString().contains('PointB')

    cleanup:
    System.out = originalOut
  }

  def "verify that elapsed fails with no original call"() {
    given: 'clear previous call'
    PerformanceUtils.lastTime = 0

    when: 'the elapsed time is calculated'
    PerformanceUtils.elapsed('PointA')

    then: 'an exception is triggered'
    def ex = thrown(IllegalArgumentException)
    UnitTestUtils.assertExceptionIsValid(ex, ['PointA'])
  }

}
