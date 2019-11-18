package org.simplemes.eframe.test


import groovy.transform.ToString
import org.simplemes.eframe.custom.AdditionHelper
import org.simplemes.eframe.custom.AdditionInterface
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
 * Builds a very simple mock/stub Addition that is found by the getHelper.
 * This class restores the original AdditionHelper.instance during BaseSpecification.cleanup().
 * <pre>
 *   new MockHelper(this,TestAddition)
 * </pre>
 * <p>
 * This will simulate an addition discovered by the framework for use in other areas for test purposes.
 */
@ToString(includePackage = false, includeNames = true)
class MockAdditionHelper implements AutoCleanupMockInterface, IDefaultResponse {

  /**
   * Holds the original AdditionHelper since we have mocked it.
   * Will be auto-restored by the BaseSpecification.cleanup() method.
   */
  AdditionHelper originalAdditionHelper

  /**
   * The additions to respond to the getAdditions() call.
   */
  List<AdditionInterface> additions = []

  /**
   * The mocked addition helper
   */
  def mockAdditionHelper

  /**
   * The test that needs this mock.
   */
  BaseSpecification baseSpec

  /**
   * Basic constructor with test additions.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param classes The addition classes to use.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  MockAdditionHelper(BaseSpecification baseSpec, List<Class> classes) {
    def mockFactory = new DetachedMockFactory()
    originalAdditionHelper = AdditionHelper.instance
    mockAdditionHelper = mockFactory.Stub(defaultResponse: this, AdditionHelper)
    for (clazz in classes) {
      additions << clazz.newInstance()
    }
    this.baseSpec = baseSpec
  }

  /**
   * Installs the mock domainUtils as the current DomainUtils instance.  Will be auto-cleaned up by the
   * BaseSpecification.
   */
  void install() {
    AdditionHelper.instance = (AdditionHelper) mockAdditionHelper
    baseSpec.registerAutoCleanup(this)
  }

  /**
   * Performs the cleanup action.
   * @param testSpec The test that requests the cleanup.
   */
  @Override
  void doCleanup(BaseSpecification testSpec) {
    AdditionHelper.instance = originalAdditionHelper
  }

  @Override
  Object respond(IMockInvocation invocation) {
    if (invocation.method.name == 'getAdditions') {
      //println "invocation = ${invocation?.dump()}"
      return additions
    }
    return ZeroOrNullResponse.INSTANCE.respond(invocation)
  }
}
