package org.simplemes.eframe.test.annotation


import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import sample.domain.Order2

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
      import sample.domain.Order2

      class TestClass {
        @Rollback
        def aMethod() { 
          def order = new Order2('M1001').save()
          return order
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the method can be called'
    clazz.newInstance().aMethod()

    then: 'the record is rolled back in the database'
    Order2.findByOrder('M1001') == null
  }

  @Rollback
  def "verify that rollback works - will not leave any records in the DB"() {
    when: 'a record is saved'
    new Order2('M1001').save()

    then: 'it can be read within the method'
    Order2.findByOrder('M1001') != null

    // When this test ends, the records will not be in the DB and the check for left over records will pass.
  }

  def "verify that rollback works when a runtime exception is thrown"() {
    given: 'a class to compile with a method marked for rollback'
    def src = """
      import org.simplemes.eframe.test.annotation.Rollback
      import sample.domain.Order2

      class TestClass {
        @Rollback
        def aMethod() { 
          new Order2('M1001').save()
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
    Order2.findByOrder('M1001') == null
  }

  def "verify that rollback works when a non-runtime exception is thrown"() {
    given: 'a class to compile with a method marked for rollback'
    def src = """
      import org.simplemes.eframe.test.annotation.Rollback
      import sample.domain.Order2
      import java.util.concurrent.TimeoutException

      class TestClass {
        @Rollback
        def aMethod() throws TimeoutException { 
          new Order2('M1001').save()
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
    Order2.findByOrder('M1001') == null
  }

}
