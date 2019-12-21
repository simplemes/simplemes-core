package org.simplemes.eframe.test.annotation

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import sample.domain.Order
import spock.util.EmbeddedSpecRunner

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class RollbackTransformationSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that the compiler will trigger the rollback logic"() {
    given: 'a class to compile with a method marked for rollback'
    def src = """
      import org.simplemes.eframe.test.annotation.Rollback
      import sample.domain.Order

      class TestClass {
        @Rollback
        def aMethod() { 
          def order = new Order('M1001').save()
          return order
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the method can be called'
    clazz.newInstance().aMethod()

    then: 'the record is rolled back in the database'
    Order.findByOrder('M1001') == null
  }

  @Rollback
  def "verify that rollback works - will not leave any records in the DB"() {
    when: 'a record is saved'
    new Order('M1001').save()

    then: 'it can be read within the method'
    Order.findByOrder('M1001') != null

    // When this test ends, the records will not be in the DB and the check for left over records will pass.
  }

  def "verify that rollback can compile a spock test that does not use a where clause"() {
    given: 'a class to compile with a method marked for rollback'
    def src = """
      import spock.lang.Specification
      import org.simplemes.eframe.test.annotation.Rollback
      import sample.domain.Order

      class TestSpec extends Specification {
        @Rollback
        def "verify that rollback use with where clause is detected"() {
          when: 'an action is performed'
          def a = 'ABC'
      
          then: 'it passes'
          a == 'ABC'
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the method can be called'
    EmbeddedSpecRunner runner = new EmbeddedSpecRunner()
    runner.runClass(clazz)

    then: 'the exception is thrown'
    notThrown(Exception)
  }

  def "verify that rollback fails when used with a spock test that uses a where clause"() {
    given: 'a class to compile with a method marked for rollback'
    CompilerTestUtils.printCompileFailureSource = false
    def src = """
      import spock.lang.Specification
      import org.simplemes.eframe.test.annotation.Rollback
      import sample.domain.Order

      class TestSpec extends Specification {
        @Rollback
        def "verify that rollback use with where clause is detected"() {
          when: 'a record is saved'
          new Order(ordr).save()
      
          then: 'it can be read within the method'
          Order.findByOrder(ordr) != null
      
          // When this test ends, the records will not be in the DB and the check for left over records will pass.
      
          where:
          ordr  | _
          "ABC" | _
        }
      }
    """

    expect: 'the compile fails'
    try {
      CompilerTestUtils.compileSource(src)
    } catch (Exception ex) {
      assert ex.toString().contains("verify that rollback use with where clause is detected")
      true
    }

    cleanup:
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that rollback works when a runtime exception is thrown"() {
    given: 'a class to compile with a method marked for rollback'
    def src = """
      import org.simplemes.eframe.test.annotation.Rollback
      import sample.domain.Order

      class TestClass {
        @Rollback
        def aMethod() { 
          new Order('M1001').save()
          throw new IllegalArgumentException('bad code')
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the method can be called'
    clazz.newInstance().aMethod()

    then: 'the exception is thrown'
    def ex = thrown(IllegalArgumentException)
    ex.toString().contains('bad')

    then: 'the record is rolled back in the database'
    Order.findByOrder('M1001') == null
  }

  def "verify that rollback works when a non-runtime exception is thrown"() {
    given: 'a class to compile with a method marked for rollback'
    def src = """
      import org.simplemes.eframe.test.annotation.Rollback
      import sample.domain.Order
      import java.util.concurrent.TimeoutException

      class TestClass {
        @Rollback
        def aMethod() throws TimeoutException { 
          new Order('M1001').save()
          throw new TimeoutException('bad code')
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the method can be called'
    clazz.newInstance().aMethod()

    then: 'the exception is thrown'
    thrown(Exception)    // Really ends up being an UndeclaredThrowableException, but that works for this test too.

    then: 'the record is rolled back in the database'
    Order.findByOrder('M1001') == null
  }

}
