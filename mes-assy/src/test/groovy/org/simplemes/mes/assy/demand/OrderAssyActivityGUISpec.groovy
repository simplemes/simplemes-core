package org.simplemes.mes.assy.demand

import groovy.json.JsonSlurper
import org.openqa.selenium.Keys
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.dashboard.controller.DashboardTestController
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.assy.demand.page.AssemblyActivityWCDashboardPage
import org.simplemes.mes.assy.demand.service.OrderAssyService
import org.simplemes.mes.assy.test.AssyUnitTestUtils
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.tracking.domain.ActionLog
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests for the dashboard assembly activity.
 */
@IgnoreIf({ !sys['geb.env'] })
class OrderAssyActivityGUISpec extends BaseDashboardSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, Order, Product, FlexType]

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that the activity is displayed in the dashboard correctly"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection', '/orderAssy/assemblyActivity'])

    and: 'an order with components and its expected state'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'])
    def service = Holders.getBean(OrderAssyService)
    def orderComponentStates = service.findComponentAssemblyState(new FindComponentAssemblyStateRequest(order))

    when: 'the dashboard is displayed'
    displayDashboard(page: AssemblyActivityWCDashboardPage)

    // No errors and the list is empty
    assert messages.text() == ''
    assert componentList.cell(0, 0).text() == ''

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)
    sendKey(Keys.TAB)

    then: 'the component list is updated'
    waitFor {
      componentList.rows(0).size() > 0
    }

    and: 'the table contains the correct values'
    componentList.rows(0).size() == orderComponentStates.size()
    componentList.headers[0].text() == lookup('sequence.label')
    componentList.headers[1].text() == lookup('componentAndTitle.label')
    componentList.headers[2].text() == lookup('assemblyDataAsString.label')
    componentList.headers[3].text() == lookup('qtyAndStateString.label')

    orderComponentStates[0].componentAndTitle == componentList.cell(0, 1).text()
    orderComponentStates[0].assemblyDataAsString == componentList.cell(0, 2).text()
    orderComponentStates[0].qtyAndStateString == componentList.cell(0, 3).text()
  }

  @SuppressWarnings(["GroovyAssignabilityCheck", "AbcMetric"])
  def "verify that the assembleComponentDialog works"() {
    given: 'a dashboard with the activity'
    def flexType = DataGenerator.buildFlexType()
    buildDashboard(defaults: ['/selection/workCenterSelection', '/orderAssy/assemblyActivity',
                              DashboardTestController.DISPLAY_EVENT_ACTIVITY])

    and: 'an order with components and its expected state'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'], assemblyDataType: flexType)

    when: 'the dashboard is displayed'
    displayDashboard(page: AssemblyActivityWCDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)
    sendKey(Keys.TAB)

    waitFor {
      componentList.rows(0).size() > 0
    }

    and: 'the assemble button is clicked on the second row'
    addButton(1).click()
    waitFor {
      dialog0.displayed
    }

    then: 'the dialog title is correct'
    def title = dialog0.title
    title.contains(order.order)
    title.contains('MOTHERBOARD')
    title.contains(flexType.flexType)

    and: 'the dialog contents are correct'
    addQtyField.label == lookup('qty.label')
    addQtyField.input.value() == '2'
    addField1.label == 'FIELD1'

    when: 'the values are filled in'
    addQtyField.input.value(NumberUtils.formatNumber(1.2))
    addField1.input.value('XYZZY')
    sendKey(Keys.TAB)

    and: 'the component is saved'
    addAssembleButton.click()
    waitForNonZeroRecordCount(OrderAssembledComponent)

    then: 'the record is correct in the DB'
    def list = OrderAssembledComponent.list()
    list.size() == 1
    def orderAssembledComponent = list[0]
    orderAssembledComponent.component.product == 'MOTHERBOARD'
    orderAssembledComponent.bomSequence == 20
    orderAssembledComponent.qty == 1.2
    orderAssembledComponent.getAssemblyDataValue('FIELD1') == 'XYZZY'

    and: 'the list is updated'
    waitFor {
      componentList.cell(1, 3).text().contains(NumberUtils.formatNumber(1.2))
    }

    and: 'the event is triggered'
    def s = TextUtils.findLine($('#events').text(), 'ORDER_COMPONENT_STATUS_CHANGED')
    def json = new JsonSlurper().parseText(s)
    json.type == 'ORDER_COMPONENT_STATUS_CHANGED'
    json.source == '/orderAssy/assemblyActivity'
    json.order == order.order
    json.component == 'MOTHERBOARD'
  }

  @SuppressWarnings(["GroovyAssignabilityCheck", "AbcMetric"])
  def "verify that the removeComponentDialog works"() {
    given: 'a dashboard with the activity'
    def flexType = DataGenerator.buildFlexType()
    buildDashboard(defaults: ['/selection/workCenterSelection', '/orderAssy/assemblyActivity',
                              DashboardTestController.DISPLAY_EVENT_ACTIVITY])

    and: 'an order with components and its expected state'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'], assemblyDataType: flexType)
    AssyUnitTestUtils.assembleComponent(order, [sequence: 20, assemblyDataType: flexType, assemblyDataValues: [FIELD1: 'ACME-SEAT']])

    when: 'the dashboard is displayed'
    displayDashboard(page: AssemblyActivityWCDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)
    sendKey(Keys.TAB)

    waitFor {
      componentList.rows(0).size() > 0
    }

    and: 'the remove button is clicked on the second row - first row with a remove button'
    removeButton(0).click()
    waitFor {
      dialog0.displayed
    }

    then: 'the dialog title is correct'
    dialog0.title == lookup('removeComponent.title', null, order.order)

    and: 'the dialog contents are correct'
    removeCheckBox(1).value() == 'on'
    def comp1Text = removeCheckBoxText(1)
    comp1Text.contains('MOTHERBOARD')
    comp1Text.contains('ACME-SEAT')
    comp1Text.contains('FIELD1')

    when: 'the remove is triggered'
    removeRemoveButton.click()
    waitForNonZeroRecordCount(OrderAssembledComponent)

    and: 'the list is updated'
    waitFor {
      componentList.cell(1, 3).text().contains(lookup('orderComponentState.EMPTY.label'))
    }

    then: 'the record is correct in the DB'
    def list = OrderAssembledComponent.list()
    list.size() == 1
    def orderAssembledComponent = list[0]
    orderAssembledComponent.state == AssembledComponentStateEnum.REMOVED


    and: 'the event is triggered'
    def s = TextUtils.findLine($('#events').text(), 'ORDER_COMPONENT_STATUS_CHANGED')
    def json = new JsonSlurper().parseText(s)
    json.type == 'ORDER_COMPONENT_STATUS_CHANGED'
    json.source == '/orderAssy/assemblyActivity'
    json.order == order.order
    json.component == 'MOTHERBOARD'
  }

  @SuppressWarnings(["GroovyAssignabilityCheck", "AbcMetric"])
  def "verify that the removeComponentDialog works - multiple checkbox with all checked"() {
    given: 'a dashboard with the activity'
    def flexType = DataGenerator.buildFlexType()
    buildDashboard(defaults: ['/selection/workCenterSelection', '/orderAssy/assemblyActivity',
                              DashboardTestController.DISPLAY_EVENT_ACTIVITY])

    and: 'an order with components and its expected state'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'], assemblyDataType: flexType)
    def comp1 = AssyUnitTestUtils.assembleComponent(order, [bomSequence     : 20, sequence: 20, qty: 0.1,
                                                            assemblyDataType: flexType, assemblyDataValues: [FIELD1: 'ACME-SEAT1']])
    def comp2 = AssyUnitTestUtils.assembleComponent(order, [bomSequence     : 20, sequence: 21, qty: 0.13,
                                                            assemblyDataType: flexType, assemblyDataValues: [FIELD1: 'ACME-SEAT2']])

    when: 'the dashboard is displayed'
    displayDashboard(page: AssemblyActivityWCDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)
    sendKey(Keys.TAB)

    waitFor {
      componentList.rows(0).size() > 0
    }

    and: 'the remove button is clicked on the second row - first row with a remove button'
    removeButton(0).click()
    waitFor {
      dialog0.displayed
    }

    then: 'the dialog title is correct'
    dialog0.title == lookup('removeComponent.title', null, order.order)

    and: 'the checkboxes are correct'
    removeCheckBox(1).value() == 'on'
    def comp1Text = removeCheckBoxText(1)
    comp1Text.contains('MOTHERBOARD')
    comp1Text.contains('ACME-SEAT1')
    comp1Text.contains('FIELD1')

    removeCheckBox(2).value() == 'on'
    def comp2Text = removeCheckBoxText(2)
    comp2Text.contains('MOTHERBOARD')
    comp2Text.contains(NumberUtils.formatNumber(0.13))
    comp2Text.contains('ACME-SEAT2')
    comp2Text.contains('FIELD1')

    when: 'the remove is triggered'
    removeRemoveButton.click()
    waitFor {
      def c = OrderAssembledComponent.findByUuid(comp1.uuid)
      c.state != comp1.state
    }
    //waitForRecordChange(comp1)

    then: 'the records are correct in the DB'
    def comp1A = OrderAssembledComponent.findByUuid(comp1.uuid)
    comp1A.state == AssembledComponentStateEnum.REMOVED
    def comp2A = OrderAssembledComponent.findByUuid(comp2.uuid)
    comp2A.state == AssembledComponentStateEnum.REMOVED
  }

  @SuppressWarnings(["GroovyAssignabilityCheck", "AbcMetric"])
  def "verify that the removeComponentDialog works - multiple checkbox with only one checked"() {
    given: 'a dashboard with the activity'
    def flexType = DataGenerator.buildFlexType()
    buildDashboard(defaults: ['/selection/workCenterSelection', '/orderAssy/assemblyActivity',
                              DashboardTestController.DISPLAY_EVENT_ACTIVITY])

    and: 'an order with components and its expected state'
    def order = AssyUnitTestUtils.releaseOrder(components: ['CPU', 'MOTHERBOARD'], assemblyDataType: flexType)
    def comp1 = AssyUnitTestUtils.assembleComponent(order, [bomSequence     : 20, sequence: 20, qty: 0.1,
                                                            assemblyDataType: flexType, assemblyDataValues: [FIELD1: 'ACME-SEAT1']])
    def comp2 = AssyUnitTestUtils.assembleComponent(order, [bomSequence     : 20, sequence: 21, qty: 0.13,
                                                            assemblyDataType: flexType, assemblyDataValues: [FIELD1: 'ACME-SEAT2']])

    when: 'the dashboard is displayed'
    displayDashboard(page: AssemblyActivityWCDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)
    sendKey(Keys.TAB)

    waitFor {
      componentList.rows(0).size() > 0
    }

    and: 'the remove button is clicked on the second row - first row with a remove button'
    removeButton(0).click()
    waitFor {
      dialog0.displayed
    }

    and: 'the second component is un-checked'
    removeCheckBox(2).value('off')

    and: 'the remove is triggered'
    removeRemoveButton.click()
    waitFor {
      def c = OrderAssembledComponent.findByUuid(comp1.uuid)
      c.state != comp1.state
    }

    then: 'the removed record is changed in the DB'
    def comp1A = OrderAssembledComponent.findByUuid(comp1.uuid)
    comp1A.state == AssembledComponentStateEnum.REMOVED

    and: 'the unchecked component is left assembled'
    def comp2A = OrderAssembledComponent.findByUuid(comp2.uuid)
    comp2A.state == AssembledComponentStateEnum.ASSEMBLED
  }

}
