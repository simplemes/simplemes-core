/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.gui.FieldAdjustmentInterface
import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.data.EncodedTypeInterface
import org.simplemes.eframe.data.format.FieldFormatInterface
import org.slf4j.Logger

/**
 * Defines the features/requirements for a given Module (Addition) to be added to the application.
 * This includes definitions of EncodedType classes, data loaders, domain class package(s) and
 * extensions to a core module.
 * <p>
 * Your module should create a sub-class and define the static <code>addition</code> variable as follows:
 * <pre>
 * class SimpleAddition extends BaseAddition &#123;
 *   AdditionConfiguration addition = Addition.configure &#123;
 *     encodedType BasicStatus
 *     field &#123;
 *       domain SampleParent
 *       name 'warehouse'
 *     &#125;
 *   &#125;
 * &#125;
 * </pre>
 */
class Addition {

  /**
   * The main entry point to define the configuration for the addition.
   * @param closure The configuration closure (DSL).
   * @return The addition configuration.
   */
  static AdditionConfiguration configure(@DelegatesTo(AdditionConfiguration) Closure closure) {
    def additionConfiguration = new AdditionConfiguration()
    def code = closure.rehydrate(additionConfiguration, this, this)
    code.resolveStrategy = Closure.DELEGATE_ONLY
    code()
    additionConfiguration.name = additionConfiguration.name ?: closure.getClass().enclosingClass.simpleName
    additionConfiguration.validate()
    return additionConfiguration
  }
}


/**
 * The addition configuration details.
 */
@Slf4j
@ToString(includePackage = false, includeNames = true, includeFields = true, ignoreNulls = true)
class AdditionConfiguration {

  /**
   * The addition name.
   */
  String name

  /**
   * The list of fields defined for this addition.
   */
  List<AdditionFieldConfiguration> fields = []

  /**
   * The encoded type classes defined by this addition.
   */
  List<Class<EncodedTypeInterface>> encodedTypes = []

  /**
   * The initial data loader classes defined by this addition.
   */
  List<Class> initialDataLoaders = []

  /**
   * Defines a single addition field.
   * @param fieldClosure The field configuration.  See {@link AdditionFieldConfiguration}.
   */
  void field(@DelegatesTo(AdditionFieldConfiguration) Closure fieldClosure) {
    def fieldConfig = new AdditionFieldConfiguration()
    def code = fieldClosure.rehydrate(fieldConfig, this, this)
    code.resolveStrategy = Closure.DELEGATE_ONLY
    code()
    fields << fieldConfig
  }

  /**
   * The list of assets defined for this addition.
   */
  List<AdditionAssetConfiguration> assets = []

  /**
   * Defines a single addition asset.
   * @param assetClosure The asset configuration.  See {@link AdditionAssetConfiguration}.
   */
  void asset(@DelegatesTo(AdditionAssetConfiguration) Closure assetClosure) {
    def assetConfiguration = new AdditionAssetConfiguration()
    def code = assetClosure.rehydrate(assetConfiguration, this, this)
    code.resolveStrategy = Closure.DELEGATE_ONLY
    code()
    assets << assetConfiguration
  }

  /**
   * The addition name.
   */
  void name(String name) {
    this.name = name
  }

  /**
   * The encoded type classes defined by this addition.
   * @param type The base class for this encoded type.
   */
  void encodedType(Class<EncodedTypeInterface> type) {
    encodedTypes << type
  }

  /**
   * Defines an initial data loader class provided by this addition.
   */
  void initialDataLoader(Class loader) {
    initialDataLoaders << loader
  }

  /**
   * Validates the addition configuration.  Mainly checks for correct Classes in various fields.
   * Logs an error message when it fails.
   * @return True if it passes validation.
   */
  boolean validate() {
    def passed = true

    for (type in encodedTypes) {
      if (!EncodedTypeInterface.isAssignableFrom(type)) {
        passed = false
        log.error('Addition {}: encodedType {} is not a valid EncodedTypeInterface', name, type.name)
      }
    }

    for (loader in initialDataLoaders) {
      def method = loader.getDeclaredMethods().find { it.name == 'initialDataLoad' }
      if (!method) {
        passed = false
        log.error('Addition {}: initialDataLoader for {} is not a valid.  It does not have an initialDataLoad() method',
                  name, loader)
      }
    }

    if (name == '???') {
      log.warn('Addition: name is not set for {}.  ', name)
    }

    // Validate each field.
    for (field in fields) {
      def fRes = field.validate(log, name)
      if (!fRes) {
        passed = false
      }
    }

    return passed
  }
}

