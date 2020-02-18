/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.task


import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.ControllerUtils

/**
 * Provides access to the controller details for building a task menu from the controllers.  THis
 * is a separate class to allow easier testing.
 */
class TaskMenuControllerUtils {
  /**
   * A singleton instance.  Makes it easier to mock for testing.
   */
  static TaskMenuControllerUtils instance = new TaskMenuControllerUtils()

  /**
   * Determines the core tasks from all defined controllers.  Checks all controllers for a property
   * taskMenuItems.
   * @return The task menus from all controllers.
   */
  List<TaskMenuItem> getCoreTasks() {
    def list = []
    def controllers = ControllerUtils.instance.allControllers
    for (clazz in controllers) {
      def controller = Holders.applicationContext.findOrInstantiateBean(clazz).get()
      //println "clazz = $clazz"

      //for (clazz in ControllerUtils.instance.allControllers) {
      //def o = clazz.newInstance()
      if (controller.hasProperty('taskMenuItems')) {
        def controllerItems = controller.taskMenuItems
        // Fill in the controllerClass value to simplify the code in the controller.
        //println "controllerItems = $controllerItems"
        controllerItems.each { it.controllerClass = clazz }
        list.addAll(controllerItems)
      }
    }
    return list
  }

}
