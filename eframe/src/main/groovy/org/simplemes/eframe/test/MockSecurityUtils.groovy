package org.simplemes.eframe.test

import groovy.transform.ToString
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.security.SecurityUtils
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
 * Builds a very simple mock/stub for the SecurityUtils.  Defines a checkRoleFromSubClass() mock
 * method that returns a given response (e.g. HttpStatus.FORBIDDEN).
 * This class restores the original SecurityUtils.instance during BaseSpecification.cleanup().
 * <pre>
 *   new MockSecurityUtils(this,HttpStatus.FORBIDDEN).install()
 * </pre>
 * <p>
 * This will create a new instance of the SecurityUtils which always returns HttpStatus.FORBIDDEN from the
 * checkRoleFromSubClass() method.
 */
@ToString(includePackage = false, includeNames = true)
class MockSecurityUtils implements AutoCleanupMockInterface, IDefaultResponse {

  /**
   * Holds the original SecurityUtils since we have mocked it.
   * Will be auto-restored by the BaseSpecification.cleanup() method.
   */
  SecurityUtils originalSecurityUtils

  /**
   * The stubbed/mocked instance.
   */
  Object stubSecurityUtils

  /**
   * The test specification.
   */
  def baseSpec

  /**
   * The status to return from the checkRoleFromSubClass() method.
   */
  HttpStatus status

  /**
   * The results of the isAllGranted() method (*Default*: true).
   */
  Boolean allGranted = true

  /**
   * Basic constructor - implementation argument version.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param status The returned HttpStatus for the checkRoleFromSubClass() method..
   * @param allGranted The results of the isAllGranted() method (*Default*: true).
   */
  MockSecurityUtils(BaseSpecification baseSpec, HttpStatus status, Boolean allGranted = true) {
    def mockFactory = new DetachedMockFactory()
    this.status = status
    originalSecurityUtils = SecurityUtils.instance
    stubSecurityUtils = mockFactory.Stub(defaultResponse: this, SecurityUtils)
    this.baseSpec = baseSpec
  }

  /**
   * Installs the mock.
   */
  void install() {
    SecurityUtils.instance = (SecurityUtils) stubSecurityUtils
    baseSpec.registerAutoCleanup(this)
  }

  /**
   * Performs the cleanup action.
   * @param testSpec The test that requests the cleanup.
   */
  @Override
  void doCleanup(BaseSpecification testSpec) {
    SecurityUtils.instance = originalSecurityUtils
  }

  @Override
  Object respond(IMockInvocation invocation) {
    if (invocation.method.name == 'checkRoleFromSubClass') {
      return HttpResponse.status(status)
    } else if (invocation.method.name == 'isAllGranted') {
      return allGranted
    }
    return ZeroOrNullResponse.INSTANCE.respond(invocation)
  }
}
