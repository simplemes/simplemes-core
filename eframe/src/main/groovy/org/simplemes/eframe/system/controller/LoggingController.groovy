package org.simplemes.eframe.system.controller

import ch.qos.logback.classic.Level
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SimpleStringPreference
import org.simplemes.eframe.preference.TreeStatePreference
import org.simplemes.eframe.preference.event.TreeStateChanged
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.service.ServiceUtils
import org.simplemes.eframe.web.task.TaskMenuItem

import javax.annotation.Nullable
import javax.transaction.Transactional
import java.security.Principal

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This controller provides access to the logging configuration for runtime logging changes.
 */
@Slf4j
@Secured("ADMIN")
@Controller("/logging")
class LoggingController {

  /**
   * Defines the entry(s) in the main Task Menu.
   */
  @SuppressWarnings("unused")
  def taskMenuItems = [new TaskMenuItem(folder: 'admin:7000', name: 'logging', uri: '/logging',
                                        displayOrder: 7020, clientRootActivity: true)]

  /**
   * The default entries in the list of 'others'.  
   */
  public static final List<String> DEFAULT_OTHERS = ['org.hibernate.SQL', 'org.hibernate.type']

  /**
   * The user preference element the 'others' are stored under.
   */
  public static final String OTHERS_ELEMENT = 'others'

  /**
   * The user preference key for the SimpleStringPreference used to store the 'other' levels.
   */
  public static final String OTHERS_KEY = 'otherValues'

  /**
   * The user preference element the 'tree state' are stored under.
   */
  public static final String TREE_STATE_ELEMENT = 'logger'

  /**
   * Displays the index page.  Requires a view  '{domain}/index' for the given domain.
   * @param principal The user logged in.
   * @return The model/view to display.
   */
  @Produces(MediaType.TEXT_HTML)
  @Get("/")
  StandardModelAndView index(@Nullable Principal principal) {
    def modelAndView = new StandardModelAndView("logging/index", principal, this)
    // Add the json needed for the list of logging entities and their current state
    Map model = (Map) modelAndView.model.get()
    model.treeData = buildTreeData(principal?.name)

    // Now, store it as a JSON string so the page can render it.
    model.treeJSON = Holders.objectMapper.writeValueAsString(model.treeData)

    log.trace('index(): {}', modelAndView)
    return modelAndView
  }

  /**
   * This sets or clears the current logging level for the given logger.  Can be used to clear the logging level for a logger
   * so that the level from the parent(s) is used.
   *
   * <h3>body</h3>
   * The request body should be a JSON object with these values:
   * <ul>
   *   <li><b>logger</b> - The logger name (e.g. full class name) </li>
   *   <li><b>level</b> - The logging level.</li>
   * </ul>
   *
   * @return The level settings for this logger.
   */
  @Post("/setLoggingLevel")
  Map setLoggingLevel(@Body String body) {
    def mapper = Holders.objectMapper
    def params = mapper.readValue(body, Map)
    log.debug('setLoggingLevel() params {}', params)
    def level = params.level
    String logger = params.logger
    level = (level == 'clear') ? null : level
    def newLevel = level ? LogUtils.toLevel((String) level) : null
    LogUtils.getLogger(logger).level = newLevel

    return getLoggingLevels(logger)
  }

  /**
   * Adds an other logger to the list displayed.  Requires <b>Admin</b> role.
   * This is persisted for future use.
   * @param body The body.  This contains a JSON object with one element: logger.
   * @return The HTTP response with the new logger's status in the body.
   */
  @Post("/addOtherLogger")
  HttpResponse addOtherLogger(@Body String body) {
    def mapper = Holders.objectMapper
    def params = mapper.readValue(body, Map)
    def logger = params?.logger

    if (logger) {
      def map = addOtherLoggerToPref((String) logger)
      return HttpResponse.status(HttpStatus.OK).body(map)
    }

    return HttpResponse.status(HttpStatus.BAD_REQUEST)
  }

  /**
   * Removes the given 'other logger' from the list displayed.  Requires <b>Admin</b> role.
   * This is persisted for future use.
   * @param body The body.  This contains a JSON object with one element: logger.
   * @return The HTTP response.
   */
  @Post("/removeOtherLogger")
  HttpResponse removeOtherLogger(@Body String body) {
    def mapper = Holders.objectMapper
    def params = mapper.readValue(body, Map)
    def logger = params?.logger

    if (logger) {
      removeOtherLoggerFromPref((String) logger)
      return HttpResponse.status(HttpStatus.OK)
    }

    return HttpResponse.status(HttpStatus.BAD_REQUEST)
  }

