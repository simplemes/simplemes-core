package org.simplemes.mes.assy.demand.service

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.archive.FileArchiver
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.json.TypeableMapper
import org.simplemes.eframe.misc.FileFactory
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.MockFileFactory
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.assy.demand.AddOrderAssembledComponentRequest
import org.simplemes.mes.assy.demand.AssembledComponentStateEnum
import org.simplemes.mes.assy.demand.ComponentRemoveUndoRequest
import org.simplemes.mes.assy.demand.FindComponentAssemblyStateRequest
import org.simplemes.mes.assy.demand.OrderComponentStateEnum
import org.simplemes.mes.assy.demand.RemoveOrderAssembledComponentRequest
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.assy.demand.domain.OrderBOMComponent
import org.simplemes.mes.assy.product.domain.ProductComponent
import org.simplemes.mes.assy.test.AssyUnitTestUtils
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.OrderReleaseRequest
import org.simplemes.mes.demand.OrderReleaseResponse
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.product.domain.MasterRouting
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.tracking.domain.ActionLog
import spock.lang.Ignore

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

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, OrderAssembledComponent, Order, ProductComponent, Product, MasterRouting]

  def setup() {
    service = Holders.getBean(OrderAssyService)
  }

  @Rollback
  def "verify postRelease for a product with components"() {
    given: 'a product with two components'
    def component1 = new Product(product: 'CPU').save()
    def component2 = new Product(product: 'MOTHERBOARD').save()
    def product = new Product(product: 'PC')
    product.components << new ProductComponent(sequence: 10, component: component1, qty: 1.2)
    product.components << new ProductComponent(sequence: 20, component: component2, qty: 2.2)
    product.save()

    and: "an order to process"
    Order order = new Order(order: 'M002', product: product).save()

    when: "the method postRelease is called"
    service.postRelease(new OrderReleaseResponse(), new OrderReleaseRequest(order: order))

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
  def "verify postRelease for a product with no components"() {
    given: 'a product with no components'
    def product = new Product(product: 'PC').save()

    and: "an order to process"
    Order order = new Order(order: 'M002', product: product).save()

    when: "the method postRelease is called"
    service.postRelease(new OrderReleaseResponse(), new OrderReleaseRequest(order: order))

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

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that addComponent can assembled a component - bomSequence from order BOM"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])
    OrderBOMComponent orderComponent = order.components[1] as OrderBOMComponent

    when: 'the component is added'
    setCurrentUser()
    def res = service.addComponent(new AddOrderAssembledComponentRequest(order: order, component: orderComponent.component,
                                                                         bomSequence: orderComponent.sequence))

    then: 'the record is saved in the DB'
    def orderAssembledComponent = OrderAssembledComponent.findByUuid(res.uuid)
    orderAssembledComponent.bomSequence == orderComponent.sequence
    orderAssembledComponent.qty == orderComponent.qty

    and: 'the method response is valid'
    res.order == order
    res.component == orderComponent.component
    res.bomSequence == orderComponent.sequence
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that addComponent can assembled a component - component from order BOM with sequence as input"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])
    OrderBOMComponent orderComponent = order.components[1] as OrderBOMComponent

    when: 'the component is added'
    setCurrentUser()
    def res = service.addComponent(new AddOrderAssembledComponentRequest(order: order, component: orderComponent.component,
                                                                         bomSequence: orderComponent.sequence))

    then: 'the record is saved in the DB'
    def orderAssembledComponent = OrderAssembledComponent.findByUuid(res.uuid)
    orderAssembledComponent.bomSequence == orderComponent.sequence
    orderAssembledComponent.qty == orderComponent.qty

    and: 'the method response is valid'
    res.order == order
    res.component == orderComponent.component
    res.bomSequence == orderComponent.sequence
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that addComponent can assembled a component - component as input"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])
    OrderBOMComponent orderComponent = order.components[1] as OrderBOMComponent

    when: 'the component is added'
    setCurrentUser()
    def res = service.addComponent(new AddOrderAssembledComponentRequest(order: order, component: orderComponent.component))

    then: 'the record is saved in the DB'
    def orderAssembledComponent = OrderAssembledComponent.findByUuid(res.uuid)
    orderAssembledComponent.bomSequence == orderComponent.sequence
    orderAssembledComponent.qty == orderComponent.qty

    and: 'the method response is valid'
    res.order == order
    res.component == orderComponent.component
    res.bomSequence == orderComponent.sequence
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that addComponent can assembled a component - bomSequence takes precedence over bomSequence"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])
    OrderBOMComponent orderComponent1 = order.components[0] as OrderBOMComponent
    OrderBOMComponent orderComponent2 = order.components[1] as OrderBOMComponent

    when: 'the component is added'
    setCurrentUser()

    def res = service.addComponent(new AddOrderAssembledComponentRequest(order: order, component: orderComponent1.component,
                                                                         bomSequence: orderComponent2.sequence))

    then: 'the record is saved in the DB'
    def orderAssembledComponent = OrderAssembledComponent.findByUuid(res.uuid)
    orderAssembledComponent.bomSequence == orderComponent2.sequence
    orderAssembledComponent.qty == orderComponent2.qty

    and: 'the method response is valid'
    res.order == order
    res.component == orderComponent2.component
    res.bomSequence == orderComponent2.sequence
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that addComponent detects invalid BOM sequence as input"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])

    when: 'the component is added'
    setCurrentUser()
    service.addComponent(new AddOrderAssembledComponentRequest(order: order,
                                                               component: order.components[1].component,
                                                               bomSequence: 237))

    then: 'the right exception is thrown'
    def ex = thrown(BusinessException)
    UnitTestUtils.assertExceptionIsValid(ex, [order.order, '237'], 10001)
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that addComponent detects missing component and BOM sequence"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])

    when: 'the component is added'
    setCurrentUser()
    service.addComponent(new AddOrderAssembledComponentRequest(order: order))

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['component'])
  }

  @Rollback
  def "verify that addComponent can assembled a component - flex data field values"() {
    given: 'a component'
    def component1 = new Product(product: 'CPU').save()

    and: 'an order'
    def order = new Order(order: 'M1001').save()

    and: 'an add component request with flexible fields'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY')

    and: 'a request to add a component'
    def request = new AddOrderAssembledComponentRequest(order: order, component: component1,
                                                        assemblyData: flexType)
    request.setAssemblyDataValue('FIELD1', 'Vendor237')

    when: 'the component is added'
    setCurrentUser()
    def res = service.addComponent(request)

    then: 'the record is saved in the DB'
    def orderAssembledComponent = OrderAssembledComponent.findByUuid(res.uuid)
    orderAssembledComponent.getAssemblyDataValue('FIELD1') == 'Vendor237'

    and: 'the method response is valid'
    res.getAssemblyDataValue('FIELD1') == 'Vendor237'
  }

  @Rollback
  def "verify that addComponent can assembled a component - unique sequence assignment"() {
    given: 'some components'
    def component1 = new Product(product: 'CPU').save()
    def component2 = new Product(product: 'DISK').save()
    def component3 = new Product(product: 'MOTHERBOARD').save()

    and: 'an order'
    def order = new Order(order: 'M1001').save()

    when: 'the components are added'
    setCurrentUser()
    def res1 = service.addComponent(new AddOrderAssembledComponentRequest(order: order, component: component1))
    def res2 = service.addComponent(new AddOrderAssembledComponentRequest(order: order, component: component2))
    def res3 = service.addComponent(new AddOrderAssembledComponentRequest(order: order, component: component3))

    then: 'the records are saved in the DB and match the returned values'
    def orderAssembledComponent1 = OrderAssembledComponent.findByUuid(res1.uuid)
    def orderAssembledComponent2 = OrderAssembledComponent.findByUuid(res2.uuid)
    def orderAssembledComponent3 = OrderAssembledComponent.findByUuid(res3.uuid)
    orderAssembledComponent1.sequence == res1.sequence
    orderAssembledComponent2.sequence == res2.sequence
    orderAssembledComponent3.sequence == res3.sequence

    and: 'the sequences are unique'
    def list = []
    list << orderAssembledComponent1.sequence
    list << orderAssembledComponent2.sequence
    list << orderAssembledComponent3.sequence
    list.unique().size() == 3
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Rollback
  def "verify that addComponent can assembled a component - optional fields"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(lsnTrackingOption: LSNTrackingOption.LSN_ONLY, components: ['CPU', 'MOTHERBOARD'])
    OrderBOMComponent orderComponent = order.components[0] as OrderBOMComponent

    and: 'the other optional values'
    def workCenter = new WorkCenter(workCenter: "WC").save()
    def location = "LOC1"
    def qty = 42.2

    when: 'the components are added'
    def request = new AddOrderAssembledComponentRequest(order: order,
                                                        component: orderComponent.component,
                                                        lsn: order.lsns[0],
                                                        workCenter: workCenter,
                                                        location: location,
                                                        qty: qty)
    setCurrentUser()
    def res = service.addComponent(request)

    then: 'the records are saved in the DB and match the returned values'
    def orderAssembledComponent = OrderAssembledComponent.findByUuid(res.uuid)
    orderAssembledComponent.lsn == order.lsns[0]
    orderAssembledComponent.workCenter == workCenter
    orderAssembledComponent.location == location
    orderAssembledComponent.qty == qty

    and: 'the response matches the passed in values'
    res.lsn == order.lsns[0]
    res.workCenter == workCenter
    res.location == location
    res.qty == qty
  }

  def "verify that findComponentAssemblyState works with no components assembled - order"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'BOARD', 'DISK'], reverseSequences: true)

    and: 'the request local is set'
    GlobalUtils.defaultLocale = locale

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    def stateString = GlobalUtils.lookup("orderComponentState.EMPTY.label", locale)
    orderComponentState.size() == 3
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'DISK'
    orderComponentState[0].location == NameUtils.DEFAULT_KEY
    orderComponentState[0].qtyRequired == 1.0
    orderComponentState[0].qtyAssembled == 0.0
    orderComponentState[0].overallStateString == stateString
    orderComponentState[0].overallState == OrderComponentStateEnum.EMPTY
    orderComponentState[0].percentAssembled == 0
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[0].qtyAndStateString, ['0/1', stateString])

    orderComponentState[1].sequence == 20
    orderComponentState[1].component == 'BOARD'

    orderComponentState[2].sequence == 30
    orderComponentState[2].component == 'CPU'

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that findComponentAssemblyState localizes the display strings"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'BOARD', 'DISK'], reverseSequences: true)

    and: 'the request local is set'
    GlobalUtils.defaultLocale = locale

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    orderComponentState.size() == 3
    def overallStateString = GlobalUtils.lookup("orderComponentState.EMPTY.label", locale)
    orderComponentState[0].overallStateString == overallStateString
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[0].qtyAndStateString, ['0/1', overallStateString])

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  @Rollback
  def "verify that findComponentAssemblyState works with no components assembled - order and lotSize greater than 1.0"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'BOARD', 'DISK'], qty: 3.0)

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    orderComponentState.size() == 3
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'CPU'
    orderComponentState[0].qtyRequired == 3.0

    orderComponentState[1].sequence == 20
    orderComponentState[1].component == 'BOARD'
    orderComponentState[1].qtyRequired == 6.0

    orderComponentState[2].sequence == 30
    orderComponentState[2].component == 'DISK'
    orderComponentState[2].qtyRequired == 9.0
  }

  def "verify that findComponentAssemblyState works with no components assembled - lsn"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'BOARD', 'DISK'],
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY,
                                               reverseSequences: true)
    def lsn = order.lsns[0] as LSN

    and: 'the request local is set'
    GlobalUtils.defaultLocale = locale

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(lsn))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    def stateString = GlobalUtils.lookup("orderComponentState.EMPTY.label", locale)
    orderComponentState.size() == 3
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'DISK'
    orderComponentState[0].location == NameUtils.DEFAULT_KEY
    orderComponentState[0].qtyRequired == 1.0
    orderComponentState[0].qtyAssembled == 0.0
    orderComponentState[0].overallStateString == stateString
    orderComponentState[0].overallState == OrderComponentStateEnum.EMPTY
    orderComponentState[0].percentAssembled == 0
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[0].qtyAndStateString, ['0/1', stateString])

    orderComponentState[1].sequence == 20
    orderComponentState[1].component == 'BOARD'

    orderComponentState[2].sequence == 30
    orderComponentState[2].component == 'CPU'

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that findComponentAssemblyState works with all components assembled - order"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'BOARD', 'DISK'])
    def comp1 = AssyUnitTestUtils.assembleComponent(order, [sequence: 10])
    def comp2 = AssyUnitTestUtils.assembleComponent(order, [sequence: 20])
    def comp3 = AssyUnitTestUtils.assembleComponent(order, [sequence: 30])

    and: 'the request local is set'
    GlobalUtils.defaultLocale = locale

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    def stateString = GlobalUtils.lookup("orderComponentState.FULL.label", locale)
    orderComponentState.size() == 3
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'CPU'
    orderComponentState[0].componentAndTitle == TypeUtils.toShortString(comp1.component, true)
    orderComponentState[0].qtyRequired == 1.0
    orderComponentState[0].qtyAssembled == 1.0
    orderComponentState[0].overallStateString == stateString
    orderComponentState[0].overallState == OrderComponentStateEnum.FULL
    orderComponentState[0].percentAssembled == 100
    orderComponentState[0].sequencesForRemoval.size() == 1
    orderComponentState[0].sequencesForRemoval[0] == comp1.sequence
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[0].qtyAndStateString, ['1/1', stateString])

    orderComponentState[1].sequence == 20
    orderComponentState[1].component == 'BOARD'
    orderComponentState[1].qtyAssembled == 2.0
    orderComponentState[1].overallStateString == GlobalUtils.lookup("orderComponentState.FULL.label")
    orderComponentState[1].overallState == OrderComponentStateEnum.FULL
    orderComponentState[1].percentAssembled == 100
    orderComponentState[1].sequencesForRemoval.size() == 1
    orderComponentState[1].sequencesForRemoval[0] == comp2.sequence
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[1].qtyAndStateString, ['2/2', stateString])

    orderComponentState[2].sequence == 30
    orderComponentState[2].component == 'DISK'
    orderComponentState[2].sequencesForRemoval.size() == 1
    orderComponentState[2].sequencesForRemoval[0] == comp3.sequence

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that findComponentAssemblyState works with all components assembled - lsn and lotSize greater than 1"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'BOARD', 'DISK'],
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY,
                                               qty: 3.0, lotSize: 3.0)
    def lsn = order.lsns[0] as LSN
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, lsn: lsn])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 20, lsn: lsn])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 30, lsn: lsn])

    and: 'the request local is set'
    GlobalUtils.defaultLocale = locale

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(lsn))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    def stateString = GlobalUtils.lookup("orderComponentState.FULL.label", locale)
    orderComponentState.size() == 3
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'CPU'
    orderComponentState[0].qtyRequired == 3.0
    orderComponentState[0].qtyAssembled == 3.0
    orderComponentState[0].overallStateString == stateString
    orderComponentState[0].overallState == OrderComponentStateEnum.FULL
    orderComponentState[0].percentAssembled == 100
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[0].qtyAndStateString, ['3/3', stateString])

    orderComponentState[1].sequence == 20
    orderComponentState[1].component == 'BOARD'
    orderComponentState[1].qtyAssembled == 6.0
    orderComponentState[1].overallStateString == stateString
    orderComponentState[1].overallState == OrderComponentStateEnum.FULL
    orderComponentState[1].percentAssembled == 100
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[1].qtyAndStateString, ['6/6', stateString])

    orderComponentState[2].sequence == 30
    orderComponentState[2].component == 'DISK'

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  def "verify that findComponentAssemblyState works with partial assembly - order"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'BOARD', 'DISK'])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, qty: 0.4])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 20, qty: 1.0])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 30, qty: 1.0])

    and: 'the request local is set'
    GlobalUtils.defaultLocale = locale

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    def stateString = GlobalUtils.lookup("orderComponentState.PARTIAL.label", locale)
    orderComponentState.size() == 3
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'CPU'
    orderComponentState[0].qtyRequired == 1.0
    orderComponentState[0].qtyAssembled == 0.4
    orderComponentState[0].overallStateString == stateString
    orderComponentState[0].overallState == OrderComponentStateEnum.PARTIAL
    orderComponentState[0].percentAssembled == 40
    def qtyString = "${NumberUtils.formatNumber(0.4, locale)}/1"
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[0].qtyAndStateString, [qtyString, stateString])

    orderComponentState[1].sequence == 20
    orderComponentState[1].component == 'BOARD'
    orderComponentState[1].qtyAssembled == 1.0
    orderComponentState[1].overallStateString == stateString
    orderComponentState[1].overallState == OrderComponentStateEnum.PARTIAL
    orderComponentState[1].percentAssembled == 50
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[1].qtyAndStateString, ['1/2', stateString])

    where:
    locale         | _
    Locale.US      | _
    Locale.GERMANY | _
  }

  @Rollback
  def "verify that findComponentAssemblyState works with partial assembly - lsn"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'BOARD', 'DISK'],
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0] as LSN
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, lsn: lsn, qty: 0.4])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 20, lsn: lsn, qty: 1.0])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 30, lsn: lsn, qty: 1.0])

    and: 'the request local is set'
    GlobalUtils.defaultLocale = Locale.US

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(lsn))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    def stateString = GlobalUtils.lookup("orderComponentState.PARTIAL.label", Locale.US)
    orderComponentState.size() == 3
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'CPU'
    orderComponentState[0].qtyRequired == 1.0
    orderComponentState[0].qtyAssembled == 0.4
    orderComponentState[0].overallStateString == stateString
    orderComponentState[0].overallState == OrderComponentStateEnum.PARTIAL
    orderComponentState[0].percentAssembled == 40
    orderComponentState[1].canBeAssembled
    orderComponentState[1].canBeRemoved
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[0].qtyAndStateString, ['0.4/1', stateString])

    orderComponentState[1].sequence == 20
    orderComponentState[1].component == 'BOARD'
    orderComponentState[1].qtyAssembled == 1.0
    orderComponentState[1].overallStateString == stateString
    orderComponentState[1].overallState == OrderComponentStateEnum.PARTIAL
    orderComponentState[1].percentAssembled == 50
    orderComponentState[1].canBeAssembled
    orderComponentState[1].canBeRemoved
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[1].qtyAndStateString, ['1/2', stateString])
  }

  @Rollback
  def "verify that findComponentAssemblyState works with some components assembled - order and hideAssembled=true"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'BOARD', 'DISK'])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 20, qty: 1.0])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 30])  // Fully Assembled

    when: 'the components are searched'
    def request = new FindComponentAssemblyStateRequest(order)
    request.hideAssembled = true
    def orderComponentState = service.findComponentAssemblyState(request)
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    orderComponentState.size() == 2
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'CPU'
    orderComponentState[0].qtyRequired == 1.0
    orderComponentState[0].qtyAssembled == 0.0
    orderComponentState[0].canBeAssembled
    !orderComponentState[0].canBeRemoved

    orderComponentState[1].sequence == 20
    orderComponentState[1].component == 'BOARD'
    orderComponentState[1].qtyAssembled == 1.0
    orderComponentState[1].canBeAssembled
    orderComponentState[1].canBeRemoved
  }

  @Rollback
  def "verify that findComponentAssemblyState works with some components assembled - lsn and hideAssembled=true"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'BOARD', 'DISK'],
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0] as LSN
    AssyUnitTestUtils.assembleComponent(order, [sequence: 20, lsn: lsn, qty: 1.0])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 30, lsn: lsn])  // Fully Assembled

    when: 'the components are searched'
    def request = new FindComponentAssemblyStateRequest(lsn)
    request.hideAssembled = true
    def orderComponentState = service.findComponentAssemblyState(request)
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    orderComponentState.size() == 2
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'CPU'
    orderComponentState[0].qtyRequired == 1.0
    orderComponentState[0].qtyAssembled == 0.0

    orderComponentState[1].sequence == 20
    orderComponentState[1].component == 'BOARD'
    orderComponentState[1].qtyAssembled == 1.0
  }

  @Rollback
  def "verify that findComponentAssemblyState works with all order assembled and lsn query"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'],
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0] as LSN
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10])

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(lsn))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    orderComponentState.size() == 1
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'CPU'
    orderComponentState[0].qtyRequired == 1.0
    orderComponentState[0].qtyAssembled == 1.0
  }

  @Rollback
  def "verify that findComponentAssemblyState works with mixed order and LSN components"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'],
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0] as LSN
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, qty: 0.4])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, lsn: lsn, qty: 0.6])

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(lsn))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    orderComponentState.size() == 1
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'CPU'
    orderComponentState[0].qtyRequired == 1.0
    orderComponentState[0].qtyAssembled == 1.0
  }

  @Rollback
  def "verify that findComponentAssemblyState works with no BOM and all non-BOM components"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder([:])

    and: 'a component that was assembled'
    def product = new Product(product: 'CPU', title: 'a title').save()
    def orderAssembledComponent = AssyUnitTestUtils.assembleComponent(order, [component: product])

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    orderComponentState.size() == 1
    orderComponentState[0].sequence != 0
    orderComponentState[0].component == 'CPU'
    orderComponentState[0].componentAndTitle == TypeUtils.toShortString(product, true)
    orderComponentState[0].qtyRequired == 0.0
    orderComponentState[0].qtyAssembled == 1.0
    orderComponentState[0].overallState == OrderComponentStateEnum.OVER
    orderComponentState[0].percentAssembled == 0
    orderComponentState[0].sequencesForRemoval.size() == 1
    orderComponentState[0].sequencesForRemoval[0] == orderAssembledComponent.sequence
    !orderComponentState[0].canBeAssembled
    orderComponentState[0].canBeRemoved
  }

  @Rollback
  def "verify that findComponentAssemblyState sorts when non-BOM components are found"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['DISK', 'CPU'])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 20])

    and: 'a component that was assembled'
    def product1 = new Product(product: 'EMBLEM').save()
    def product2 = new Product(product: 'DECAL').save()
    def product3 = new Product(product: 'PATTERN').save()
    AssyUnitTestUtils.assembleComponent(order, [component: product2])
    AssyUnitTestUtils.assembleComponent(order, [component: product1])
    AssyUnitTestUtils.assembleComponent(order, [component: product3])

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are in the correct order'
    orderComponentState.size() == 5
    orderComponentState[0].component == 'DISK'
    orderComponentState[1].component == 'CPU'
    orderComponentState[2].component == 'DECAL'          // non-BOMs are sorted by product, at the end of the list.
    orderComponentState[3].component == 'EMBLEM'
    orderComponentState[4].component == 'PATTERN'
  }

  @Rollback
  def "verify that findComponentAssemblyState gracefully handles no order or LSN - empty list result"() {
    when: 'the components are searched'
    def res = service.findComponentAssemblyState(null)

    then: 'the list is empty'
    res.size() == 0
  }

  @Rollback
  def "verify that findComponentAssemblyState displays assembly data as a string"() {
    given: 'a flex type for the assy data'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldCount: 2)

    and: 'a released order with components assembled'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])
    AssyUnitTestUtils.assembleComponent(order, [sequence          : 10, assemblyDataType: flexType,
                                                assemblyDataValues: [FIELD1: 'ACME', FIELD2: '2017103']])

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    orderComponentState.size() == 1
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'CPU'
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[0].assemblyDataAsString, ['ACME', '2017103'])
  }

  @Rollback
  def "verify that findComponentAssemblyState displays assembly data as a string - non BOM"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder()

    and: 'a component that was assembled'
    def product1 = new Product(product: 'EMBLEM').save()
    def orderAssyComp = AssyUnitTestUtils.assembleComponent(order, [component: product1])

    and: 'some old assy data is forced into the custom fields'
    orderAssyComp.fields = '{"FIELD1": "ACME"}'
    orderAssyComp.save()

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))

    then: 'the returned values are correct'
    orderComponentState.size() == 1
    orderComponentState[0].sequence == 10
    orderComponentState[0].component == 'EMBLEM'
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[0].assemblyDataAsString, ['FIELD1', 'ACME'])
  }

  @Rollback
  def "verify that findComponentAssemblyState displays assembly data as a string with multiple assy records - several"() {
    given: 'a flex type for the assy data'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY')

    and: 'a released order with components assembled'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])
    AssyUnitTestUtils.assembleComponent(order, [sequence          : 10,
                                                qty               : 0.4,
                                                assemblyDataType  : flexType,
                                                assemblyDataValues: [FIELD1: 'ACME1']])
    AssyUnitTestUtils.assembleComponent(order, [sequence          : 10,
                                                qty               : 0.6,
                                                assemblyDataType  : flexType,
                                                assemblyDataValues: [FIELD1: 'ACME2']])

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    orderComponentState.size() == 1
    orderComponentState[0].sequence == 10
    orderComponentState[0].qtyAssembled == 1.0
    orderComponentState[0].component == 'CPU'
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentState[0].assemblyDataAsString, ['ACME1', 'ACME2', ';'])
  }

  @Rollback
  def "verify that findComponentAssemblyState provides correct sequencesForRemoval with multiple assy records for on component"() {
    given: 'a flex type for the assy data'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY')

    and: 'a released order with components assembled'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])
    def comp1 = AssyUnitTestUtils.assembleComponent(order, [sequence          : 10,
                                                            qty               : 0.4,
                                                            assemblyDataType  : flexType,
                                                            assemblyDataValues: [FIELD1: 'ACME1']])
    def comp2 = AssyUnitTestUtils.assembleComponent(order, [sequence          : 10,
                                                            qty               : 0.6,
                                                            assemblyDataType  : flexType,
                                                            assemblyDataValues: [FIELD1: 'ACME2']])

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))
    //println "orderComponentState = $orderComponentState"

    then: 'the right removal sequences are provided'
    orderComponentState.size() == 1
    orderComponentState[0].sequence == 10
    orderComponentState[0].sequencesForRemoval.size() == 2
    orderComponentState[0].sequencesForRemoval[0] == comp1.sequence
    orderComponentState[0].sequencesForRemoval[1] == comp2.sequence

    and: 'the labels for the sequences are correct'
    !orderComponentState[0].removalLabels[0].contains('.label')
    orderComponentState[0].removalLabels[0] == service.formatForRemoval(comp1)
    orderComponentState[0].removalLabels[1] == service.formatForRemoval(comp2)
  }

  @Rollback
  def "verify that findComponentAssemblyState displays assembly data as a string with multiple assy records - several identical"() {
    given: 'a flex type for the assy data'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY')

    and: 'a released order with components assembled'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])
    AssyUnitTestUtils.assembleComponent(order, [sequence          : 10,
                                                qty               : 0.4,
                                                assemblyDataType  : flexType,
                                                assemblyDataValues: [FIELD1: 'ACME1']])
    AssyUnitTestUtils.assembleComponent(order, [sequence          : 10,
                                                qty               : 0.6,
                                                assemblyDataType  : flexType,
                                                assemblyDataValues: [FIELD1: 'ACME1']])

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    orderComponentState.size() == 1
    orderComponentState[0].sequence == 10
    orderComponentState[0].qtyAssembled == 1.0
    orderComponentState[0].component == 'CPU'

    and: 'only a single set of assy data values are in the string'
    def s = orderComponentState[0].assemblyDataAsString
    !s.contains(';')
    def loc = s.indexOf('ACME')
    s.indexOf('ACME', loc + 1) == -1
  }

  @Rollback
  def "verify that findComponentAssemblyState displays assembly data string is length limited"() {
    given: 'a flex type for the assy data'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY')

    and: 'a released order with components assembled'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])
    AssyUnitTestUtils.assembleComponent(order, [sequence          : 10,
                                                qty               : 0.2,
                                                assemblyDataType  : flexType,
                                                assemblyDataValues: [FIELD1: 'A' * 80]])
    AssyUnitTestUtils.assembleComponent(order, [sequence          : 10,
                                                qty               : 0.2,
                                                assemblyDataType  : flexType,
                                                assemblyDataValues: [FIELD1: 'B' * 80]])
    AssyUnitTestUtils.assembleComponent(order, [sequence          : 10,
                                                qty               : 0.2,
                                                assemblyDataType  : flexType,
                                                assemblyDataValues: [FIELD1: 'C' * 80]])
    AssyUnitTestUtils.assembleComponent(order, [sequence          : 10,
                                                qty               : 0.4,
                                                assemblyDataType  : flexType,
                                                assemblyDataValues: [FIELD1: 'D' * 80]])

    when: 'the components are searched'
    def orderComponentState = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))
    //println "orderComponentState = $orderComponentState"

    then: 'the returned values are correct'
    orderComponentState.size() == 1
    orderComponentState[0].sequence == 10
    orderComponentState[0].qtyAssembled == 1.0
    orderComponentState[0].component == 'CPU'

    and: 'only the first 3 sets of data are displayed - 240 chars'
    def s = orderComponentState[0].assemblyDataAsString
    s.contains(';')
    s.length() < 320
    s.endsWith('...')
    !s.contains('DDD')
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that removeComponent can mark a component as removed"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])

    and: 'both components are assembled'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 20])

    when: 'one component is marked as removed'
    setCurrentUser()
    def request = new RemoveOrderAssembledComponentRequest(order.assembledComponents[1], order)
    def response = service.removeComponent(request)

    then: 'the record is marked as removed'
    def orderAssembledComponent = OrderAssembledComponent.findByUuid(response.uuid)
    orderAssembledComponent == response
    orderAssembledComponent.state == AssembledComponentStateEnum.REMOVED
    orderAssembledComponent.removedByUserName == SecurityUtils.currentUserName
    UnitTestUtils.compareDates(orderAssembledComponent.removedDate, new Date())
  }

  def "verify that removeComponent fails with missing values"() {
    when: 'the method is called'
    service.removeComponent(request)

    then: 'a valid exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, [missingName.toString()])

    where:
    missingName | request
    'request'   | null
    'order'     | new RemoveOrderAssembledComponentRequest()
  }

  @Rollback
  def "verify that removeComponent fails with missing sequence"() {
    given: 'an order'
    def order = new Order(order: 'M1001').save()

    when: 'the method is called'
    //noinspection GroovyAssignabilityCheck
    service.removeComponent(new RemoveOrderAssembledComponentRequest(order: order.order))

    then: 'a valid exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['sequence'])
  }

  @Rollback
  def "verify that removeComponent fails with bad sequence"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])

    when: 'the method call is attempted with the wrong sequence'
    //noinspection GroovyAssignabilityCheck
    def request = new RemoveOrderAssembledComponentRequest(order: order.order, sequence: 247)
    service.removeComponent(request)

    then: 'a valid exception is thrown'
    def ex = thrown(BusinessException)
    UnitTestUtils.assertExceptionIsValid(ex, ['sequence', '247'], 10001)
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Rollback
  def "verify that removeComponent will fail when removing a component that was already removed"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])

    and: 'the component is assembled and removed'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10])
    def comp = order.assembledComponents[0] as OrderAssembledComponent
    comp.state = AssembledComponentStateEnum.REMOVED
    comp.removedByUserName = 'JOE'
    comp.removedDate = new Date() - 1
    comp.save()

    when: 'one component is marked as removed'
    setCurrentUser()
    def request = new RemoveOrderAssembledComponentRequest(comp, order)
    service.removeComponent(request)

    then: 'an exception is thrown'
    def ex = thrown(Exception)
    //error.10002.message=Component {0} has already been removed from order {1} by {2} at {3}
    UnitTestUtils.assertExceptionIsValid(ex, [order.order, 'JOE', comp.component.toString()], 10002)
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  @Rollback
  def "verify that undoComponentRemove can mark a component as re-assembled"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])

    and: 'both components are assembled'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 20])

    and: 'the component is removed'
    setCurrentUser()
    service.removeComponent(new RemoveOrderAssembledComponentRequest(order.assembledComponents[1], order))

    when: 'one component is marked as removed'
    def request = new ComponentRemoveUndoRequest(order.assembledComponents[1], order)
    def response = service.undoComponentRemove(request)

    then: 'the record is marked as removed'
    def orderAssembledComponent = OrderAssembledComponent.findByUuid(response.uuid)
    orderAssembledComponent == response
    orderAssembledComponent.state == AssembledComponentStateEnum.ASSEMBLED
    orderAssembledComponent.removedByUserName == null
    orderAssembledComponent.removedDate == null
  }

  def "verify that undoComponentRemove fails with missing values"() {
    when: 'the method is called'
    service.undoComponentRemove(request)

    then: 'a valid exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, [missingName.toString()])

    where:
    missingName | request
    'request'   | null
    'order'     | new ComponentRemoveUndoRequest()
  }

  @Rollback
  def "verify that undoComponentRemove fails with missing sequence"() {
    given: 'an order'
    def order = new Order(order: 'M1001').save()

    when: 'the method is called'
    //noinspection GroovyAssignabilityCheck
    service.removeComponent(new ComponentRemoveUndoRequest(order: order.order))

    then: 'a valid exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['sequence'])
  }

  @Rollback
  def "verify that undoComponentRemove fails with bad sequence"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])

    when: 'the method call is attempted with the wrong sequence'
    //noinspection GroovyAssignabilityCheck
    def request = new ComponentRemoveUndoRequest(order: order.order, sequence: 247)
    service.undoComponentRemove(request)

    then: 'a valid exception is thrown'
    def ex = thrown(BusinessException)
    UnitTestUtils.assertExceptionIsValid(ex, ['sequence', '247'], 10001)
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that undoComponentRemove will fail when removing a component that was already restored"() {
    given: 'a released order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])

    and: 'the component is assembled and removed'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10])
    def comp = order.assembledComponents[0]
    comp.save()

    when: 'one component is restored'
    def request = new ComponentRemoveUndoRequest(comp, order)
    service.undoComponentRemove(request)

    then: 'an exception is thrown'
    def ex = thrown(Exception)
    //error.10003.message=Component {0} has already been un-removed on order {1}
    UnitTestUtils.assertExceptionIsValid(ex, [order.order, comp.component.toString()], 10003)
  }

  @Ignore("Implement when search is available")
  def "verify that adjustQueryAssemblyData detects assembly data field shorthand"() {
    given: 'a flex type with an assembly data field'
    UnitTestUtils.buildFlexType('XYZ', ['LOT', 'VENDOR'])

    expect: 'the query string is adjusted correctly'
    service.adjustQueryAssemblyData(query, Order, query) == result

    where:
    query            | result
    'lot:abc*'       | 'order.assembledComponents.assemblyDataValues.LOT:abc*'
    'LOT:abc*'       | 'order.assembledComponents.assemblyDataValues.LOT:abc*'
    'assy.lot:abc*'  | 'order.assembledComponents.assemblyDataValues.LOT:abc*'
    'ASSY.lot:abc*'  | 'order.assembledComponents.assemblyDataValues.LOT:abc*'
    'assy.LOT:abc'   | 'order.assembledComponents.assemblyDataValues.LOT:abc'
    '"complex"'      | '"complex"'
    'notLot'         | 'notLot'
    'defect.LOT:abc' | 'defect.LOT:abc'
  }

}
