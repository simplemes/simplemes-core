/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain.annotation


import io.micronaut.transaction.SynchronousTransactionManager
import io.micronaut.transaction.jdbc.DataSourceUtils
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.exception.MessageBasedException
import org.simplemes.eframe.exception.SimplifiedSQLException
import org.simplemes.eframe.security.domain.Role
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.AllFieldsDomain
import sample.domain.CustomOrderComponent
import sample.domain.Order
import sample.domain.OrderLine
import sample.domain.OrderRepository
import sample.domain.SampleChild
import sample.domain.SampleGrandChild
import sample.domain.SampleParent

import javax.sql.DataSource
import java.lang.reflect.Field
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Tests.
 */
class DomainEntityHelperSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [Order, OrderLine, SampleParent]

  def "verify that getPreparedStatement detects use outside of txn and fails"() {
    given: 'make sure the checkForTransactionStatic method in the repository operations object is initialized'
    SampleParent.withTransaction {
      new SampleParent(name: 'ABC').save()
    }

    when: 'the method is called'
    DomainEntityHelper.instance.getPreparedStatement("SELECT * from usr")

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['active', 'transaction'])
  }

  def "verify that lazyRefListLoad creates a read-only txn on all loads"() {
    given: 'a domain record with child records that need to be loaded using a preparedStatement'
    SampleParent.withTransaction {
      new SampleParent(name: 'ABC').save()
    }

    when: 'a read of the child records is attempted outside of a txn'
    def sampleParent = SampleParent.findByName('ABC')
    sampleParent.getAllFieldsDomains()

    then: 'no exception is thrown'
    notThrown(Exception)
  }

  def "verify that getPreparedStatement works inside txn and does not fail"() {
    given: 'a domain record with child records that need to be loaded using a preparedStatement'
    SampleParent.withTransaction {
      new SampleParent(name: 'ABC').save()
    }

    when: 'a read of the child records is attempted inside of a txn'
    SampleParent.withTransaction {
      def sampleParent = SampleParent.findByName('ABC')
      sampleParent.getAllFieldsDomains()
    }

    then: 'no exception is thrown'
    notThrown(Exception)
  }

  def "verify that getRepository works for simple case"() {
    expect: 'the repository is found'
    DomainEntityHelper.instance.getRepository(Order) instanceof OrderRepository
  }

  def "verify that the annotation adds the static repository holder"() {
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

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that save will create a record"() {
    when: ' the record is saved'
    def order = new Order('M1001')
    DomainEntityHelper.instance.save((DomainEntityInterface) order)

    then: 'the record is in the DB'
    def list = (List<Order>) DomainEntityHelper.instance.list(Order)
    list[0].uuid == order.uuid
  }

  @Rollback
  def "verify that delete will delete a record"() {
    when: ' the record is saved'
    def order = new Order('M1001')
    DomainEntityHelper.instance.save((DomainEntityInterface) order)

    and: 'the record is deleted'
    DomainEntityHelper.instance.delete((DomainEntityInterface) order)

    then: 'the record is not in the DB'
    Order.list().size() == 0
  }

  def "verify that getTransactionManager works"() {
    expect: 'the method works'
    DomainEntityHelper.instance.getTransactionManager() instanceof SynchronousTransactionManager
  }

  def "verify that determineRepository works"() {
    expect: 'the method works'
    DomainEntityHelper.instance.determineRepository(Order) instanceof OrderRepository
  }

  def "verify that executeWrite works - commit"() {
    when: 'the method works'
    DomainEntityHelper.instance.executeWrite { status ->
      new Order('M1001').save()
    }

    then: 'there are no records in the DB'
    Order.list().size() == 1
  }

  def "verify that executeWrite works - rollback"() {
    when: 'the method works'
    DomainEntityHelper.instance.executeWrite { status ->
      new Order('M1001').save()
      status.setRollbackOnly()
    }

    then: 'there are no records in the DB'
    Order.list().size() == 0
  }

  @Rollback
  def "verify that lazyChildLoad works"() {
    given: 'a domain record with children'
    def order = new Order(order: 'M1001')
    def orderLine1 = new OrderLine(order: order, product: 'BIKE', sequence: 1)
    def orderLine2 = new OrderLine(order: order, qty: 2.0, product: 'WHEEL', sequence: 2)
    order.orderLines = [orderLine1, orderLine2]
    order.save()

    when: 'the lazy load is used - on an empty parent domain object'
    def order2 = Order.findByOrder('M1001')
    def list = (List<OrderLine>) DomainEntityHelper.instance.lazyChildLoad((DomainEntityInterface) order2, 'orderLines', 'order', OrderLine)

    then: 'the record is in the DB'
    list.size() == 2
    list[0].uuid == orderLine1.uuid
    list[1].uuid == orderLine2.uuid

    and: 'the field has the list - the list is the same on later calls'
    DomainEntityHelper.instance.lazyChildLoad((DomainEntityInterface) order2, 'orderLines', 'order', OrderLine).is(list)

    when: 'the loaded is called again'
    DomainEntityHelper.instance.lastLazyChildParentLoaded = null
    DomainEntityHelper.instance.lazyChildLoad((DomainEntityInterface) order2, 'orderLines', 'order', OrderLine)

    then: 'the value is not re-read from the DB'
    DomainEntityHelper.instance.lastLazyChildParentLoaded == null
  }

  @Rollback
  def "verify that lazyChildLoad is called after the find completes"() {
    given: 'a domain record with children'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(order: order, product: 'BIKE', sequence: 1)
    order.orderLines << new OrderLine(order: order, qty: 2.0, product: 'WHEEL', sequence: 2)
    order.save()
    assert OrderLine.list().size() == 2

    when: 'the record is re-read'
    def order2 = Order.findByUuid(order.uuid)

    then: 'the list is not in memory yet'
    // Use direct access to the field to avoid triggering the lazy loader
    Field field = Order.getDeclaredField('orderLines')
    field.setAccessible(true)  // Ignore the private state
    def lines = field.get(order2)
    lines == null

    when: 'the lazy loader is triggered by the get'
    order2.orderLines.size() == 2

    then: 'the lazy loader was triggered and works'
    def lines2 = field.get(order2)
    lines2.size() == 2
  }

  @Rollback
  def "verify that lazyChildLoad sorts the list after it loads it"() {
    given: 'a domain record with children'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(order: order, product: 'BIKE', sequence: 5)
    order.orderLines << new OrderLine(order: order, product: 'BIKE', sequence: 1)
    order.orderLines << new OrderLine(order: order, product: 'BIKE', sequence: 4)
    order.orderLines << new OrderLine(order: order, product: 'BIKE', sequence: 2)
    order.orderLines << new OrderLine(order: order, product: 'BIKE', sequence: 3)
    order.save()

    when: 'the list is loaded'
    def order2 = Order.findByUuid(order.uuid)

    then: 'the list is sorted'
    order2.orderLines[0].sequence == 1
    order2.orderLines[1].sequence == 2
    order2.orderLines[2].sequence == 3
    order2.orderLines[3].sequence == 4
    order2.orderLines[4].sequence == 5
  }

  @Rollback
  def "verify that saveChildList sorts the list before saving it"() {
    when: 'list is saved'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(order: order, product: 'BIKE', sequence: 5)
    order.orderLines << new OrderLine(order: order, product: 'BIKE', sequence: 1)
    order.orderLines << new OrderLine(order: order, product: 'BIKE', sequence: 4)
    order.orderLines << new OrderLine(order: order, product: 'BIKE', sequence: 2)
    order.orderLines << new OrderLine(order: order, product: 'BIKE', sequence: 3)
    order.save()

    then: 'the list is sorted'
    order.orderLines[0].sequence == 1
    order.orderLines[1].sequence == 2
    order.orderLines[2].sequence == 3
    order.orderLines[3].sequence == 4
    order.orderLines[4].sequence == 5
  }

  @Rollback
  def "verify that lazyChildLoad keeps a list of the record loaded for later update checks"() {
    given: 'a domain record with children'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(order: order, product: 'BIKE', sequence: 1)
    order.orderLines << new OrderLine(order: order, qty: 2.0, product: 'WHEEL', sequence: 2)
    order.save()

    when: 'the lazy load is used on a fresh read'
    def order2 = Order.findByUuid(order.uuid)
    DomainEntityHelper.instance.lazyChildLoad((DomainEntityInterface) order2, 'orderLines', 'order', OrderLine)

    then: 'the list of loaded children is in the domain settings holder'
    def holder = order2[DomainEntityHelper.DOMAIN_SETTINGS_FIELD_NAME]
    List loadedList = holder["${DomainEntityHelper.SETTINGS_LOADED_CHILDREN_PREFIX}orderLines"] as List
    loadedList.size() == 2
    loadedList.contains(order.orderLines[0].uuid)
    loadedList.contains(order.orderLines[1].uuid)
  }

  @Rollback
  def "verify that SQL exception during lazyChildLoad is unwrapped for the caller"() {
    when: 'a bad order is saved'
    new Order(order: 'ABC', notes: "A" * 500).save()

    then: 'the right exception is thrown'
    thrown(SimplifiedSQLException)
  }

  @Rollback
  def "verify that save and lazyChildLoad gracefully handles when the parent record is not saved yet"() {
    when: 'a domain record with children is not saved'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(product: 'BIKE', sequence: 1)
    order.orderLines << new OrderLine(qty: 2.0, product: 'WHEEL', sequence: 2)

    then: 'the record is unsaved in memory'
    order.orderLines.size() == 2

    when: 'the record is finally saved'
    order.save()

    then: 'the child records are saved too.'
    def order2 = Order.findByUuid(order.uuid)
    order2.orderLines.size() == 2
  }

  @Rollback
  def "verify that save works with added child records"() {
    when: 'a domain record with children is saved'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(product: 'BIKE', sequence: 1)
    order.orderLines << new OrderLine(qty: 2.0, product: 'WHEEL', sequence: 2)
    order.save()

    then: 'the child records are saved too.'
    def order2 = Order.findByUuid(order.uuid)
    order2.orderLines.size() == 2
  }

  @Rollback
  def "verify that save detects errors with child lists"() {
    when: 'a domain record with children is saved'
    def order = new Order(order: 'M1001')
    def orderLine = new OrderLine(product: 'BIKE', sequence: 1)
    orderLine.sequence = null
    order.orderLines << orderLine
    order.save()

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['sequence'])
  }

  @Rollback
  def "verify that save works with updated child record"() {
    when: 'a domain record with children is saved'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(product: 'BIKE', sequence: 1)
    order.save()

    and: 'the records are changed and new ones added for a second save'
    order.orderLines[0].qty = 10.0
    order.save()

    then: 'the child records are saved too.'
    def order2 = Order.findByUuid(order.uuid)
    order2.orderLines[0].qty == 10.0
    order2.orderLines.size() == 1

    and: 'no duplicate records exist'
    OrderLine.list().size() == 1
  }

  @Rollback
  def "verify that save works with mix of updated and added records"() {
    when: 'a domain record with children is saved'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(product: 'BIKE', sequence: 1)
    order.orderLines << new OrderLine(qty: 2.0, product: 'WHEEL', sequence: 2)
    order.save()

    and: 'the records are changed and new ones added for a second save'
    order.orderLines << new OrderLine(qty: 3.0, product: 'SPOKE', sequence: 3)
    order.orderLines[0].qty = 10.0
    order.save()

    then: 'the child records are saved too.'
    def order2 = Order.findByUuid(order.uuid)
    order2.orderLines[0].qty == 10.0
    order2.orderLines.size() == 3

    and: 'no duplicate records exist'
    OrderLine.list().size() == 3
  }

  @Rollback
  def "verify that save works with some deleted records"() {
    when: 'a domain record with children is saved'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(product: 'BIKE', sequence: 1)
    order.orderLines << new OrderLine(qty: 2.0, product: 'WHEEL', sequence: 2)
    order.save()

    and: 'a record is removed and new ones added for a second save'
    order.orderLines << new OrderLine(qty: 3.0, product: 'SPOKE', sequence: 3)
    order.orderLines.remove(0)
    order.save()
    //println "OrderLine.list() = ${OrderLine.list()*.product}"

    then: 'the child records are saved too.'
    def order2 = Order.findByUuid(order.uuid)
    order2.orderLines.size() == 2

    and: 'the delete record is gone'
    !order2.orderLines.findAll { it.product == 'BIKE' }

    and: 'the updated record is correct'
    order2.orderLines.findAll { it.product == 'SPOKE' }
    order2.orderLines.findAll { it.product == 'WHEEL' }

    and: 'no duplicate records exist'
    OrderLine.list().size() == 2
  }

  @Rollback
  def "verify that save detects deleted child record"() {
    when: 'a domain record with children is saved'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(product: 'BIKE', sequence: 1)
    order.orderLines << new OrderLine(product: 'SEAT', sequence: 2)
    order.orderLines << new OrderLine(product: 'WHEEL', sequence: 3)
    order.save()

    and: 'a record is removed from the children list'
    order.orderLines.remove(1)
    order.save()

    then: 'the child records are saved too.'
    def order2 = Order.findByUuid(order.uuid)
    order2.orderLines.size() == 2
    order2.orderLines.find { it.product == 'BIKE' }
    order2.orderLines.find { it.product == 'WHEEL' }
    !order2.orderLines.find { it.product == 'SEAT' }

    and: 'the deleted record is removed from the DB'
    OrderLine.list().size() == 2
  }

  @Rollback
  def "verify that save handles multiple remove and save cycles"() {
    when: 'a domain record with children is saved'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(product: 'BIKE', sequence: 1)
    order.save()

    then: 'add and remove a record for several cycles with the same parent domain object instance'
    for (i in (1..9)) {
      // Delete the original row.
      order.orderLines.remove(0)
      // Add a child and save
      def orderLine = new OrderLine(product: "BIKE$i", sequence: i)
      order.orderLines << orderLine
      order.save()

      // Now, verify that the database is correct.
      assert order.orderLines.size() == 1
      def order2 = Order.findByUuid(order.uuid)
      assert order2.orderLines.size() == 1
      assert order2.orderLines.find { it.product == "BIKE$i" }
      true
    }
  }

  @Rollback
  def "verify that save provides a user-friendly exception on unique constraint violations"() {
    given: 'an existing field extension'
    new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name).save()

    when: 'a duplicate field extension is validated'
    new FieldExtension(fieldName: 'abc', domainClassName: FieldExtension.name).save()

    then: 'the right exception is thrown'
    def ex = thrown(SimplifiedSQLException)
    UnitTestUtils.assertExceptionIsValid(ex, ['Unique'])
  }

  @Rollback
  def "verify that delete removes child record"() {
    when: 'a domain record with children is saved'
    def order = new Order(order: 'M1001')
    order.orderLines << new OrderLine(product: 'BIKE', sequence: 1)
    order.orderLines << new OrderLine(product: 'SEAT', sequence: 2)
    order.orderLines << new OrderLine(product: 'WHEEL', sequence: 3)
    order.save()

    and: 'a record is removed from the children list'
    order.orderLines.remove(1)

    and: 'the parent record is deleted'
    order.delete()

    then: 'the child records are delete too'
    OrderLine.list().size() == 0
  }

  @Rollback
  def "verify that save handles many-to-many relationship on creation"() {
    given: 'some roles'
    new Role(authority: 'AUTH1', title: 'X').save()
    new Role(authority: 'AUTH2', title: 'X').save()

    when: 'a domain record with the relationship is saved'
    def user = new User(userName: 'ABC', password: 'ABC')
    user.userRoles << Role.findByAuthority('AUTH1')
    user.userRoles << Role.findByAuthority('AUTH2')
    user.save()

    then: 'the record and relationships can be read'
    def user2 = User.findByUserName('ABC')
    user2.userRoles.size() == 2
    user2.userRoles.contains(Role.findByAuthority('AUTH1'))
    user2.userRoles.contains(Role.findByAuthority('AUTH2'))
  }

  @Rollback
  def "verify that save handles many-to-many relationship on update"() {
    given: 'some roles'
    new Role(authority: 'AUTH1', title: 'X').save()
    new Role(authority: 'AUTH2', title: 'X').save()

    when: 'a domain record with the relationship is saved'
    def user = new User(userName: 'ABC', password: 'ABC')
    user.userRoles << Role.findByAuthority('AUTH1')
    user.userRoles << Role.findByAuthority('AUTH2')
    user.save()

    and: 'one reference is removed'
    user.userRoles.remove(Role.findByAuthority('AUTH2'))
    user.save()

    then: 'the record and relationships can be read'
    def user2 = User.findByUserName('ABC')
    user2.userRoles.size() == 1
    user2.userRoles.contains(Role.findByAuthority('AUTH1'))
    !user2.userRoles.contains(Role.findByAuthority('AUTH2'))
  }

  @Rollback
  def "verify that delete handles many-to-many relationship"() {
    when: 'a domain record with the relationship is saved'
    def user = new User(userName: 'ABC', password: 'ABC')
    user.userRoles << Role.findByAuthority('ADMIN')
    user.userRoles << Role.findByAuthority('MANAGER')
    user.save()

    and: 'then the top-level record is deleted'
    user.delete()

    then: 'the record and relationships can be read'
    User.findByUserName('ABC') == null

    and: 'the join table is empty for the deleted user'
    DataSource dataSource = Holders.getApplicationContext().getBean(DataSource.class)
    Connection connection = DataSourceUtils.getConnection(dataSource)
    def ps = connection.prepareStatement("SELECT * from user_role where user_id=?")
    ps.setString(1, user.uuid.toString())
    ps.execute()
    def resultSet = ps.getResultSet()
    def list = []
    while (resultSet.next()) {
      list << UUID.fromString(resultSet.getString(1))
    }
    list.size() == 0
  }

  def "verify that getPersistentProperties finds the correct fields"() {
    given: 'a domain'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import javax.annotation.Nullable
  
      @DomainEntity
      class TestClass {
        UUID uuid
        String notNullableField
        @Nullable String title
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the props are found'
    def properties = DomainEntityHelper.instance.getPersistentProperties(clazz)

    then: 'the props are correct'
    properties.size() == 3
    properties[0].name == 'uuid'
    properties[0].type == UUID
    !properties[0].nullable
    properties[1].name == 'notNullableField'
    properties[1].type == String
    !properties[1].nullable
    properties[2].name == 'title'
    properties[2].type == String
    properties[2].nullable
  }

  def "verify that getPersistentProperties handles fields marked as Transient - annotation from micronaut-data"() {
    when: 'the props are found'
    def properties = DomainEntityHelper.instance.getPersistentProperties(User)

    then: 'the transient field is not in the list'
    !properties.find { it.name == 'password' }
  }

  def "verify that validate detects the domains validate() method returns the wrong value"() {
    given: 'a domain with the wrong type of return'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      
      @DomainEntity
      class TestClass {
        UUID uuid
        def validate() {
          return "ABC"
        }
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the object is validated'
    DomainEntityHelper.instance.validate((DomainEntityInterface) object)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['TestClass.validate()', 'ValidationError', 'list'])
  }

  def "verify that validate calls validate method in domain - if defined"() {
    given: 'a domain'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      
      @DomainEntity
      class TestClass {
        UUID uuid
        def validate() {
          return new ValidationError(100,"ABC")
        }
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the object is validated'
    def errors = DomainEntityHelper.instance.validate((DomainEntityInterface) object)

    then: 'the validation error is correct'
    errors.size() == 1
    errors[0].code == 100
    errors[0].fieldName == 'ABC'
  }

  def "verify that validate supports non-fail return values from validate method in domain"() {
    given: 'a domain'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      
      @DomainEntity
      class TestClass {
        UUID uuid
        def validate() {
          $returnClaus
        }
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the object is validated'
    def errors = DomainEntityHelper.instance.validate((DomainEntityInterface) object)

    then: 'no errors are found'
    errors.size() == 0

    where:
    returnClaus   | _
    'return []'   | _
    'return null' | _
    ''            | _

  }

  def "verify that validate calls validate method in domain - multiple errors returned"() {
    given: 'a domain'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      
      @DomainEntity
      class TestClass {
        UUID uuid
        def validate() {
          return [new ValidationError(10,"ABC"),new ValidationError(11,"XYZ")]
        }
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the object is validated'
    def errors = DomainEntityHelper.instance.validate((DomainEntityInterface) object)

    then: 'the validation errors are correct'
    errors.size() == 2
    errors[0].code == 10
    errors[0].fieldName == 'ABC'
    errors[1].code == 11
    errors[1].fieldName == 'XYZ'
  }

  def "verify that validate detects null values in non-nullable fields"() {
    given: 'a domain'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import javax.annotation.Nullable
  
      @DomainEntity
      class TestClass {
        UUID uuid
        String notNullableField
        @Nullable String title
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the object is validated'
    def errors = DomainEntityHelper.instance.validate((DomainEntityInterface) object)

    then: 'the validation error is correct'
    //error.1.message=Required value is missing "{0}" ({1}).
    errors.size() == 1
    errors[0].code == 1
    errors[0].fieldName == 'notNullableField'
    errors[0].args[0] == 'TestClass'
  }

  def "verify that validate detects blank string values in non-nullable fields"() {
    given: 'a domain'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import javax.annotation.Nullable
  
      @DomainEntity
      class TestClass {
        UUID uuid
        String notNullableField
        @Nullable String title
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()
    object.notNullableField = ''

    when: 'the object is validated'
    def errors = DomainEntityHelper.instance.validate((DomainEntityInterface) object)

    then: 'the validation error is correct'
    //error.1.message=Required value is missing "{0}" ({1}).
    errors.size() == 1
    errors[0].code == 1
    errors[0].fieldName == 'notNullableField'
    errors[0].args[0] == 'TestClass'
  }

  def "verify that validate handles nullable in the JPA column annotation"() {
    given: 'a domain'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import javax.persistence.Column

      @DomainEntity
      class TestClass {
        UUID uuid
        @Column(nullable=true) String title
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the object is validated'
    def errors = DomainEntityHelper.instance.validate((DomainEntityInterface) object)

    then: 'there are no validation errors'
    errors.size() == 0
  }

  def "verify that validate passes when a non-nullable field has a value"() {
    given: 'a domain'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import javax.annotation.Nullable
  
      @DomainEntity
      class TestClass {
        UUID uuid
        String notNullableField
        @Nullable String title
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the object is validated'
    object.notNullableField = "ABC"
    def errors = DomainEntityHelper.instance.validate((DomainEntityInterface) object)

    then: 'the validation passes'
    errors.size() == 0
  }

  def "verify that validate detects value too long for column"() {
    given: 'a domain'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import javax.persistence.Column
  
      @DomainEntity
      class TestClass {
        UUID uuid
        @Column(length = 128)
        String title
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()
    object.title = "A" * 500

    when: 'the object is validated'
    def errors = DomainEntityHelper.instance.validate((DomainEntityInterface) object)

    then: 'the validation error is correct'
    //error.2.message=Value is too long (max={2}, length={1}) for field {0}.
    errors.size() == 1
    errors[0].code == 2
    errors[0].fieldName == 'title'
    UnitTestUtils.assertContainsAllIgnoreCase(errors[0].toString(), ["long", "128", "500", "title"])
  }

  def "verify that validate handles column length passing and edge cases"() {
    given: 'a domain'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import javax.persistence.Column
  
      @DomainEntity
      class TestClass {
        UUID uuid
        @Column(length = 128)
        String title
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()
    object.title = "A" * sLength

    when: 'the object is validated'
    def errors = DomainEntityHelper.instance.validate((DomainEntityInterface) object)

    then: 'the validation fails, if expected'
    fails == errors.size() > 0

    where:
    sLength | fails
    0       | false
    1       | false
    127     | false
    128     | false
    229     | true
  }

  def "verify that save detects missing column and throws an exception"() {
    given: 'a domain'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.domain.validate.ValidationError
      import javax.persistence.Column
  
      @DomainEntity(repository=sample.domain.OrderRepository)
      class TestClass {
        UUID uuid
        String title
      }
    """
    def object = CompilerTestUtils.compileSource(src).newInstance()

    when: 'the object is saved'
    DomainEntityHelper.instance.save((DomainEntityInterface) object)

    then: 'the right exception is thrown'
    def ex = thrown(MessageBasedException)
    UnitTestUtils.assertExceptionIsValid(ex, ['title', 'missing'])

    and: 'the exception values are correct'
    ex.code == 3
    ex.params.size() > 0
  }

  @Rollback
  def "verify that save calls the beforeSave method on the domain"() {
    when: 'the domain is saved'
    def order = new Order(order: 'ABC')
    order.product = 'XYZZY'
    order.save()

    then: 'the product field is altered by the beforeSave method'
    order.product == "XYZZYAlteredByBeforeSave"
  }

  @Rollback
  def "verify that validate calls the beforeValidate method on the domain"() {
    when: 'the domain is validated'
    def order = new Order(order: 'ABC')
    order.product = 'XYZZY'
    DomainEntityHelper.instance.validate(order as DomainEntityInterface)

    then: 'the product field is altered by the beforeValidate method'
    order.product == "XYZZYAlteredByBeforeValidate"
  }

  @Rollback
  def "verify that save custom child list on creation"() {
    when: 'a domain record with the custom child records is saved'
    def order = new Order(order: 'ABC')
    def comp1 = new CustomOrderComponent(order: order, sequence: 123)
    def comp2 = new CustomOrderComponent(order: order, sequence: 456)
    def comp3 = new CustomOrderComponent(order: order, sequence: 789)
    def comps = [comp1, comp2, comp3]
    order.setFieldValue('customComponents', comps)
    order.save()

    then: 'the records are saved in the DB'
    def list = CustomOrderComponent.findAllByOrder(order)
    list.size() == 3
    list.find { it.sequence == 123 }
    list.find { it.sequence == 456 }
    list.find { it.sequence == 789 }
  }

  @Rollback
  def "verify that save custom child list on update - changed/remove/added records"() {
    when: 'a domain record with the custom child records is created'
    def order = new Order(order: 'ABC')
    def comp1 = new CustomOrderComponent(order: order, sequence: 123)
    def comp2 = new CustomOrderComponent(order: order, sequence: 456)
    def comp3 = new CustomOrderComponent(order: order, sequence: 789)
    def comps = [comp1, comp2, comp3]
    order.setFieldValue('customComponents', comps)
    order.save()

    and: 'the records are changed'
    List customComponents = order.getFieldValue('customComponents') as List
    customComponents.remove(1)
    comp1.sequence = 1234
    comp3.sequence = 7894
    customComponents << new CustomOrderComponent(order: order, sequence: 4999)
    customComponents << new CustomOrderComponent(order: order, sequence: 5999)
    order.save()

    then: 'the records are saved in the DB'
    def list = CustomOrderComponent.list()
    list.size() == 4
    list.find { it.sequence == 1234 }
    list.find { it.sequence == 7894 }
    list.find { it.sequence == 4999 }
    list.find { it.sequence == 5999 }
    !list.find { it.sequence == 456 }

    and: 'the list in the domain is consistent'
    customComponents.size() == 4
    customComponents.find { it.sequence == 1234 }
    customComponents.find { it.sequence == 7894 }
    customComponents.find { it.sequence == 4999 }
    customComponents.find { it.sequence == 5999 }
    !customComponents.find { it.sequence == 456 }
  }

  @Rollback
  def "verify that delete calls the beforeDelete method on the domain"() {
    when: 'the domain is saved'
    def order = new Order(order: 'ABC')
    order.save()
    order.product = 'XYZZY'
    order.delete()

    then: 'the product field is altered by the beforeDelete method'
    order.product == "PDQAlteredByBeforeSave"
  }

  @Rollback
  def "verify that lazyReferenceLoad handles unloaded case with a valid foreign reference"() {
    given: 'a foreign reference'
    def order = new Order(order: 'ABC').save()
    DomainEntityHelper.instance.lastLazyRefLoaded = null

    when: 'the simple reference is set'
    def afd = new AllFieldsDomain(order: new Order(uuid: order.uuid))

    then: 'the foreign domain object is retrieved'
    afd.order.order == 'ABC'

    and: 'the query is made for the first call'
    DomainEntityHelper.instance.lastLazyRefLoaded == order.uuid

    when: 'the getter is called a second time'
    DomainEntityHelper.instance.lastLazyRefLoaded = null
    afd.order.order == 'ABC'

    then: 'no query is made'
    DomainEntityHelper.instance.lastLazyRefLoaded == null
  }

  @Rollback
  def "verify that lazyReferenceLoad handles null foreign reference"() {
    given: 'set the last loaded uuid to a value for testing'
    DomainEntityHelper.instance.lastLazyRefLoaded = UUID.randomUUID()

    when: 'the simple reference is set'
    def afd = new AllFieldsDomain()

    then: 'the foreign domain object is retrieved and is null'
    afd.order == null

    and: 'no read was attempted'
    DomainEntityHelper.instance.lastLazyRefLoaded != null
  }

  @Rollback
  def "verify that lazyReferenceLoad handles empty foreign reference"() {
    // This is the way Micronaut-data initializes the value when it is null.
    given: 'set the last loaded uuid to a value for testing'
    def uuid = UUID.randomUUID()
    DomainEntityHelper.instance.lastLazyRefLoaded = uuid

    when: 'the simple reference is set'
    def afd = new AllFieldsDomain(order: new Order())

    then: 'the foreign domain object is retrieved and is null'
    afd.order == null

    and: 'no read was attempted'
    DomainEntityHelper.instance.lastLazyRefLoaded == uuid
  }

  @Rollback
  def "verify that lazyReferenceLoad handles invalid foreign reference"() {
    given: 'set the last loaded uuid to a value for testing'
    def uuid = UUID.randomUUID()  // The uuid for a non-existent reference.
    DomainEntityHelper.instance.lastLazyRefLoaded = null

    when: 'the simple reference is set to simulate a JOIN style read from DB with an invalid uuid'
    def afd = new AllFieldsDomain(name: 'ABC').save()
    afd.order = new Order(uuid: uuid)

    and: 'the foreign domain object is retrieved and is null'
    afd.order == null

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['AllFieldsDomain', 'order', uuid.toString(), afd.uuid.toString()])
  }

  @Rollback
  def "verify that lazyReferenceLoad handles foreign reference loaded by a standard join annotation"() {
    given: 'a domain with a valid foreign reference'
    def order = new Order(order: 'XYZ').save()
    new AllFieldsDomain(name: 'ABC', title: 'ABX', order: order).save()

    and: 'the last record read is cleared'
    DomainEntityHelper.instance.lastLazyRefLoaded = null

    when: 'the find is used with the join syntax'
    def afd2 = AllFieldsDomain.getRepository().findByTitle('ABX').get()

    then: 'the foreign reference is populated without a read from the DB'
    afd2.order.order == 'XYZ'
    DomainEntityHelper.instance.lastLazyRefLoaded == null
  }

  @Rollback
  def "verify that find will retrieve foreign references"() {
    given: 'a domain with a foreign reference'
    def order = new Order(order: 'ABC').save()
    def afd = new AllFieldsDomain(name: 'XYZ', order: order).save()

    when: 'the domain is read'
    def afd2 = AllFieldsDomain.findByUuid(afd.uuid)

    then: 'the foreign reference is populated'
    afd2.order.order == order.order
  }

  @Rollback
  def "verify that lazyChildLoad will work with grand-children"() {
    given: 'a domain with grand children'
    def sampleParent = new SampleParent(name: 'ABC').save()
    def sampleChild = new SampleChild(sampleParent: sampleParent, key: 'XYZ').save()
    def sampleGrandChild1 = new SampleGrandChild(sampleChild: sampleChild, grandKey: 'PDQ1').save()
    def sampleGrandChild2 = new SampleGrandChild(sampleChild: sampleChild, grandKey: 'PDQ2').save()

    when: 'the domain is read'
    def sampleParent2 = SampleParent.findByUuid(sampleParent.uuid)

    then: 'the child list is correct'
    def sampleChildren = sampleParent2.sampleChildren
    sampleChildren.size() == 1
    sampleChildren[0] == sampleChild

    and: 'the grand child list is correct'
    def sampleChild2 = sampleChildren[0]
    sampleChild2.sampleGrandChildren.size() == 2
    sampleChild2.sampleGrandChildren[0] == sampleGrandChild1
    sampleChild2.sampleGrandChildren[1] == sampleGrandChild2
  }

  @Rollback
  def "verify that single foreign reference relationships are null when not found"() {
    given: 'a domain with no list of foreign references'
    def sampleParent = new SampleParent(name: 'ABC').save()

    when: 'the domain is read and the field is accessed'
    def sampleParent2 = SampleParent.findByUuid(sampleParent.uuid)
    sampleParent2.getAllFieldsDomain()
    //println "sampleParent2 = $sampleParent2"

    then: 'the reference list is correct'
    sampleParent2.allFieldsDomain == null
  }

  def "verify that getComplexHolder finds the holder"() {
    when: ' the class is compiled'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
      
      @DomainEntity(repository=sample.domain.OrderRepository)
      class TestClass {
        @ExtensibleFieldHolder
        String customFields
        UUID uuid
      }
    """
    def o = CompilerTestUtils.compileSource(src).newInstance()

    then: 'the field has the repository '
    DomainEntityHelper.instance.getComplexHolder(o as DomainEntityInterface) instanceof Map
  }

  def "verify that executeDomainMethod detects method not found exception in the method called dynamically"() {
    when: ' the class is compiled'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import java.lang.reflect.Method

      
      @DomainEntity(repository=sample.domain.OrderRepository)
      class TestClass {
        UUID uuid
        
        def testMethod() {
          Method method = this.getClass().getDeclaredMethod('badMethod');
        }
      }
    """
    def o = CompilerTestUtils.compileSource(src).newInstance()

    and: 'the domain method is executed'
    DomainEntityHelper.instance.executeDomainMethod(o as DomainEntityInterface, 'testMethod')

    then: 'the right exception is thrown'
    def ex = thrown(NoSuchMethodException)
    UnitTestUtils.assertExceptionIsValid(ex, ['badMethod'])
  }

  /**
   * Counts the records in the given table.
   * @param tableName The real table name.
   * @return The count.
   */
  @SuppressWarnings("GroovyUnusedAssignment")
  int countRecords(String tableName) {
    PreparedStatement ps = null
    ResultSet rs = null
    int res = 0
    try {
      ps = getPreparedStatement("SELECT count(*)  from $tableName")
      ps.execute()
      rs = ps.getResultSet()
      while (rs.next()) {
        res = rs.getInt(1)
      }
    } finally {
      if (ps != null) {
        ps.close()
      }
      if (rs != null) {
        rs.close()
      }
    }
    return res
  }

  @Rollback
  def "verify that lazyRefListLoad works for read"() {
    given: 'a domain record with list of references'
    List<AllFieldsDomain> afdList = DataGenerator.generate {
      domain AllFieldsDomain
      count 5
      values name: 'ABC-$i'
    }

    def sampleParent = new SampleParent(name: 'ABC')
    sampleParent.allFieldsDomains = [afdList[0], afdList[1], afdList[2]] as List<AllFieldsDomain>
    sampleParent.save()

    when: 'the lazy load is used - on an uninitialized parent domain object'
    def sampleParent2 = SampleParent.findByName('ABC')
    def list = (List<AllFieldsDomain>) DomainEntityHelper.instance.lazyRefListLoad((DomainEntityInterface) sampleParent2,
                                                                                   'allFieldsDomains',
                                                                                   'sample_parent_all_fields_domain', AllFieldsDomain)

    then: 'the record is in the DB'
    list.size() == 3
    list[0].uuid == afdList[0].uuid
    list[1].uuid == afdList[1].uuid
    list[2].uuid == afdList[2].uuid

    and: 'the field has the list - the list is the same on later calls'
    DomainEntityHelper.instance.lazyRefListLoad((DomainEntityInterface) sampleParent2, 'allFieldsDomains',
                                                'sample_parent_all_fields_domain', AllFieldsDomain)

    when: 'the loaded is called again'
    DomainEntityHelper.instance.lastLazyRefParentLoaded = null
    DomainEntityHelper.instance.lazyRefListLoad((DomainEntityInterface) sampleParent2, 'allFieldsDomains',
                                                'sample_parent_all_fields_domain', AllFieldsDomain)

    then: 'the value is not re-read from the DB'
    DomainEntityHelper.instance.lastLazyRefParentLoaded == null

    and: 'the records in the DB match'
    countRecords('sample_parent_all_fields_domain') == 3
  }

  @Rollback
  def "verify that save handles reference list changes"() {
    given: 'a domain record with list of references'
    List<AllFieldsDomain> afdList = DataGenerator.generate {
      domain AllFieldsDomain
      count 5
      values name: 'ABC-$i'
    }

    def sampleParent = new SampleParent(name: 'ABC')
    sampleParent.allFieldsDomains = [afdList[0], afdList[1], afdList[2]] as List<AllFieldsDomain>
    sampleParent.save()

    when: 'the reference list is changed'
    sampleParent.allFieldsDomains.remove(1)
    sampleParent.allFieldsDomains << (AllFieldsDomain) afdList[3]
    sampleParent.allFieldsDomains << (AllFieldsDomain) afdList[4]
    sampleParent.save()

    then: 'the records are correct in the DB'
    def sampleParent2 = SampleParent.findByName('ABC')
    sampleParent2.allFieldsDomains.size() == 4
    sampleParent2.allFieldsDomains.contains(afdList[0])
    sampleParent2.allFieldsDomains.contains(afdList[2])
    sampleParent2.allFieldsDomains.contains(afdList[3])
    sampleParent2.allFieldsDomains.contains(afdList[4])
    !sampleParent2.allFieldsDomains.contains(afdList[1])

    and: 'the records in the DB match'
    countRecords('sample_parent_all_fields_domain') == 4
  }

  @Rollback
  def "verify that delete cleans up the reference list records"() {
    given: 'a domain record with list of references'
    List<AllFieldsDomain> afdList = DataGenerator.generate {
      domain AllFieldsDomain
      count 5
      values name: 'ABC-$i'
    }

    def sampleParent = new SampleParent(name: 'ABC')
    sampleParent.allFieldsDomains = [afdList[0], afdList[1], afdList[2]] as List<AllFieldsDomain>
    sampleParent.save()

    when: 'the record is deleted'
    def sampleParent2 = SampleParent.findByName('ABC')
    sampleParent2.delete()

    then: 'the records are deleted from the DB join table'
    countRecords('sample_parent_all_fields_domain') == 0
  }

}
