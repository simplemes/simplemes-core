package org.simplemes.mes.assy.demand.service

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.archive.FileArchiver
import org.simplemes.eframe.json.TypeableMapper
import org.simplemes.eframe.misc.FileFactory
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockFileFactory
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.assy.demand.AddOrderAssembledComponentRequest
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.assy.demand.domain.OrderBOMComponent
import org.simplemes.mes.assy.product.domain.ProductComponent
import org.simplemes.mes.demand.OrderReleaseRequest
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.product.domain.Product

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OrderAssyServiceSpec extends BaseSpecification {

  OrderAssyService service

  static specNeeds = SERVER

  def setup() {
    service = Holders.getBean(OrderAssyService)
  }

  @Rollback
  def "verify orderReleasePostProcessor for a product with components"() {
    given: 'a product with two components'
    def component1 = new Product(product: 'CPU').save()
    def component2 = new Product(product: 'MOTHERBOARD').save()
    def product = new Product(product: 'PC')
    product.components << new ProductComponent(sequence: 10, component: component1, qty: 1.2)
    product.components << new ProductComponent(sequence: 20, component: component2, qty: 2.2)
    product.save()

    and: "an order to process"
    Order order = new Order(order: 'M002', product: product).save()

    when: "the method orderReleasePostProcessor is called"
    service.orderReleasePostProcessor(new OrderReleaseRequest(order: order))

    then: "the BOM components are copied to the order"

    List<OrderBOMComponent> components = order.components
    components.size() == 2
    components[0].sequence == 10
    components[0].component == component1
    components[0].qty == 1.2
    components[1].sequence == 20
    components[1].component == component2
    components[1].qty == 2.2
  }

  @Rollback
  def "verify orderReleasePostProcessor for a product with no components"() {
    given: 'a product with no components'
    def product = new Product(product: 'PC').save()

    and: "an order to process"
    Order order = new Order(order: 'M002', product: product).save()

    when: "the method orderReleasePostProcessor is called"
    service.orderReleasePostProcessor(new OrderReleaseRequest(order: order))

    then: "no BOM components are copied to the order"
    List<OrderBOMComponent> components = order.components
    components.size() == 0
    OrderBOMComponent.list().size() == 0
  }

  @Rollback
  @SuppressWarnings(["UnnecessaryGetter", "GroovyAssignabilityCheck"])
  def "verify order archive of components"() {
    given: 'a mock file archiver to avoid file operations'
    def stringWriter = new StringWriter()
    FileFactory.instance = new MockFileFactory(stringWriter)

    and: 'a product with two components'
    def component1 = new Product(product: 'CPU').save()
    def component2 = new Product(product: 'MOTHERBOARD').save()
    def product = new Product(product: 'PC')
    def productComponent1 = new ProductComponent(sequence: 10, component: component1, qty: 1.2)
    def productComponent2 = new ProductComponent(sequence: 20, component: component2, qty: 2.2)
    product.components << productComponent1
    product.components << productComponent2
    product.save()

    and: "an order to process"
    Order order = new Order(order: 'M002', product: product)
    order.components << new OrderBOMComponent(productComponent1)
    order.components << new OrderBOMComponent(productComponent2)
    order.save()

    when: "the order is archived"
    def archiver = new FileArchiver()
    archiver.archive(order)
    archiver.close()
    def s = stringWriter.toString()
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the order component records are removed'
    OrderBOMComponent.list().size() == 0

    and: 'the JSON for the components is correct'
    def list = TypeableMapper.instance.read(new StringReader(s))
    def order2 = list[0] as Order
    order2.components.size() == 2
    //order2.components[0].component.uuid == order.components[0].component.uuid
    for (int i = 0; i < order2.components.size(); i++) {
      order2.components[i].sequence == order.components[i].sequence
      order2.components[i].qty == order.components[i].qty
      order2.components[i].component == order.components[i].component
    }

    cleanup:
    FileFactory.instance = new FileFactory()
  }

  @Rollback
  def "verify that addComponent can assembled a component - no order BOM"() {
    given: 'a component'
    def component1 = new Product(product: 'CPU').save()

    and: 'an order'
    def order = new Order(order: 'M1001').save()

    and: 'a user'
    SecurityUtils.currentUserOverride = SecurityUtils.TEST_USER

    when: 'the component is added'
    def res = service.addComponent(new AddOrderAssembledComponentRequest(order: order, component: component1))

    then: 'the record is saved in the DB'
    def orderAssembledComponent = OrderAssembledComponent.findByUuid(res.uuid)
    orderAssembledComponent != null
    orderAssembledComponent.userName == SecurityUtils.currentUserName
    orderAssembledComponent.qty == 1.0

    and: 'the sequence is auto-assigned'
    orderAssembledComponent.sequence == 1

    and: 'the method response is valid'
    res.order == order
    res.component == component1
  }


/*  def "verify that addComponent can assembled a component - component from order BOM"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])
    OrderBOMComponent orderComponent = order.components[index] as OrderBOMComponent

    when: 'the first component is added'
    def res = service.addComponent(new AddOrderAssembledComponentRequest(order: order, component: orderComponent.component,
                                                                         orderBOMComponent: orderComponent))

    and: 'the handler is called directly to simulate the normal persistence handler logic'
    UnitTestUtils.simulatePersistenceUpdateEvent(order)

    and: 'the child validation passed'
    List<OrderAssembledComponent> orderAssembledComponents = order.assembledComponents
    orderAssembledComponents[0].validate()
    assert orderAssembledComponents[0].errors.allErrors.size() == 0

    then: 'the first record is saved in the DB'
    def orderAssembledComponent = OrderAssembledComponent.findByOrderIdAndComponent(order.id, orderComponent.component)
    orderAssembledComponent != null
    orderAssembledComponent.bomSequence == orderComponent.sequence
    orderAssembledComponent.qty == orderComponent.qty

    and: 'the method response is valid'
    res.orderId == order.id
    res.component == orderComponent.component
    res.bomSequence == orderComponent.sequence

    where:
    index | _
    0     | _
    1     | _
  }*/
}
