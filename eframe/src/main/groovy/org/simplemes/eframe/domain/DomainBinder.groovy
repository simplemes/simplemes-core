package org.simplemes.eframe.domain

import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.data.FieldDefinitions
import org.simplemes.eframe.data.format.ConfigurableTypeDomainFormat
import org.simplemes.eframe.misc.ArgumentUtils

import java.text.ParseException

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides standardized binding of HTTP-style parameters to a domain or POGO class.
 *  <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Debugging information. Each parameter as it is processed. </li>
 *   <li><b>warn</b> - Unknown values in the input parameters. </li>
 * </ul>

 */
@Slf4j
class DomainBinder {

  /**
   * Factory method to build a new DomainBinder.
   * @param parentBinder The parent binder that created this binder.  Used for bind child elements and tracking errors.
   */
  static DomainBinder build(DomainBinder parentBinder = null) {
    def domainBinder = new DomainBinder()
    domainBinder.parentBinder = parentBinder
    return domainBinder
  }

  /**
   * The parent binder that created this binder.  Used for bind child elements and tracking errors.
   */
  DomainBinder parentBinder

  /**
   * The top-level field definitions for the domain class.
   */
  FieldDefinitions fieldDefs

  /**
   * The domain class this binder was built for.
   */
  Class domainClass

  /**
   * The domain object that this binder will bind to.  Only set for the top-level object.
   */
  Object domainObject

  /**
   * If true, then the string values are parsed using the UI formats.  Otherwise, uses internal decoding formats.
   * (<b>Default:</b> false)
   */
  Boolean ui = false

  /**
   * The prefix for this level of the domain (e.g. 'sampleChildren' for the SampleParent children records).
   * Used mainly for validation error reporting.
   */
  String fieldNamePrefix = ''

  /**
   * These are fields that not bound to domain objects.
   */
  List<String> fieldsToSkipBinding = ['id']

  /**
   * Binds the given parameters to the given object instance.
   * @param object The object to bind to.
   * @param params The parameters.
   * @param ui If true, then the string values are parsed using the UI formats.  Otherwise, uses internal decoding formats.
   *        (<b>Default:</b> false)
   * @return Any errors found while binding.  Mostly parse errors.
   */
  @SuppressWarnings(["GroovyAssignabilityCheck", "EmptyIfStatement"])
  void bind(Object object, Map params, Boolean ui = false) {
    ArgumentUtils.checkMissing(object, 'object')
    if (object.getClass() != domainClass) {
      domainClass = object.getClass()
      fieldDefs = ExtensibleFieldHelper.instance.getEffectiveFieldDefinitions(domainClass, object)
    }
    this.ui = ui

    // We need to get the current value for any Configurable Type fields so that the parameters
    // can be correctly processed.  Check all ConfigurableTypeDomainFormat and attempt to parse the value
    // from the input.

    // the  addConfigurableTypeFields() is called.
    boolean ctFieldsFound = false
    for (fieldDefinition in fieldDefs) {
      if (fieldDefinition.format == ConfigurableTypeDomainFormat.instance) {
        ctFieldsFound = true
        def value = params[fieldDefinition.name]
        if (value) {
          if (ui) {
            fieldDefinition.setFieldValue(object, fieldDefinition.format.parseForm(value, null, fieldDefinition))
          } else {
            fieldDefinition.setFieldValue(object, fieldDefinition.format.decode(value, fieldDefinition))
          }
        }
      }
    }
    if (ctFieldsFound) {
      fieldDefs = ExtensibleFieldHelper.instance.addConfigurableTypeFields(fieldDefs, object)
    }


    // See if this is the top-level object,
    if (!parentBinder) {
      // Remember the domain object being bound so we have a place to store validation errors.
      domainObject = object
    }

    convertUIChildParamsToAPIForm(params)
    params.each { key, value ->
      def fieldDef = fieldDefs[key as String]
      if (!key.contains('[')) {
        log.debug('bind() Binding {} for {} with value {}', key, domainClass, value)
      }
      if (fieldDef) {
        // Only process fields know of.  Ignores all others.
        Class propertyClass = fieldDef.type
        //println "$fieldDef.name = ${value?.getClass()}"
        if (value == null) {
          fieldDef.setFieldValue(object, value)
        } else if (value instanceof String) {
          // Some conversion is needed, so find the right format for conversion.
          if (fieldDef.format) {
            try {
              if (ui) {
                fieldDef.setFieldValue(object, fieldDef.format.parseForm(value, null, fieldDef))
              } else {
                fieldDef.setFieldValue(object, fieldDef.format.decode(value, fieldDef))
              }
              //println "$key -> ${object[key]}, value = ${fieldDef.format.parseForm(value,null, fieldDef).getClass()}"
            } catch (ParseException | IllegalArgumentException ignored) {
              //println "ignored = $ignored"
              //ignored.printStackTrace()
              addError(object, key, value, 'parseError')
            }
          }
        } else if (value instanceof Collection) {
          // API Mode, the value is a list, so attempt to bind any children.  Usually only custom child list supported here.
          bindChildren(object, params, fieldDef.name)
        } else {
          if (propertyClass?.isAssignableFrom(value.getClass())) {
            // Handle Unit test scenarios such as passing an Integer for an Integer field.
            if (!(value instanceof Collection)) {
              object[key] = value
            }
          } else {
            def s = "Invalid value type ${value.getClass()}, value: ${value}. Expected String or ${propertyClass}. Property $key in object $object"
            throw new UnsupportedOperationException(s)
          }
        }
      } else if (key.contains('[') && key.contains(']')) {
        // Child objects have been converted to a child list.
      } else if (fieldsToSkipBinding.contains(key)) {
        // Skip some important fields like 'id'.
      } else if (!(key?.startsWith('_'))) {
        log.warn('bind() Ignoring field {}.  No field definition in {}', key, domainClass)
      }
    }
    //bindAnyChildrenValues(object, params)
  }

