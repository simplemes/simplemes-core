package org.simplemes.mes.system

import groovy.util.logging.Slf4j
import org.simplemes.eframe.dashboard.domain.DashboardButton
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.dashboard.domain.DashboardPanel
import org.simplemes.eframe.dashboard.domain.DashboardPanelSplitter
import org.simplemes.eframe.system.DemoDataLoaderInterface

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

    res.addAll(loadManagerDashboard())
    res.addAll(loadScanDashboard())

    return res
  }


  /**
   * Loads the a traditional dashboard with default activities.
   *
   * @return A list of Maps with the elements defined above.
   */
  List<Map<String, Object>> loadManagerDashboard() {
    def res = []

    def possible = 1
    def count = 0
    def name = "$DashboardConfig.simpleName - Traditional"
    def uri = '/dashboard'

    if (!DashboardConfig.findByDashboard('TRADITIONAL')) {
      def dashboardConfig = new DashboardConfig(dashboard: 'TRADITIONAL', category: 'MANAGER', title: 'Traditional')
      dashboardConfig.splitterPanels << new DashboardPanelSplitter(panelIndex: 0, vertical: false)
      dashboardConfig.dashboardPanels << new DashboardPanel(panelIndex: 1, defaultURL: '/selection/workCenterSelection', parentPanelIndex: 0)
      dashboardConfig.dashboardPanels << new DashboardPanel(panelIndex: 2, defaultURL: '/workList/workListActivity', parentPanelIndex: 0)
      dashboardConfig.buttons << new DashboardButton(label: 'start.label', url: '/work/startActivity', panel: 'A',
                                                     title: 'start.title', buttonID: 'START')
      dashboardConfig.buttons << new DashboardButton(label: 'complete.label', url: '/work/completeActivity', panel: 'A',
                                                     title: 'complete.title', buttonID: 'COMPLETE')
      dashboardConfig.buttons << new DashboardButton(label: 'reverseStart.label', url: '/work/reverseStartActivity', panel: 'A',
                                                     title: 'reverseStart.title', buttonID: 'REVERSE_START')
      dashboardConfig.buttons << new DashboardButton(label: 'reverseComplete.label', url: '/work/reverseCompleteActivity', panel: 'A',
                                                     title: 'reverseComplete.title', buttonID: 'REVERSE_COMPLETE')
      dashboardConfig.save()
      count++
      log.warn("Created Dashboard ${dashboardConfig}.")
    }
    res << [name: name, uri: uri, count: count, possible: possible]


    return res
  }

  /**
   * Loads the a scan-based dashboard with default activities.
   *
   * @return A list of Maps with the elements defined above.
   */
  List<Map<String, Object>> loadScanDashboard() {
    def res = []

    def possible = 1
    def count = 0
    def name = "$DashboardConfig.simpleName - Scan"
    def uri = '/dashboard'

    if (!DashboardConfig.findByDashboard('SCAN')) {
      DashboardConfig dashboardConfig
      dashboardConfig = new DashboardConfig(dashboard: 'SCAN', category: 'OPERATOR', title: 'Scan Dashboard')
      dashboardConfig.splitterPanels << new DashboardPanelSplitter(panelIndex: 0, vertical: false)
      dashboardConfig.dashboardPanels << new DashboardPanel(panelIndex: 1, defaultURL: '/scan/scanActivity', parentPanelIndex: 0)
      dashboardConfig.dashboardPanels << new DashboardPanel(panelIndex: 2, defaultURL: '/workList/workListActivity', parentPanelIndex: 0)
      dashboardConfig.buttons << new DashboardButton(label: 'complete.label', url: '/work/completeActivity', panel: 'A',
                                                     title: 'complete.title', css: 'caution-button', buttonID: 'COMPLETE')
      dashboardConfig.buttons << new DashboardButton(label: 'reverseComplete.label', url: '/work/reverseCompleteActivity', panel: 'A',
                                                     title: 'reverseComplete.title', buttonID: 'REVERSE_COMPLETE')
      dashboardConfig.save()
      count++
      log.warn("Created Dashboard ${dashboardConfig}.")
    }

    res << [name: name, uri: uri, count: count, possible: possible]

    return res
  }
}
