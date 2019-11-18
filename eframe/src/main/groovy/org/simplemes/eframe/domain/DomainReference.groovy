package org.simplemes.eframe.domain

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.misc.ArgumentUtils

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A simple POGO that contains the information needed for a reference to a domain property.
 * Used primarily to encapsulate the way to access a property on an arbitrary domain with a possibly
 * nested reference.
 */
@Slf4j
@ToString(includeNames = true)
class DomainReference {
  /**
   * The delimiter used to denote a sub-object reference (e.g. flexType.fields).
   */
  public static final String SUB_OBJECT_DELIMITER = '.'

  /**
   * The model this field belongs in.
   */
  Object model

  /**
   * The real field name (without any nesting references).
   */
  String fieldName

  /**
   * The domain class this field is a part of.
   */
  Class clazz

  /**
   * The prefix of the nested reference.
   */
  String prefix

  /**
   * The domain class the property is part of.
   */
  Class domainClass

  /**
   * The field definition for this field.
   */
  FieldDefinitionInterface fieldDefinition

  /**
   * Determines the value of the field from the model.
   * @return The value(can be null).
   */
  Object getValue() {
    if (model == null) {
      return null
    }
    if (!model.hasProperty(fieldName)) {
      def originalRef = fieldName
      if (prefix) {
        originalRef = "${prefix}.${fieldName}"
      }
      throw new IllegalArgumentException("No property ${this.fieldName} in (${this.model.getClass().name}) for reference $originalRef")
    }

    return model?."${fieldName}"
  }

  /**
   * Builds domain reference from a given field name.
   * Supports nested object references (e.g. router.operations) from a given field name.
   * Returns the effective references to the object.  If the reference does not contain a nested reference, then the
   * current model/name are used.  Also supports custom list of object references.
   * @param fieldName The field name.  If it contains a period, then this method
   *                  will resolve the correct model and effective field name.  Only supports one level deep.
   * @param model The object value to extract values from.
   * @return The correct domain reference.
   */
  static DomainReference buildDomainReference(String fieldName, Object model) {
    ArgumentUtils.checkMissing(model, 'model')
    ArgumentUtils.checkMissing(fieldName, 'fieldName')
    def domainReference = new DomainReference()
    domainReference.resolveNestedObjectReference(fieldName, model)
    return domainReference
  }

  /**
   * Resolves a nested object reference (e.g. router.operations) from a given field name.
   * This is similar to the static factory method, but operates on the current object to set the fields.
   * Returns the effective references to the object.  If the reference does not contain a nested reference, then the
   * current model/name are used.
   * @param fieldName The field name.  If it contains a period, then this method
   *                  will resolve the correct model and effective field name.  Only supports one level deep.
   * @param model The object the field is in.
   */
  protected void resolveNestedObjectReference(String fieldName, Object model) {
    log.trace("resolve() fieldName = {}, model = {}, clazz = {}", fieldName, model, clazz)

    if (fieldName?.contains(SUB_OBJECT_DELIMITER)) {
      def l = fieldName.tokenize(SUB_OBJECT_DELIMITER)
      assert l.size() == 2, "Does not support nesting more than one level.  '${fieldName}'"
      this.fieldName = l[1]
      def subModelName = l[0]
      domainClass = model.getClass()
      def fieldDefs = DomainUtils.instance.getFieldDefinitions(domainClass)
      if (!model.hasProperty(subModelName)) {
        throw new IllegalArgumentException("No property $subModelName in (${model.getClass().name}) for reference $fieldName")
      }
      this.model = model[subModelName]
      this.prefix = l[0]
      this.clazz = fieldDefs[this.fieldName]?.type
    } else {
      this.fieldName = fieldName
      this.model = model
      this.clazz = clazz
    }
  }

  /**
   * Builds domain reference from a given field name.
   * Supports the field name format: 'User.password') to find the correct field.
   * @param fieldName The field name.  If it contains a period, then this method
   *                  will resolve the correct domain from the list of domains. Only supports one level deep.
   * @return The correct domain reference.
   */
  static DomainReference buildDomainReference(String fieldName) {
    ArgumentUtils.checkMissing(fieldName, 'fieldName')
    def domainReference = new DomainReference()
    domainReference.resolveDomainPropertyReference(fieldName)
    return domainReference
  }

  /**
   * Resolves a domain property reference (e.g. Order.qty) from a given field name.
   * @param fieldName The field name.  If it contains a period, then this method
   *                  will resolve the correct domain from the list of domains. Only supports one level deep.
   */
  protected void resolveDomainPropertyReference(String fieldName) {
    log.trace("resolve() fieldName = {}, clazz = {}", fieldName, clazz)
    if (!fieldName?.contains(SUB_OBJECT_DELIMITER)) {
      throw new IllegalArgumentException("Field '$fieldName' does not contain a period.")
    }

    def l = fieldName.tokenize(SUB_OBJECT_DELIMITER)
    assert l.size() == 2, "Does not support nesting more than one level.  '${fieldName}'"
    this.fieldName = l[1]
    def domainName = l[0]
    domainClass = DomainUtils.instance.getAllDomains().find { it.simpleName == domainName }
    if (!domainClass) {
      throw new IllegalArgumentException("No domain '$domainName' found for field reference '$fieldName'")
    }

    def fieldDefs = DomainUtils.instance.getFieldDefinitions(domainClass)
    def fieldDef = fieldDefs[this.fieldName]
    if (!fieldDef) {
      throw new IllegalArgumentException("No property '${this.fieldName}' in (${domainClass.name}) for reference $fieldName")
    }
    this.prefix = l[0]
    clazz = fieldDef.type
    fieldDefinition = fieldDef
  }

}
