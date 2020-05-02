package org.simplemes.mes.assy.demand.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseAPISpecification
import org.simplemes.eframe.test.ControllerTester
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.mes.assy.demand.AssembledComponentStateEnum
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.assy.test.AssyUnitTestUtils
import org.simplemes.mes.demand.LSNTrackingOption
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.tracking.domain.ActionLog

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OrderAssyControllerSpec extends BaseAPISpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, Order, Product, FlexType, WorkCenter]

  OrderAssyController controller

  def setup() {
    controller = Holders.getBean(OrderAssyController)
  }

  def "verify that the controller passes the standard controller test - security, etc"() {
    expect: 'the controller passes'
    ControllerTester.test {
      controller OrderAssyController
      role 'OPERATOR'
      taskMenu name: 'assemblyReport', uri: '/orderAssy/assemblyReport', folder: 'assembly:1000', displayOrder: 1010
      taskMenu name: 'componentReport', uri: '/orderAssy/componentReport', folder: 'assembly:1000', displayOrder: 1020
    }
  }

  def "verify addComponent with custom fields via HTTP API"() {
    given: 'a released order with components'
    def flexType = DataGenerator.buildFlexType(fieldCount: 2)
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'], assemblyDataType: flexType)

    and: 'a request in JSON format'
    def request = """{
      "order": "$order.order",
      "component": "CPU",
      "assemblyData": "$flexType.flexType",
      "assemblyData_FIELD1": "ACME-101",
      "assemblyData_FIELD2": "ACME-102"
    }
    """

    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: '/orderAssy/addComponent', method: 'post', content: request)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is valid'
    def orderAssembledComponent = Holders.objectMapper.readValue(res, OrderAssembledComponent)
    orderAssembledComponent.component.product == 'CPU'
    orderAssembledComponent.qty == 1.0

    and: 'the record is in the database'
    OrderAssembledComponent.withTransaction {
      def comp2 = OrderAssembledComponent.findByUuid(orderAssembledComponent.uuid)
      assert comp2.order == order
      assert comp2.getAssemblyDataValue('FIELD1') == 'ACME-101'
      assert comp2.getAssemblyDataValue('FIELD2') == 'ACME-102'
      true
    }
  }

  def "verify addComponent with LSN and optional fields via HTTP API"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'],
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY,)
    LSN lsn = order.lsns[0]
    def workCenter = null
    WorkCenter.withTransaction {
      workCenter = new WorkCenter(workCenter: 'CENTER-27').save()
    }

    and: 'a request in JSON format'
    def request = """{
      "order": "$order.order",
      "component": "CPU",
      "lsn": "$lsn.lsn",
      "workCenter": "$workCenter.workCenter",
      "location": "BIN-27"
    }
    """

    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: '/orderAssy/addComponent', method: 'post', content: request)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)
    json.lsn.lsn == lsn.lsn
    json.workCenter.workCenter == workCenter.workCenter
    json.location == "BIN-27"

    and: 'the record is in the database'
    OrderAssembledComponent.withTransaction {
      def comp2 = OrderAssembledComponent.findByUuid(UUID.fromString((String) json.uuid))
      assert comp2.lsn == lsn
      assert comp2.workCenter == workCenter
      assert comp2.location == "BIN-27"
      true
    }
  }

  def "verify addComponent gracefully fails with missing order via HTTP API"() {
    given: 'a request in JSON format'
    def request = """{
      "component": "CPU"
    }
    """

    and: 'the logging is disabled'
    disableStackTraceLogging()

    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: '/orderAssy/addComponent', method: 'post', content: request, status: HttpStatus.BAD_REQUEST)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is a bad request with a valid message'
    def json = new JsonSlurper().parseText(res)
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['component', 'CPU'])
  }

  def "verify findComponentAssemblyState works - order case"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])

    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: "/orderAssy/findComponentAssemblyState?order=${order.order}")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)

    and: 'result content is correct'
    json.total_count == 2
    json.fullyAssembled == false
    def orderComponentStates = json.data as List
    orderComponentStates.size() == 2

    orderComponentStates[0].component == 'CPU'
    orderComponentStates[0].sequence == 10
    orderComponentStates[0].overallState == 'EMPTY'
    orderComponentStates[1].component == 'MOTHERBOARD'
    orderComponentStates[1].sequence == 20
    orderComponentStates[1].overallState == 'EMPTY'
  }

  def "verify findComponentAssemblyState works with JSON input - lsn with components assembled"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'],
                                               lsnTrackingOption: LSNTrackingOption.LSN_ONLY)

    and: 'a flex type for the assy data'
    def flexType = DataGenerator.buildFlexType(fieldCount: 2)

    and: 'the components are assembled'
    LSN lsn = order.lsns[0]
    AssyUnitTestUtils.assembleComponent(order, [sequence          : 10, assemblyDataType: flexType, lsn: lsn,
                                                assemblyDataValues: [FIELD1: 'ACME_DEPOT', FIELD2: '2017103']])
    AssyUnitTestUtils.assembleComponent(order, [sequence          : 20, assemblyDataType: flexType, lsn: lsn,
                                                assemblyDataValues: [FIELD1: 'ACME_PRIME', FIELD2: '2016879']])

    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: "/orderAssy/findComponentAssemblyState?lsn=$lsn.lsn")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)

    and: 'result content is correct'
    json.total_count == 2
    def orderComponentStates = json.data as List
    orderComponentStates.size() == 2

    orderComponentStates[0].component == 'CPU'
    orderComponentStates[0].sequence == 10
    orderComponentStates[0].overallState == 'FULL'
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentStates[0].assemblyDataAsString, ['ACME_DEPOT', '2017103'])

    orderComponentStates[1].component == 'MOTHERBOARD'
    orderComponentStates[1].sequence == 20
    orderComponentStates[1].overallState == 'FULL'
    UnitTestUtils.assertContainsAllIgnoreCase(orderComponentStates[1].assemblyDataAsString, ['ACME_PRIME', '2016879'])
  }

  def "verify findComponentAssemblyState works with JSON input - hideAssembled"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])

    and: 'some components are assembled'
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10])

    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: "/orderAssy/findComponentAssemblyState?order=$order.order&hideAssembled=true")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)

    and: 'result content is correct'
    json.total_count == 1
    def orderComponentStates = json.data as List
    orderComponentStates.size() == 1

    orderComponentStates[0].component == 'MOTHERBOARD'
    orderComponentStates[0].sequence == 20
    orderComponentStates[0].overallState == 'EMPTY'
  }

  def "verify findComponentAssemblyState works with no demand value - fullyAssembled flag is not set for empty list"() {
    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: "/orderAssy/findComponentAssemblyState?order=")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)

    and: 'result content is correct'
    json.total_count == 0
    !json.fullyAssembled
  }

  def "verify findComponentAssemblyState sets fullyAssembled flag correctly when order has no components"() {
    given: 'a released order with no components'
    def order = AssyUnitTestUtils.releaseOrder([:])

    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: "/orderAssy/findComponentAssemblyState?order=$order.order")
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is valid'
    def json = new JsonSlurper().parseText(res)

    and: 'result content is correct'
    json.total_count == 0
    !json.fullyAssembled
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that removeComponent works with JSON input"() {
    given: 'a released order with an assembled component'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10])
    def component = order.assembledComponents[0] as OrderAssembledComponent

    and: 'a request in JSON format'
    def request = """{
      "order": "${order.order}",
      "sequence": $component.sequence
    }
    """

    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: '/orderAssy/removeComponent', method: 'post', content: request)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is correct'
    def json = new JsonSlurper().parseText(res)

    json.orderAssembledComponent.sequence == component.sequence
    json.orderAssembledComponent.state == AssembledComponentStateEnum.REMOVED.toString()
    // reversedAssemble.message=Removed component {0} from {1}.
    json.infoMsg == lookup('reversedAssemble.message', null, component.component.product, order.order)

    and: 'the records are saved in the DB and match the returned values'
    def orderAssembledComponent = OrderAssembledComponent.findByUuid(UUID.fromString(json.orderAssembledComponent.uuid))
    orderAssembledComponent.sequence == component.sequence
    orderAssembledComponent.state == AssembledComponentStateEnum.REMOVED

    and: 'the undo actions are populated'
    def undoAction = json.undoActions[0]
    undoAction.uri.contains('/orderAssy/undoComponentRemove')
    def undo = new JsonSlurper().parseText(undoAction.json)
    undo.order == order.order
    undo.sequence == orderAssembledComponent.sequence
  }

  def "verify that removeComponent gracefully fails with bad order"() {
    given: 'a request in JSON format'
    def request = """{
      "order": "gibberish",
      "sequence": 10
    }
    """

    and: 'the logging is disabled'
    disableStackTraceLogging()

    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: '/orderAssy/removeComponent', method: 'post', content: request, status: HttpStatus.BAD_REQUEST)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is correct'
    def json = new JsonSlurper().parseText(res)
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['order', 'gibberish'])
  }

  def "verify that removeComponent gracefully fails with bad sequence"() {
    given: 'a released order with an assembled component'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])

    and: 'a request in JSON format'
    def request = """{
      "order": "${order.order}",
      "sequence": Z
    }
    """

    and: 'the logging is disabled'
    disableStackTraceLogging()

    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: '/orderAssy/removeComponent', method: 'post', content: request, status: HttpStatus.BAD_REQUEST)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is correct'
    def json = new JsonSlurper().parseText(res)
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['Z'])
  }

  def "verify that undoComponentRemove works with JSON input"() {
    given: 'a released order with an assembled component'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU'])
    AssyUnitTestUtils.assembleComponent(order, [sequence: 10, removed: true])
    //noinspection GroovyAssignabilityCheck
    def component = order.assembledComponents[0] as OrderAssembledComponent

    and: 'a request in JSON format'
    def request = """{
      "order": "${order.order}",
      "sequence": $component.sequence
    }
    """

    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: '/orderAssy/undoRemoveComponent', method: 'post', content: request)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is correct'
    def json = new JsonSlurper().parseText(res)

    json.sequence == component.sequence
    json.state == AssembledComponentStateEnum.ASSEMBLED.toString()

    and: 'the records are saved in the DB and match the returned values'
    def orderAssembledComponent = OrderAssembledComponent.findByUuid(UUID.fromString((String) json.uuid))
    orderAssembledComponent.sequence == component.sequence
    orderAssembledComponent.state == AssembledComponentStateEnum.ASSEMBLED
  }

  def "verify that undoComponentRemove gracefully fails with bad order"() {
    given: 'a request in JSON format'
    def request = """{
      "order": "gibberish",
      "sequence": 10
    }
    """

    and: 'the logging is disabled'
    disableStackTraceLogging()

    when: 'the request is sent to the controller'
    login()
    def res = sendRequest(uri: '/orderAssy/undoRemoveComponent', method: 'post', content: request, status: HttpStatus.BAD_REQUEST)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(res)}"

    then: 'the response is correct'
    def json = new JsonSlurper().parseText(res)
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['order', 'gibberish'])
  }

  def "verify that assembleComponentDialog handles the simple case - order and bomSequence"() {
    given: 'a released order with components'
    def flexType = DataGenerator.buildFlexType(fieldCount: 2)
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'], assemblyDataType: flexType)

    when: 'the request is sent to the controller'
    def others = "&_panel=A&_variable=_A"
    login()
    def page = sendRequest(uri: "/orderAssy/assembleComponentDialog?order=$order.order&bomSequence=20$others")

    then: 'the response is valid'
    def qtyFieldLine = TextUtils.findLine(page, 'id: "qty"')
    JavascriptTestUtils.extractProperty(qtyFieldLine, 'value') == "2"

    // {view: "combo", id: "assemblyData", name: "assemblyData", value: "e02217a0-e495-4b5c-85be-73131d59f2d1" , editable: true  ,inputWidth: tk.pw("15em") ,options: [
    def assyDataFieldLine = TextUtils.findLine(page, 'id: "assemblyData"')
    JavascriptTestUtils.extractProperty(assyDataFieldLine, 'value') == "${flexType.uuid}"
  }

  def "verify that resolveComponent handles the case - order and bomSequence"() {
    given: 'a released order with components'
    def flexType = DataGenerator.buildFlexType(fieldCount: 2)
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'], assemblyDataType: flexType)

    when: 'the request is sent to the controller'
    def orderComp = controller.resolveComponent([order: order.order, bomSequence: '20'])

    then: 'the resolved response is valid'
    orderComp.component.product == 'MOTHERBOARD'
    orderComp.qty == 2.0
    orderComp.assemblyData == flexType
  }

  def "verify that resolveComponent handles the case - order and component"() {
    given: 'a released order with components'
    def flexType = DataGenerator.buildFlexType(fieldCount: 2)
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'], assemblyDataType: flexType)

    when: 'the request is sent to the controller'
    def orderComp = controller.resolveComponent([order: order.order, component: 'MOTHERBOARD'])

    then: 'the resolved response is valid'
    orderComp.component.product == 'MOTHERBOARD'
    orderComp.qty == 2.0
    orderComp.assemblyData == flexType
  }

  def "verify that resolveComponent handles the case - order and no sequence/component"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])

    when: 'the request is sent to the controller'
    controller.resolveComponent([order: order.order])

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['component', order.order])
  }

  def "verify that resolveComponent handles the case - no order"() {
    given: 'a released order with components'
    AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])

    when: 'the request is sent to the controller'
    controller.resolveComponent([:])

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['order'])
  }

  def "verify that resolveComponent handles the case - order and wrong sequence"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])

    when: 'the request is sent to the controller'
    controller.resolveComponent([order: order.order, bomSequence: 237])

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['component', order.order, '237'])
  }

  def "verify that resolveComponent handles the case - order and wrong component"() {
    given: 'a released order with components'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])

    when: 'the request is sent to the controller'
    controller.resolveComponent([order: order.order, component: 'gibberish'])

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['component', order.order, 'gibberish'])
  }

}
