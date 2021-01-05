/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.data.ConfigurableTypeInterface
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.data.FieldDefinitions
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.data.format.ConfigurableTypeDomainFormat
import org.simplemes.eframe.data.format.ListFieldLoaderInterface
import org.simplemes.eframe.domain.DomainBinder
import org.simplemes.eframe.misc.NameUtils

/**
 * A serializer for writing an encoded type field.  Writes just the ID string.
 */
class DeserializationProblemHandler extends com.fasterxml.jackson.databind.deser.DeserializationProblemHandler {

  /**
   * Method called when a JSON Object property with an unrecognized
   * name is encountered.
   * Content (supposedly) matching the property are accessible via
   * parser that can be obtained from passed deserialization context.
   * Handler can also choose to skip the content; if so, it MUST return
   * true to indicate it did handle property successfully.
   * Skipping is usually done like so:
   * <pre>
   *  parser.skipChildren();
   * </pre>
   * <p>
   * Note: {@link com.fasterxml.jackson.databind.DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES})
   * takes effect only <b>after</b> handler is called, and only
   * if handler did <b>not</b> handle the problem.
   *
   * @param ctxt
   * @param p Parser to use for handling problematic content
   *
   * @param deserializer @param beanOrClass Either bean instance being deserialized (if one
   *   has been instantiated so far); or Class that indicates type that
   *   will be instantiated (if no instantiation done yet: for example
   *   when bean uses non-default constructors)
   * @param propertyName @return True if the problem is resolved (and content available used or skipped);
   *  false if the handler did not anything and the problem is unresolved. Note that in
   *  latter case caller will either throw an exception or explicitly skip the content,
   *  depending on configuration.
   */
  @Override
  boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException {
    if (NameUtils.isDisplayFieldNameForJSON(propertyName)) {
      // Handle the case when a special field (e.g. _titleDisplay_ ) is in the JSON.
      // This is a common mechanism to pass the display value for combo-boxes and similar GUI elements.
      // These frequently get passed back under some conditions (e.g. updates), so we ignore them.
      return true
    }

    if (propertyName == ExtensibleFieldHolder.COMPLEX_CUSTOM_FIELD_NAME) {
      // The complex field holder will be in all extensible objects, so ignore it.  It is always null.
      return true
    }

    // Check for a custom field
    def fieldDefinitions = ExtensibleFieldHelper.instance.getEffectiveFieldDefinitions(beanOrClass.class)
    def fieldDefinition = fieldDefinitions[propertyName]
    if (fieldDefinition) {
      if (fieldDefinition.format instanceof ListFieldLoaderInterface) {
        JsonNode node = p.getCodec().readTree(p)
        deserializeCustomList(beanOrClass, node, fieldDefinition)
        return true
      } else {
        // Let the field field format convert the value to be saved
        def value = fieldDefinition.format.convertFromJsonFormat(p.getText(), fieldDefinition)
        //println "value ($fieldDefinition.name,$fieldDefinition.format) = ${p.currentName()} $value $p ${p.getText()}"
        fieldDefinition.setFieldValue(beanOrClass, value)
        return true
      }
    } else {
      // See if the property is a configurable type field name
      return deserializeConfigurableTypes(propertyName, fieldDefinitions, beanOrClass, p)
    }
  }

  /**
   * Will de-serialize a field from a configurable type.
   * @param propertyName The propertyName
   * @param fieldDefinitions The object's field definitions.
   * @param beanOrClass The object to deserialize into.
   * @param p The parser.
   * @return True if the field is part of a configurable type and the value was parsed into the object.
   */
  boolean deserializeConfigurableTypes(String propertyName, FieldDefinitions fieldDefinitions, Object beanOrClass, JsonParser p) {
    for (fieldDefinition in fieldDefinitions) {
      if (fieldDefinition.format == ConfigurableTypeDomainFormat.instance) {
        def configType = beanOrClass[fieldDefinition.name]
        if (configType instanceof ConfigurableTypeInterface) {
          def configFields = configType.determineInputFields(fieldDefinition.name)
          for (configField in configFields) {
            if (configField.name == propertyName) {
              def jsonValue = configField.format.convertFromJsonFormat(p.getText(), configField)
              //println "no field found. propertyName = $propertyName for $jsonValue ${jsonValue.getClass()}"
              ExtensibleFieldHelper.instance.setFieldValue(beanOrClass, propertyName, jsonValue)
              return true
            }
          }
        }
      }
    }
    return false

  }

  /**
   * Deserialize the given custom child list for the given bean node.  Stores the values in the complex custom field holder.
   * @param beanOrClass The object to deserialize the results into.
   * @param node The JSON input node for the array of child records.
   * @param fieldDefinition The field to deserialize into.
   */
  protected void deserializeCustomList(Object beanOrClass, JsonNode node, FieldDefinitionInterface fieldDefinition) {
    // Build the params data for the binder for the custom child list
    def list = []
    for (Iterator<JsonNode> rowIter = node.elements(); rowIter.hasNext();) {
      JsonNode rowNode = rowIter.next()
      def row = [:]
      for (Iterator paramIter = rowNode.fields(); paramIter.hasNext();) {
        Map.Entry entry = paramIter.next()
        row[entry.key] = convertNode(entry.key, entry.value)
        //println "$entry.key = ${row[entry.key]}"
      }
      list << row
    }
    def params = [:]
    params[fieldDefinition.name] = list
    DomainBinder.build().bind(beanOrClass, params)
  }

  /**
   * Converts the given node to text or a Map as needed.
   * @param node The node.
   * @return The text or map.
   */
  @SuppressWarnings("unused")
  protected Object convertNode(Object key, Object node) {
    if (node instanceof ObjectNode) {
      def map = [:]
      for (element in node.fields()) {
        map[element.key] = element.value.asText()
      }
      return map
    } else {
      return node.asText()
    }
  }

}
