package org.simplemes.eframe.test.annotation

import io.micronaut.transaction.SynchronousTransactionManager
import org.simplemes.eframe.application.Holders
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
        def handleWrapped(Closure closure) {
          println "closure "+closure
          closure.call()
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the method can be called'
    clazz.newInstance().aMethod()

    then: 'the record is rolled back in the database'
    Order2.findByOrder('M1001') == null
  }

  def handleWrapped(Closure closure) {
    println "closureSpec " + closure
    closure.call()
  }


  @Rollback
  def "verify that rollback works - will not leave any records in the DB"() {
    expect: 'a can be record is saved'
    def order = new Order2('M1001').save()
    println "saved order = $order"

    // When this test ends, the records will not be in the DB and the check for left over records will pass.
  }

  // TODO: Move to @Rollback Spec
  def "verify that rollback logic can work"() {
    given: ''
    def manager = Holders.applicationContext.getBean(SynchronousTransactionManager)
    println "manager = $manager"


    when: ' a record is saved'
    manager.executeWrite { status ->
      println "status = $status"
      def order = new Order2('M1002').save()
      println "order = $order"
      status.setRollbackOnly()
    }

    then: 'the record is in the DB'
    Order2.findByOrder('M1002') == null
    println "find = ${Order2.findByOrder('M1002')}"
  }


}
