package org.simplemes.eframe.custom

import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.custom.gui.FieldAdjuster
import org.simplemes.eframe.data.AdditionFieldDefinition
import org.simplemes.eframe.data.ConfigurableTypeInterface
import org.simplemes.eframe.data.CustomFieldDefinition
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.data.FieldDefinitions
import org.simplemes.eframe.data.annotation.ExtensibleFields
import org.simplemes.eframe.data.format.ConfigurableTypeDomainFormat
import org.simplemes.eframe.data.format.ListFieldLoaderInterface
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.ShortTermCacheMap
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.misc.TypeUtils

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This class defines methods to access extensible field definitions define by module additions, users
 * and flex types.  This ties together data definition and GUI hints.
 *
 * <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Lists custom fields added to core objects. </li>
 *   <li><b>trace</b> - Logs whenever a value is added/retrieved from the custom field storage.
 *                      Also logs when the child records are stored for custom child lists. </li>
 * </ul>
 *
 */
@Slf4j
class ExtensibleFieldHelper {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static ExtensibleFieldHelper instance = new ExtensibleFieldHelper()

  /**
   * The name of the short-term cache stored in the complex custom fields for performance caching.
   */
  static final String SHORT_TERM_CACHE_NAME = '_shortFieldCache'

  /**
   * The name of the element in the short-term cache to hold the core field definitions.  These are the
   * core fields with additions and field extensions.
   */
  static final String FIELD_DEF_CACHE_NAME = 'fieldDefs'


  /**
   * Find the effective FieldDefinitions for the given domain/POGO class.
   * This includes the core fields and any custom fields added by FieldExtensions.
   * <p>
   * <b>Note:</b> This method does not populate the Configurable Type elements.  Those a typically
   *   dynamic and are based on values in the specific object. The can be added by the addConfigurableTypeFields() method.
   * @param domainClass The class to find the field order in.
   * @param object Optional domain object the get is for.  This is used to cache the field definitions
   *        when processing multiple fields in setFieldValue/getFieldValue methods.
   *        The cache is for a short term only (<1 second typically).
   * @return A list of field names, in the display field order.
   */
  FieldDefinitions getEffectiveFieldDefinitions(Class domainClass, Object object = null) {
    // Get the field defs from the short term cache, if available
    FieldDefinitions fieldDefs = (FieldDefinitions) getFromShortTermCache(object, FIELD_DEF_CACHE_NAME)
    if (fieldDefs) {
      return fieldDefs
    }

    // Start with the core fields
    fieldDefs = DomainUtils.instance.getFieldDefinitions(domainClass)

    // Now, add any field added by any modules (additions)
    for (addition in AdditionHelper.instance.additions) {
      for (field in addition.fields) {
        if (field.domainClass == domainClass) {
          def fieldDefinition = new AdditionFieldDefinition(field, addition.addition)
          log.debug('getEffectiveFieldDefinitions(): Adding {} to {}', fieldDefinition, domainClass)
          fieldDefs[field.name] = fieldDefinition
        }
      }
    }

    // Now, add any user-defined custom fields.
    FieldExtension.withTransaction {
      def fieldExtensions = FieldExtension.findAllByDomainClassName(domainClass.name, [cache: true])
      for (extension in fieldExtensions) {
        def fieldDefinition = new CustomFieldDefinition(extension)
        log.debug('getEffectiveFieldDefinitions(): Adding {} to {}', fieldDefinition, domainClass)
        fieldDefs[extension.fieldName] = fieldDefinition
      }
    }

    cacheShortTerm(object, FIELD_DEF_CACHE_NAME, fieldDefs)

    return fieldDefs
  }

