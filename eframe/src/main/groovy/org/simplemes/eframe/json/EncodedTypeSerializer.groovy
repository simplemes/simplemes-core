package org.simplemes.eframe.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.simplemes.eframe.misc.NameUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A serializer for writing an encoded type field.  Writes just the ID string.
 */
class EncodedTypeSerializer extends JsonSerializer<Object> {

  String fieldName

  EncodedTypeSerializer(String fieldName) {
    this.fieldName = fieldName
  }

  /**
   * Serializes the enum using the toString() and adding a display value.
   * @param value
   * @param gen
   * @param provider
   */
  @Override
  void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException, JsonProcessingException {
    gen.writeString(value?.id?.toString())
    gen.writeStringField(NameUtils.buildDisplayFieldNameForJSON(fieldName), value?.toStringLocalized())
  }

}
