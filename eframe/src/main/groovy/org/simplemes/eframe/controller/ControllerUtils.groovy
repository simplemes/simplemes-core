/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.controller

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpParameters
import io.micronaut.http.annotation.Controller
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.misc.UUIDUtils
import org.simplemes.eframe.web.task.TaskMenuControllerUtils
import org.simplemes.eframe.web.ui.UIDefaults

/**
 * Controller support utilities.
 * These class provide common controller class utilities that simplify
 * how the controllers operate.
 *
 */
@Slf4j
class ControllerUtils {
  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static ControllerUtils instance = new ControllerUtils()

  /**
   * Defines the name of the fallback object in the StandardModelAndView to hold the current domain object.
   * Used when the controller is not specific to single domain.
   */
  static final String MODEL_KEY_DOMAIN_OBJECT = '_domainObject'

  /**
   * Defines the name of the object in the StandardModelAndView to hold the current domain's errors.
   * The domain errors is a list of ValidationError's.
   */
  static final String MODEL_KEY_DOMAIN_ERRORS = '_domainErrors'

  /**
   * Returns all of the controller classes defined in the system.
   * <p>
   * This looks for classes that use the @Controller annotation.
   * @return The list of controller classes.
   */
  List<Class> getAllControllers() {
    Collection<BeanDefinition> controllers = Holders.applicationContext?.getBeanDefinitions(Qualifiers.byStereotype(Controller))
    return controllers*.beanType
  }

/**
 * Returns all of the paths defined by the controllers in the server.  This checks all controllers
 * for the Task Menu Items that define a clientRootActivity=true.
 * @return The list of paths.
 */
  List<String> getAllBrowserPaths() {
    def res = []

    def tasks = TaskMenuControllerUtils.instance.coreTasks
    for (task in tasks) {
      if (task.clientRootActivity) {
        res << task.uri
      }
    }

    return res
  }

  /**
   * Finds the controller Class using the given simpleName for the class.
   * @param controllerName The controller name (e.g. 'UserController').
   * @return The controller class.
   */
  Class getControllerByName(String controllerName) {
    // Uses the instance to allow MockControllerUtils to use this original method in a test of the method.
    return instance.getAllControllers().find { it.simpleName == controllerName }
  }

  /**
   * Converts the given http request parameters to a true map.
   * @param parameters The parameters.
   * @return The map.
   */
  Map convertToMap(HttpParameters parameters) {
    def res = [:]

    for (name in parameters.names()) {
      res[name] = parameters.get(name)
    }

    return res
  }

  /**
   * Finds the domain class associated with the given controller.  Mainly used for CRUD and API actions.
   * Not all controllers have a single domain.  <p>
   * This method uses the static 'domainClass' property or the name of the controller to find the domain class.
   * The static property is used as:
   * <pre>
   *   static domainClass = Order
   * </pre>
   * @param controller The controller instance.
   * @return The domain (can be null).
   */
  Class getDomainClass(Object controllerInstance) {
    def clazz = controllerInstance?.getClass()
    if (clazz) {
      return getDomainClass(clazz)
    }
    return null
  }

  /**
   * Finds the domain class associated with the given controller.  Mainly used for CRUD and API actions.
   * Not all controllers have a single domain.  <p>
   * This method uses the static 'domainClass' property or the name of the controller to find the domain class.
   * The static property is used as:
   * <pre>
   *   static domainClass = Order
   * </pre>
   * @param controllerClass The controller class.
   * @return The domain (can be null).
   */
  Class getDomainClass(Class controllerClass) {
    def res = TypeUtils.getStaticProperty(controllerClass, 'domainClass')
    if (res) {
      // Controller has a specific domain class
      return res as Class
    }
    // Attempt to find it based on the controller name.
    def simpleName = controllerClass.simpleName - "Controller"
    def allDomains = DomainUtils.instance.allDomains
    return allDomains.find() { it.simpleName == simpleName }
  }


  /**
   * Finds the domain class associated with the base controller URI.
   * @param uri The uri for the page.
   * @return The domain (can be null).
   */
  Class getDomainClass(String uri) {
    def domainName = uri[1..-1]
    if (domainName.indexOf('/')) {
      // Strip off trailing parts of url.
      domainName = domainName[0..(domainName.indexOf('/') - 1)]
    }
    domainName = NameUtils.uppercaseFirstLetter(domainName)
    return DomainUtils.instance.getDomain(domainName)
  }

