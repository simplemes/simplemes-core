/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import groovy.transform.ToString
import org.simplemes.eframe.data.FieldDefinitions
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.web.PanelUtils
import org.spockframework.mock.IDefaultResponse
import org.spockframework.mock.IMockInvocation
import org.spockframework.mock.ZeroOrNullResponse
import spock.mock.DetachedMockFactory

import java.lang.reflect.Field

/**
 * Builds a very simple mock/stub version of DomainUtils that returns the given domain(s) classes as possible domains.
 * <p>
 * <b>Note:</b> This does not mock the domain class itself.  It just provides a DomainUtils.instance that
 * provides a few mock methods.
 * <p>
 * <h3>Mocked Methods</h3>
 * <ul>
 *   <li><b>getAllDomains()</b> - Returns the given domain in the list.</li>
 *   <li><b>getPersistentFields()</b> - For the given domain, returns a list of fields in the <code>fieldOrder</code> variable. </li>
 * </ul>
 *
 * This class restores the original DomainUtils.instance during BaseSpecification.cleanup().
 * You can use this method like this:
 * <pre>
 *   new MockDomainUtils(this,Order).install()
 * </pre>
 * <p>
 */
@ToString(includePackage = false, includeNames = true)
class MockDomainUtils implements AutoCleanupMockInterface, IDefaultResponse {

  /**
   * Holds the original DomainUtils since we have mocked it.
   * Will be auto-restored by the BaseSpecification.cleanup() method.
   */
  DomainUtils originalDomainUtils

  /**
   * Holds the mocked DomainUtils.
   */
  DomainUtils mockedDomainUtils

  /**
   * The list of classes to pretend are domain classes.
   */
  List<Class> domainClassList = []

  /**
   * The persistent entities for the simulated domain.
   */
  List<Field> persistentProperties

  /**
   * Some mocked field definitions to return when getFieldDefinitions is called.
   */
  FieldDefinitions fieldDefinitions

  BaseSpecification baseSpec

  /**
   * Basic constructor.  You must call install() to make this mock active.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param clazz The class to pretend is a domain class.
   */
  MockDomainUtils(BaseSpecification baseSpec, Class clazz) {
    this(baseSpec, [clazz])
  }

  /**
   * Basic constructor.  You must call install() to make this mock active.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param fieldDefinitions The field definitions to return for all request to getFieldDefinitions.
   */
  MockDomainUtils(BaseSpecification baseSpec, FieldDefinitions fieldDefinitions) {
    this(baseSpec, [], fieldDefinitions)
  }

  /**
   * Basic constructor.  You must call install() to make this mock active.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param classes The classes to  pretend they are domain classes.
   */
  MockDomainUtils(BaseSpecification baseSpec, List<Class> classes) {
    this.baseSpec = baseSpec
    def mockFactory = new DetachedMockFactory()
    originalDomainUtils = DomainUtils.instance
    mockedDomainUtils = mockFactory.Stub(defaultResponse: this, DomainUtils) as DomainUtils
    domainClassList.addAll(classes)
  }

  /**
   * Basic constructor.  You must call install() to make this mock active.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param classes The classes to  pretend they are domain classes.
   * @param fieldDefinitions The field definitions to return for all request to getFieldDefinitions.
   */
  MockDomainUtils(BaseSpecification baseSpec, List<Class> classes, FieldDefinitions fieldDefinitions) {
    this.baseSpec = baseSpec
    def mockFactory = new DetachedMockFactory()
    originalDomainUtils = DomainUtils.instance
    mockedDomainUtils = mockFactory.Stub(defaultResponse: this, DomainUtils) as DomainUtils
    domainClassList.addAll(classes)
    this.fieldDefinitions = fieldDefinitions
  }
  /**
   * Installs the mock domainUtils as the current DomainUtils instance.  Will be auto-cleaned up by the
   * BaseSpecification.
   */
  void install() {
    DomainUtils.instance = (DomainUtils) mockedDomainUtils
    baseSpec.registerAutoCleanup(this)
  }

  /**
   * Returns the list of mocked properties for this object.  Simulates a mocked property from the
   * list in fieldOrder.
   * @return The list of persistent fields.
   */
  List<Field> getPersistentFields() {
    if (!persistentProperties) {
      persistentProperties = []
      def clazz = domainClassList[0]
      def fieldOrders = originalDomainUtils.getStaticFieldOrder(clazz)
      //println "fieldOrders = $fieldOrders"
      //println "declared = ${clazz.getDeclaredFields()*.name}"

      for (fieldName in fieldOrders) {
        if (PanelUtils.isPanel(fieldName)) {
          // Ignore panels.
          continue
        }

        def type = DomainUtils.instance.getFieldType(clazz, fieldName) ?: String
        //noinspection GroovyAssignabilityCheck
        persistentProperties << new MockPersistentProperty(fieldName as String, type) as Field
      }
    }

    return persistentProperties
  }


  /**
   * Performs the cleanup action.
   * @param testSpec The test that requests the cleanup.
   */
  @Override
  void doCleanup(BaseSpecification testSpec) {
    DomainUtils.instance = originalDomainUtils
  }

  /**
   * A list of methods that work just fine without mocking.  We will just let the real
   * DomainUtils handle them.
   */
  static List<String> delegateToOriginal = ['getStaticFieldOrder', 'getPrimaryKeyField', 'getKeyFields',
                                            'getFieldType', 'getFieldDefinitions']

  /**
   * A real domain utils class for delegation.
   */
  static DomainUtils realDomainUtils = new DomainUtils()

  @Override
  Object respond(IMockInvocation invocation) {
    def methodName = invocation.method.name
    if (methodName == 'getAllDomains') {
      return domainClassList
    } else if (methodName == 'getDomain') {
      return domainClassList.find() { it.simpleName == invocation.arguments[0] }
    } else if (methodName == 'isGormEntity') {
      return domainClassList.contains(invocation.arguments[0])
    } else if (methodName == 'getPersistentFields') {
      return persistentFields
      //} else if (methodName == 'getPrimaryKeyField') {
      //  return persistentFields[0].name
    } else if (methodName == 'getFieldDefinitions' && fieldDefinitions) {
      return fieldDefinitions
/*
    } else if (methodName=='addChildToDomain') {
      // Perform a dummy addTo for non-hibernate unit tests.
      def domainObject = invocation.arguments[0]
      def child = invocation.arguments[1]
      def collectionName = invocation.arguments[2]
      domainObject."${collectionName}" << child
*/
    } else if (delegateToOriginal.contains(methodName)) {
      // Let the real class handle it.
      def method = DomainUtils.getDeclaredMethod(methodName, invocation.method.parameterTypes as Class[])
      if (method) {
        return method.invoke(realDomainUtils, invocation.arguments as Object[])
      }

      return persistentFields
    }
    return ZeroOrNullResponse.INSTANCE.respond(invocation)
  }
}
