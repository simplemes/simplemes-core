package org.simplemes.eframe.test

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.data.format.ChildListFieldFormat
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.web.PanelUtils


/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Helps tests common features of domain classes, such as constraints and field order.
 * <p>
 * <b>Note:</b> This tester does not save any records to the database.
 */
@Slf4j
@ToString(includeNames = true, includePackage = false)
class DomainTester {
  /**
   * The domain class to test.
   */
  Class _domain

  /**
   * A map of the max size checks to make.
   */
  Map<String, Integer> _maxSizes = [:]

  /**
   * A list of the fields to make sure the non-null validation works.
   */
  List<String> _notNullChecks = []

  /**
   * A map of the required field values for a valid save.
   */
  Map<String, Object> _requiredValues = [:]

  /**
   * The list of fields that should not be in the field order.
   */
  List<String> _notInFieldOrder = []

  /**
   * If true, then the field order checks are made.
   */
  Boolean _fieldOrderCheck = true

  /**
   * Sets the domain class to test.
   * @param domain The domain class
   */
  void domain(final Class domain) { this._domain = domain }

  /**
   * Sets the required values for the domain object.
   * @param domain The domain class
   */
  void requiredValues(final Map<String, Object> requiredValues) { this._requiredValues = requiredValues }

  /**
   * Adds a maxSize check for the given field.
   * @param fieldName The field to check
   * @param size The expected max size
   */
  void maxSize(String fieldName, int size) { this._maxSizes[fieldName] = size }

  /**
   * Verifies that the nulls can't be saved for the given field.
   * @param fieldName The field to check
   */
  void notNullCheck(String fieldName) { this._notNullChecks << fieldName }

  /**
   * Verifies that the given fields are not in the Domain's field order.
   * All other persistent fields must be in the fieldOrder.
   * @param fieldNames The fields that should not be in the fieldOrder.
   */
  void notInFieldOrder(List<String> fieldNames) { this._notInFieldOrder = fieldNames }


  /**
   * If true, then the field order checks are made.
   * @param fieldOrderCheck If true, then the checks are performed (<b>Default:</b> true)
   */
  void fieldOrderCheck(Boolean fieldOrderCheck = true) { this._fieldOrderCheck = fieldOrderCheck }


  static DomainTester test(@DelegatesTo(DomainTester) final Closure config) {
    DomainTester domainTester = new DomainTester()
    domainTester.with config

    ArgumentUtils.checkMissing(domainTester._domain, 'domain')

    domainTester.doTest()

    return domainTester
  }

  /**
   * Performs the test.  Throws an exception on failures.
   * @return True if no exception thrown.
   */
  boolean doTest() {
    _maxSizes.each() { k, v ->
      doMaxSizeTest(k, v)
    }
    for (fieldName in _notNullChecks) {
      doNotNullTest(fieldName)
    }
    testFieldOrder()

    return true
  }

  /**
   * Performs a single max size check.
   * @param fieldName The field to check
   * @param maxLength The expected max size
   */
  void doMaxSizeTest(String fieldName, int maxLength) {
    // Build a valid object with all required fields populated.
    def obj = _domain.newInstance()
    def fieldDefs = DomainUtils.instance.getFieldDefinitions(_domain)
    for (p in _requiredValues) {
      def fieldDef = fieldDefs[p.key]
      if (fieldDef?.format == ChildListFieldFormat.instance) {
        // Special case a true child list.
        DomainUtils.instance.addChildToDomain(obj, p.value, p.key)
      } else {
        obj[p.key] = p.value
      }
    }

    // Test max length
    StringBuffer sb = new StringBuffer()
    for (int i = 0; i < maxLength; i++) {
      sb.append('A')
    }

    _domain.withTransaction() {
      obj[fieldName] = sb.toString()
      obj.validate()
      def errors = null
      if (obj.errors.errorCount) {
        errors = GlobalUtils.lookupValidationErrors(obj)
      }
      assert obj.errors.errorCount == 0, "Expected No Failure on  $_domain.simpleName($obj) for field '$fieldName', size = ${maxLength}. Errors = $errors"
      assert obj.validate()

      // Make sure it fails for one char too big.
      sb.append('X')
      obj[fieldName] = sb.toString()
      assert !obj.validate(), "Expected Validation Failure on  $_domain.simpleName($obj) for field '$fieldName', size = ${maxLength + 1}"
      assert obj.errors.errorCount == 1
      //noinspection GroovyAssignabilityCheck
      def error = obj.errors.allErrors[0]
      assert error.codes.contains('maxSize.exceeded')
    }

  }

  /**
   * Performs a not null check.
   * @param fieldName The field to check
   */
  void doNotNullTest(String fieldName) {
    // Build a valid object with all required fields populated.
    def obj = _domain.newInstance()
    for (p in _requiredValues) {
      obj[p.key] = p.value
    }

    // Now, clear the non-nullable field
    obj[fieldName] = null

    _domain.withTransaction() {
      // and make sure it fails
      assert !obj.validate(), "Nulls allowed on field '$fieldName' in $_domain.simpleName for $obj.  Expected a validation failure on the null field"
      assert obj.errors.errorCount == 1
      //noinspection GroovyAssignabilityCheck
      def error = obj.errors.allErrors[0]
      assert error.codes.contains("nullable.$fieldName")
    }
  }

  /**
   * Verifies that all fields are listed in the fieldOrder static value.
   * Checks parent classes too.
   */
  void testFieldOrder() {
    if (!_fieldOrderCheck) {
      log.debug('Field Order Check disabled')
      return
    }
    // Find all fieldOrder elements that apply to this domain.
    def fields = DomainUtils.instance.getStaticFieldOrder(_domain)

    //def d = Grails.application.getDomainClass(domainClass.name)
    def props = DomainUtils.instance.getPersistentFields(_domain)
    def propNames = props*.name
    //println "propNames = $propNames"

    // Remove any ignored fields (if needed).
    if (_notInFieldOrder) {
      propNames = propNames.findAll { !_notInFieldOrder.contains(it) }
    }
    for (name in propNames) {
      if (!DomainUtils.instance.isPropertySpecial(_domain, name) && !_notInFieldOrder?.contains(name)) {
        assert fields.contains(name): "Field $name not in fieldOrder"
      }
      fields.remove(name)
    }

    // Remove any grouping elements.
    fields = fields.findAll { !PanelUtils.isPanel(it) }

    // Remove any transient fields since they are allowed in the fieldOrder.
    def transients = TypeUtils.getStaticPropertyInSuperClasses(_domain, 'transients')
    if (transients) {
      // Flatten into a single list.
      def flatList = []
      transients.each { flatList.addAll(it) }
      fields = fields.findAll { !flatList.contains(it) }
    }

    // Now, make sure no extra properties are added to the fieldOrder array.
    assert fields.size() == 0: "Fields (${fields}) is in fieldOrder but not a property in the domain class."
  }

}
