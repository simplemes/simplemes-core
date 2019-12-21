package org.simplemes.eframe.domain.annotation


import io.micronaut.transaction.TransactionStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import sample.domain.Order
import sample.domain.OrderRepository

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DomainEntityTransformationSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [Order]

  def "verify that the annotation adds the DomainEntityInterface for singleton use in the runtime"() {
    given:
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      
      @DomainEntity
      class TestClass {
        UUID uuid
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'an instance is created'
    def o = clazz.newInstance()

    then: 'the instance has the marker interface'
    o instanceof DomainEntityInterface
  }

  def "verify that the annotation adds the static repository holder and getter method"() {
    when: ' the class is compiled'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      
      @DomainEntity(repository=sample.domain.OrderRepository)
      class TestClass {
        UUID uuid
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    //println "clazz = ${clazz.declaredFields}"
    //println "clazz = ${clazz.declaredMethods}"

    then: 'the field has the repository '
    clazz.repository instanceof OrderRepository
  }

  def "verify that the annotation adds the save method that calls the helper save method"() {
    given: 'an object to save'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      
      @DomainEntity
      class TestClass {
        UUID uuid
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    def object = clazz.newInstance()

    and: 'a mocked helper'
    def mock = Mock(DomainEntityHelper)
    DomainEntityHelper.instance = mock

    when: 'the object is saved'
    object.save()

    then: 'the helper save method was called'
    1 * mock.save(object)
  }

  def "verify that save works"() {
    when: ' the record is saved'
    def order = new Order('M1001').save()
    order.qtyToBuild = 12.0
    order.save()

    then: 'the record is in the DB'
    def list = Order.list()
    list[0].uuid == order.uuid
    list[0].qtyToBuild == 12.0
  }

  def "verify that list works"() {
    when: ' a record is saved'
    def order = new Order('M1001').save()

    then: 'the record is in the DB'
    def list = Order.list()
    list.size() == 1
    list[0].uuid == order.uuid
  }

  def "verify that delete works"() {
    when: ' the record is saved and then deleted'
    def order = new Order('M1001').save()
    order.delete()

    then: 'the record is not the DB'
    def list = Order.list()
    list.size() == 0
  }

  def "verify that getRepository works"() {
    when: ' a record is saved'
    def order = new Order('M1001').save()

    then: 'the record is in the DB'
    def o3 = Order.repository.findByOrder('M1001').orElse(null)
    o3.uuid == order.uuid
  }

  def "verify that findByXYZ works"() {
    when: ' a record is saved'
    def order = new Order('M1001').save()

    then: 'the record is in the DB'
    def o3 = Order.findByOrder('M1001')
    o3.uuid == order.uuid
  }

  def "verify that findByXYZ handles missing record correctly"() {
    expect: 'the record is not found'
    def order = Order.findByOrder('M1001')
    !order
  }

  def "verify that findById works"() {
    when: ' a record is saved'
    def order = new Order('M1001').save()

    then: 'the record is in the DB'
    def o3 = Order.findById(order.uuid)
    o3.uuid == order.uuid
  }

  def "verify that withTransaction works"() {
    when: ' a record is saved'
    def order = null
    Order.withTransaction { status ->
      order = new Order('M1001').save()
      assert status instanceof TransactionStatus
    }

    then: 'the record is in the DB'
    def o3 = Order.findById((UUID) order.uuid)
    o3.uuid == order.uuid
  }

  def "verify that withTransaction works - Compile within the test"() {
    given:
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import sample.domain.Order
      
      @DomainEntity
      class TestClass {
        UUID uuid
        
        static aMethod() {
          TestClass.withTransaction {status ->
            new Order('M1001').save()
          }
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the method is called'
    clazz.newInstance().aMethod()

    then: 'the record is in the DB'
    Order.findByOrder('M1001') != null
  }

  def "verify that withTransaction rolls back the transaction on failure"() {
    given:
    def src = """
      import sample.domain.Order
      
      class TestClass {
        def aMethod() {
          Order.withTransaction {status ->
            new Order('M1001').save()
            throw new IllegalArgumentException('bad code')
          }
        }
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the method is called'
    clazz.newInstance().aMethod()

    then: 'the right exception is thrown'
    thrown(IllegalArgumentException)

    and: 'no records are in the DB'
    Order.findByOrder('M1001') == null
  }
}
