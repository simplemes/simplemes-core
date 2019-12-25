package org.simplemes.eframe.domain.annotation


import io.micronaut.transaction.TransactionStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.Order
import sample.domain.OrderLine
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

    and: 'the transformation can be instantiated'
    new DomainEntityTransformation()
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

  @Rollback
  def "verify that lazy load getter for child record is added to the domain"() {
    given: 'a mocked domainEntityHelper for the lazy method call'
    def domainEntityHelper = Mock(DomainEntityHelper)
    DomainEntityHelper.instance = domainEntityHelper

    and: 'a compiled domain class'
    def src = """
      import sample.domain.OrderLine
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import io.micronaut.data.annotation.MappedEntity
      import io.micronaut.data.annotation.MappedProperty
      import groovy.transform.EqualsAndHashCode
      import javax.persistence.CascadeType
      import javax.persistence.Column
      import javax.persistence.OneToMany
      
      @DomainEntity
      @MappedEntity()
      @EqualsAndHashCode(includes = ['uuid'])
      class Order2 {
        UUID uuid
        
        @OneToMany(cascade= CascadeType.ALL, mappedBy="order")
        List<OrderLine> orderLines
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)


    when: 'the method is available'
    def instance = clazz.newInstance()
    instance.uuid = UUID.randomUUID()
    def orderLines = instance.getOrderLines()

    then: 'the right exception is thrown'
    orderLines.size() == 1
    orderLines == ["ABC"]

    and: 'the method is called correctly'
    1 * domainEntityHelper.lazyChildLoad(_, 'orderLines', 'order', OrderLine) >> { args ->
      //println "args = $args";
      // For some reason, Spock does not match the instance variable in the DSL above.  Instead, we need to do this manually.
      //noinspection GroovyAssignabilityCheck
      assert instance.is(args[0])
      return ["ABC"]
    }


    cleanup:
    DomainEntityHelper.instance = new DomainEntityHelper()

  }

  @Rollback
  def "verify that lazy load of child records works"() {
    given: 'a domain record with children'
    def order = new Order(order: 'M1001')
    order.save()
    new OrderLine(order: order, product: 'BIKE', sequence: 1).save()
    new OrderLine(order: order, qty: 2.0, product: 'WHEEL', sequence: 2).save()

    when: 'the get and join method is called to read the value'
    //def order2 = Order.get(order.uuid)
    def order2 = Order.findByUuid(order.uuid)

    then: 'the orderLines are populated'
    order2.orderLines.size() == 2
  }

  def "verify that OneToMany mappings require a parameterized type on the child list"() {
    given: 'a class with an error'
    def src = """
      package dummy.pack
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import javax.persistence.OneToMany

      @DomainEntity    
      class TestClass {
        UUID uuid
        
        @OneToMany(mappedBy = "order")
        List orderLines
      }
    """

    and: 'no need to print the failing source to the console'
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the domain is compiled'
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['Child', 'List', 'parameterized', 'type', 'dummy.pack.TestClass'])

    cleanup: 'reset the the console printing'
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that an existing repository getter method will fails compilation failure"() {
    given: 'a class with an error'
    def src = """
      package dummy.pack
      import org.simplemes.eframe.domain.annotation.DomainEntity

      @DomainEntity    
      class TestClass {
        def getRepository() {
        }
      }
    """

    and: 'no need to print the failing source to the console'
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the domain is compiled'
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['getRepository', 'exists', 'dummy.pack.TestClass'])

    cleanup: 'reset the the console printing'
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that an existing delegated method will fails compilation failure"() {
    given: 'a class with an error'
    def src = """
      package dummy.pack
      import org.simplemes.eframe.domain.annotation.DomainEntity

      @DomainEntity    
      class TestClass {
        def save() {
        }
      }
    """

    and: 'no need to print the failing source to the console'
    CompilerTestUtils.printCompileFailureSource = false

    when: 'the domain is compiled'
    CompilerTestUtils.compileSource(src)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['save', 'exists', 'dummy.pack.TestClass'])

    cleanup: 'reset the the console printing'
    CompilerTestUtils.printCompileFailureSource = true
  }

  def "verify that a holder for domain settings is added as a field - not a property"() {
    given: 'a class with the annotation'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import groovy.transform.ToString
      
      @DomainEntity
      @ToString(includeNames = true)
      class TestClass {
        UUID uuid
      }
    """

    when: 'the domain is compiled and a new value is used'
    def object = CompilerTestUtils.compileSource(src).newInstance()

    then: 'the holder is in the object'
    object[DomainEntityHelper.DOMAIN_SETTINGS_FIELD_NAME] instanceof Map

    and: 'there is no getter or setter for the object'
    def methods = object.getClass().getDeclaredMethods()
    !methods.find { it.name == "get$DomainEntityHelper.DOMAIN_SETTINGS_FIELD_NAME" }
    !methods.find { it.name == "set$DomainEntityHelper.DOMAIN_SETTINGS_FIELD_NAME" }
  }

}
