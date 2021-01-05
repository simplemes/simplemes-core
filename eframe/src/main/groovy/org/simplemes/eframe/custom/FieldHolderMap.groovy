/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom


import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.data.EncodedTypeInterface
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.data.SimpleFieldDefinition
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.ChildListFieldFormat
import org.simplemes.eframe.data.format.CustomChildListFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.FieldFormatInterface
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.eframe.misc.TypeUtils

/**
 * Defines the Map used to hide the ExtensibleFieldHolder extensions to the Map logic.
 * This includes serializing/deserializing to/from JSON, retaining data type info and tracking history of
 * values.  The main implementation is in the Groovy source tree.
 * <p>
 * This works with the {@link org.simplemes.eframe.data.annotation.ExtensibleFieldHolderTransformation}  and
 * {@link ExtensibleFieldHelper} classes to convert the Map to/from JSON upon use.
 * <p>
 * Extra elements are added to the map to retain the data type for values, the history and configuration data
 * in case the definitions change (e.g. FlexType changes).
 * <p>
 * This Groovy class is sub-classed from the {@link BaseFieldHolderMap} class because Groovy is cleaner.
 * A Java base-class is used so the {@link org.simplemes.eframe.data.annotation.ExtensibleFieldHolderTransformation}
 * can reference it in the Java source tree.
 */
@Slf4j
class FieldHolderMap extends BaseFieldHolderMap implements FieldHolderMapInterface {

  /**
   * The name of the top-level element in the map that holds the config.  Value: <b>'_config'</b>.
   */
  static final CONFIG_ELEMENT_NAME = "_config"

  /**
   * The name of the element in the '_config' map that holds the type ID (format encoding). Value: <b>'type'</b>.
   */
  static final TYPE_ELEMENT_NAME = "type"

  /**
   * The name of the element in the '_config' map that holds the valueClassName. Value: <b>'valueClassName'</b>.
   */
  static final VALUE_CLASS_ELEMENT_NAME = "valueClassName"

  /**
   * True if the Map has changed since the last time the JSON was generated (toJSON()).
   *
   */
  protected boolean dirty = false

  /**
   * True if the Map has changed since the last time the JSON was generated (toJSON()).
   * Defaults to true to work around issues with JSON parser.
   *
   */
  protected boolean parsingFromJSON = true

  /**
   * Parses the given JSON text into this Map.  Removes any existing elements.
   * @param text The JSON text.
   */
  static FieldHolderMapInterface fromJSON(String text) {
    def map = Holders.objectMapper.readValue(text, FieldHolderMap)
    map?.parsingFromJSON = false  // Finished mapping from JSON
    map?.dirty = false
    return map
  }

  /**
   * Serializes this Map to JSON.
   * @return The JSON.
   */
  @Override
  String toJSON() {
    return Holders.objectMapper.writeValueAsString(this)
  }

  /**
   * Returns the value to which the specified key is mapped,
   * or {@code null} if this map contains no mapping for the key.
   *
   * <p>More formally, if this map contains a mapping from a key
   * {@code k} to a value {@code v} such that {@code (key==null ?k==null :
   *key.equals(k) )}, then this method returns {@code v}; otherwise
   * it returns {@code null}.  (There can be at most one such mapping.)
   *
   * <p>A return value of {@code null} does not <i>necessarily</i>
   * indicate that the map contains no mapping for the key; it's also
   * possible that the map explicitly maps the key to {@code null}.
   * The {@link #containsKey containsKey} operation may be used to
   * distinguish these two cases.
   *
   * @see #put(Object, Object)
   */
  @Override
  Object get(Object key) {
    if ((Holders.environmentDev || Holders.environmentTest) && (key == 'dirty' || key == 'parsingFromJSON')) {
      // Flag probably error when trying to get the dirt/parsingFromJSON flags.
      log.warn("get(): You used a key '{}' to get the value from the Map.  Did you mean is{}()", key, NameUtils.uppercaseFirstLetter(key as String))
    }
    return convertToOriginalType(key, super.get(key))
  }

  /**
   * Converts the given value to the correct type, if it is a String and has a defined element type in the _config.
   * @param key The field name.
   * @param value The value.
   * @returns The (possibly) converted type.
   */
  protected Object convertToOriginalType(Object key, Object value) {
    if (value instanceof String) {
      def fieldDefinition = getOriginalFieldDefinition(key)
      if (fieldDefinition) {
        return fieldDefinition.format.convertFromJsonFormat(value, fieldDefinition)
      }
    } else if (value instanceof List) {
      def fieldDefinition = getOriginalFieldDefinition(key)
      if (fieldDefinition && fieldDefinition.format == CustomChildListFieldFormat.instance) {
        return fieldDefinition.format.parseJSONForCustomList(value, fieldDefinition)
      }
    }
    return value
  }

  /**
   * Gets the original field definition of the given field (if found) from the internal config map.
   * Only restores the format and type elements.
   * @param key The field name.
   * @returns The field definition.  Can be null.
   */
  protected FieldDefinitionInterface getOriginalFieldDefinition(Object key) {
    def fieldMap = getFieldMap(key as String, false)
    if (fieldMap) {
      def type = fieldMap[TYPE_ELEMENT_NAME] as String
      if (type) {
        def format = BasicFieldFormat.valueOf(type)
        if (format) {
          def s = fieldMap[VALUE_CLASS_ELEMENT_NAME] as String
          def valueType = null
          if (s) {
            valueType = TypeUtils.loadClass(s)
          }
          def fieldDefinition = new SimpleFieldDefinition(format: format, type: valueType)
          if (format == DomainReferenceFieldFormat.instance || format == CustomChildListFieldFormat.instance) {
            fieldDefinition.referenceType = valueType
          }
          return fieldDefinition
        }
      }
    }
    return null
  }


