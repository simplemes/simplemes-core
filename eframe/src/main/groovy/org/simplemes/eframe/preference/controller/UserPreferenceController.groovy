package org.simplemes.eframe.preference.controller


import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.preference.service.UserPreferenceService
import org.simplemes.eframe.security.SecurityUtils

import javax.inject.Inject

/**
 * The controller for user preference changes from the GUI.  This is normally called from the toolkit.ui.js
 * javascript methods and similar methods elsewhere.
 * <p/>
 *
 * <h3>Logging</h3>
 * The logging for this class can be enabled:
 * <ul>
 *   <li><b>debug</b> - Logs the method input parameters. </li>
 * </ul>
 *
 *
 */
@Secured(SecurityRule.IS_AUTHENTICATED)
@Slf4j
@Controller("/userPreference")
class UserPreferenceController {

  /**
   * The service that handles most preference actions.
   */
  @Inject
  UserPreferenceService userPreferenceService

  /**
   * Handles user's changes to the GUI state.  These are stored in the session and persisted for later displays of the page.
   * The request body should be a JSON object with these values:
   * <ul>
   *   <li><code>pageURI</code> - The URI for the page these settings changed on.</li>
   *   <li><code>element</code> - The HTML ID of the element that changed.</li>
   *   <li><code>event</code> - The change event (e.g.'ColumnResized' for  {@link org.simplemes.eframe.preference.event.ColumnResized}, etc).</li>
   *   <li><code><i>value</i></code> - The value(s).  This depends on the event type.</li>
   * </ul>
   *
   * @param body The request body.
   */
  @Post("/guiStateChanged")
  HttpResponse guiStateChanged(@Body String body) {
    def mapper = Holders.objectMapper
    def params = mapper.readValue(body, Map)
    userPreferenceService.guiStateChanged(params)

    return HttpResponse.status(HttpStatus.OK)
  }

  /**
   * Finds the given preference.  This is designed for use in the browser for dynamic preferences such as dialog
   * sizes.<p/>
   * Inputs from the request params are:
   * <ul>
   *   <li><code>pageURI</code> - The URI for the page to find the preferences for (<b>Required</b>).</li>
   *   <li><code>element</code> - The ID of the element for the preference (<b>Optional</b>).</li>
   *   <li><code>preferenceType</code> - The preference type to return.  For example 'DialogPreference' (<b>Optional</b>).</li>
   * </ul>
   * <p/>
   * The response is a JSON map with the preferences encoded as JSON objects.  The structure will vary
   * depending on the preference type.
   */
  @Get("/findPreferences")
  Map findPreferences(HttpRequest request) {
    def params = request.parameters
    //println "params = $params"
    def preferences = userPreferenceService.findPreferences(ControllerUtils.instance.determineBaseURI(params.get('pageURI')),
                                                            params.get('element'), params.get('preferenceType'))
    return preferences
  }

  /**
   * Saves the given preference value into the user's preferences for the give element.
   * This is a simplified view of preferences that just supports a simple name/value setting.
   *
   * <p/>
   * The content must contain a JSON object with the following values:
   * <ul>
   *   <li><code>pageURI</code> - The URI for the page to find the preferences for (<b>Required</b>).</li>
   *   <li><code>element</code> - The ID of the element for the preference (<b>Required</b>).</li>
   *   <li><code>key</code> - The preference key name.  String (<b>Default: 'key'</b>).</li>
   *   <li><code>value</code> - The preference value.  String or JSON object (<b>Default: ''</b>).</li>
   * </ul>
   * Only the HTTP POST method is supported.
   * @param params The body (JSON) converted to a Map.
   */
  @Post("/saveSimplePreference")
  void saveSimplePreference(Map params) {
    ArgumentUtils.checkMissing(params, 'params')
    log.debug("saveSimplePreference() params = {}, user = {}", params, SecurityUtils.currentUserName)

    String pageURI = params.pageURI
    String element = params.element
    String value = params.value ?: ''

    ArgumentUtils.checkMissing(pageURI, 'pageURI')
    ArgumentUtils.checkMissing(element, 'element')

    userPreferenceService.saveSimplePreference(pageURI, element, value)
  }

  /**
   * Finds the simple preference value for the current user and the given page.
   * This is a simplified view of preferences that just supports a simple name/value setting.
   *
   * <p/>
   * The HTTP parameters supported are:
   * <ul>
   *   <li><code>pageURI</code> - The URI for the page to find the preferences for (<b>Required</b>).</li>
   *   <li><code>element</code> - The ID of the element for the preference (<b>Required</b>).</li>
   * </ul>
   * Only the HTTP POST method is supported.<p>
   * @param request The HTTP request.
   * @returns A JSON map object with the 'value'.
   */
  @Get('/findSimplePreference')
  Map findSimplePreference(HttpRequest request) {
    def params = request.parameters
    String pageURI = params.get('pageURI')
    String element = params.get('element')
    ArgumentUtils.checkMissing(pageURI, 'pageURI')
    ArgumentUtils.checkMissing(element, 'element')

    def pref = userPreferenceService.findSimplePreference(pageURI, element)
    def map
    if (pref == null) {
      map = [:]
    } else {
      map = [value: pref]
    }
    return map
  }


}