  /**
   * Find the effective FieldDefinitions for the dynamic Configurable Type field values.
   * @param fieldDefinitions The list to add the fields to.  Will be cloned if changes are made, so the original
   *        list is unchanged.
   * @param domainClass The class to find the field order in.
   * @return A list of field names, in the display field order.
   */
  FieldDefinitions addConfigurableTypeFields(FieldDefinitions fieldDefinitions, Object domainObject) {
    //def cachedFieldDefs = getFromShortTermCache(domainObject)
    // Check the cache for all of the current Configurable Type values in the current domain.
    // The cache key are the hash codes for each CT value added.
    // This key makes sure we get the added fields for all of the current Configurable Type values.
    // Having more than one Configurable Type value in a domain is unusual, but this has code approach
    // should handle it without collision issues.

    Long cacheKey = 0
    for (fieldDefinition in fieldDefinitions) {
      if (fieldDefinition.format == ConfigurableTypeDomainFormat.instance) {
        def ctValue = (ConfigurableTypeInterface) domainObject[fieldDefinition.name]
        if (ctValue) {
          cacheKey = 31 * cacheKey + ctValue.hashCode()
        }
      }
    }
    if (cacheKey == 0) {
      // no Configurable Type fields, so do nothing.
      return fieldDefinitions
    }
    //println "cacheKey = $cacheKey"
    def cachedFieldDefs = getFromShortTermCache(domainObject, cacheKey)
    if (cachedFieldDefs) {
      return (FieldDefinitions) cachedFieldDefs
    }

    def list = []
    for (fieldDefinition in fieldDefinitions) {
      if (fieldDefinition.format == ConfigurableTypeDomainFormat.instance) {
        def ctValue = (ConfigurableTypeInterface) domainObject[fieldDefinition.name]
        if (ctValue) {
          def fieldsToAdd = ctValue.determineInputFields(fieldDefinition.name)
          for (fieldToAdd in fieldsToAdd) {
            list << fieldToAdd
          }
        }
      }
    }
    if (list) {
      fieldDefinitions = (FieldDefinitions) fieldDefinitions.clone()
      for (fieldToAdd in list) {
        //println "in list? ${fieldDefinitions[fieldToAdd.name]!=null} fieldToAdd = $fieldToAdd"
        fieldDefinitions << (FieldDefinitionInterface) fieldToAdd
      }
    }

    cacheShortTerm(domainObject, cacheKey, fieldDefinitions)

    return fieldDefinitions
  }

  /**
   * Determines the effective field order after applying the addition (extension) modules to the domain and then
   * the user's adjustments to the static fieldOrder from the domain class.
   * @param domainClass The domain class to determine the field order for.
   * @param fieldOrder The original field order to adjust (<b>Optional</b>).
   * @return The effective field order list.  This is a single flattened list of field names.
   */
  List<String> getEffectiveFieldOrder(Class domainClass, List<String> fieldOrder = null) {
    if (fieldOrder == null) {
      fieldOrder = DomainUtils.instance.getStaticFieldOrder(domainClass)
    }
    if (fieldOrder) {
      fieldOrder = (List<String>) fieldOrder.clone()
    }

    // Now, apply any field order changes added by any modules (additions)
    for (addition in AdditionHelper.instance.additions) {
      for (field in addition.fields) {
        if (field.domainClass == domainClass) {
          log.debug('getEffectiveFieldOrder(): Applying adjustments from {} to {}', field, domainClass)
          for (adj in field.fieldOrderAdjustments) {
            adj.apply(fieldOrder)
          }
        }
      }
    }

    // Now, apply the user's customizations to flattened list.  Will recursively check all super classes (if any).
    FieldGUIExtension.withTransaction {
      return FieldAdjuster.applyUserAdjustments(domainClass, fieldOrder)
    }
  }

  /**
   * Sets the value in a custom field holder.  Uses JSON as the internal format.
   * @param object The object to store the value in.
   * @param fieldName The name of the field.
   * @param value The value.
   * @param prefix The field prefix to use for the getter (e.g. prefix='rmaType' will use a prefix of 'rmaType_').
   */
  void setFieldValue(Object object, String fieldName, Object value, String prefix) {
    if (prefix) {
      fieldName = "${prefix}_$fieldName"
    }
    setFieldValue(object, fieldName, value)
  }

