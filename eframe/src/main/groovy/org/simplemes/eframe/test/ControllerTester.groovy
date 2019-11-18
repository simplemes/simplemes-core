package org.simplemes.eframe.test

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.micronaut.security.annotation.Secured
import org.simplemes.eframe.misc.ArgumentUtils

import java.lang.reflect.Method

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Helps tests common features of controller classes, such as security and the TaskMenu support.
 */
@Slf4j
@ToString(includeNames = true, includePackage = false)
class ControllerTester {
  /**
   * The controller class to test.
   */
  Class _controller

  /**
   * Sets the controller class to test.
   * @param controller The controller class
   */
  void controller(final Class controller) { this._controller = controller }

  /**
   * If true, then the security checks are made.
   */
  Boolean _securityCheck = true

  /**
   * If true, then the method security checks are made.
   * @param securityCheck If true, then the checks are performed (<b>Default:</b> true)
   */
  void securityCheck(Boolean securityCheck = true) { this._securityCheck = securityCheck }

  /**
   * The security role that should be specified for the class (or all methods).  Optional.
   */
  String _role

  /**
   * The security role that should be specified for the class (or all methods).  Optional.
   * @param role The role to check for.
   */
  void role(String role) { this._role = role }

  /**
   * A map of the specific method checks to make.
   */
  Map<String, String> _secured = [:]

  /**
   * Adds a specific test for a single method name and role.
   * @param methodName The method to check
   * @param role The role required.
   */
  void secured(String methodName, String role) { this._secured[methodName] = role }

  /**
   * Defines the expected task menus needed in the controller.
   */
  List<Map<String, Object>> _taskMenus = []

  /**
   * Defines a single expected task menus needed in the controller.
   *
   * @param taskMenuOptions The task menu values to check.  Supports: 'name', 'uri', 'clientRootActivity'.
   */
  void taskMenu(final Map<String, Object> taskMenuOptions) {
    taskMenuOptions.each { k, v ->
      if (k != 'name' && k != 'uri' && k != 'clientRootActivity') {
        throw new IllegalArgumentException("Invalid option for taskMenu: '$k'")
      }
    }
    _taskMenus << taskMenuOptions
  }

  static ControllerTester test(@DelegatesTo(ControllerTester) final Closure config) {
    ControllerTester controllerTester = new ControllerTester()
    controllerTester.with config

    ArgumentUtils.checkMissing(controllerTester._controller, 'domain')

    controllerTester.doTest()

    return controllerTester
  }

  /**
   * Performs the test.  Throws an exception on failures.
   * @return True if no exception thrown.
   */
  boolean doTest() {
    checkSecurity()
    checkTaskMenus()
    return true
  }

  /**
   * Verifies that all methods are flagged with the security annotation.
   */
  void checkSecurity() {
    if (!_securityCheck) {
      log.warn("Security Check disabled for ${_controller}")
      return
    }

    checkAllMethodsAreSecured()

    if (!_role) {
      checkForClassLevelAnonymous()
    }
    checkMethodSecurity()

  }

  List<String> ignoredMethods = ['setProperty', 'getProperty', 'getMetaClass', 'setMetaClass', 'invokeMethod']

  /**
   * Verifies that all methods are secured, using the optional _role.
   */
  void checkAllMethodsAreSecured() {
    if (isSecured(_controller, _role)) {
      // All methods are secured.
      return
    }

    def roleMsg = _role ? " with role ${_role}" : ''

    // Now, check each method.
    for (method in _controller.methods) {
      //println "method = $method, is = ${isSecured(method,_role)}, dc = ${method.declaringClass}, m.class = ${method.class}"
      if (method.declaringClass == _controller && !ignoredMethods.contains(method.name)) {
        //println "$method.name, is = ${isSecured(method,_role)}, dc = ${method.declaringClass}, m.class = ${method.class}"
        assert isSecured(method, _role), "Method ${_controller.name}.$method.name is not secured${roleMsg}. "
      }
    }
  }

