package sample.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.controller.BaseCrudController2
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.web.task.TaskMenuItem
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Handles the controller actions for the SampleParent objects.  Includes CRUD actions on the user.
 * <p>
 * Not shipped with the framework.
 */
@Slf4j
@Secured("MANAGER")
@Controller("/sampleParent")
class SampleParentController extends BaseCrudController2 {

  static domainClass = SampleParent

  /**
   * Defines the entry(s) in the main Task Menu.
   */
  @SuppressWarnings("unused")
  def taskMenuItems = [new TaskMenuItem(folder: 'sample:9500', name: 'sampleParent', uri: '/sampleParent',
                                        displayOrder: 9510, clientRootActivity: true)]

  /**
   * The location of the index page.
   */
  String indexView = 'client/sample/sampleParent'

  @Get("/get")
  HttpResponse<SampleParent> get() {
    def p = null
    SampleParent.withTransaction {
      p = SampleParent.list()[0]
      //println "p = $p"
      DomainUtils.instance.resolveProxies(p)
    }
    return HttpResponse.ok(p)
  }

}