  /**
   * Sets the value in a custom field holder.  Uses JSON as the internal format.
   * @param object The object to store the value in.
   * @param fieldName The name of the field.
   * @param value The value.
   */
  void setFieldValue(Object object, String fieldName, Object value) {
    // Build a Map from the current values
    String holderName = getCustomHolderFieldName(object)
    def clazz = object.getClass()
    if (!holderName) {
      //error.131.message=The domain class {0} does not support extensible fields. Add @ExtensibleFields.
      throw new BusinessException(131, [clazz.name])
    }
    String text = object[holderName]

    // See if we need to process the value.
    def fieldDefinitions = addConfigurableTypeFields(getEffectiveFieldDefinitions(clazz, object), object)
    def fieldDefinition = fieldDefinitions[fieldName]

    if (fieldDefinition?.format instanceof ListFieldLoaderInterface) {
      // A list of children, so just set the list for later saving.
      def map = getComplexHolder(object)
      map[fieldName] = value
      log.trace('setFieldValue(): {} value = {} to object {}', fieldName, value, object)
    } else {
      // A simple field type, stored in the JSON text in the holder.
      def map
      if (text) {
        map = Holders.objectMapper.readValue(text, Map)
      } else {
        map = [:]
      }
      if (fieldDefinition) {
        value = fieldDefinition.format.convertToJsonFormat(value, fieldDefinition)
      }
      map[fieldName] = value
      // If space is tight, we can switch to the non-indented version.  Could use cached writer to avoid cost
      // of new writer on each setField() call
      //ObjectWriter w = Holders.objectMapper.writer()
      //object[ExtensibleFields.DEFAULT_FIELD_NAME] = w.with(SerializationFeature.INDENT_OUTPUT).writeValueAsString(map)
      // The default indenting is faster (25%).
      def s = Holders.objectMapper.writeValueAsString(map)
      if (s.size() > getFieldSize(object)) {
        def recordName = TypeUtils.toShortString(object)
        //error.130.message=Not enough room to store {0} in custom fields for {1} {2}.  Max size = {3}.
        throw new BusinessException(130, [fieldName, clazz.name, recordName, getFieldSize(object)])
      }
      object[holderName] = Holders.objectMapper.writeValueAsString(map)
      log.trace('setFieldValue(): {} value = {} to object {}', fieldName, value, object)
    }
  }

  /**
   * Gets the value in a custom field holder.  Uses JSON as the internal format.
   * @param object The object to get tha custom value from.
   * @param fieldName The name of the field.
   * @param prefix The field prefix to use for the getter call (e.g. prefix='rmaType' will use a prefix of 'rmaType_').
   * @return The value.
   */
  Object getFieldValue(Object object, String fieldName, String prefix) {
    if (prefix) {
      fieldName = "${prefix}_$fieldName"
    }
    return getFieldValue(object, fieldName)
  }

