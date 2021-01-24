/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.simplemes.eframe.custom.FieldHolderMap
import org.simplemes.eframe.custom.FieldHolderMapInterface

/**
 * A serializer for writing a The FieldHolderMap to JSON output.  Supports ability to alter the field name
 * to allow the search engine to index the custom fields.
 */
class FieldHolderMapSerializer extends StdSerializer<FieldHolderMapInterface> {

  String fieldName

  String fieldNameWithoutUnderscore

  String configNameWithoutUnderscore

  FieldHolderMapSerializer(Class<FieldHolderMapInterface> t, String fieldName) {
    super(t)
    this.fieldName = fieldName
    this.fieldNameWithoutUnderscore = fieldName
    if (fieldName.startsWith('_')) {
      fieldNameWithoutUnderscore = fieldName[1..-1]
    }
    configNameWithoutUnderscore = FieldHolderMap.CONFIG_ELEMENT_NAME[1..-1]
  }

  @Override
  void serialize(FieldHolderMapInterface value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    def useUnderscoresInJson = value.isUseUnderscoresInJson()
    def originalParsingFromJson = value.isParsingFromJSON()
    if (!useUnderscoresInJson) {
      // Close out the original field with the underscore and then start a new object with the right field name (no underscore).
      gen.writeNull()
      gen.writeFieldName(fieldNameWithoutUnderscore)

      // Convert the internal '_config' to the new name 'config'.
      value.setParsingFromJSON(true)  // Prevent special processing on put() calls.
      value[configNameWithoutUnderscore] = value[FieldHolderMap.CONFIG_ELEMENT_NAME]
      value.remove(FieldHolderMap.CONFIG_ELEMENT_NAME)
    }

    gen.writeStartObject()
    value.each { k, v ->
      gen.writeObjectField((String) k, v)
    }
    gen.writeEndObject()
    if (!useUnderscoresInJson) {
      value.setUseUnderscoresInJson(true)
      // Convert the internal 'config' back to the original '_config'.
      value[FieldHolderMap.CONFIG_ELEMENT_NAME] = value[configNameWithoutUnderscore]
      value.remove(configNameWithoutUnderscore)
      value.setParsingFromJSON(originalParsingFromJson)  // Restore the setting on the parsing flag.
    }
  }

}