  /**
   * Associates the specified value with the specified key in this map.
   * If the map previously contained a mapping for the key, the old
   * value is replaced.
   *
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with {@code key}, or
   * {@code null} if there was no mapping for {@code key}.
   *         (A {@code null} return can also indicate that the map
   *         previously associated {@code null} with {@code key}.)
   */
  @Override
  Object put(Object key, Object value) {
    put(key, value, null)
  }

  /**
   * Puts the given value, with the given field definition.
   *
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @param fieldDefinition The field definition for this element.  Optional.
   * @return the previous value associated with {@code key}, or
   */
  Object put(Object key, Object value, FieldDefinitionInterface fieldDefinition) {
    if (fieldDefinition?.format == ChildListFieldFormat.instance) {
      throw new UnsupportedOperationException("This map does not support elements with the format ChildListFieldFormat.  See CustomChildListFieldFormat.")
    }
    def res = super.put(key, value)
    dirty = true
    if (!parsingFromJSON) {
      // Only set config type when not called from the JSON parser.
      setTypeInConfig(key, value, fieldDefinition)
      fixSpecialCasesForPut(key, value, fieldDefinition)
    }
    return res
  }

  /**
   * Fixes some special cases that need to be stored in non-standard format (e.g. Longs as Strings, Enums as the ID, etc).
   * @param key The field name.
   * @param value The value.
   * @param fieldDefinition The field definition for this element.  Optional.
   */
  protected void fixSpecialCasesForPut(Object key, Object value, FieldDefinitionInterface fieldDefinition) {
    if (value) {
      if (value instanceof Long) {
        // Force long to a string so we can restore the type later on get().
        super.put(key, value.toString())
      } else if (value instanceof EncodedTypeInterface) {
        // Just store the ID for encoded types.
        super.put(key, value.id)
      } else if (fieldDefinition?.format == DomainReferenceFieldFormat.instance) {
        // Just store the ID for encoded types.
        super.put(key, fieldDefinition.format.convertToJsonFormat(value, fieldDefinition))
      } else if (fieldDefinition?.format == CustomChildListFieldFormat.instance) {
        // Just store the ID for encoded types.
        super.put(key, fieldDefinition.format.formatCustomListForJSON(value, fieldDefinition))
      }
    }


  }

  /**
   * Records the type in the config element, if needed for JSON deserialization.
   * @param key The field name.
   * @param value The value.
   * @param fieldDefinition The field definition for this element.  Optional.
   */
  protected void setTypeInConfig(Object key, Object value, FieldDefinitionInterface fieldDefinition) {
    if (!value) {
      return
    }
    def format = BasicFieldFormat.findByType(value.class)
    if (!format) {
      if (fieldDefinition) {
        format = fieldDefinition.format
      }

      if (!format) {
        throw new IllegalArgumentException("Value type is not supported for field '$key'.  Value = '$value', class = '${value.getClass()}'.  You may need a fieldDefintion.")
      }
    }

    if (!isNativeJSONType(format)) {
      def fieldMap = getFieldMap(key as String, true)
      fieldMap[TYPE_ELEMENT_NAME] = format.id
      if (fieldDefinition) {
        fieldMap[VALUE_CLASS_ELEMENT_NAME] = fieldDefinition?.type?.name
      }
    }
  }

  /**
   * Determines if the given format is one of the native JSON types that is supported.
   * @param format The format.
   */
  protected boolean isNativeJSONType(FieldFormatInterface format) {
    if (format == StringFieldFormat.instance || format == BooleanFieldFormat.instance || format == BigDecimalFieldFormat.instance) {
      return true
    }
    return false
  }

  /**
   * Returns the master config Map from this map.  Will create the empty map if needed.
   * @return The map.
   */
  protected Map getConfigMap() {
    def configMap = super.get(CONFIG_ELEMENT_NAME) as Map
    if (configMap == null) {
      configMap = [:]
      super.put(CONFIG_ELEMENT_NAME, configMap)
    }
    return configMap
  }

  /**
   * Returns the field config Map for the given field.  This typically contains these elements:
   * <h3>Field Config Elements</h3>
   * <ul>
   *   <li><b>type</b> - The field type ({@link FieldFormatInterface} encoding). </li>
   *   <li><b>valueClassName</b> - The name of the class if the element is a UUID. </li>
   * </ul>
   *
   *
   * @param fieldName The key (field name).
   * @param createAsNeeded If true, then will create the field map.
   * @return The fields configuration map.
   */
  protected Map getFieldMap(String fieldName, boolean createAsNeeded) {
    def configMap = getConfigMap()
    def fieldMap = configMap[fieldName] as Map
    if (fieldMap == null && createAsNeeded) {
      fieldMap = [:]
      configMap[fieldName] = fieldMap
    }
    return fieldMap
  }


  boolean isDirty() {
    return dirty
  }

  void setDirty(boolean dirty) {
    this.dirty = dirty
  }

  boolean isParsingFromJSON() {
    return parsingFromJSON
  }

  void setParsingFromJSON(boolean parsingFromJSON) {
    this.parsingFromJSON = parsingFromJSON
  }
}