  /**
   * Gets the value in a custom field holder.  Uses JSON as the internal format.
   * @param object The object to get tha custom value from.
   * @param fieldName The name of the field.
   * @return The value.
   */
  Object getFieldValue(Object object, String fieldName) {
    String holderName = getCustomHolderFieldName(object)
    def clazz = object.getClass()
    if (!holderName) {
      //error.131.message=The domain class {0} does not support extensible fields. Add @ExtensibleFields.
      throw new BusinessException(131, [clazz.name])
    }
    String text = object[holderName]
    def fieldDefinitions = addConfigurableTypeFields(getEffectiveFieldDefinitions(clazz, object), object)
    def fieldDefinition = fieldDefinitions[fieldName]
    //if (!fieldDefinition && prefix) {
    // Not a top-level element, so check for a Configurable Type element
    //fieldDefinitions["${prefix}_${fieldName}"]
    //}
    //println "fieldDefinitions($fieldName) = $fieldDefinitions"
    if (fieldDefinition?.format instanceof ListFieldLoaderInterface) {
      // A list of children, so attempt to read values, if not already read.
      def map = getComplexHolder(object)
      def list = map[fieldName]
      ListFieldLoaderInterface format = (ListFieldLoaderInterface) fieldDefinition.format
      if (list == null) {
        // Not already in the object, so read it from DB.
        list = format.readList(object, fieldDefinition)
        map[fieldName] = list
        def idListName = '_IDs' + fieldName
        map[idListName] = list*.id
      }
      log.trace('getFieldValue(): {} List = {} from object {}', fieldName, list, object)
      return list
    } else {
      if (text) {
        // A simple field type, so try to convert from JSON.
        def map = Holders.objectMapper.readValue(text, Map)
        def value = map[fieldName]
        log.trace('getFieldValue(): {} value = {} from object {}', fieldName, value, object)
        //println "  format = ${fieldDefinition?.format}"
        // See if we need to convert the type.
        if (!fieldDefinition) {
          // Return as-is if the definition is now missing.
          return value
        }
        return fieldDefinition.format.convertFromJsonFormat(value, fieldDefinition)
      }
    }

    return null
  }

  /**
   * Gets the complex field holder.  Creates one if needed.
   * @param object The object to get the complex field holder from.
   * @return The map.  Never null.
   */
  Map getComplexHolder(Object object) {
    String holderName = ExtensibleFields.COMPLEX_CUSTOM_FIELD_NAME
    if (!object.hasProperty(holderName)) {
      //error.131.message=The domain class {0} does not support extensible fields. Add @ExtensibleFields.
      throw new BusinessException(131, [object.getClass().name])
    }
    Map map = (Map) object[holderName]
    if (map == null) {
      map = [:]
      object[holderName] = map
    }
    return map
  }


  /**
   * Gets the max size of the custom field value holder in the domain object.
   * @param object The object with the ExtensibleFields annotation.
   * @return The size.
   */
  Integer getFieldSize(Object object) {
    def annotation = object.getClass().getAnnotation(ExtensibleFields)
    if (annotation) {
      return annotation.maxSize()
    }
    return 0
  }

  /**
   * Gets the name of the custom field value holder in the domain object.
   * @param object The object or Class with the ExtensibleFields annotation.
   * @return The name.  Null means no custom fields.
   */
  String getCustomHolderFieldName(Object object) {
    def clazz = object.getClass()
    if (object instanceof Class) {
      clazz = object
    }
    def annotation = clazz.getAnnotation(ExtensibleFields)
    if (annotation) {
      return annotation.fieldName()
    }
    return null
  }


  /**
   * Returns true if the given class has extensible fields (@ExtensibleFields annotation).
   * @param clazz The class.
   * @return True if it has extensible fields.
   */
  boolean hasExtensibleFields(Class clazz) {
    return (clazz?.getAnnotation(ExtensibleFields) != null)
  }

  /**
   * Add an object to the short-term cache in the domain object.  This is a short-term cache (<1 second)
   * to avoid repeated generation of expensive objects.
   * If the domain does not support Extensible Fields, then this does nothing.
   * @param object The object to store the cache in.
   * @param cacheKey The key to store the value in the short term cache.
   * @param value The value to store.
   */
  protected void cacheShortTerm(Object object, Object cacheKey, Object value) {
    if (object == null) {
      return
    }
    def domainClass = object.getClass()
    if (object && hasExtensibleFields(domainClass)) {
      def holder = getComplexHolder(object)
      ShortTermCacheMap cache = (ShortTermCacheMap) holder[SHORT_TERM_CACHE_NAME]
      if (cache == null) {
        cache = new ShortTermCacheMap()
        holder[SHORT_TERM_CACHE_NAME] = cache
      }
      cache[cacheKey] = value
    }
  }
  /**
   * Gets the value from the short-term cache in the domain object.  This is a short-term cache (<1 second)
   * to avoid repeated generation of expensive objects.
   * If the domain does not support Extensible Fields, then this does nothing.
   *
   * @param object The object to store the cache in.
   * @param cacheKey The key to store the value in the short term cache.
   * @return The value from the cache.
   */
  protected Object getFromShortTermCache(Object object, Object cacheKey) {
    if (object == null) {
      return null
    }
    def domainClass = object.getClass()

    if (object && hasExtensibleFields(domainClass)) {
      def holder = getComplexHolder(object)
      Map map = (Map) holder[SHORT_TERM_CACHE_NAME]
      return map?.get(cacheKey)
    }

    return null
  }

