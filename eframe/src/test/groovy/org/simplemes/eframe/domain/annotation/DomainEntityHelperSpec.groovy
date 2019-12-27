package org.simplemes.eframe.domain.annotation

import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.transaction.SynchronousTransactionManager
import io.micronaut.transaction.jdbc.DataSourceUtils
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.security.domain.Role
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.Order
import sample.domain.OrderLine
import sample.domain.OrderRepository

import javax.sql.DataSource
import java.sql.Connection

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DomainEntityHelperSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [Order, OrderLine]
  //static specNeeds= SERVER

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

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that save will create a record"() {
    when: ' the record is saved'
    def order = new Order('M1001')
    DomainEntityHelper.instance.save((DomainEntityInterface) order)

    then: 'the record is in the DB'
    def list = (List<Order>) DomainEntityHelper.instance.list(Order)
    list[0].uuid == order.uuid
  }

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
    order.save()
    def orderLine1 = new OrderLine(order: order, product: 'BIKE', sequence: 1).save()
    def orderLine2 = new OrderLine(order: order, qty: 2.0, product: 'WHEEL', sequence: 2).save()

    when: 'the lazy load is used'
    def list = (List<OrderLine>) DomainEntityHelper.instance.lazyChildLoad((DomainEntityInterface) order, 'orderLines', 'order', OrderLine)

    then: 'the record is in the DB'
    list.size() == 2
    list[0].uuid == orderLine1.uuid
    list[1].uuid == orderLine2.uuid

    and: 'the field has the list - the list is the same on later calls'
    DomainEntityHelper.instance.lazyChildLoad((DomainEntityInterface) order, 'orderLines', 'order', OrderLine).is(list)
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

  def "verify that SQL exception during lazyChildLoad is unwrapped for the caller"() {
    when: 'a bad order is saved'
    new Order(order: "A" * 500).save()

    then: 'the right exception is thrown'
    thrown(DataAccessException)
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
  def "verify that save works with added records"() {
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
    when: 'a domain record with the relationship is saved'
    def user = new User(userName: 'ABC', password: 'ABC')
    user.userRoles << Role.findByAuthority('ADMIN')
    user.userRoles << Role.findByAuthority('MANAGER')
    user.save()

    then: 'the record and relationships can be read'
    def user2 = User.findByUserName('ABC')
    user2.userRoles.size() == 2
    user2.userRoles.contains(Role.findByAuthority('ADMIN'))
    user2.userRoles.contains(Role.findByAuthority('MANAGER'))
    !user2.userRoles.contains(Role.findByAuthority('CUSTOMIZER'))
  }

  @Rollback
  def "verify that save handles many-to-many relationship on update"() {
    when: 'a domain record with the relationship is saved'
    def user = new User(userName: 'ABC', password: 'ABC')
    user.userRoles << Role.findByAuthority('ADMIN')
    user.userRoles << Role.findByAuthority('MANAGER')
    user.save()

    and: 'one reference is removed'
    user.userRoles.remove(1)
    user.save()

    then: 'the record and relationships can be read'
    def user2 = User.findByUserName('ABC')
    user2.userRoles.size() == 1
    user2.userRoles.contains(Role.findByAuthority('ADMIN'))
    !user2.userRoles.contains(Role.findByAuthority('MANAGER'))
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

}
