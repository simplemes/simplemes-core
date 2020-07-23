package org.simplemes.mes.assy.system.service

import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.MockBean
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.assy.demand.AssembledComponentAction
import org.simplemes.mes.assy.demand.AssembledComponentStateEnum
import org.simplemes.mes.assy.demand.DisplayAssembleDialogAction
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.assy.test.AssyUnitTestUtils
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.system.ScanRequest
import org.simplemes.mes.system.ScanResponse
import org.simplemes.mes.system.service.ScanService

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ScanAssyServiceSpec extends BaseSpecification {

  ScanAssyService service


  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def setup() {
    service = Holders.getBean(ScanAssyService)
    setCurrentUser()
  }

  def "verify postGetBarcodePrefixMapping adds the modules barcode prefixes"() {
    when: "the method is called"
    def map = [orig: 'original']
    service.postGetBarcodePrefixMapping(map)

    then: 'the result map is updated'
    map.PRD == ScanAssyService.BARCODE_PRODUCT
    map.LOT == ScanAssyService.BARCODE_LOT
    map.SN == ScanAssyService.BARCODE_SERIAL
    map.VND == ScanAssyService.BARCODE_VENDOR

    and: 'the original elements are left as-is'
    map.orig == 'original'
  }

  def "verify the mes-core getBarcodePrefixMapping calls this services extension"() {
    when: "the method is called"
    def scanService = Holders.getBean(ScanService)
    def map = scanService.barcodePrefixMapping

    then: 'the result map has this modules prefixes'
    map.PRD == ScanAssyService.BARCODE_PRODUCT
    map.LOT == ScanAssyService.BARCODE_LOT
    map.SN == ScanAssyService.BARCODE_SERIAL
    map.VND == ScanAssyService.BARCODE_VENDOR
  }

  def "verify that the mes-core scan method calls this service extension post method"() {
    given: 'a mocked bean that implements the interface'
    def scanRequest = new ScanRequest(barcode: 'GIBBERISH')
    def mock = Mock(ScanAssyService)
    new MockBean(this, ScanAssyService, [mock]).install()

    when: 'the extension is triggered'
    def scanService = Holders.getBean(ScanService)
    scanService.scan(scanRequest)

    then: 'the extension method is called'
    1 * mock.postScan(_ as ScanResponse, scanRequest)
  }

  @Rollback
  def "verify determineOrderComponent works with simple case"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'])
    //println "order = $order.components"

    when: "the order component is found"
    def orderComponent = service.determineOrderComponent(new ScanRequest(order: order), 'WHEEL')

    then: 'the order component is the correct one'
    orderComponent.sequence == 10
    orderComponent.component.product == 'WHEEL'
  }

  @Rollback
  def "verify determineOrderComponent sort works with multiple records for one component - nothing previously assembled"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT', 'WHEEL'], reverseSequences: true)
    //println "order = $order.components"

    when: "the order component is found"
    def orderComponent = service.determineOrderComponent(new ScanRequest(order: order), 'WHEEL')

    then: 'the order component is the correct one'
    orderComponent.sequence == 10
    orderComponent.component.product == 'WHEEL'
  }

  @Rollback
  def "verify determineOrderComponent sort works with multiple records for one component - one previously assembled"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT', 'WHEEL'])
    //println "order = $order.components"

    and: 'the first wheel is assembled fully'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10])

    when: "the order component is found"
    def orderComponent = service.determineOrderComponent(new ScanRequest(order: order), 'WHEEL')

    then: 'the order component is the correct one'
    orderComponent.sequence == 30
    orderComponent.component.product == 'WHEEL'
  }

  @Rollback
  def "verify determineOrderComponent works with multiple records for one component - one partially assembled"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT', 'WHEEL'])
    //println "order = $order.components"

    and: 'the first wheel is assembled fully'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, qty: 0.4])

    when: "the order component is found"
    def orderComponent = service.determineOrderComponent(new ScanRequest(order: order), 'WHEEL')

    then: 'the order component is the correct one'
    orderComponent.sequence == 10
    orderComponent.component.product == 'WHEEL'
  }

  @Rollback
  def "verify determineOrderComponent works with multiple records for one component - all fully assembled"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT', 'WHEEL'])
    //println "order = $order.components"

    and: 'all wheels are assembled fully'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 30])

    when: "the order component is found"
    def orderComponent = service.determineOrderComponent(new ScanRequest(order: order), 'WHEEL')

    then: 'the order component is the correct one'
    orderComponent.sequence == 10
    orderComponent.component.product == 'WHEEL'
  }

  @Rollback
  def "verify determineOrderComponent works with order qty of 2.2"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(qty: 2.2, components: ['WHEEL', 'SEAT', 'WHEEL'])
    //println "order = $order.components"

    and: 'all wheels are assembled fully'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, qty: 1.1])

    when: "the order component is found"
    def orderComponent = service.determineOrderComponent(new ScanRequest(order: order), 'WHEEL')

    then: 'the order component is the correct one'
    orderComponent.sequence == 10
    orderComponent.component.product == 'WHEEL'
  }

  @Rollback
  def "verify determineOrderComponent works with multiple records for one component - another LSN was assembled"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(qty: 2, components: ['WHEEL', 'SEAT', 'WHEEL'],
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    LSN lsn1 = order.lsns[0]
    LSN lsn2 = order.lsns[1]

    and: 'the first wheel is assembled fully'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, lsn: lsn1])

    when: "the order component is found"
    def orderComponent1 = service.determineOrderComponent(new ScanRequest(order: order, lsn: lsn1), 'WHEEL')

    then: 'the order component is the correct one for the first LSN'
    orderComponent1.sequence == 30
    orderComponent1.component.product == 'WHEEL'

    when: "the order component is found"
    def orderComponent2 = service.determineOrderComponent(new ScanRequest(order: order, lsn: lsn2), 'WHEEL')

    then: 'the order component is the correct one for the second LSN - second WHEEL component'
    orderComponent2.sequence == 10
    orderComponent2.component.product == 'WHEEL'
  }

  @Rollback
  def "verify determineOrderComponent works with multiple records for one component - LSN is partially assembled at second comp"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(qty: 2, components: ['WHEEL', 'SEAT', 'WHEEL'],
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    LSN lsn1 = order.lsns[0]
    LSN lsn2 = order.lsns[1]

    and: 'the first wheel is assembled fully and second is partially'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, lsn: lsn1])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 30, lsn: lsn1, qty: 0.6])

    when: "the order component is found"
    def orderComponent1 = service.determineOrderComponent(new ScanRequest(order: order, lsn: lsn1), 'WHEEL')

    then: 'the order component is the correct one for the first LSN'
    orderComponent1.sequence == 30
    orderComponent1.component.product == 'WHEEL'

    when: "the other LSN is unaffected"
    def orderComponent2 = service.determineOrderComponent(new ScanRequest(order: order, lsn: lsn2), 'WHEEL')

    then: 'the order component is the correct one for the second LSN - second WHEEL component'
    orderComponent2.sequence == 10
    orderComponent2.component.product == 'WHEEL'
  }

  @Rollback
  def "verify determineOrderComponent works with multiple records for one component - LSN qty is larger than 1.0"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(qty: 4.0, components: ['WHEEL', 'SEAT', 'WHEEL'],
                                               lotSize: 2.0,
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    LSN lsn1 = order.lsns[0]

    and: 'the first wheel is partially assembled'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, lsn: lsn1, qty: 1.0])

    when: "the order component is found"
    def orderComponent1 = service.determineOrderComponent(new ScanRequest(order: order, lsn: lsn1), 'WHEEL')

    then: 'the order component is the correct one for the LSN'
    orderComponent1.sequence == 10
    orderComponent1.component.product == 'WHEEL'
  }

  @Rollback
  def "verify postScan assembles when passed a barcode with component and lot"() {
    given: 'an order'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldName: 'LOT')
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'], assemblyDataType: flexType)

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, component: 'WHEEL', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the component is assembled with the right data'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 1
    assembledComponents[0].order == order
    assembledComponents[0].bomSequence == 10
    assembledComponents[0].component.product == 'WHEEL'
    assembledComponents[0].qty == 1.0
    assembledComponents[0].userName == SecurityUtils.instance.currentUserName
    assembledComponents[0].state == AssembledComponentStateEnum.ASSEMBLED

    and: 'the assembly data is correct'
    assembledComponents[0].getAssemblyDataValue('LOT') == '12345'

    and: 'the scan was resolved'
    response.resolved
  }

  @Rollback
  def "verify postScan assembles when passed a barcode with component and lot - field name wrong case"() {
    given: 'an order'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldName: 'lot')
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'], assemblyDataType: flexType)

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, component: 'WHEEL', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the component is assembled with the right data'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 1
    response.resolved
  }

  @Rollback
  def "verify that postScan already resolved scans are ignored"() {
    given: 'an order'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldName: 'LOT')
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'], assemblyDataType: flexType)

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, component: 'WHEEL', lot: '12345')

    and: 'it is resolved'
    response.resolved = true

    when: "the post method is called"
    service.postScan(response, request)

    then: 'no are assembled'
    OrderAssembledComponent.list().size() == 0

    and: 'the scan was resolved'
    response.resolved
  }

  @Rollback
  def "verify that postScan with a second copy of the component in the order components is found"() {
    given: 'an order'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldName: 'LOT')
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT', 'WHEEL'], assemblyDataType: flexType)

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, component: 'WHEEL', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the component is assembled with the right data'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 1
    assembledComponents[0].bomSequence == 10

    when: 'a second scan of the same component is processed'
    ScanRequest request2
    ScanResponse response2
    (request2, response2) = AssyUnitTestUtils.buildRequestAndResponse(order: order, component: 'WHEEL', lot: '45678')

    and: "the post method is called"
    service.postScan(response2, request2)

    then: 'the component is assembled with the right data'
    def assembledComponents2 = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents2.size() == 2
    assembledComponents2[1].bomSequence == 30
    assembledComponents2[1].getAssemblyDataValue('LOT') == '45678'

    and: 'the scan was resolved'
    response.resolved
  }

  @Rollback
  def "verify postScan assembles when passed a barcode with request for an LSN"() {
    given: 'an order with an LSN'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldName: 'LOT')
    def order = AssyUnitTestUtils.releaseOrder(qty: 1, components: ['WHEEL', 'SEAT'],
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY,
                                               assemblyDataType: flexType)
    def lsn = order.lsns[0]

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(lsn: lsn, component: 'WHEEL', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the component is assembled with the right data'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 1
    assembledComponents[0].order == order
    assembledComponents[0].lsn == lsn

    and: 'the assembly data is correct'
    assembledComponents[0].getAssemblyDataValue('LOT') == '12345'

    and: 'the scan was resolved'
    response.resolved
  }

  @Rollback
  def "verify postScan assembles when passed a barcode with request for an LSN mixed with previous order-based assembly"() {
    given: 'an order with an LSN'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldName: 'LOT')
    def order = AssyUnitTestUtils.releaseOrder(qty: 1, components: ['WHEEL', 'SEAT', 'WHEEL'],
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY,
                                               assemblyDataType: flexType)
    def lsn = order.lsns[0]

    and: 'some partial qty is assembled for the order'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, qty: 0.4])

    and: 'some qty is assembled for the LSN'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, lsn: lsn])

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(lsn: lsn, component: 'WHEEL', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the component is assembled for the LSN on the second BOM component'
    def order2 = Order.findByUuid(order.uuid)
    def assembledComponents = order2.assembledComponents as List<OrderAssembledComponent>
    def secondAssembledComponent = assembledComponents.find { it.bomSequence == 30 }
    secondAssembledComponent.order == order
    secondAssembledComponent.lsn == lsn

    and: 'the scan was resolved'
    response.resolved
  }

  @Rollback
  def "verify postScan assembles when passed a barcode with component but no assembly data type for the component"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'])

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, component: 'WHEEL', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the component is assembled with the right data'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 1
    assembledComponents[0].bomSequence == 10
    assembledComponents[0].component.product == 'WHEEL'
    assembledComponents[0].qty == 1.0

    and: 'the scan was resolved'
    response.resolved
  }

  @Rollback
  def "verify postScan assembles when passed a barcode with component but no BOM components for the order"() {
    given: 'a component that will not be used on the order'
    new Product(product: 'WHEEL').save()

    and: 'an order'
    def order = AssyUnitTestUtils.releaseOrder()
    //println "order = $order"

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, component: 'WHEEL', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the component is assembled with the right data'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 1
    assembledComponents[0].bomSequence == 0
    assembledComponents[0].component.product == 'WHEEL'
    assembledComponents[0].qty == 1.0

    and: 'the scan was resolved'
    response.resolved
  }

  @Rollback
  def "verify postScan fails to assemble when passed a barcode with component that does not exist"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder()
    //println "order = $order"

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, component: 'WHEEL', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'nothing is assembled'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 0

    and: 'the scan was not resolved'
    !response.resolved
  }

  @Rollback
  def "verify postScan assembles and returns correct scanAction for an order"() {
    given: 'an order'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldName: 'LOT')
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'], assemblyDataType: flexType)

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, component: 'WHEEL', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'scan action tells the client that a component was assembled'
    AssembledComponentAction assembledAction = response.scanActions.find() {
      it.type == AssembledComponentAction.TYPE_ORDER_COMPONENT_STATUS_CHANGED
    }
    assembledAction.order == order.order
    assembledAction.component == 'WHEEL'
    assembledAction.bomSequence == 10
  }

  @Rollback
  def "verify postScan assembles and returns correct scanAction for an LSN"() {
    given: 'an order'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldName: 'LOT')
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'], assemblyDataType: flexType,
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    LSN lsn = order.lsns[0]

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, lsn: lsn, component: 'WHEEL', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'scan action tells the client that a component was assembled'
    AssembledComponentAction assembledAction = response.scanActions.find() {
      it.type == AssembledComponentAction.TYPE_ORDER_COMPONENT_STATUS_CHANGED
    }
    assembledAction.order == order.order
    assembledAction.lsn == lsn.lsn
    assembledAction.component == 'WHEEL'
    assembledAction.bomSequence == 10
  }

  @Rollback
  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify postScan assembles and returns correct message for an order"() {
    given: 'an order'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldName: 'LOT')
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'], assemblyDataType: flexType)

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, component: 'WHEEL', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'scan messages indicate the order was assembled'
    response.messageHolder.level == MessageHolder.LEVEL_INFO
    UnitTestUtils.assertContainsAllIgnoreCase(response.messageHolder.text, ['assembled', order.order, 'WHEEL'])

    and: 'the undo action is correct'
    response.undoActions.size() == 1
    def undoAction = response.undoActions[0]
    def json = new JsonSlurper().parseText(undoAction.json)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(undoAction.json)}"
    json.sequence == order.assembledComponents[0].sequence
    json.order == order.order
  }

  @Rollback
  def "verify postScan assembles and returns correct message for an LSN"() {
    given: 'an order'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldName: 'LOT')
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'], assemblyDataType: flexType,
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY)
    def lsn = order.lsns[0]

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, lsn: lsn, component: 'WHEEL', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'scan messages indicate the order was assembled'
    response.messageHolder.level == MessageHolder.LEVEL_INFO
    UnitTestUtils.assertContainsAllIgnoreCase(response.messageHolder.text, ['assembled', lsn.lsn, 'WHEEL'])
  }

  @Rollback
  def "verify postScan assembles when passed a simple component barcode"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'])

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(barcode: 'WHEEL', order: order)

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the response is correct'
    response.scanActions.size() == 1
    def assembledComponentAction = response.scanActions[0] as AssembledComponentAction
    assembledComponentAction.order == order.order
    assembledComponentAction.component == 'WHEEL'
    assembledComponentAction.bomSequence == 10

    and: 'the component is assembled'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 1
    assembledComponents[0].order == order
    assembledComponents[0].bomSequence == 10
    assembledComponents[0].component.product == 'WHEEL'
    assembledComponents[0].qty == 1.0
    assembledComponents[0].userName == SecurityUtils.currentUserName
    assembledComponents[0].state == AssembledComponentStateEnum.ASSEMBLED
  }

  @Rollback
  def "verify postScan assembles when passed a simple component barcode that is not on the BOM, but is a Product"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'])

    and: 'a product not on the BOM'
    new Product(product: 'PEDAL').save()

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(barcode: 'PEDAL', order: order)

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the response is correct'
    response.scanActions.size() == 1
    def assembledComponentAction = response.scanActions[0] as AssembledComponentAction
    assembledComponentAction.order == order.order
    assembledComponentAction.component == 'PEDAL'
    assembledComponentAction.bomSequence == 0

    and: 'the component is assembled'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 1
    assembledComponents[0].order == order
    assembledComponents[0].bomSequence == 0
    assembledComponents[0].component.product == 'PEDAL'
    assembledComponents[0].qty == 1.0
    assembledComponents[0].userName == SecurityUtils.currentUserName
    assembledComponents[0].state == AssembledComponentStateEnum.ASSEMBLED
  }

  @Rollback
  def "verify postScan does nothing when passed a simple component barcode that is not a Product"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'])

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(barcode: 'PEDAL', order: order)

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the response is correct'
    !response.resolved
    response.scanActions.size() == 0

    and: 'no components are assembled'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 0
  }

  @Rollback
  def "verify postScan does nothing when passed a structured component barcode that is not a Product"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'])

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, component: 'NON_PRODUCT', lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the response is correct'
    !response.resolved
    response.scanActions.size() == 0

    and: 'no components are assembled'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 0
  }

  @Rollback
  def "verify postScan does nothing when passed a structured component barcode that does not contain a Product"() {
    given: 'an order'
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'])

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, lot: '12345')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the response is correct'
    !response.resolved
    response.scanActions.size() == 0

    and: 'no components are assembled'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 0
  }

  @Rollback
  def "verify postScan for simple barcode with assembly data type triggers an DisplayAssembleDialogAction event for the client"() {
    given: 'an order with a component and flex type'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldName: 'LOT')
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL'], assemblyDataType: flexType)

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(barcode: 'WHEEL', order: order)

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the response is correct'
    response.scanActions.size() == 1
    response.resolved
    def displayAssembleDialogAction = response.scanActions[0] as DisplayAssembleDialogAction
    displayAssembleDialogAction.order == order.order
    displayAssembleDialogAction.component == 'WHEEL'
    displayAssembleDialogAction.assemblyData == 'ASSEMBLY'
    displayAssembleDialogAction.assemblyDataUuid == flexType.uuid.toString()
    displayAssembleDialogAction.firstAssemblyDataField == 'LOT'
    displayAssembleDialogAction.bomSequence == 10
    displayAssembleDialogAction.type == DisplayAssembleDialogAction.TYPE_DISPLAY_ASSEMBLE_DIALOG

    and: 'no components are assembled'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 0
  }

  @Rollback
  def "verify postScan for structured barcode with wrong assembly data type values triggers dialog on client"() {
    given: 'an order with a component and flex type'
    def flexType = DataGenerator.buildFlexType(flexType: 'ASSEMBLY', fieldName: 'LOT')
    def order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL'], assemblyDataType: flexType)

    and: 'a scan request and response as given to the postScan method'
    ScanRequest request
    ScanResponse response
    (request, response) = AssyUnitTestUtils.buildRequestAndResponse(order: order, vendor: 'ACME', component: 'WHEEL')

    when: "the post method is called"
    service.postScan(response, request)

    then: 'the response is correct'
    response.scanActions.size() == 1
    def displayAssembleDialogAction = response.scanActions[0] as DisplayAssembleDialogAction
    displayAssembleDialogAction.order == order.order
    displayAssembleDialogAction.component == 'WHEEL'
    displayAssembleDialogAction.bomSequence == 10

    and: 'no components are assembled'
    def assembledComponents = order.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 0
  }
}
