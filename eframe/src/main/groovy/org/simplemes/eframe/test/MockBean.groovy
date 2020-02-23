/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.TypeUtils
import org.spockframework.mock.IDefaultResponse
import org.spockframework.mock.IMockInvocation
import org.spockframework.mock.ZeroOrNullResponse
import spock.mock.DetachedMockFactory

/**
 * Builds a very simple mock/stub context and lets the context find the given bean with the getBean() method.
 * This class restores the original Holders.applicationContext during BaseSpecification.cleanup().
 * <pre>
 *   new MockBean(this,AssetPipelineService).install()
 * </pre>
 * <p>
 * This will create a new instance of AssetPipelineService and return it when the getBean(AssetPipelineService)
 * method is called.  You can provide your own instance of the bean if needed.
 */
@Slf4j
@ToString(includePackage = false, includeNames = true)
class MockBean implements AutoCleanupMockInterface, IDefaultResponse {

  /**
   * Holds the original ApplicationContext since we have mocked it.
   * Will be auto-restored by the BaseSpecification.cleanup() method.
   */
  ApplicationContext originalApplicationContext

  /**
   * The class to mock the bean for.
   */
  Class clazz

  /**
   * The response to the getBean() call.
   */
  Object beanImpl

  /**
   * The mocked application context.
   */
  def context

  /**
   * The test specification.
   */
  def baseSpec

  /**
   * Basic constructor.  This constructor will use a newInstance() from the class for the implementation.
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param clazz The class to create a bean for.
   */
  MockBean(BaseSpecification baseSpec, Class clazz) {
    this(baseSpec, clazz, null)
  }


  /**
   * Basic constructor - implementation argument version.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param clazz The class to create a bean for.
   * @param impl The implementation returned by the mocked application context.  The response to the getBean() call.
   */
  MockBean(BaseSpecification baseSpec, Class clazz, Object impl) {
    def mockFactory = new DetachedMockFactory()
    this.clazz = clazz
    originalApplicationContext = Holders.applicationContext
    context = mockFactory.Stub(defaultResponse: this, ApplicationContext)
    if (!impl) {
      impl = clazz.newInstance()
    }
    beanImpl = impl
    this.baseSpec = baseSpec
  }

  /**
   * Installs the mock.
   */
  void install() {
    Holders.applicationContext = (ApplicationContext) context
    baseSpec.registerAutoCleanup(this)
    log.debug('install() Mock Context {}', context)
  }

  /**
   * Performs the cleanup action.
   * @param testSpec The test that requests the cleanup.
   */
  @Override
  void doCleanup(BaseSpecification testSpec) {
    Holders.applicationContext = originalApplicationContext
  }

  /**
   * Returns the appropriate bean implementation, if needed.
   * @return The bean implementation for the getBean() response.
   */
  Object getBeanImpl() {
    if (!beanImpl) {
      beanImpl = clazz.newInstance()
    }
    log.debug('getBeanImpl() Returning bean {}', beanImpl)
    return beanImpl
  }

  @Override
  Object respond(IMockInvocation invocation) {
    if (invocation.method.name == 'getBean') {
      //println "invocation = ${invocation?.dump()}"
      Class expectedClass = (Class) invocation?.arguments[0]
      def o = getBeanImpl()
      if (expectedClass?.isAssignableFrom(o?.getClass())) {
        // Only return the impl if it is the right class for the impl we have
        return o
      } else {
        if (originalApplicationContext && !TypeUtils.isMock(originalApplicationContext)) {
          return originalApplicationContext.getBean(expectedClass)
        }
        return null
      }
    } else if (invocation.method.name == 'findOrInstantiateBean') {
      return Optional.of(getBeanImpl())
    }
    return ZeroOrNullResponse.INSTANCE.respond(invocation)
  }
}
