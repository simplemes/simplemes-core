/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard

import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.dashboard.domain.DashboardButton
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.dashboard.domain.DashboardPanel
import org.simplemes.eframe.dashboard.domain.DashboardPanelSplitter
import org.simplemes.eframe.system.DemoDataLoaderInterface

import javax.inject.Singleton

/**
 * Used to test the loading of demo data - Test dashboards using mockup activities.
 */
@Slf4j
@Singleton
class DashboardDemoDataLoader implements DemoDataLoaderInterface {
  /**
   * Loads the demo data.  Only loads for the EFrame test application.
   *
   * <h3>Results Map</h3>
   * The elements in the list of Maps includes:
   * <ul>
   *   <li><b>name</b> - The name of the data loaded.  Usually a Class.simpleName. </li>
   *   <li><b>count</b> - The number of records actually loaded. </li>
   *   <li><b>possible</b> - The number of records that could be potentially loaded. </li>
   *   <li><b>uri</b> - The URI to a definition page (if available) that will show the the loaded records (e.g. '/flexType'). </li>
   * </ul>
   *
   * @return A list of Maps with the elements defined above.
   */
  @Override
  List<Map<String, Object>> loadDemoData() {
    def res = []

    def possible = 3
    def count = 0
    def name = DashboardConfig.simpleName
    def uri = '/dashboard'
    if (Holders.configuration.appName == 'EFrame') {
      if (!DashboardConfig.findByDashboard('SUPERVISOR_DEFAULT')) {
        DashboardConfig dashboardConfig
        dashboardConfig = new DashboardConfig(dashboard: 'SUPERVISOR_DEFAULT', category: 'SUPERVISOR', title: 'Supervisor')
        dashboardConfig.splitterPanels << (new DashboardPanelSplitter(panelIndex: 0, vertical: false))
        dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 1, defaultURL: '/test/dashboard/page?view=sample/dashboard/wcSelection', parentPanelIndex: 0))
        dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 2, defaultURL: '/order/orderWorkList', parentPanelIndex: 0))
        dashboardConfig.save()

        count++
        log.info("loadDemoData(): Loaded {}", dashboardConfig)
      }
      if (!DashboardConfig.findByDashboard('OPERATOR_DEFAULT')) {
        DashboardConfig dashboardConfig
        dashboardConfig = new DashboardConfig(dashboard: 'OPERATOR_DEFAULT', category: 'OPERATOR', title: 'Operator')
        dashboardConfig.splitterPanels << (new DashboardPanelSplitter(panelIndex: 0, vertical: false))
        dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 1, parentPanelIndex: 0,
                                                               defaultURL: '/test/dashboard/page?view=sample/dashboard/wcSelection'))
        dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 2, parentPanelIndex: 0,
                                                               defaultURL: '/test/dashboard/page?view=sample/dashboard/workList'))
        def button1 = new DashboardButton(label: 'pass.label', url: '/dashSample/display?page=pass', panel: 'A',
                                          title: 'pass.title', size: 1.5, buttonID: 'PASS')
        def button2 = new DashboardButton(label: 'Complete', url: '/test/dashboard/page?view=sample/dashboard/complete', panel: 'B',
                                          buttonID: 'COMPLETE')
        def button3 = new DashboardButton(label: 'Log Failure', url: '/test/dashboard/page?view=sample/dashboard/logFailure', panel: 'B',
                                          css: 'caution-button', buttonID: 'FAIL')
        def button4 = new DashboardButton(label: 'reports.label', url: '/report/reportActivity', panel: 'A',
                                          buttonID: 'REPORTS')
        dashboardConfig.buttons << (button1)
        dashboardConfig.buttons << (button2)
        dashboardConfig.buttons << (button3)
        dashboardConfig.buttons << (button4)
        dashboardConfig.save()

        count++
        log.info("loadDemoData(): Loaded {}", dashboardConfig)
      }
      if (!DashboardConfig.findByDashboard('MANAGER_DEFAULT')) {
        DashboardConfig dashboardConfig
        dashboardConfig = new DashboardConfig(dashboard: 'MANAGER_DEFAULT', category: 'MANAGER', title: 'Manager')
        dashboardConfig.splitterPanels << (new DashboardPanelSplitter(panelIndex: 0, vertical: false))
        dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 1, defaultURL: '/test/dashboard/page?view=sample/dashboard/wcSelection', parentPanelIndex: 0))
        dashboardConfig.splitterPanels << (new DashboardPanelSplitter(panelIndex: 2, vertical: true, parentPanelIndex: 0))
        dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 3, defaultURL: '/test/dashboard/page?view=sample/dashboard/workList', parentPanelIndex: 2))
        dashboardConfig.dashboardPanels << (new DashboardPanel(panelIndex: 6, defaultURL: '/test/dashboard/page?view=sample/dashboard/workList', parentPanelIndex: 2))
        dashboardConfig.save()

        count++
        log.info("loadDemoData(): Loaded {}", dashboardConfig)
      }
    }
    res << [name: name, uri: uri, count: count, possible: possible]

    return res
  }


}
