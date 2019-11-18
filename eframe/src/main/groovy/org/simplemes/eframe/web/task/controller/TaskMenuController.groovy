package org.simplemes.eframe.web.task.controller


import groovy.util.logging.Slf4j
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.web.task.TaskMenuFolder
import org.simplemes.eframe.web.task.TaskMenuHelper

import javax.annotation.Nullable
import java.security.Principal

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the controller access to the main menu (Task Menu).
 * This provides a cache-able script file that defines the main menu for a user.
 */
@Slf4j
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/taskMenu")
class TaskMenuController {


  /**
   * Provides the javascript used to define the task menu.  Mainly a string stored in the toolkit JS
   * library.  (See eframe_toolkit.js). We use a string to reduce the load times on the client.  It is only
   * parsed when the TaskMenu is displayed.
   * @param principal The user logged in (Optional).  If null, then the default menu is returned.
   * @return The response.
   */
  @Produces('application/javascript')
  @Get("/")
  HttpResponse index(@Nullable Principal principal) {
    //def s = '[{"id":"demand","value":"Demanddd","data":[{"id":"demand.1","value":"Order","link":"/sampleParent","tooltip":".tooltip"}]},{"id":"admin","value":"Admin","data":[{"id":"admin.1","value":"Logging"},{"id":"admin.2","value":"Search Admin"},{"id":"sample","value":"Sample/Test","data":[{"id":"1.2.1","value":"Javascript Test Page"},{"id":"1.2.1","value":"Test Page"},{"id":"1.2.1","value":"AllFieldsDomain"},{"id":"1.2.5","value":"SampleParent"}]}]}]'
    // Build the tree for the JSON string from the menu definitions.
    def taskMenu = TaskMenuHelper.instance.taskMenu
    def tree = []
    for (menuFolder in taskMenu.root.folders) {
      tree << buildTree(menuFolder)
    }

    def json = buildJSON(tree)

    def script = """
      tk._setTaskMenuString('$json');
    """
    def cacheTime = Holders.configuration.cacheStableResources
    return HttpResponse.status(HttpStatus.OK).body(script).header(HttpHeaders.CACHE_CONTROL, "max-age=${cacheTime}")
  }

  /**
   * Optimizes the string for use as a single line of quoted text in Javascript.
   * @param tree The list of root Task Menu nodes.
   * @return The JSON string as a single line.
   */
  String buildJSON(List tree) {
    // Make sure the single quote in the content survives the JSON creation.
    // The single line of JSON will be stored in a string.
    def json = Holders.objectMapper.writeValueAsString(tree)
    json = json.replaceAll("'", "\\\\'")
    json = json.replaceAll("[\\n\\r]", "")

    return json
  }

  /**
   * Builds the menu tree for a single level.  Will build child levels recursively.
   * @param folder The folder to build the tree for,
   * @return A map that represents the tree.  Each level includes these items: id, label,html and child items.
   */
  protected Map buildTree(TaskMenuFolder folder) {
    def res = [id: folder.name, value: folder.getLocalizedName(), tooltip: folder.getLocalizedTooltip()]
    def data = []
    res.data = data
    // Add the menu items (if any)
    for (menuItem in folder.menuItems) {
      def name = menuItem.name
      data << [id: name, link: menuItem.uri, value: menuItem.getLocalizedName(), tooltip: menuItem.getLocalizedTooltip()]
    }

    // Now, add any child folders
    for (menuFolder in folder.folders) {
      data << buildTree(menuFolder)
    }

    return res
  }


}
