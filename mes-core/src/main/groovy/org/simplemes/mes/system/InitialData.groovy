package org.simplemes.mes.system

import groovy.transform.ToString
import groovy.util.logging.Slf4j
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
  static Map<String, List<String>> initialDataRecords = [Role           : ['SUPERVISOR', 'ENGINEER', 'LEAD', 'OPERATOR']]

  /**
   * Loads initial data for domains provided by plugins.
   */
  static Map<String, List<String>> initialDataLoad() {
    createRoles()
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
