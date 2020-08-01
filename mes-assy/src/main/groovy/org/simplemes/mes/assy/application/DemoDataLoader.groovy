package org.simplemes.mes.assy.application

import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.domain.FlexField
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.dashboard.domain.DashboardButton
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.dashboard.domain.DashboardPanel
import org.simplemes.eframe.dashboard.domain.DashboardPanelSplitter
import org.simplemes.eframe.system.DemoDataLoaderInterface
import org.simplemes.mes.assy.product.domain.ProductComponent
import org.simplemes.mes.product.domain.Product

import javax.inject.Singleton

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Loads some demo data for the Assy module.
 */
@Slf4j
@Singleton
class DemoDataLoader implements DemoDataLoaderInterface {
  /**
   * Loads the demo data.  Loads a FlexType (LOT) and some Products to build a Bike.  Also loads a dashboard config for
   * Assembly scanning.
   *
   * <h3>Results Map</h3>
   * The elements in the list of Maps includes:
   * <ul>
   *   <li><b>name</b> - The name of the data loaded.  Usually a Class.simpleName. </li>
   *   <li><b>count</b> - The number of records actually loaded. </li>
   *   <li><b>total</b> - The number of records that could be potentially loaded. </li>
   *   <li><b>uri</b> - The URI to a definition page (if available) that will show the the loaded records (e.g. '/flexType'). </li>
   * </ul>
   *
   * @return A list of Maps with the elements defined above.
   */
  @Override
  List<Map<String, Object>> loadDemoData() {
    def res = []

    res.addAll(loadProductDemoData())
    res.addAll(loadScanAssyDashboard())

    return res
  }

  /**
   * Loads the demo data for Products.  Loads a FlexType (LOT) and some Products to build a Bike.
   *
   *
   * @return A list of Maps with the elements defined above.
   */
  List<Map<String, Object>> loadProductDemoData() {
    def res = []

    def possible = 1
    def count = 0
    def name = FlexType.simpleName
    def uri = '/flexType'
    def flexType = FlexType.findByFlexType('LOT')
    if (!flexType) {
      flexType = new FlexType(flexType: 'LOT', title: "Lot Only Collected")
      flexType.fields << new FlexField(fieldName: 'LOT', fieldLabel: 'lot.label')
      flexType.save()
      count++
      log.info("loadDemoData(): Loaded {}", flexType)
    }
    res << [name: name, uri: uri, count: count, possible: possible]

    possible = 3
    count = 0
    name = Product.simpleName
    uri = '/product'
    if (!Product.findByProduct('SEAT')) {
      def record = new Product(product: 'SEAT', title: 'Bike Seat - Adult', lotSize: 10.0)
      record.assemblyDataType = flexType
      record.save()
      count++
      log.info("loadDemoData(): Loaded {}", record)
    }
    if (!Product.findByProduct('WHEEL-27')) {
      def record = new Product(product: 'WHEEL-27', title: '27" Wheel', lotSize: 2.0)
      record.assemblyDataType = flexType
      record.save()
      count++
      log.info("loadDemoData(): Loaded {}", record)
    }
    if (!Product.findByProduct('BIKE-27')) {
      def seat = Product.findByProduct('SEAT')
      def wheel = Product.findByProduct('WHEEL-27')

      def record = new Product(product: 'BIKE-27', title: '27" Bike')
      record.components << new ProductComponent(component: seat, sequence: 10, qty: 1.0)
      record.components << new ProductComponent(component: wheel, sequence: 20, qty: 2.0)
      record.save()
      count++
      log.info("loadDemoData(): Loaded {}", record)
    }
    res << [name: name, uri: uri, count: count, possible: possible]

    return res
  }

  /**
   * Loads the a scan-based dashboard with default activities.
   *
   * @return A list of Maps with the elements defined above.
   */
  List<Map<String, Object>> loadScanAssyDashboard() {
    def res = []

    def possible = 1
    def count = 0
    def name = "$DashboardConfig.simpleName - Scan Assembly"
    def uri = '/dashboard'

    if (!DashboardConfig.findByDashboard('SCAN_ASSY')) {
      DashboardConfig dashboardConfig
      dashboardConfig = new DashboardConfig(dashboard: 'SCAN_ASSY', category: 'OPERATOR', title: 'Scan Assembly')
      dashboardConfig.splitterPanels << new DashboardPanelSplitter(panelIndex: 0, vertical: false)
      dashboardConfig.dashboardPanels << new DashboardPanel(panelIndex: 1, defaultURL: '/scan/scanActivity', parentPanelIndex: 0)
      dashboardConfig.dashboardPanels << new DashboardPanel(panelIndex: 2, defaultURL: '/orderAssy/assemblyActivity', parentPanelIndex: 0)
      dashboardConfig.buttons << new DashboardButton(label: 'complete.label', url: '/work/completeActivity', panel: 'A',
                                                     title: 'complete.title', css: 'caution-button', buttonID: 'COMPLETE')
      dashboardConfig.buttons << new DashboardButton(label: 'reverseComplete.label', url: '/work/reverseCompleteActivity', panel: 'A',
                                                     title: 'reverseComplete.title', buttonID: 'REVERSE_COMPLETE')
      dashboardConfig.save()
      count++
      log.info("Created Dashboard ${dashboardConfig}.")
    }

    res << [name: name, uri: uri, count: count, possible: possible]

    return res
  }

}
