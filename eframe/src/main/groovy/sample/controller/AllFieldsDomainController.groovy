package sample.controller

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.Controller
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.controller.BaseCrudRestController
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
class AllFieldsDomainController extends BaseCrudRestController {

  /**
   * Defines the entry(s) in the main Task Menu.
   */
  @SuppressWarnings("unused")
  def taskMenuItems = [new TaskMenuItem(folder: 'sample:9500', name: 'allFieldsDomain', uri: '/allFieldsDomain', displayOrder: 9520, clientRootActivity: true),
                       new TaskMenuItem(folder: 'sample:9500', name: 'javascriptTester', uri: '/javascriptTester?s=ef.alert();', displayOrder: 9610),
                       new TaskMenuItem(folder: 'sample:9500', name: 'pageTester', uri: '/pageTester', displayOrder: 9620)]


  /*
    <li><a href="/javascriptTester?s=ef.alert('Hi');">Javascript Tester</a></li>
  <li><a href="/pageTester">pageTester</a></li>

   */

  /**
   * Determines the view to display for the given method.  <p>
   * This sample class uses the views from the sample folder.
   * @param methodName The method that needs the view (e.g. 'index').
   * @return The resulting view path.
   */
  @Override
  String getView(String methodName) {
    return "sample/allFieldsDomain/$methodName"
  }
}