  /**
   * Verifies that the specified task menus are in the controller.
   */
  void checkTaskMenus() {
    if (!_taskMenus) {
      return
    }
    def controller = _controller.newInstance()
    if (controller.hasProperty('taskMenuItems')) {
      def controllerTaskMenuItems = controller.taskMenuItems
      // Make sure each menu from the tester is in the controller's list
      for (tmi in _taskMenus) {
        def controllerTaskMenuItem = controllerTaskMenuItems.find { it.name == tmi.name }
        assert controllerTaskMenuItem, "Task Menu ${tmi.name} not found in controller's (${_controller.name}) task menu"
        if (tmi.uri) {
          def msg = "Task Menu ${tmi.name} has wrong URI.  Found '${controllerTaskMenuItem.uri}', expected '${tmi.uri}'"
          assert controllerTaskMenuItem.uri == tmi.uri, msg
        }
        if (tmi.clientRootActivity != null) {
          def msg = "Task Menu ${tmi.name} has wrong clientRootActivity.  Found '${controllerTaskMenuItem.clientRootActivity}', expected '${tmi.clientRootActivity}'"
          assert controllerTaskMenuItem.clientRootActivity == tmi.clientRootActivity, msg
        }
      }
    } else {
      assert controller.hasProperty('taskMenuItems'), "Controller ${_controller.name} has no taskMenuItems property."
    }
  }

  /**
   * Determines if the given class is secured, with an optional role.
   * @param clazz The class to check.
   * @param role The role string.  If null, then any role is acceptable.  Optional.
   * @return True if secured.
   */
  protected boolean isSecured(Class clazz, String role = null) {
    if (role) {
      def annotation = clazz.getAnnotation(Secured)
      return annotation?.value()?.contains(role)
    } else {
      return (clazz.getAnnotation(Secured)) != null
    }
  }

  /**
   * Determines if the given class is secured, with an optional role.
   * @param method The method to check.
   * @param role The role string.  If null, then any role is acceptable.
   * @return True if secured.
   */
  protected boolean isSecured(Method method, String role) {
    if (role) {
      def annotation = method.getAnnotation(Secured)
      return annotation?.value()?.contains(role)
    } else {
      return (method.getAnnotation(Secured)) != null
    }
  }

  /**
   * Makes sure that the class is not flagged as anonymous.
   */
  protected void checkForClassLevelAnonymous() {
    def annotation = (Secured) _controller.getAnnotation(Secured)
    if (annotation) {
      assert !isJustAnonymous(annotation), "Class ${_controller.name} is secured by only 'isAnonymous()'. "
    }
  }

  /**
   * Checks for method level security issues.  Checks for:
   */
  protected void checkMethodSecurity() {
    // Now, check all specific secured entries.
    _secured.each { name, role ->
      def roleMsg = role ? " with role ${role}" : ''
      for (method in _controller.methods) {
        if (method.name == name && method.declaringClass == _controller && !ignoredMethods.contains(method.name)) {
          //println "$method.name, is = ${isSecured(method,_role)}, dc = ${method.declaringClass}, m.class = ${method.class}"
          assert isSecured(method, role), "Method ${_controller.name}.$method.name is not secured${roleMsg}. "
        }
      }
    }

    // Make sure the method level security is not anonymous.
    if (!_role) {
      for (method in _controller.methods) {
        if (method.declaringClass == _controller && !ignoredMethods.contains(method.name)) {
          def annotation = method.getAnnotation(Secured)
          if (annotation) {
            assert !isJustAnonymous(annotation), "Method ${_controller.name}.$method.name is secured by only 'isAnonymous()'. "
          }
        }
      }
    }

    // Finally, check for method security that differs from the class-level security, if not in the special method list.
    def classLevelAnnotation = (Secured) _controller.getAnnotation(Secured)
    if (classLevelAnnotation) {
      for (method in _controller.methods) {
        if (method.declaringClass == _controller && !_secured[method.name]) {
          def annotation = method.getAnnotation(Secured)
          if (annotation && classLevelAnnotation) {
            def s1 = "Method ${_controller.name}.$method.name is secured differently than the class level(${annotation.value()} vs. ${classLevelAnnotation.value()})."
            def s2 = "Maybe add a 'secured' option to the tester call. "
            assert annotation.value() == classLevelAnnotation.value(), "$s1 $s2"
          }
        }
      }
    }
  }

  /**
   * Returns true if the given annotation just contains isAnonymous().
   * @param annotation The annotation.
   * @return True if the given annotation only contains isAnonymous().
   */
  protected boolean isJustAnonymous(Secured annotation) {
    return annotation?.value()?.size() == 1 && annotation?.value()?.contains('isAnonymous()')
  }

}
