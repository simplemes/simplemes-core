package sample.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.Controller
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.controller.BaseCrudRestController
import org.simplemes.eframe.web.task.TaskMenuItem
import sample.domain.RMA

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Handles the controller actions for the Order objects.  Includes CRUD actions on the user.
 * <p>
 * Not shipped with the framework.
 */
@Slf4j
@Secured("MANAGER")
@Controller("/rma")
@SuppressWarnings("unused")
class RMAController extends BaseCrudRestController {

  static domainClass = RMA

  /**
   * Defines the entry(s) in the main Task Menu.
   */
  def taskMenuItems = [new TaskMenuItem(folder: 'sample:9500', name: 'RMA', uri: '/rma',
                                        displayOrder: 9715, clientRootActivity: true)]


  /**
   * Determines the view to display for the given method.  This can be overridden in your controller class to
   * use a different naming scheme.<p>
   * This sub-class points to a sample directory.
   * @param methodName The method that needs the view (e.g. 'index').
   * @return The resulting view path.
   */
  @Override
  String getView(String methodName) {
    return "sample/rma/$methodName"
  }
}