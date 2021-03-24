package org.simplemes.eframe.custom.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.Controller
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.controller.BaseCrudController2
import org.simplemes.eframe.web.task.TaskMenuItem

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Handles the controller actions for the Flex Type objects.  Includes CRUD actions on the flex types.
 */
@Slf4j
@Secured("CUSTOMIZER")
@Controller("/flexType")
class FlexTypeController extends BaseCrudController2 {
  /**
   * Defines the entry(s) in the main Task Menu.
   */
  @SuppressWarnings("unused")
  def taskMenuItems = [new TaskMenuItem(folder: 'custom:100', name: 'flexType', uri: '/flexType', displayOrder: 110, clientRootActivity: true)]

  /**
   * The location of the index page.
   */
  String indexView = 'client/eframe/flexType'


}