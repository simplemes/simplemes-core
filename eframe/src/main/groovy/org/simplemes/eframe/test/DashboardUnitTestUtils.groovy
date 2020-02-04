/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import org.simplemes.eframe.dashboard.domain.DashboardButton
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.dashboard.domain.DashboardPanel
import org.simplemes.eframe.dashboard.domain.DashboardPanelSplitter

/**
 * Utility methods to help test Dashboard features.
 *
 *
 */
class DashboardUnitTestUtils {
  /**
   * Defines a dashboard config for testing.
   * <b>Note:</b> You should cleanup these records with the static variable:
   * <pre>
   * static domainClassesToClearOnEachRun = [DashboardConfig]
   * </pre>
   * @param dashboardName The dashboard name.
   * @param defaultPages The default pages for each panel (if starts with 'vertical' or 'horizontal', then creates a splitter with later panels).
   * @param buttons The buttons.  An Array of Maps with elements that match the DashboardButton fields: [[label:'',url:'page1', panel: 'A']]
   * @return The configuration.
   */
  static DashboardConfig buildDashboardConfig(String dashboardName, List<String> defaultPages, List<Map> buttons = null) {
    DashboardConfig dashboardConfig = new DashboardConfig(dashboard: dashboardName)
    int index = 0
    int splitterIndex = 0
    int lastSplitterIndex = -1
    for (page in defaultPages) {
      if (page.startsWith('vertical') || page.startsWith('horizontal')) {
        boolean vertical = page.startsWith('vertical')
        def splitter = new DashboardPanelSplitter(panelIndex: splitterIndex, vertical: vertical, parentPanelIndex: lastSplitterIndex)
        lastSplitterIndex = splitterIndex
        dashboardConfig.splitterPanels << splitter
        splitterIndex++
      } else {
        if (page == '') {
          page = null
        }
        def panel = new DashboardPanel(panelIndex: index, defaultURL: page, parentPanelIndex: lastSplitterIndex)
        dashboardConfig.dashboardPanels << panel
        index++
      }
    }
    // Now, create the buttons (if any)
    for (button in buttons) {
      // Must handle child Map separately since in functional (GEB) test mode, the child activities are not saved.
      //button.activities = null
      def dashboardButton = new DashboardButton(button)
      dashboardConfig.buttons << dashboardButton
    }

    dashboardConfig.save()
    //println "dashboardConfig = ${dashboardConfig.toString()}"
    //println "dashboardConfig = ${dashboardConfig.hierarchyToString()}"
    return dashboardConfig
  }

}