  /**
   * Converts any child elements from the UI format to API format.  This converts elements like:
   * 'sampleChildren[2].title' to a sub-list of maps: sampleChildren: [ [title: 'abc']. . . ].
   * This is only done when in UI mode.
   * This method all creates a '_originalIndex' to allow for UI updates ofr existing records with key changes.
   * @param params The request parameters.
   */
  void convertUIChildParamsToAPIForm(Map params) {
    if (!ui) {
      return
    }

    // Make a copy of the list of keys we will convert to allow changes to the input params.
    List<String> keys = []
    for (String key in params.keySet()) {
      if (key.contains('[')) {
        keys << key
      }
    }
    for (String key in keys) {
      def start = key.indexOf('[')
      def prefix = key[0..start - 1]
      def end = key.indexOf(']')
      def indexS = key[start + 1..end - 1]
      def index = Integer.valueOf(indexS)
      def elementName = key[(end + 2)..-1]

      // Now, add the value to a sub-list in the right location
      List list = (List) params[prefix]
      if (list == null) {
        list = []
        params[prefix] = list
      }
      Map row = (Map) list[index]
      if (row == null) {
        row = [:]
        list[index] = row
      }
      row[elementName] = params[key]
      row._originalIndex = index
    }
  }

  /**
   * Binds the given parameters to the given object instance for child elements.
   * @param object The object to bind to.
   * @param paramsIn the Top-level map of parameters to process.
   * @param prefix The parameter name prefix to process.
   */
  protected void bindChildren(Object object, Map paramsIn, String prefix) {
    def subBinder = build(this)
    subBinder.fieldNamePrefix = prefix

    // First, get the list
    def fieldDef = fieldDefs[prefix]
    Collection originalList = (Collection) fieldDef.getFieldValue(object)
    //println "fieldDef = $fieldDef"

    // and the list of input maps to use for the binding
    List<Map> list = (List<Map>) paramsIn[prefix]

    deleteUnusedChildRecords(originalList, list, prefix, fieldDef)

    // Now, bind the rows, one row at a time for all elements in that row.
    for (Map params in list) {
      if (params) {
        // Remove the 'id' column since it is not used in child updates ('dbId' is).
        params.remove('id')

        // Then Bind them into the right object
        if (fieldDef?.child) {
          // Find the right object to bind into (may be existing row)
          def record = null
          if (params._dbId) {
            // Should exist already in the DB, so find it
            def dbId = Long.valueOf((String) params._dbId)
            params.remove('_dbId')
            record = originalList.find { it.id == dbId }
          }
          if (!record) {
            // Must be a new child record, so just create it.
            record = fieldDef.referenceType.newInstance()
            if (object.hasProperty(prefix)) {
              // This is a natural child list in the domain.
              DomainUtils.instance.addChildToDomain(object, record, prefix)
            } else {
              // This is a custom child list, so we can just add the new record to the list.
              // The save() interceptor will persist it.
              originalList << record
            }
          }
          // Now, change record with the current field values.
          subBinder.bind(record, params, ui)
        }
      }
    }
  }

  /**
   * Deletes the DB records for child records that are not in the updated list of children.
   * @param originalList The original list of records. This is updated too (removes the deleted record).
   * @param list The new list of children (map of input parameters).
   * @param fieldName The name of the field these children belongs to.
   * @param fieldDef The field definition.
   */
  void deleteUnusedChildRecords(Collection originalList, List<Map> list, String fieldName, FieldDefinitionInterface fieldDef) {
    // If this is an update, then figure out which rows are no longer in the child list, then remove them.

    // Start with a list of all IDs and remove them as they are found in the input params.
    // Leaves us with a list of IDs the user removed.
    def idToRemoveList = originalList*.id
    for (params in list) {
      if (params?._dbId) {
        def id = Long.valueOf((String) params._dbId)
        idToRemoveList.removeAll { it == id }
      }
    }

    // Now, remove any IDs left over from the input search above
    for (id in idToRemoveList) {
      def record = originalList.find { it.id == id }
      originalList.removeAll { it.id == id }
      if (fieldDef?.child && record) {
        log.debug('bindChildren(): Deleting ID {} for {} ({}).  Record: {}', id, fieldName, record.getClass().simpleName, record)
        record.delete()
      }
    }

  }

  /**
   * Adds the given message as a validation error to the current list of errors.
   * This will bubble-up the error to the top-level domain object as needed.
   * @param domainObject The domain object the error is for.
   * @param fieldName The field that failed.
   * @param value The bad value.
   * @param messageKey The message.properties error key for the message.
   */
  void addError(Object domainObject, String fieldName, Object value, String messageKey) {
    if (parentBinder) {
      // Let the parent know of the binding error.
      def childFieldName = "${fieldNamePrefix}.$fieldName"
      parentBinder.addError(domainObject, childFieldName, value, messageKey)
      return
    }
    // We must be the top-level binder, so build the error list in the top-level domain object.
    DomainUtils.instance.addValidationError(this.domainObject, fieldName, value, messageKey)
  }

}
