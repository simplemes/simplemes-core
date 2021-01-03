package org.simplemes.mes.assy.system


import org.openqa.selenium.Keys
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.dashboard.controller.DashboardTestController
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.mes.assy.demand.AssembledComponentStateEnum
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.assy.demand.page.AssemblyActivityWCDashboardPage
import org.simplemes.mes.assy.product.domain.ProductComponent
import org.simplemes.mes.assy.test.AssyUnitTestUtils
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.system.page.ScanDashboardPage
import org.simplemes.mes.tracking.domain.ActionLog
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
@SuppressWarnings(["GroovyUnusedDeclaration", "GroovyAssignabilityCheck"])
class ScanAssyDashboardGUISpec extends BaseDashboardSpecification {

  def workService

  /**
   * Semi-unique ID for records created by this test.
   */
  public static final String ID = 'SAD'

  static dirtyDomains = [ActionLog, Order, ProductComponent, Product, FlexType]

  /**
   * Sends a scan text to the client, followed by the terminator (TAB).
   * @param barcode The barcode string.
   */
  void sendScan(String barcode) {
    sendKey(barcode)
    sendKey(Keys.TAB)
  }

  def "verify that scan of component assembles it"() {
    given: 'a dashboard with the display page from the scan dashboard'
    buildDashboard(defaults: ['/scan/scanActivity', DashboardTestController.DISPLAY_EVENT_ACTIVITY])

    and: 'a released order'
    def flexType = DataGenerator.buildFlexType(fieldName: 'LOT')
    Order order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'], assemblyDataType: flexType)

    and: 'the dashboard is displayed'
    displayDashboard(page: ScanDashboardPage)

    when: 'the order ID is typed as if scanned by the user'
    sendScan(order.order)
    waitForDashboardEvent('ORDER_LSN_CHANGED')

    and: 'the component barcode is scanned'
    sendScan('^PRD^WHEEL^LOT^87929459')
    waitFor {
      messages.text().contains('WHEEL')
    }

    then: 'the assembly happened'
    def order2 = Order.findByOrder(order.order)
    def assembledComponents = order2.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 1
    assembledComponents[0].bomSequence == 10
    assembledComponents[0].component.product == 'WHEEL'
    assembledComponents[0].qty == 1.0
    assembledComponents[0].userName == 'admin'
    assembledComponents[0].state == AssembledComponentStateEnum.ASSEMBLED
    assembledComponents[0].getFieldValue('LOT') == '87929459'

    and: 'the message is displayed with the correct values'
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), [order.order, 'WHEEL'])
  }

  def "verify that scan of component assemble and the undo assembly works"() {
    given: 'a dashboard with the display page from the scan dashboard'
    buildDashboard(defaults: ['/scan/scanActivity', DashboardTestController.DISPLAY_EVENT_ACTIVITY])

    and: 'a released order'
    def flexType = DataGenerator.buildFlexType(fieldName: 'LOT')
    Order order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'], assemblyDataType: flexType)

    and: 'the dashboard is displayed'
    displayDashboard(page: ScanDashboardPage)

    when: 'the order ID is typed as if scanned by the user'
    sendScan(order.order)
    waitForDashboardEvent('ORDER_LSN_CHANGED')

    and: 'the component barcode is scanned'
    sendScan('^PRD^WHEEL^LOT^87929459')
    waitFor {
      messages.text().contains('WHEEL')
    }
    clearDashboardEvents()

    then: 'the undo button is enabled'
    undoButton.classes().contains('undo-button')

    when: 'the undo is clicked'
    undoButton.click()
    waitForDashboardEvent('ORDER_COMPONENT_STATUS_CHANGED')

    then: 'the remove happened'
    def order2 = Order.findByOrder(order.order)
    def assembledComponents = order2.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 1
    assembledComponents[0].state == AssembledComponentStateEnum.REMOVED

    and: 'the status change event is published'
    def event = getDashboardEvent('ORDER_COMPONENT_STATUS_CHANGED')
    event.order == order2.order
    event.component == 'WHEEL'

    and: 'the message is displayed with the correct values'
    messages.text() == GlobalUtils.lookup('reversedAssemble.message', 'WHEEL', order2.order)
  }

  def "verify that scan of wrong component fails"() {
    given: 'a dashboard with the display page from the scan dashboard'
    buildDashboard(defaults: ['/scan/scanActivity'])

    and: 'a released order'
    def flexType = DataGenerator.buildFlexType(fieldName: 'LOT')
    Order order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'], assemblyDataType: flexType)

    and: 'the dashboard is displayed'
    displayDashboard(page: ScanDashboardPage)

    when: 'the order ID is typed as if scanned by the user'
    sendScan(order.order)
    waitFor {
      messages.text()
    }

    and: 'the bad component barcode is scanned'
    def scanString = '^PRD^WHEEL-BAD^LOT^87929459'
    sendScan(scanString)
    waitFor {
      messages.text().contains('WHEEL')
    }

    then: 'no assembly happened'
    def order2 = Order.findByOrder(order.order)
    def assembledComponents = order2.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 0

    and: 'the message is displayed with the correct values'
    messages.text == lookup('scanDashboard.couldNotFind.message', null, scanString)
  }

  def "verify that scan of component without data will trigger assemble component dialog"() {
    given: 'a dashboard with the display page from the scan dashboard'
    buildDashboard(defaults: ['/scan/scanActivity', '/orderAssy/assemblyActivity',
                              DashboardTestController.DISPLAY_EVENT_ACTIVITY])

    and: 'a released order'
    def flexType = DataGenerator.buildFlexType()
    Order order = AssyUnitTestUtils.releaseOrder(components: ['WHEEL', 'SEAT'], assemblyDataType: flexType)

    and: 'the dashboard is displayed'
    displayDashboard(page: AssemblyActivityWCDashboardPage)

    when: 'the order ID is typed as if scanned by the user'
    sendScan(order.order)
    waitForDashboardEvent('ORDER_LSN_CHANGED')

    and: 'the component barcode is scanned'
    sendScan('WHEEL')
    waitFor {
      dialog0.title.contains('WHEEL')
    }

    then: 'the dialog is correct'
    def title = dialog0.title
    title.contains(order.order)
    title.contains('WHEEL')
    title.contains(flexType.flexType)

    when: 'a value is entered in the current focus field - the first data field'
    sendKey('768349034')
    sendKey(Keys.TAB)
    addField1.input.value() == '768349034'
    addAssembleButton.click()
    waitForNonZeroRecordCount(OrderAssembledComponent)

    then: 'the assembly happened'
    def order2 = Order.findByOrder(order.order)
    def assembledComponents = order2.assembledComponents as List<OrderAssembledComponent>
    assembledComponents.size() == 1
    assembledComponents[0].bomSequence == 10
    assembledComponents[0].component.product == 'WHEEL'
    assembledComponents[0].qty == 1.0
    assembledComponents[0].userName == 'admin'
    assembledComponents[0].state == AssembledComponentStateEnum.ASSEMBLED
    assembledComponents[0].getFieldValue('FIELD1') == '768349034'
  }


}