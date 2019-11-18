package org.simplemes.eframe.test

import groovy.transform.ToString
import org.simplemes.eframe.domain.ConstraintUtils
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
 * Builds a very simple mock/stub version of ConstraintUtils that returns the maxSize for a specific field.
 * <p>
 * <b>Note:</b> This does not mock the domain class itself.  It just provides a ConstraintUtils.instance that
 * provides a few mock methods.
 * <p>
 * <h3>Mocked Methods</h3>
 * <ul>
 *   <li><b>getPropertyMaxSize()</b> - Returns maxSize for the field.</li>
 * </ul>
 *
 * This class restores the original ConstraintUtils.instance during BaseSpecification.cleanup().
 * You can use this method like this:
 * <pre>
 *   new MockConstraintUtils(this,40).install()
 * </pre>
 * <p>
 */
@ToString(includePackage = false, includeNames = true)
class MockConstraintUtils implements AutoCleanupMockInterface, IDefaultResponse {

  /**
   * Holds the original ConstraintUtils since we have mocked it.
   * Will be auto-restored by the BaseSpecification.cleanup() method.
   */
  ConstraintUtils originalConstraintUtils

  /**
   * Holds the mocked ConstraintUtils.
   */
  ConstraintUtils mockedConstraintUtils

  /**
   * The maxSize to return for all properties.
   */
  Integer maxSize

  BaseSpecification baseSpec

  /**
   * Basic constructor.  You must call install() to make this mock active.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param maxSize The maxSize to return for all calls to getPropertyMaxSize()
   */
  MockConstraintUtils(BaseSpecification baseSpec, Integer maxSize) {
    this.maxSize = maxSize
    this.baseSpec = baseSpec
    def mockFactory = new DetachedMockFactory()
    originalConstraintUtils = ConstraintUtils.instance
    mockedConstraintUtils = mockFactory.Stub(defaultResponse: this, ConstraintUtils) as ConstraintUtils
  }

  /**
   * Installs the mock ConstraintUtils as the current ConstraintUtils instance.  Will be auto-cleaned up by the
   * BaseSpecification.
   */
  void install() {
    ConstraintUtils.instance = (ConstraintUtils) mockedConstraintUtils
    baseSpec.registerAutoCleanup(this)
  }

  /**
   * MaxSize constraint value for a property.  This mocked method always returns the maxSize.
   * @return The max size.
   */
  Integer getPropertyMaxSize() {
    return maxSize
  }


  /**
   * Performs the cleanup action.
   * @param testSpec The test that requests the cleanup.
   */
  @Override
  void doCleanup(BaseSpecification testSpec) {
    ConstraintUtils.instance = originalConstraintUtils
  }

  @Override
  Object respond(IMockInvocation invocation) {
    def methodName = invocation.method.name
    if (methodName == 'getPropertyMaxSize') {
      return getPropertyMaxSize()
    }
    return ZeroOrNullResponse.INSTANCE.respond(invocation)
  }
}
