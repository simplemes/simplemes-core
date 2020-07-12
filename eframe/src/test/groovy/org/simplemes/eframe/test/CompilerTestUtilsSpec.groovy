/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test
/**
 * Tests.
 */
class CompilerTestUtilsSpec extends BaseSpecification {

  def "verify that compile works on a simple class"() {
    given: 'source for a simple class'
    def src = """
    package sample
    
    class SampleClass {
      String test() {
        return "ABC"
      }
    }
    """

    when: 'the class is compiled'
    def clazz = CompilerTestUtils.compileSource(src)

    then: 'the class can be used'
    def o = clazz.newInstance()
    o.test() == "ABC"
  }

  def "verify that compile fails gracefully with compiler errors"() {
    given: 'source for a simple class'
    def src = """
    package sample
    
    class SampleClass {
      yString test() {
        return "ABC"
      }
    }
    """

    and: 'the src printing is suppressed to reduce console output'
    def original = CompilerTestUtils.printCompileFailureSource
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the class is compiled'
    CompilerTestUtils.compileSource(src)

    then: 'the class can be used'
    def ex = thrown(Exception)
    UnitTestUtils.assertContainsAllIgnoreCase(ex, ['yString'])

    cleanup:
    CompilerTestUtils.printCompileFailureSource = original
  }


}
