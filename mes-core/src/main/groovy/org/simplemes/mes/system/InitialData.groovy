package org.simplemes.mes.system

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.dashboard.domain.DashboardButton
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.dashboard.domain.DashboardPanel
import org.simplemes.eframe.dashboard.domain.DashboardPanelSplitter
import org.simplemes.eframe.security.domain.Role
import org.simplemes.eframe.security.domain.User

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A dummy domain class to add initial data load records to domains that are not in this class.
 * This domain class has no persistence itself.
 */
@Slf4j
@ToString(includePackage = false, includeNames = true)
class InitialData {

  /**
   * Forces MES Roles to be loaded before User, so the admin user will have these roles by default.
   */
  @SuppressWarnings("unused")
  static initialDataLoadBefore = [User]

  /**
   * A list of the records created by the initial data load.
   * Used only for test cleanup by {@link org.simplemes.eframe.test.BaseSpecification}.
   */
  static Map<String, List<String>> initialDataRecords = [Role           : ['SUPERVISOR', 'ENGINEER', 'LEAD', 'OPERATOR'],
                                                         DashboardConfig: ['OPERATOR_DEFAULT', 'MANAGER_DEFAULT']]

  /**
   * Loads initial data for domains provided by plugins.
   */
  static Map<String, List<String>> initialDataLoad() {
    createRoles()
    createDashboards()
    //createBackgroundTasks()
    return initialDataRecords
  }

  /**
   * Creates the roles needed for this core module.
   */
  static createRoles() {
    createRoleIfNeeded('SUPERVISOR', 'Supervisor')
    createRoleIfNeeded('ENGINEER', 'Engineer')
    createRoleIfNeeded('LEAD', 'Lead')
    createRoleIfNeeded('OPERATOR', 'Operator')
  }

  /**
   * Convenience method for initial data loading.
   * @param authority The Role name.
   * @param title The role's short title.
   */
  private static createRoleIfNeeded(String authority, String title) {
    if (!Role.findByAuthority(authority)) {
      new Role(authority: authority, title: title).save()
      log.debug("createRoleIfNeeded() Loaded Role: {}", authority)
    }
  }

  /**
   * If true, then this will force the dashboard loading in test mode.
   */
  static boolean forceDashboardLoad = false

  /**
   * Creates the default dashboards needed for this module.
   */
  static createDashboards() {
    if (Holders.environmentDev && Holders.configuration.appName.contains('mes-core')) {
      // Delete existing dashboards for testing in development mode.
      for (record in DashboardConfig.list()) {
        //println "deleting record = $record"
        record.delete()
      }
      log.warn("Removed default dashboards for development mode.")
    }

    def shouldLoad = (!Holders.environmentTest) || forceDashboardLoad

    if (DashboardConfig.count() == 0 && shouldLoad) {
      DashboardConfig dashboardConfig
      dashboardConfig = new DashboardConfig(dashboard: 'OPERATOR_DEFAULT', category: 'OPERATOR', title: 'Operator')
      dashboardConfig.splitterPanels << new DashboardPanelSplitter(panelIndex: 0, vertical: false)
      dashboardConfig.dashboardPanels << new DashboardPanel(panelIndex: 1, defaultURL: '/scan/scanActivity', parentPanelIndex: 0)
      dashboardConfig.dashboardPanels << new DashboardPanel(panelIndex: 2, defaultURL: '/workList/workListActivity', parentPanelIndex: 0)
      //dashboardConfig.addToButtons(new DashboardButton(label: 'reverseStart.label', url: '/work/reverseStartActivity', panel: 'A',
      //                                                 title: 'reverseStart.title', style: 'font-size:1.0em', buttonID: 'REVERSE_START'))
      dashboardConfig.buttons << new DashboardButton(label: 'complete.label', url: '/work/completeActivity', panel: 'A',
                                                     title: 'complete.title', css: 'caution-button', buttonID: 'COMPLETE')
      dashboardConfig.buttons << new DashboardButton(label: 'reverseComplete.label', url: '/work/reverseCompleteActivity', panel: 'A',
                                                     title: 'reverseComplete.title', buttonID: 'REVERSE_COMPLETE')
      dashboardConfig.save()
      log.warn("Created Dashboard ${dashboardConfig}.")

      dashboardConfig = new DashboardConfig(dashboard: 'MANAGER_DEFAULT', category: 'MANAGER', title: 'Manager')
      dashboardConfig.splitterPanels << new DashboardPanelSplitter(panelIndex: 0, vertical: false)
      dashboardConfig.dashboardPanels << new DashboardPanel(panelIndex: 1, defaultURL: '/selection/workCenterSelection', parentPanelIndex: 0)
      dashboardConfig.dashboardPanels << new DashboardPanel(panelIndex: 2, defaultURL: '/workList/workListActivity', parentPanelIndex: 0)
      dashboardConfig.buttons << new DashboardButton(label: 'start.label', url: '/work/startActivity', panel: 'A',
                                                     title: 'start.title', buttonID: 'START')
      dashboardConfig.buttons << new DashboardButton(label: 'complete.label', url: '/work/completeActivity', panel: 'A',
                                                     title: 'complete.title', buttonID: 'COMPLETE')
      dashboardConfig.buttons << new DashboardButton(label: 'reverseStart.label', url: '/work/reverseStartActivity', panel: 'A',
                                                     title: 'reverseStart.title', buttonID: 'REVERSE_START')
      dashboardConfig.save()
      log.warn("Created Dashboard ${dashboardConfig}.")
    }
  }


  /**
   * Creates the default background tasks needed.
   */
/*
  static createBackgroundTasks() {
    if (!BackgroundTask.findByTask(ProductionLogArchiveTask.TASK_NAME)) {
      def handler = new ProductionLogArchiveTask(ageDays: 180, batchSize: 500)
      def task = new BackgroundTask(task: ProductionLogArchiveTask.TASK_NAME, backgroundTaskHandler: handler)
      task.backgroundTaskHandlerData = handler.data
      task.runInterval = TimeIntervalEnum.DAYS_1
      task.save()

      log.info("Loaded Initial BackgroundTask: ${ProductionLogArchiveTask.TASK_NAME}")
    }
  }
*/

}
