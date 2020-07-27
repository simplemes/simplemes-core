package org.simplemes.mes.app.system

import org.openqa.selenium.Keys
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.dashboard.controller.DashboardTestController
import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.mes.assy.demand.AssembledComponentStateEnum
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
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
 * Tests. Copied rom mes-assy module's ScanAssyDashboardGUISpec.
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

  /**
   * Basic test.  Copied from
   * @return
   */
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
    assembledComponents[0].getAssemblyDataValue('LOT') == '87929459'

    and: 'the message is displayed with the correct values'
    UnitTestUtils.assertContainsAllIgnoreCase(messages.text(), [order.order, 'WHEEL'])
  }


}