  /**
   * Logs a client message to the server log.  Requires user to be logged in.
   * @param body The body.  This contains a JSON object with one element: logger.
   * @return The HTTP response with the new logger's status in the body.
   */
  @Secured(SecurityRule.IS_AUTHENTICATED)
  @Post("/client")
  HttpResponse client(HttpRequest request, @Body String body) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    def json = Holders.objectMapper.readValue(body, Map)
    json.remoteIP = request.remoteAddress
    LogUtils.logClientMessage(json, params)

    return HttpResponse.status(HttpStatus.OK)
  }

  /**
   * Finds the current logging level for the given logger (class).
   * @param userName The logged in user name.
   * @return The logging level state.
   */
  protected List buildTreeData(String userName) {
    def res = []
    def openLevels = getOpenLevels(userName)

    res << buildMapForOneTopLevel('domains.label', DomainUtils.instance.allDomains, openLevels)
    res << buildMapForOneTopLevel('controllers.label', ControllerUtils.instance.allControllers, openLevels)
    res << buildMapForOneTopLevel('services.label', ServiceUtils.instance.allServices, openLevels)
    res << buildMapForOneTopLevel('jsClient.label', getClientLevels(), openLevels, false)

    // Now, add the other from the default list.
    def otherLevels = buildMapForOneTopLevel('others.label', getOtherLevels(userName), openLevels)
    otherLevels.add = true  // User can add other levels.
    // and all others are removable
    for (child in otherLevels.data) {
      child.remove = true
    }
    res << otherLevels

    assignIDs(res)
    return res
  }

  /**
   * Assign a unique ID to all elements.
   * @param res The results tree data.
   */
  protected void assignIDs(res) {
    int id = 1
    for (topLevel in res) {
      topLevel.id = id.toString()
      id++
      // Now, assign in the second level
      for (row in topLevel.data) {
        row.id = id.toString()
        id++
      }
    }

  }

  /**
   * Determines the 'other' logger levels the user cares about.
   *
   * @param userName The logged in user name.
   * @return The list of levels.
   */
  // TODO: Replace with non-hibernate alternative
  @Transactional
  //(readOnly = true)
  protected List<String> getOtherLevels(String userName) {
    def res = []
    res.addAll(DEFAULT_OTHERS)

    // Now, add any entries configured from the user preferences.
    def preference = PreferenceHolder.find {
      page '/logging'
      user userName
      //noinspection UnnecessaryQualifiedReference
      element LoggingController.OTHERS_ELEMENT
    }
    SimpleStringPreference s = (SimpleStringPreference) preference[OTHERS_KEY]

    if (s?.value) {
      def list = s.value.tokenize(',')
      res.addAll(list)
    }

    return res
  }

  /**
   * The prefix to apply to all client views for the logger name.
   */
  public static final String CLIENT_PREFIX = 'client'

  /**
   * The name of the top-level client logger.
   */
  public static final String CLIENT_LOGGER = 'client'

  /**
   * The name of the logger that controls which messages are sent to the server.
   */
  public static final String CLIENT_TO_SERVER_LOGGER = 'client.to-server'

  /**
   * Returns the list of client javascript levels defined in the system.
   * @return The list of levels.
   */
  protected List<String> getClientLevels() {
    def res = []

    // Find all of the paths to full GUIs a user might browse to and then build a logger name for them.
    for (path in ControllerUtils.instance.allBrowserPaths) {
      def s = "${CLIENT_PREFIX}${path}"
      s = ControllerUtils.instance.determineBaseURI(s)
      s = s?.replaceAll('/', '.')
      if (s && !res.contains(s)) {
        res << s
      }
    }

    // Sort this portion of the list.
    res.sort()

    // Now, add the special logger names to the top.
    res.add(0, CLIENT_LOGGER)
    res.add(1, CLIENT_TO_SERVER_LOGGER)

    return res
  }
  /**
   * Adds a single level to the 'Others' logger list in the user preferences.
   *
   * @param logger The logger name.
   * @return The logger levels.
   */
  @Transactional
  protected Map addOtherLoggerToPref(String logger) {
    // Now, add any entries configured from the user preferences.
    def preference = PreferenceHolder.find {
      page '/logging'
      user SecurityUtils.currentUserName
      //noinspection UnnecessaryQualifiedReference
      element LoggingController.OTHERS_ELEMENT
    }
    SimpleStringPreference sPref = (SimpleStringPreference) preference[OTHERS_KEY]
    sPref = sPref ?: new SimpleStringPreference(OTHERS_KEY)

    def value = sPref.value ?: ''
    if (!value.contains(logger)) {
      if (value.size() > 0) {
        value += ','
      }
      value += logger
      sPref.value = value
    }
    log.debug('Saving other loggers: {}', value)
    preference.setPreference(sPref).save()

    def res = getLoggingLevels(logger)
    res.remove = true
    return res
  }

  /**
   * Removes a single level from the 'Others' logger list in the user preferences.
   *
   * @param logger The logger name.
   */
  @Transactional
  protected void removeOtherLoggerFromPref(String logger) {
    // Now, find the preference to change.
    def preference = PreferenceHolder.find {
      page '/logging'
      user SecurityUtils.currentUserName
      //noinspection UnnecessaryQualifiedReference
      element LoggingController.OTHERS_ELEMENT
    }
    SimpleStringPreference sPref = (SimpleStringPreference) preference[OTHERS_KEY]
    if (sPref && sPref.value.contains(logger)) {
      def value = sPref.value ?: ''
      if (value.startsWith(logger)) {
        sPref.value = value - logger
      } else {
        // Remove the logger, along with the leading comma.
        sPref.value = value - ",$logger"
      }
      log.debug('Saving other loggers: {}', value)
      preference.setPreference(sPref).save()
    }
  }

  /**
   * Determines the 'open' top-level entries in the tree from the user preferences.
   *
   * @param userName The logged in user name.
   * @return The list of open tree levels.
   */
  // TODO: Replace with non-hibernate alternative
  @Transactional
  //(readOnly = true)
  protected List<String> getOpenLevels(String userName) {
    def res = []
    res.addAll(DEFAULT_OTHERS)

    // Now, add any entries configured from the user preferences.
    def preference = PreferenceHolder.find {
      page '/logging'
      user userName
      //noinspection UnnecessaryQualifiedReference
      element LoggingController.TREE_STATE_ELEMENT
    }
    TreeStatePreference s = (TreeStatePreference) preference[TreeStateChanged.KEY]

    if (s?.expandedKeys) {
      def list = s.expandedKeys.tokenize(',')
      res.addAll(list)
    }

    return res
  }

  /**
   * Builds one top level node of loggers for the given list.  Adds the given list of classes
   * as children.
   * @param titleKey The messages.properties label key for the top-level entry (e.g. 'domains.label').
   * @param classesOrNames The list of classes or list of class names.
   * @param openLevels The list of tree entries that should be open by default.
   * @param sort If true, the sort the list.  (*Default*: true).
   * @return The top-level Map for the client display.
   */
  protected Map buildMapForOneTopLevel(String titleKey, List<Object> classesOrNames, List<String> openLevels, boolean sort = true) {
    def subList = []
    def title = GlobalUtils.lookup(titleKey)
    def res = [title: title, data: subList]

    if (openLevels.contains(title)) {
      res.open = true
    }

    if (classesOrNames) {
      List<String> names = (List) classesOrNames
      if (classesOrNames[0] instanceof Class) {
        // Need to extract the list of class names
        names = classesOrNames*.name
      }
      for (String name in names) {
        subList << getLoggingLevels(name)
      }
      if (sort) {
        subList.sort { a, b -> a.title <=> b.title }
      }
    }

    return res
  }

  /**
   * Finds the current logging level for the given logger (class).
   * @param logger The logger name (class).
   * @return The logging level state.
   */
  protected Map getLoggingLevels(String logger) {
    def level = LogUtils.getLogger(logger).level
    def effective = getLoggingLevelIfClear(logger).toLowerCase()

    def error = (level == Level.ERROR ? 'on' : 'off')
    def warn = (level == Level.WARN ? 'on' : 'off')
    def info = (level == Level.INFO ? 'on' : 'off')
    def debug = (level == Level.DEBUG ? 'on' : 'off')
    def trace = (level == Level.TRACE ? 'on' : 'off')

    return [title: logger, error: error, warn: warn, info: info, debug: debug, trace: trace, effective: effective]
  }

  /**
   * Finds the logging level, if the current level is clear (null).
   * <p>
   * <b>Note: </b>Temporarily clears the level for the logger.  Restores it when done.
   * @param loggerName The logger name (class).
   * @return The logging level state if the given level is cleared.
   */
  protected String getLoggingLevelIfClear(String loggerName) {
    def logger = LogUtils.getLogger(loggerName)
    def originalLevel = logger.level
    logger.level = null
    def eff = logger.effectiveLevel
    logger.level = originalLevel
    return eff.toString()
  }


}
