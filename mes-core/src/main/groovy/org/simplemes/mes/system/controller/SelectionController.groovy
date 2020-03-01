package org.simplemes.mes.system.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.dashboard.controller.DashboardController
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SimpleStringPreference
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.security.SecurityUtils

import javax.annotation.Nullable
import javax.transaction.Transactional
import java.security.Principal

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The controller for the core MES selection activities.
 *
 *  <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Debugging information. Typically includes inputs and outputs. </li>
 * </ul>
 */
@Slf4j
@Secured('OPERATOR')
@Controller("/selection")
class SelectionController extends BaseController {

  /**
   * The user preference element the current work center is stored under.  Also used as the preference key.
   */
  static final String WORK_CENTER_ELEMENT = 'workCenter'

  /**
   * Displays the work center selection dashboard activity.
   * @param request The request.
   * @param principal The user.
   * @return The page.
   */
  @Get("/workCenterSelection")
  @Produces(MediaType.TEXT_HTML)
  @SuppressWarnings("unused")
  StandardModelAndView workCenterSelection(HttpRequest request, @Nullable Principal principal) {
    def res = new StandardModelAndView("selection/workCenterSelection", principal, this)
    def params = res.model.get().params

    // Get the preference setting for work center, if not already on the URL.
    if (!params.workCenter) {
      UserPreference.withTransaction {
        def preference = PreferenceHolder.find {
          page DashboardController.ROOT_URI
          user SecurityUtils.instance.currentUserName
          element WORK_CENTER_ELEMENT
        }
        params.workCenter = preference[WORK_CENTER_ELEMENT]?.value
      }
    }

    return res
  }

  /**
   * Displays the change work center dialog contents.
   * @param request The request.
   * @param principal The user.
   * @return The page.
   */
  @Get("/changeWorkCenterDialog")
  @Produces(MediaType.TEXT_HTML)
  @SuppressWarnings("unused")
  StandardModelAndView changeWorkCenterDialog(HttpRequest request, @Nullable Principal principal) {
    return new StandardModelAndView("selection/changeWorkCenterDialog", principal, this)
  }

  /**
   * Saves the given work center preference value into the user's preferences for the give element.
   *
   * <h3>Body Content</h3>
   * The JSON body for this request supports:
   * <ul>
   *   <li><b>page</b> - The page (URI) the preference applies to (window.location.pathname). Usually '/dashboard'. </li>
   *   <li><b>workCenter</b> - The work center value.  Not validated against the WorkCenter table. </li>
   * </ul>
   */
  @Post('/workCenterChanged')
  @Transactional
  @SuppressWarnings("unused")
  void workCenterChanged(@Body String body, @Nullable Principal principal) {
    // Grab the current preference (in case it is stored in the session).
    Map params = Holders.objectMapper.readValue(body, Map)
    ArgumentUtils.checkMissing(params.page, 'page')
    ArgumentUtils.checkMissing(params.workCenter, 'workCenter')

    def pageParam = (String) params.page
    String userParam = SecurityUtils.currentUserName
    PreferenceHolder holder = PreferenceHolder.find {
      page pageParam
      user userParam
      element WORK_CENTER_ELEMENT
    }

    // See if this preference is already in the list.
    def simplePreference = new SimpleStringPreference(WORK_CENTER_ELEMENT, (String) params.workCenter)
    if (holder?.settings[0]) {
      holder.settings[0] = simplePreference
    } else {
      holder.settings << simplePreference
    }
    holder.save()

    log.trace("Stored Preference {}", holder)
  }

}
