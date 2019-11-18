package org.simplemes.eframe.test

import groovy.transform.ToString
import org.simplemes.eframe.controller.ControllerUtils
import org.spockframework.mock.IDefaultResponse
import org.spockframework.mock.IMockInvocation
import org.spockframework.mock.ZeroOrNullResponse
import spock.mock.DetachedMockFactory

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Builds a very simple mock/stub version of ControllerUtils that returns the given controller(s) classes as possible controllers.
 * <p>
 * <b>Note:</b> This does not mock the controller class itself.  It just provides a ControllerUtils.instance that
 * provides a few mock methods.
 * <p>
 * <h3>Mocked Methods</h3>
 * <ul>
 *   <li><b>getAllControllers()</b> - Returns the mocked controller list.</li>
 * </ul>
 *
 * This class restores the original ControllerUtils.instance during BaseSpecification.cleanup().
 * You can use this method like this:
 * <pre>
 *   new MockControllerUtils(this,OrderController).install()
 * </pre>
 * <p>
 */
@ToString(includePackage = false, includeNames = true)
class MockControllerUtils implements AutoCleanupMockInterface, IDefaultResponse {

  /**
   * Holds the original ControllerUtils since we have mocked it.
   * Will be auto-restored by the BaseSpecification.cleanup() method.
   */
  ControllerUtils originalControllerUtils

  /**
   * Holds the mocked ControllerUtils.
   */
  ControllerUtils mockedControllerUtils

  /**
   * The list of classes to pretend are controller classes.
   */
  List<Class> controllerClassList = []

  BaseSpecification baseSpec

  /**
   * The list of original methods to call on the real ControllerUtils class.
   * This allows testing of those real methods with a mocked ControllerUtils class.
   * Defaults to include 'getControllerByName' and 'getDomainClass'.
   */
  List<String> originalMethods = ['getControllerByName', 'getDomainClass', 'getRootPath']

  /**
   * Basic constructor.  You must call install() to make this mock active.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param clazz The class to pretend is a controller class.
   * @param originalMethods The list of method names to use in the original (real) method for testing (<b>Optional</b>).
   */
  MockControllerUtils(BaseSpecification baseSpec, Class clazz, List<String> originalMethods = []) {
    this(baseSpec, [clazz])
  }

  /**
   * Basic constructor.  You must call install() to make this mock active.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param classes The classes to  pretend they are controller classes.
   * @param originalMethods The list of method names to use in the original (real) method for testing (<b>Optional</b>).
   */
  MockControllerUtils(BaseSpecification baseSpec, List<Class> classes, List<String> originalMethods = []) {
    this.baseSpec = baseSpec
    def mockFactory = new DetachedMockFactory()
    originalControllerUtils = ControllerUtils.instance
    mockedControllerUtils = mockFactory.Stub(defaultResponse: this, ControllerUtils) as ControllerUtils
    controllerClassList.addAll(classes)
    this.originalMethods.addAll(originalMethods)
  }
  /**
   * Installs the mock ControllerUtils as the current ControllerUtils instance.  Will be auto-cleaned up by the
   * BaseSpecification.
   */
  void install() {
    ControllerUtils.instance = (ControllerUtils) mockedControllerUtils
    baseSpec.registerAutoCleanup(this)
  }

  /**
   * Performs the cleanup action.
   * @param testSpec The test that requests the cleanup.
   */
  @Override
  void doCleanup(BaseSpecification testSpec) {
    ControllerUtils.instance = originalControllerUtils
  }

  @Override
  Object respond(IMockInvocation invocation) {
    def methodName = invocation.method.name
    //println "methodName = $methodName"

    // Delegate to real method.

    if (methodName == 'getAllControllers') {
      return controllerClassList
    } else if (originalMethods.contains(methodName)) {
      // User wants to delegate to the original (real) method in the ControllerUtils class.
      def method = ControllerUtils.getDeclaredMethod(methodName, invocation.method.parameterTypes as Class[])
      if (method) {
        return method.invoke(new ControllerUtils(), invocation.arguments as Object[])
      }
    }
    return ZeroOrNullResponse.INSTANCE.respond(invocation)
  }
}
