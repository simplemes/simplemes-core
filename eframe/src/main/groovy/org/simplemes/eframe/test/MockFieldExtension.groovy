package org.simplemes.eframe.test

import groovy.transform.ToString
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.data.CustomFieldDefinition
import org.simplemes.eframe.data.FieldDefinitions
import org.simplemes.eframe.data.format.FieldFormatInterface
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.domain.DomainUtils
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
 * Builds a very simple mock/stub version of ExtensibleFieldHelper that returns the given custom fields.
 * <p>
 * <b>Note:</b> This does not mock the domain class itself.  It just provides a ExtensibleFieldHelper.instance that
 * provides a few mock methods.
 * <p>
 * <h3>Mocked Methods</h3>
 * <ul>
 *   <li><b>getEffectiveFieldDefinitions()</b> - Returns the field definitions from the domain class plus these extensions.</li>
 *   <li><b>getEffectiveFieldOrder()</b> - Returns the field order from the domain class plus these extensions. </li>
 * </ul>
 *
 * This class restores the original ExtensibleFieldHelper.instance during BaseSpecification.cleanup().
 * You can use this method like this:
 * <pre>
 *   new ExtensibleFieldHelper(this,[fieldName:'abc']).install()
 * </pre>
 * <p>
 * <h3>Support options</h3>
 * <ul>
 *   <li><b>domainClassName</b> - The domain class to add the custom field to.</li>
 *   <li><b>fieldName</b> - The custom field name.</li>
 *   <li><b>format</b> - The custom field's format.</li>
 *   <li><b>afterFieldName</b> - The field this custom field should be displayed after.</li>
 * </ul>
 * <p>
 * <b>Note:</b> The options can be changed after the mock is created.
 *
 */
@ToString(includePackage = false, includeNames = true)
class MockFieldExtension implements AutoCleanupMockInterface, IDefaultResponse {

  /**
   * Holds the original ExtensibleFieldHelper since we have mocked it.
   * Will be auto-restored by the BaseSpecification.cleanup() method.
   */
  ExtensibleFieldHelper originalExtensibleFieldHelper

  /**
   * Holds the mocked ExtensibleFieldHelper.
   */
  ExtensibleFieldHelper mockedExtensibleFieldHelper

  /**
   * The input options for the custom field(s) to mock.
   */
  List<Map> options

  /**
   * The test this is run in.
   */
  BaseSpecification baseSpec

  /**
   * Basic constructor.  You must call install() to make this mock active.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   */
  MockFieldExtension(BaseSpecification baseSpec) {
    this(baseSpec, [:])
  }

  /**
   * Basic constructor.  You must call install() to make this mock active.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param options The options for a single mocked custom field.
   */
  MockFieldExtension(BaseSpecification baseSpec, Map options) {
    this(baseSpec, [options])
  }

  /**
   * Basic constructor.  You must call install() to make this mock active.
   *
   * @param baseSpec The test specification that needs the mock (usually this).
   * @param options The options for 1+ mocked custom field.
   */
  MockFieldExtension(BaseSpecification baseSpec, List<Map> options) {
    this.baseSpec = baseSpec
    def mockFactory = new DetachedMockFactory()
    originalExtensibleFieldHelper = ExtensibleFieldHelper.instance
    mockedExtensibleFieldHelper = mockFactory.Stub(defaultResponse: this, ExtensibleFieldHelper) as ExtensibleFieldHelper
    this.options = options
  }

  /**
   * Installs the mock domainUtils as the current DomainUtils instance.  Will be auto-cleaned up by the
   * BaseSpecification.
   */
  MockFieldExtension install() {
    ExtensibleFieldHelper.instance = (ExtensibleFieldHelper) mockedExtensibleFieldHelper
    baseSpec.registerAutoCleanup(this)
    return this
  }

  /**
   * Mock for: Find the effective FieldDefinitions for the given domain/POGO class.
   * This includes the core fields and any custom fields added by FieldExtensions.
   * @param domainClass The class to find the field order in.
   * @return A list of field names, in the display field order.
   */
  FieldDefinitions getEffectiveFieldDefinitions(Class domainClass) {
    def fieldDefinitions = DomainUtils.instance.getFieldDefinitions(domainClass)
    // Build any extra fields for the mocked extensions
    for (option in options) {
      if (option.domainClass == domainClass) {
        String name = option.fieldName
        FieldFormatInterface format = (FieldFormatInterface) option.format ?: StringFieldFormat.instance
        fieldDefinitions[name] = new CustomFieldDefinition(name: name, format: format, label: option.label)
        if (option.required) {
          fieldDefinitions[name].required = option.required
        }
        fieldDefinitions[name].historyTracking = option.historyTracking ?: fieldDefinitions[name].historyTracking
      }
    }
    return fieldDefinitions
  }

  /**
   * Mock for: Determines the effective field order after applying the addition (extension) modules to the domain and then
   * the user's adjustments to the static fieldOrder from the domain class.
   * @param domainClass The domain class to determine the field order for.
   * @param fieldOrder The field order to adjust.
   * @return The effective field order list.  This is a single flattened list of field names.
   */
  List<String> getEffectiveFieldOrder(Class domainClass, List<String> fieldOrder = null) {
    if (fieldOrder == null) {
      fieldOrder = (List) DomainUtils.instance.getStaticFieldOrder(domainClass).clone()
    }
    // Build any extra fields for the mocked extensions
    for (option in options) {
      if (option.domainClass == domainClass && option.afterFieldName) {
        //     [A, B, C] | [A, B, D, C]    | [adj("Insert $D $B"), adj("Move $C $D")]                                         | 'Add new one in the middle'
        new FieldInsertAdjustment(fieldName: option.fieldName, afterFieldName: option.afterFieldName).apply(fieldOrder)
        //new FieldMoveAdjustment(fieldName: option.fieldName, afterFieldName: option.afterFieldName).apply(fieldOrder)
      }
    }
    return fieldOrder
  }

  /**
   * Performs the cleanup action.
   * @param testSpec The test that requests the cleanup.
   */
  @Override
  void doCleanup(BaseSpecification testSpec) {
    ExtensibleFieldHelper.instance = originalExtensibleFieldHelper
  }

  @Override
  Object respond(IMockInvocation invocation) {
    def methodName = invocation.method.name
    if (methodName == 'getEffectiveFieldDefinitions') {
      return getEffectiveFieldDefinitions((Class) invocation.arguments[0])
    } else if (methodName == 'getEffectiveFieldOrder') {
      return getEffectiveFieldOrder((Class) invocation.arguments[0], (List) invocation.arguments[1])
    }
    return ZeroOrNullResponse.INSTANCE.respond(invocation)
  }
}
