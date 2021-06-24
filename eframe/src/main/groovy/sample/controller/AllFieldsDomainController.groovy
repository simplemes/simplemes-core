package sample.controller

import groovy.transform.CompileStatic
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
 * Handles the controller actions for the AllFieldsDomain objects.  Includes CRUD actions on the AllFieldsDomain sample
 * test domain.
 */
@CompileStatic
@Slf4j
@Secured("ADMIN")
@Controller("/allFieldsDomain")
class AllFieldsDomainController extends BaseCrudController2 {

  /**
   * Defines the entry(s) in the main Task Menu.
   */
  @SuppressWarnings("unused")
  def taskMenuItems = [new TaskMenuItem(folder: 'sample:9500', name: 'allFieldsDomain', uri: '/allFieldsDomain', displayOrder: 9520, clientRootActivity: true),
                       new TaskMenuItem(folder: 'sample:9500', name: 'javascriptTester', uri: '/javascriptTester?s=ef.alert();', displayOrder: 9610),
                       new TaskMenuItem(folder: 'sample:9500', name: 'pageTester', uri: '/pageTester', displayOrder: 9620)]


  /**
   * The location of the index page.
   */
  String indexView = 'client/sample/allFieldsDomain'

}