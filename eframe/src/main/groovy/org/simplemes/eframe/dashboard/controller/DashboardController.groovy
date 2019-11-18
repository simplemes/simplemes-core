package org.simplemes.eframe.dashboard.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.web.task.TaskMenuItem
import org.simplemes.eframe.web.ui.webix.freemarker.DashboardMarker

import javax.annotation.Nullable
import java.security.Principal

/**
 * Provides runtime access to a dashboard (configured or not).
 * Most configuration actions require the Designer role.
 */
@Slf4j
@Secured(['ADMIN', 'DESIGNER'])
@Controller("/dashboard")
class DashboardController extends BaseController {

  /**
   * The URL parameters not passed to dashboard activities.
   */
  static final PARAMS_NOT_PASSED = ['controller', 'format', 'action', 'category', 'dashboard']

  /**
   * Defines the standard end-user task entry points that this controller handles.
   * This method finds all current default dashboards and adds them to the list.
   */
  List<TaskMenuItem> getTaskMenuItems() {
    def res = []
    DashboardConfig.withTransaction {
      def defaultDashboards = DashboardConfig.findAllByDefaultConfig(true)
      defaultDashboards.sort { a, b -> a.dashboard <=> b.dashboard }

      def displayOrder = 210
      for (dashboard in defaultDashboards) {
        def name = dashboard.title ?: dashboard.dashboard
        res << new TaskMenuItem(folder: 'dashboard:200', name: name,
                                uri: "/dashboard?dashboard=${dashboard.dashboard}", displayOrder: displayOrder)
        displayOrder += 10
      }
    }
    return res
  }

  /**
   * The main dashboard entry point used at runtime.
   */
  @Secured(SecurityRule.IS_AUTHENTICATED)
  @Produces(MediaType.TEXT_HTML)
  @Get('/')
  StandardModelAndView index(HttpRequest request, @Nullable Principal principal) {
    def modelAndView = new StandardModelAndView('dashboard/index', principal, this)
    def params = ControllerUtils.instance.convertToMap(request.parameters)

    // Store the non-dashboard parameters so the dashboard can pass them to the dashboard activities that are displayed.
    def otherParams = [:]
    params?.each { k, v ->
      if (!PARAMS_NOT_PASSED.contains(k)) {
        otherParams[k as String] = v
      }
    }
    Map model = (Map) modelAndView.model.get()
    model[DashboardMarker.ACTIVITY_PARAMETERS_NAME] = otherParams
    //println "modelAndView = $modelAndView"

    log.trace('index(): {}', modelAndView)
    return modelAndView
  }

  /**
   * Contains the editor page.
   */
  def editor() {}

  /**
   * Contains the details dialog for a single dashboard.
   */
  def detailsDialog() {
  }

  /**
   * Contains the panel details dialog for a panel
   */
  def panelDetailsDialog() {
  }

  /**
   * Contains the button details dialog for a button.
   */
  def buttonDetailsDialog() {
  }

}