  /**
   * Interceptor that handles object saves.  This will force the parent ID to be set for custom children lists.
   * @param entity The domain object being saved.
   */
  void onObjectSave(Object entity) {
    def clazz = entity.getClass()
    def fieldDefinitions = getEffectiveFieldDefinitions(clazz)
    for (fieldDefinition in fieldDefinitions) {
      if (fieldDefinition?.format instanceof ListFieldLoaderInterface) {
        def map = getComplexHolder(entity)
        def list = map[fieldDefinition.name]

        // Get the list of record IDs from the last read.
        def idListName = '_IDs' + fieldDefinition.name
        def idList = map[idListName]?.clone()

        // Now, try to save each record, with the proper parent ID.
        def recordDomainClass = fieldDefinition.referenceType
        String parentFieldName = NameUtils.lowercaseFirstLetter(clazz.simpleName) + 'Id'
        for (record in list) {
          if (record[parentFieldName] == null) {
            record[parentFieldName] = entity.id
          }
          record.save()
          log.trace("onObjectSave(): Saving child ({}) record (id={}) {}", recordDomainClass.simpleName, record.id, record)
          idList?.remove(record.id)
        }
        // Now, delete any records that are not in the current list (when com
        for (id in idList) {
          def record = recordDomainClass.load(id)
          record.delete()
          log.trace("onObjectSave(): Deleting child ({}) record (id={}) {}", recordDomainClass.simpleName, record.id, record)
        }
      }
    }

  }

  /**
   * Formats all of the Extensible Field name/value pairs for display.  Optionally highlights the field name
   * with HTML bold notation and limits the length to a given max length.
   * The @ExtensibleField annotation must be used on the domain object.
   * The output will be generated in the flex field sequence order for predictable display order.
   * <p>
   * This is useful for a column in the standard definition page's list.
   * <p>
   * <b>Note:</b> The first field will always be in the output, even if it is too large.
   *
   * @param ctFieldName The name of the field that is the Configurable Type selection field.
   * @param domainObject The domain object to retrieve the values from.
   * @param options The options for building the display string
   *                (see {@link TextUtils#addFieldForDisplay(java.lang.StringBuilder, java.lang.String, java.lang.String)}.
   * @return The display value.
   */
  String formatConfigurableTypeValues(String ctFieldName, Object domainObject, Map options = null) {
    def sb = new StringBuilder()

    FieldDefinitions fieldDefinitions = getEffectiveFieldDefinitions(domainObject.getClass(), domainObject)
    fieldDefinitions = addConfigurableTypeFields(fieldDefinitions, domainObject)
    log.debug('formatValuesForDisplay: fieldDefinitions = {}', fieldDefinitions)
    def keys = fieldDefinitions.keySet()?.sort() { fieldDefinitions[it]?.sequence }
    // Sort on sequence for predictable output order.
    for (key in keys) {
      def fieldDefinition = (FieldDefinitionInterface) fieldDefinitions[(String) key]
      if (fieldDefinition instanceof ConfigurableTypeFieldDefinition && fieldDefinition.configTypeFieldName == ctFieldName) {
        def label = fieldDefinition.label ?: key
        label = GlobalUtils.lookup(label)
        def value = fieldDefinition.getFieldValue(domainObject)
        def valueString = fieldDefinition.format.format(value, null, fieldDefinition)
        TextUtils.addFieldForDisplay(sb, label, valueString, options)
      }
    }

    return sb.toString()
  }
}