  /**
   * Builds a URI from the base URI and the given parameters.
   * @param uri The base URI.
   * @param parameters The params to add to the URI.
   * @return The URI with parameters (encoded).
   */
  String buildURI(String uri, Map params) {
    def sb = new StringBuilder()
    sb.append(uri)
    if (params) {
      params.each { k, v ->
        if (v) {
          if (!sb.contains('?')) {
            sb.append('?')
          } else {
            sb.append('&')
          }
          sb.append(URLEncoder.encode((String) k, 'UTF-8'))
          sb.append('=')
          sb.append(URLEncoder.encode(v?.toString(), 'UTF-8'))
        }
      }
    }
    return sb.toString()
  }


  /**
   * Finds the root path for this controller (as specified in the @Controller annotation).
   * @param controllerClass The controller class.
   * @return The root path for the controller.
   */
  String getRootPath(Class controllerClass) {
    def annotation = controllerClass.getAnnotation(Controller)
    return annotation?.value()
  }

  /**
   * Calculates the effective <code>from</code> (page start) and <code>size</code>(page size) for standard list queries.
   * Supports GUI toolkit-style <code>start</code> and <code>count</code>.  Limits the <code>size</code> to 100 (configurable)
   * for safety.
   * <p/>
   * Typical usage in a controller:
   * <pre>
   * def (int from, int size) = ControllerUtils.calculateFromAndSizeForList(params)
   * </pre>
   * @param params The request parameters. Integer and String values are supported.
   * @param allowNull If true, then the returned values can be null.  Only use this option if you have fallback's for max in your code.
   * @return A Tuple with the effective offset and max.  Never null, always Integers. (<b>Defaults:</b> 0,UIDefaults.PAGE_SIZE).
   */
  Tuple2<Integer, Integer> calculateFromAndSizeForList(Map params, boolean allowNull = false) {
    Integer size = null
    Integer from = null
    if (!allowNull) {
      size = UIDefaults.PAGE_SIZE
      from = 0
    }
    if (params?.size) {
      size = params.size as Integer
    } else if (params?.count) {
      size = params.count as Integer
    }
    size = size ? Math.min(size, Holders.configuration.maxRowLimit) : null

    if (params?.from) {
      from = params.from as Integer
    } else if (params?.start) {
      from = (params.start as Integer) / size as Integer
    }
    return [from, size]
  }

  /**
   * Calculates the effective <code>sortdatafield</code> and <code>sortorder</code> for standard list queries.
   * Supports toolkit-style <code>sort[field]=asc</code> as well as the simpler
   * <code>sort</code> and <code>order</code> parameters.  The toolkit-style <code>sort[field]=asc</code>
   * takes precedence over the simpler style.<p/>
   * Typical usage in a controller:
   * <pre>
   * def (sortField, direction) = ControllerUtils.calculateSortingForList(params)
   * </pre>
   * @param params The request parameters. String values are supported.  The toolkit style takes precedence over the simple style.
   * @return A Tuple with two values: the sort field and direction.
   */
  Tuple2<String, String> calculateSortingForList(Map params) {
    String field
    String direction

    def keys = params.keySet()
    def key = keys.find() { it.startsWith('sort[') }

    if (key) {
      // GUI Toolkit style:  sort[field]:direction
      field = key - 'sort[' - ']'
      direction = params.get(key) ?: 'asc'
    } else {
      // Simple style as a fallback.
      field = params.sort
      if (field) {
        direction = params.order ?: 'asc'
      } else {
        direction = null
      }
    }
    return [field, direction]
  }


  /**
   * Pauses for a user-configurable delay for testing purposes.
   */
  void delayForTesting(String location) {
    def delay = Holders.configuration.testDelay
    if (delay) {
      log.warn("Sleeping ${delay}ms for config setting eframe.testDelay in $location")
      sleep(delay)
    }
  }

  /**
   * Determines the base URI for a given page URI.  Mainly just strips the trailing parameters and any record UUID.
   * @param uri The raw URI from the web-page.
   * @return The baseURI.
   */
  String determineBaseURI(String uri) {
    if (!uri) {
      return uri
    }
    // Strip any HTTP parameters
    def l = uri.indexOf('?')
    if (l >= 0) {
      l--
      uri = uri[0..l]
    }

    // Check for a UUID at the end.
    def idx = uri.lastIndexOf('/')
    if (idx > 0) {
      idx++
      if (idx == uri.length() - 36) {
        def trailing = uri[idx..-1]
        if (UUIDUtils.isUUID(trailing)) {
          uri = uri[0..(idx - 2)]
        }
      }
    }

    return uri
  }

}