/**
 * Defines a single field's configuration.
 */
@ToString(includePackage = false, includeNames = true, ignoreNulls = true)
class AdditionFieldConfiguration {
  Class domainClass
  String name
  String label
  FieldFormatInterface format
  Integer maxLength
  Class valueClass
  List<FieldAdjustmentInterface> fieldOrderAdjustments = []
  String guiHints


  /**
   * The domain to add this field to.
   * @param clazz The domain class.
   */
  void domain(Class clazz) { domainClass = clazz }

  /**
   * The name of the custom field to add.
   * @param name The field name.
   */
  void name(String name) { this.name = name }

  /**
   * The label of the custom field to add. (Optional).
   * @param label The field label.
   */
  void label(String label) { this.label = label }

  /**
   * The format of the custom field.
   * @param format The format (Class).
   */
  void format(Class<FieldFormatInterface> format) { this.format = format.instance }

  /**
   * The max length of the custom field value (String).
   * @param length The max length.
   */
  void maxLength(Integer length) { this.maxLength = length }

  /**
   * The class for the value.  This is used mainly for DomainReferences,
   * Enumeration and EncodedTypes.
   * @param clazz The value class.
   */
  void valueClass(Class clazz) { this.valueClass = clazz }

  /**
   * GUI Hints to add to the display of these additions.  These
   * are typically attributes supported by markers such as efCreate.
   *
   * @param hints The hints.
   */
  void guiHints(String hints) { this.guiHints = hints }

  /**
   * A field order definition for the field.
   * @param fieldClosure The field order configuration elements.  See {@link AdditionFieldOrderConfiguration}.
   */
  void fieldOrder(@DelegatesTo(AdditionFieldOrderConfiguration) Closure fieldClosure) {
    def fieldOrderConfiguration = new AdditionFieldOrderConfiguration()
    def code = fieldClosure.rehydrate(fieldOrderConfiguration, this, this)
    code.resolveStrategy = Closure.DELEGATE_ONLY
    code()
    def adj = new FieldInsertAdjustment(fieldName: fieldOrderConfiguration.name,
                                        afterFieldName: fieldOrderConfiguration.after)
    fieldOrderAdjustments << adj
  }

  /**
   * Validates the field extension configuration.  Mainly checks for correct Classes in various fields.
   * Logs an error message when it fails.
   * @param log The log to write the messages to.  This typically uses the logger from the AdditionConfiguration.
   * @return True if it passes validation.
   */
  boolean validate(Logger log, String additionName) {
    def passed = true

    if (!name) {
      passed = false
      log.error('Addition {}: A field extension has no name.', additionName)
    }

    // We can't validate the domainClass since the domain list is not valid during startup.

    // Validate each field order.
    for (adj in fieldOrderAdjustments) {
      if (!adj.fieldName) {
        passed = false
        log.error('Addition {}: field {} fieldOrder has no name.', additionName, name)
      }
    }

    return passed
  }
}

/**
 * Defines a single field order definition for a field.
 */
@ToString(includePackage = false, includeNames = true, ignoreNulls = true)
class AdditionFieldOrderConfiguration {
  String name
  String after

  /**
   * Defines the field name that is adjusted in the GUI display order.
   * @param name The field name to insert in the GUI display order.
   */
  void name(String name) { this.name = name }

  /**
   * Defines the field this new field is inserted after.
   * @param after The field this new field is inserted after.
   */
  void after(String after) { this.after = after }
}

/**
 * Defines a single asset addition to page(s).
 */
@ToString(includePackage = false, includeNames = true, ignoreNulls = true)
class AdditionAssetConfiguration {
  String page
  String script
  String css

  /**
   * The page the asset should be applied to.
   * @param page The page (e.g. 'dashboard/index', does not include .ftl).
   */
  void page(String page) { this.page = page }

  /**
   * The javascript asset that should be used on the page.
   * @param script The javascript asset that should be used on the page.
   */
  void script(String script) { this.script = script }

  /**
   * The CSS asset that should be used on the page.
   * @param css The css asset that should be used on the page.
   */
  void css(String css) { this.css = css }

}
